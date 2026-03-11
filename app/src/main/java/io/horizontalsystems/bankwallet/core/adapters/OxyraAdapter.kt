package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BackgroundManager
import io.horizontalsystems.bankwallet.core.BackgroundManagerState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ISendOxyraAdapter
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.OxyraNodeManager.OxyraNode
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.oxyrakit.Balance
import io.horizontalsystems.oxyrakit.OxyraKit
import io.horizontalsystems.oxyrakit.Seed
import io.horizontalsystems.oxyrakit.SyncState
import io.horizontalsystems.oxyrakit.data.Subaddress
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import android.util.Log
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val TAG = "OxyraAdapter"

class OxyraAdapter(
    private val kit: OxyraKit,
    private val transactionsProvider: OxyraTransactionsProvider,
    private val transactionsAdapter: OxyraTransactionsAdapter,
    private val backgroundManager: BackgroundManager,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendOxyraAdapter, ITransactionsAdapter by transactionsAdapter {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private var balance = io.horizontalsystems.oxyrakit.Balance(0, 0)

    override var balanceState: AdapterState = kit.syncStateFlow.value.toAdapterState()

    override val balanceData: BalanceData
        get() = balance.toBalanceData()

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val receiveAddress: String
        get() = kit.receiveAddress

    override val isMainNet: Boolean
        get() = true

    override fun start() {
        kit.balanceFlow.collectWith(coroutineScope) {
            balance = it

            balanceUpdatedSubject.onNext(Unit)
        }

        kit.syncStateFlow.collectWith(coroutineScope) {
            Log.d(TAG, "syncState changed: $it")
            balanceState = it.toAdapterState()

            balanceStateUpdatedSubject.onNext(Unit)
        }

        kit.allTransactionsFlow.collectWith(coroutineScope, transactionsProvider::onTransactions)

        coroutineScope.launch {
            Log.d(TAG, "start() calling kit.start()")
            try {
                kit.start()
                Log.d(TAG, "start() kit.start() completed")
            } catch (e: Exception) {
                Log.e(TAG, "start() kit.start() failed", e)
            }
        }

        coroutineScope.launch {
            backgroundManager.stateFlow.collect {
                if (it == BackgroundManagerState.EnterBackground) {
                    kit.saveState()
                }
            }
        }
    }

    override fun stop() {
        val job = coroutineScope.launch {
            kit.saveState()
            kit.stop()
        }

        job.invokeOnCompletion {
            coroutineScope.cancel()
        }
    }

    override fun refresh() {
        if (kit.syncStateFlow.value is SyncState.NotSynced) {
            coroutineScope.launch {
                kit.stop()
                kit.start()
            }
        }
    }

    override val debugInfo: String
        get() = ""

    override suspend fun send(amount: BigDecimal, address: String, memo: String?) {
        val amountInAtomicUnits = amount.movePointRight(DECIMALS).toLong()
        kit.send(amountInAtomicUnits, address, memo)
    }

    override suspend fun estimateFee(
        amount: BigDecimal,
        address: String,
        memo: String?
    ): BigDecimal {
        val amountInAtomicUnits = amount.movePointRight(DECIMALS).toLong()
        return kit.estimateFee(amountInAtomicUnits, address, memo).scaledDown(DECIMALS)
    }

    fun getSubaddresses(): List<Subaddress> {
        return kit.getSubaddresses()
    }

    val statusInfo: Map<String, Any>
        get() = kit.statusInfo()

    companion object {
        const val DECIMALS = 8

        fun create(
            context: Context,
            wallet: Wallet,
            restoreSettings: RestoreSettings,
            node: OxyraNode
        ): OxyraAdapter {
            val birthdayHeightStr: String?
            val seed: Seed
            when (val accountType = wallet.account.type) {
                is AccountType.Mnemonic -> {
                    birthdayHeightStr = restoreSettings.birthdayHeight?.toString()
                    seed = accountType.toOxyraSeed()
                }

                else -> throw IllegalStateException("Unsupported account type: ${wallet.account.type.javaClass.simpleName}")
            }

            val birthdayHeightOrDate: String = when (wallet.account.origin) {
                AccountOrigin.Created -> {
                    birthdayHeightStr ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                }

                AccountOrigin.Restored -> {
                    birthdayHeightStr ?: "0"
                }
            }

            Log.d(TAG, "create() node=${node.serialized} trusted=${node.trusted} birthday=$birthdayHeightOrDate accountId=${wallet.account.id}")

            val kit = OxyraKit.getInstance(
                context,
                seed,
                birthdayHeightOrDate,
                wallet.account.id,
                node.serialized,
                node.trusted
            )

            val transactionsProvider = OxyraTransactionsProvider()
            val transactionsAdapter = OxyraTransactionsAdapter(kit, transactionsProvider, wallet)

            return OxyraAdapter(
                kit,
                transactionsProvider,
                transactionsAdapter,
                App.backgroundManager
            )
        }

        fun clear(walletId: String) {
            OxyraKit.deleteWallet(App.instance, walletId)
        }
    }
}

fun SyncState.toAdapterState(): AdapterState = when (this) {
    is SyncState.NotSynced -> {
        if (error is OxyraKit.SyncError.NotStarted) {
            AdapterState.Connecting
        } else {
            AdapterState.NotSynced(error)
        }
    }
    is SyncState.Synced -> AdapterState.Synced
    is SyncState.Connecting -> AdapterState.Connecting
    is SyncState.Syncing -> AdapterState.Syncing(
        progress = progress?.let {
            (it * 100).roundToInt().coerceAtMost(100)
        },
        blocksRemained = remainingBlocks
    )
}

fun AccountType.toOxyraSeed() = when (this) {
    is AccountType.Mnemonic -> {
        if (words.size == 25) {
            // 25-word Oxyra/Monero native seed — use directly
            Seed.Electrum(words, "")
        } else {
            // BIP39 seed — convert to Monero legacy via CakeWalletStyleConverter
            Seed.Bip39(words, passphrase)
        }
    }
    else -> throw IllegalArgumentException("Account type ${this.javaClass.simpleName} can not be converted to Oxyra Seed")
}

fun io.horizontalsystems.oxyrakit.Balance.toBalanceData(): BalanceData {
    val available = unlocked.scaledDown(OxyraAdapter.DECIMALS)
    val pending = (all - unlocked).coerceAtLeast(0).scaledDown(OxyraAdapter.DECIMALS)
    return BalanceData(available, pending = pending)
}

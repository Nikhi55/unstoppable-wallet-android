package io.horizontalsystems.bankwallet.modules.blockchainsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.imageResource
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class BlockchainSettingsViewModel(
    private val service: BlockchainSettingsService
) : ViewModel() {

    var btcLikeChains by mutableStateOf<List<BlockchainSettingsModule.BlockchainViewItem>>(listOf())
        private set

    var otherChains by mutableStateOf<List<BlockchainSettingsModule.BlockchainViewItem>>(listOf())
        private set

    init {
        viewModelScope.launch {
            service.blockchainItemsObservable.asFlow().collect {
                sync(it)
            }
        }

        service.start()
        sync(service.blockchainItems)
    }

    override fun onCleared() {
        service.stop()
    }

    private fun sync(blockchainItems: List<BlockchainSettingsModule.BlockchainItem>) {
        viewModelScope.launch {
            val btcItems = blockchainItems
                .filterIsInstance<BlockchainSettingsModule.BlockchainItem.Btc>()
                .map { item ->
                    BlockchainSettingsModule.BlockchainViewItem(
                        title = item.blockchain.name,
                        subtitle = Translator.getString(item.restoreMode.title),
                        imageSource = item.blockchain.type.imageResource?.let { ImageSource.Local(it) }
                            ?: ImageSource.Remote(item.blockchain.type.imageUrl, R.drawable.ic_platform_placeholder_32),
                        blockchainItem = item
                    )
                }
            val moneroItems = blockchainItems
                .filterIsInstance<BlockchainSettingsModule.BlockchainItem.Monero>()
                .map { item ->
                    BlockchainSettingsModule.BlockchainViewItem(
                        title = item.blockchain.name,
                        subtitle = item.node.name,
                        imageSource = item.blockchain.type.imageResource?.let { ImageSource.Local(it) }
                            ?: ImageSource.Remote(item.blockchain.type.imageUrl, R.drawable.ic_platform_placeholder_32),
                        blockchainItem = item
                    )
                }
            val oxyraItems = blockchainItems
                .filterIsInstance<BlockchainSettingsModule.BlockchainItem.Oxyra>()
                .map { item ->
                    BlockchainSettingsModule.BlockchainViewItem(
                        title = item.blockchain.name,
                        subtitle = item.node.name,
                        imageSource = item.blockchain.type.imageResource?.let { ImageSource.Local(it) }
                            ?: ImageSource.Remote(item.blockchain.type.imageUrl, R.drawable.ic_platform_placeholder_32),
                        blockchainItem = item
                    )
                }
            btcLikeChains = (btcItems + moneroItems + oxyraItems).sortedBy { it.blockchainItem.blockchain.type.order }

            otherChains = blockchainItems
                .filterNot { it is BlockchainSettingsModule.BlockchainItem.Btc }
                .mapNotNull { item ->
                    when (item) {
                        is BlockchainSettingsModule.BlockchainItem.Evm -> BlockchainSettingsModule.BlockchainViewItem(
                            title = item.blockchain.name,
                            subtitle = item.syncSource.name,
                            imageSource = item.blockchain.type.imageResource?.let { ImageSource.Local(it) }
                            ?: ImageSource.Remote(item.blockchain.type.imageUrl, R.drawable.ic_platform_placeholder_32),
                            blockchainItem = item
                        )
                        is BlockchainSettingsModule.BlockchainItem.Solana -> BlockchainSettingsModule.BlockchainViewItem(
                            title = item.blockchain.name,
                            subtitle = item.rpcSource.name,
                            imageSource = item.blockchain.type.imageResource?.let { ImageSource.Local(it) }
                            ?: ImageSource.Remote(item.blockchain.type.imageUrl, R.drawable.ic_platform_placeholder_32),
                            blockchainItem = item
                        )
                        else -> null
                    }
                }
        }
    }

}

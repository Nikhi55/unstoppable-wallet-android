package io.horizontalsystems.bankwallet.modules.manageaccount.showoxyrakey

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.adapters.toOxyraSeed
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.oxyrakit.OxyraKit
import kotlinx.parcelize.Parcelize

object ShowOxyraKeyModule {

    @Parcelize
    class OxyraKeys(
        val spendKey: String,
        val viewKey: String,
        val isPrivate: Boolean,
        val title: Int = if (isPrivate) R.string.OxyraKeyType_Private else R.string.OxyraKeyType_Public
    ) : Parcelable {

        fun getKey(type: OxyraKeyType) = when (type) {
            OxyraKeyType.Spend -> spendKey
            OxyraKeyType.View -> viewKey
        }

    }

    enum class OxyraKeyType(val title: Int) {
        Spend(R.string.OxyraKeyType_Spend),
        View(R.string.OxyraKeyType_View)
    }

    fun getPrivateOxyraKeys(account: Account) = getOxyraKeys(account, true)

    fun getPublicOxyraKeys(account: Account) = getOxyraKeys(account, false)

    private fun getOxyraKeys(account: Account, isPrivate: Boolean): OxyraKeys? = try {
        val oxyraSeed = account.type.toOxyraSeed()
        val keys = OxyraKit.getKeys(oxyraSeed)
        if (isPrivate) {
            OxyraKeys(keys.privateSpendKey, keys.privateViewKey, true)
        } else {
            OxyraKeys(keys.publicSpendKey, keys.publicViewKey, false)
        }
    } catch (_: Throwable) {
        null
    }

}

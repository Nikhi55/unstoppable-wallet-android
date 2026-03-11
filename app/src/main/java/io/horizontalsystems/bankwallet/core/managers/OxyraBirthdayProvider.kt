package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.oxyrakit.OxyraKit

class OxyraBirthdayProvider {
    fun restoreHeightForNewWallet(): Long {
        return OxyraKit.restoreHeightForNewWallet()
    }
}

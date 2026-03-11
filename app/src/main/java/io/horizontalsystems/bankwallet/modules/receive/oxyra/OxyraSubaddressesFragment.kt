package io.horizontalsystems.bankwallet.modules.receive.oxyra

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment

class OxyraSubaddressesFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<SubaddressesParams>(navController) {
            OxyraSubaddressesScreen(it) { navController.popBackStack() }
        }
    }
}

package io.horizontalsystems.bankwallet.modules.oxyranetwork.addnode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.core.managers.OxyraNodeManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import java.net.MalformedURLException
import java.net.URI

class AddOxyraNodeViewModel(
    private val nodeManager: OxyraNodeManager
) : ViewModel() {

    private var url = ""
    private var username: String? = null
    private var password: String? = null
    private var urlCaution: Caution? = null

    var viewState by mutableStateOf(AddOxyraNodeViewState(null))
        private set

    fun onEnterUsername(username: String) {
        this.username = username.trim()
    }

    fun onEnterPassword(password: String) {
        this.password = password
    }

    fun onEnterRpcUrl(enteredUrl: String) {
        urlCaution = null
        url = enteredUrl.trim()
        syncState()
    }

    fun onScreenClose() {
        viewState = AddOxyraNodeViewState()
    }

    fun onAddClick() {
        val sourceUri: URI

        try {
            sourceUri = URI(url)
            val hasRequiredProtocol = listOf("https").contains(sourceUri.scheme)
            if (!hasRequiredProtocol) {
                throw MalformedURLException()
            }
        } catch (_: Throwable) {
            urlCaution = Caution(Translator.getString(R.string.AddMoneroNode_Error_InvalidUrl), Caution.Type.Error)
            syncState()
            return
        }

        if (nodeManager.allNodes.any { it.host == url }) {
            urlCaution = Caution(Translator.getString(R.string.AddMoneroNode_Warning_UrlExists), Caution.Type.Warning)
            syncState()
            return
        }

        nodeManager.addOxyraNode(url, username, password, true)

        viewState = AddOxyraNodeViewState(null, true)
    }

    private fun syncState() {
        viewState = AddOxyraNodeViewState(urlCaution)
    }
}

data class AddOxyraNodeViewState(
    val urlCaution: Caution? = null,
    val closeScreen: Boolean = false
)

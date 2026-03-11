package io.horizontalsystems.bankwallet.modules.oxyranetwork

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.managers.OxyraNodeManager
import io.horizontalsystems.bankwallet.core.managers.OxyraNodeManager.OxyraNode
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class OxyraNetworkViewModel(
    private val oxyraNodeManager: OxyraNodeManager
) : ViewModel() {

    private val currentNode get() = oxyraNodeManager.currentNode

    var viewState by mutableStateOf(ViewState(emptyList(), emptyList()))
        private set

    val title = "Oxyra"

    init {
        oxyraNodeManager.nodesUpdatedFlow
            .onEach { syncState() }
            .launchIn(viewModelScope)

        syncState()
    }

    private fun syncState() {
        viewState = ViewState(
            defaultItems = viewItems(oxyraNodeManager.defaultNodes),
            customItems = viewItems(oxyraNodeManager.customNodes)
        )
    }

    private fun viewItems(nodes: List<OxyraNode>): List<ViewItem> {
        val selectedNode = currentNode

        return nodes.map { node ->
            ViewItem(
                node = node,
                id = node.host,
                name = node.name,
                url = node.host,
                selected = node == selectedNode
            )
        }
    }

    fun onSelectNode(node: OxyraNode) {
        if (currentNode == node) return
        oxyraNodeManager.save(node)
        syncState()
    }

    fun onRemoveCustomNode(node: OxyraNode) {
        oxyraNodeManager.delete(node)
    }

    data class ViewItem(
        val node: OxyraNode,
        val id: String,
        val name: String,
        val url: String,
        val selected: Boolean,
    )

    data class ViewState(
        val defaultItems: List<ViewItem>,
        val customItems: List<ViewItem>,
    )
}

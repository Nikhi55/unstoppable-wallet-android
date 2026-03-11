package io.horizontalsystems.bankwallet.modules.oxyranetwork

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePopup
import io.horizontalsystems.bankwallet.core.managers.OxyraNodeManager.OxyraNode
import io.horizontalsystems.bankwallet.modules.btcblockchainsettings.BlockchainSettingCell
import io.horizontalsystems.bankwallet.modules.oxyranetwork.addnode.AddOxyraNodeScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.ActionsRow
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.DraggableCardSimple
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.getShape
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.showDivider
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

class OxyraNetworkFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        OxyraNetworkNavHost(navController)
    }

}

private const val OxyraNetworkPage = "oxyra_network"
private const val AddNodePage = "add_node"

@Composable
private fun OxyraNetworkNavHost(
    fragmentNavController: NavController
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = OxyraNetworkPage,
    ) {
        composable(OxyraNetworkPage) {
            OxyraNetworkScreen(
                navController = navController,
                onBackPress = { fragmentNavController.popBackStack() },
            )
        }
        composablePopup(AddNodePage) {
            AddOxyraNodeScreen(
                navController = navController
            )
        }
    }
}

@Composable
private fun OxyraNetworkScreen(
    navController: NavController,
    onBackPress: () -> Unit,
) {
    val viewModel = viewModel<OxyraNetworkViewModel>(factory = OxyraNetworkModule.Factory())
    var revealedCardId by remember { mutableStateOf<String?>(null) }
    val view = LocalView.current
    val skipHalfExpanded by remember { mutableStateOf(true) }
    val modalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = skipHalfExpanded
    )
    val coroutineScope = rememberCoroutineScope()
    var selectedNode by remember { mutableStateOf<OxyraNode?>(null) }

    fun showTrustedSettings(node: OxyraNode) {
        selectedNode = node
        coroutineScope.launch { modalBottomSheetState.show() }
    }

    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            selectedNode?.let { node ->
                OxyraNodeTrustBottomSheet(
                    node,
                    onDone = { checked ->
                        viewModel.onSelectNode(node.copy(trusted = checked))
                        coroutineScope.launch { modalBottomSheetState.hide() }
                    },
                    onCloseClick = {
                        viewModel.onSelectNode(node)
                        coroutineScope.launch { modalBottomSheetState.hide() }
                    }
                )
            }
        }

    ) {
        Surface(color = ComposeAppTheme.colors.tyler) {
            Column {
                AppBar(
                    title = viewModel.title,
                    navigationIcon = {
                        Image(
                            painter = painterResource(R.drawable.oxyra_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 14.dp)
                                .size(24.dp)
                        )
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = {
                                onBackPress.invoke()
                            }
                        )
                    )
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                ) {

                    item {
                        VSpacer(12.dp)
                        subhead2_grey(
                            modifier = Modifier.padding(horizontal = 32.dp),
                            text = stringResource(R.string.MoneroNodeSettings_Description)
                        )
                        VSpacer(32.dp)
                    }

                    item {
                        CellUniversalLawrenceSection(viewModel.viewState.defaultItems) { item ->
                            BlockchainSettingCell(item.name, item.url, item.selected, null) {
                                showTrustedSettings(item.node)
                            }
                        }
                    }

                    if (viewModel.viewState.customItems.isNotEmpty()) {
                        customNodeListSection(
                            viewModel.viewState.customItems,
                            revealedCardId,
                            onClick = { node ->
                                showTrustedSettings(node)
                            },
                            onReveal = { id ->
                                if (revealedCardId != id) {
                                    revealedCardId = id
                                }
                            },
                            onConceal = {
                                revealedCardId = null
                            }
                        ) {
                            viewModel.onRemoveCustomNode(it)
                            HudHelper.showErrorMessage(view, R.string.Hud_Removed)

                        }
                    }

                    item {
                        Spacer(Modifier.height(32.dp))
                        AddButton {
                            navController.navigate(AddNodePage)
                        }
                        Spacer(Modifier.height(60.dp))
                    }
                }
            }
        }
    }
}

private fun LazyListScope.customNodeListSection(
    items: List<OxyraNetworkViewModel.ViewItem>,
    revealedCardId: String?,
    onClick: (OxyraNode) -> Unit,
    onReveal: (String) -> Unit,
    onConceal: () -> Unit,
    onDelete: (OxyraNode) -> Unit
) {
    item {
        Spacer(Modifier.height(32.dp))
        HeaderText(
            stringResource(R.string.EvmNetwork_Added),
        )
    }
    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
        val showDivider = showDivider(items.size, index)
        val shape = getShape(items.size, index)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ActionsRow(
                content = {
                    HsIconButton(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(88.dp),
                        onClick = { onDelete(item.node) },
                        content = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_circle_minus_24),
                                tint = ComposeAppTheme.colors.grey,
                                contentDescription = "delete",
                            )
                        }
                    )
                },
            )
            DraggableCardSimple(
                key = item.id,
                isRevealed = revealedCardId == item.id,
                cardOffset = 72f,
                onReveal = { onReveal(item.id) },
                onConceal = onConceal,
                content = {
                    OxyraRpcCell(
                        shape = shape,
                        showDivider = showDivider,
                        item = item,
                        onItemClick = onClick
                    )
                }
            )
        }
    }
}

@Composable
private fun AddButton(
    onClick: () -> Unit
) {
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                onClick = onClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    modifier = Modifier.size(24.dp),
                    tint = ComposeAppTheme.colors.jacob,
                    contentDescription = null
                )
                Spacer(Modifier.width(16.dp))
                body_jacob(
                    text = stringResource(R.string.EvmNetwork_AddNew)
                )
            }
        }
    )
}

@Composable
fun OxyraRpcCell(
    shape: Shape,
    showDivider: Boolean = false,
    item: OxyraNetworkViewModel.ViewItem,
    onItemClick: (OxyraNode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(ComposeAppTheme.colors.lawrence)
            .clickable {
                onItemClick.invoke(item.node)
            },
        contentAlignment = Alignment.Center
    ) {
        if (showDivider) {
            HsDivider(modifier = Modifier.align(Alignment.TopCenter))
        }
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val title = when {
                    item.name.isNotBlank() -> item.name
                    else -> stringResource(id = R.string.WalletConnect_Unnamed)
                }

                headline2_leah(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subhead2_grey(text = item.url)
            }
            if (item.selected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_checkmark_20),
                    tint = ComposeAppTheme.colors.jacob,
                    contentDescription = null
                )
            }
        }
    }
}

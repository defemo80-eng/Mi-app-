package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.catalog.StoreCatalog
import com.example.domain.model.StoreCategory
import com.example.domain.model.StoreItem
import com.example.ui.avatar3d.Avatar3DView
import com.example.ui.components.CodBalanceChip
import com.example.ui.components.StoreItemArtwork
import com.example.ui.theme.CodGold
import com.example.ui.theme.CodGoldContainer
import com.example.ui.theme.DeFittBlack
import com.example.ui.theme.DeFittOutline
import com.example.ui.theme.DeFittPureBlack
import com.example.ui.theme.DeFittSurface
import com.example.ui.theme.DeFittSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.DeFittMainState

@Composable
fun TiendaScreen(
    state: DeFittMainState,
    previewItemId: String?,
    contentPadding: PaddingValues,
    onPreviewItem: (String?) -> Unit,
    onBuyItem: (StoreItem) -> Unit,
    onToggleEquip: (StoreItem) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0=Tienda, 1=Mi Armario
    var selectedCategory by remember { mutableStateOf<StoreCategory?>(null) }

    val ownedIds = remember(state.purchases) { state.purchases.map { it.itemId }.toSet() }
    val equippedIds = remember(state.equipped) { state.equipped.map { it.itemId } }

    // Live 3D Try-on Preview
    val displayEquippedIds = remember(equippedIds, previewItemId) {
        val previewItem = previewItemId?.let { StoreCatalog.getById(it) }
        if (previewItem == null) {
            equippedIds
        } else {
            val filtered = equippedIds.filter { id ->
                StoreCatalog.getById(id)?.categoryName != previewItem.categoryName
            }
            filtered + previewItem.id
        }
    }

    val visibleItems = remember(activeTab, selectedCategory, ownedIds) {
        val base = if (activeTab == 0) {
            StoreCatalog.allItems
        } else {
            StoreCatalog.allItems.filter { ownedIds.contains(it.id) }
        }
        if (selectedCategory == null) base else base.filter { it.category == selectedCategory }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DeFittBlack)
            .padding(contentPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        val stageHeight = (maxHeight * 0.30f).coerceIn(155.dp, 240.dp)
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tienda & Armario", style = MaterialTheme.typography.headlineMedium, color = TextWhite)
                    Text(
                        "${StoreCatalog.allItems.size} artículos locales • ${ownedIds.size} en tu armario",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                CodBalanceChip(balance = state.activeUser?.codBalance ?: 0L)
            }

            // Live 3D Try-on Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stageHeight)
            ) {
                Avatar3DView(
                    avatarParams = state.avatarParams,
                    equippedItemIds = displayEquippedIds,
                    modifier = Modifier.fillMaxSize(),
                    showAngleControls = true,
                    testTag = "store_avatar_3d"
                )
                if (previewItemId != null) {
                    val pItem = StoreCatalog.getById(previewItemId)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeFittSurfaceElevated.copy(alpha = 0.92f))
                            .border(1.dp, CodGold, RoundedCornerShape(12.dp))
                            .clickable { onPreviewItem(null) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Probando: ${pItem?.name ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CodGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.Close, "Quitar vista previa", tint = CodGold, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Tab Switcher: Tienda vs Mi Armario
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    leadingIcon = { Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Tienda (${StoreCatalog.allItems.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGreen,
                        selectedLabelColor = DeFittPureBlack,
                        containerColor = DeFittSurface,
                        labelColor = TextWhite
                    ),
                    modifier = Modifier.weight(1f).testTag("store_tab_shop")
                )
                FilterChip(
                    selected = activeTab == 1,
                    onClick = {
                        activeTab = 1
                        onPreviewItem(null)
                    },
                    leadingIcon = { Icon(Icons.Default.Checkroom, null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Mi Armario (${ownedIds.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGreen,
                        selectedLabelColor = DeFittPureBlack,
                        containerColor = DeFittSurface,
                        labelColor = TextWhite
                    ),
                    modifier = Modifier.weight(1f).testTag("store_tab_wardrobe")
                )
            }

            // 11 Category Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Todas") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGreen,
                        selectedLabelColor = DeFittPureBlack,
                        containerColor = DeFittSurface,
                        labelColor = TextWhite
                    )
                )
                StoreCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonGreen,
                            selectedLabelColor = DeFittPureBlack,
                            containerColor = DeFittSurface,
                            labelColor = TextWhite
                        ),
                        modifier = Modifier.testTag("store_cat_${cat.name}")
                    )
                }
            }

            // Items List
            if (visibleItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Checkroom, null, tint = NeonGreen.copy(alpha = 0.6f), modifier = Modifier.size(44.dp))
                        Text(
                            text = if (activeTab == 1) "Aún no tienes prendas en esta categoría." else "No hay artículos disponibles.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleItems, key = { it.id }) { item ->
                        val isOwned = ownedIds.contains(item.id)
                        val isEquipped = equippedIds.contains(item.id)
                        val isPreviewing = previewItemId == item.id
                        val canAfford = (state.activeUser?.codBalance ?: 0L) >= item.priceCod

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isEquipped || isPreviewing) DeFittSurfaceElevated else DeFittSurface
                            ),
                            border = BorderStroke(
                                1.2.dp,
                                when {
                                    isEquipped -> NeonGreen
                                    isPreviewing -> CodGold
                                    else -> DeFittOutline
                                }
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Studio Product Illustration (NO empty circle!)
                                    StoreItemArtwork(
                                        item = item,
                                        size = 86.dp,
                                        modifier = Modifier.clickable {
                                            if (!isOwned) {
                                                onPreviewItem(if (isPreviewing) null else item.id)
                                            } else {
                                                onToggleEquip(item)
                                            }
                                        }
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.category.displayName.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeonGreen
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(CodGoldContainer, RoundedCornerShape(10.dp))
                                                    .border(1.dp, CodGold.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${item.priceCod} COD",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = CodGold,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Clear full-width action buttons: PROBAR + COMPRAR (or EQUIPADO / QUITAR)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!isOwned) {
                                        OutlinedButton(
                                            onClick = {
                                                onPreviewItem(if (isPreviewing) null else item.id)
                                            },
                                            border = BorderStroke(1.dp, if (isPreviewing) CodGold else DeFittOutline),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (isPreviewing) CodGold else TextWhite
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("try_item_${item.id}")
                                        ) {
                                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text(if (isPreviewing) "QUITAR PRUEBA" else "PROBAR", fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { onBuyItem(item) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (canAfford) CodGold else DeFittSurfaceElevated,
                                                contentColor = if (canAfford) DeFittPureBlack else TextSecondary
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("buy_item_${item.id}")
                                        ) {
                                            Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text("COMPRAR", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = { onToggleEquip(item) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isEquipped) NeonGreenContainer else SuccessGreen,
                                                contentColor = if (isEquipped) NeonGreen else DeFittPureBlack
                                            ),
                                            border = if (isEquipped) BorderStroke(1.dp, NeonGreen) else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .testTag("equip_item_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (isEquipped) Icons.Default.CheckCircle else Icons.Default.Checkroom,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text(
                                                text = if (isEquipped) "EQUIPADO • TOCA PARA QUITAR" else "EQUIPAR EN AVATAR",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

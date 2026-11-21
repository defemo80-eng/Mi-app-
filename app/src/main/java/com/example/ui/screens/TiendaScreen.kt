package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.catalog.StoreCatalog
import com.example.domain.model.StoreCategory
import com.example.domain.model.StoreItem
import com.example.ui.avatar3d.Avatar3DView
import com.example.ui.components.CodBalanceChip
import com.example.ui.theme.CodGold
import com.example.ui.theme.DeFittBlack
import com.example.ui.theme.DeFittOutline
import com.example.ui.theme.DeFittPureBlack
import com.example.ui.theme.DeFittSurface
import com.example.ui.theme.DeFittSurfaceElevated
import com.example.ui.theme.NeonGreen
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
    var activeTab by remember { mutableIntStateOf(0) } // 0=Tienda, 1=Armario
    var selectedCategory by remember { mutableStateOf<StoreCategory?>(null) }

    val ownedIds = remember(state.purchases) { state.purchases.map { it.itemId }.toSet() }
    val equippedIds = remember(state.equipped) { state.equipped.map { it.itemId } }

    // Merge equipped items with currently previewed item for live 3D try-on!
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeFittBlack)
            .padding(contentPadding),
        contentAlignment = Alignment.TopCenter
    ) {
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
                Text("Tienda & Armario", style = MaterialTheme.typography.headlineMedium, color = TextWhite)
                CodBalanceChip(balance = state.activeUser?.codBalance ?: 0L)
            }

            // Live 3D Try-on Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
            ) {
                Avatar3DView(
                    avatarParams = state.avatarParams,
                    equippedItemIds = displayEquippedIds,
                    modifier = Modifier.fillMaxSize(),
                    testTag = "store_avatar_3d"
                )
                if (previewItemId != null) {
                    val pItem = StoreCatalog.getById(previewItemId)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(DeFittSurfaceElevated.copy(alpha = 0.88f), RoundedCornerShape(12.dp))
                            .border(1.dp, CodGold, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Probando: ${pItem?.name ?: ""}", style = MaterialTheme.typography.labelSmall, color = CodGold)
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
                        selectedLabelColor = DeFittPureBlack
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
                        selectedLabelColor = DeFittPureBlack
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
                        selectedLabelColor = DeFittPureBlack
                    )
                )
                StoreCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonGreen,
                            selectedLabelColor = DeFittPureBlack
                        )
                    )
                }
            }

            // Items List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleItems, key = { it.id }) { item ->
                    val isOwned = ownedIds.contains(item.id)
                    val isEquipped = equippedIds.contains(item.id)
                    val isPreviewing = previewItemId == item.id

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEquipped || isPreviewing) DeFittSurfaceElevated else DeFittSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            when {
                                isEquipped -> NeonGreen
                                isPreviewing -> CodGold
                                else -> DeFittOutline
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Color Swatch Preview
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(item.primaryColorHex)))
                                    .border(
                                        2.5.dp,
                                        Color(android.graphics.Color.parseColor(item.accentColorHex)),
                                        CircleShape
                                    )
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium, color = TextWhite)
                                Text(item.category.displayName, style = MaterialTheme.typography.labelSmall, color = NeonGreen)
                                Text(item.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (!isOwned) {
                                    OutlinedButton(
                                        onClick = {
                                            onPreviewItem(if (isPreviewing) null else item.id)
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("try_item_${item.id}")
                                    ) {
                                        Icon(Icons.Default.Visibility, null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text(if (isPreviewing) "Quitar" else "Probar", style = MaterialTheme.typography.labelSmall)
                                    }

                                    Button(
                                        onClick = { onBuyItem(item) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CodGold,
                                            contentColor = DeFittPureBlack
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("buy_item_${item.id}")
                                    ) {
                                        Text("${item.priceCod} COD", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    }
                                } else {
                                    Button(
                                        onClick = { onToggleEquip(item) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isEquipped) DeFittSurfaceElevated else SuccessGreen,
                                            contentColor = if (isEquipped) NeonGreen else DeFittPureBlack
                                        ),
                                        border = if (isEquipped) BorderStroke(1.dp, NeonGreen) else null,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("equip_item_${item.id}")
                                    ) {
                                        Text(if (isEquipped) "Quitar" else "Equipar", fontWeight = FontWeight.Bold)
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

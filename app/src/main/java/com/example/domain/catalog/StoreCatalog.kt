package com.example.domain.catalog

import com.example.domain.model.StoreCategory
import com.example.domain.model.StoreItem

object StoreCatalog {
    val allItems: List<StoreItem> = listOf(
        // Camisetas
        StoreItem("shirt_defitt_black", "Camiseta DeFitt Pro Negra", StoreCategory.CAMISETAS.name, 120, "#151815", "#39FF14", "Camiseta técnica negra con emblema verde neón.", 0),
        StoreItem("shirt_neon_pulse", "Camiseta Pulso Neón", StoreCategory.CAMISETAS.name, 180, "#1D3318", "#39FF14", "Corte atlético transpirable con franjas de alto contraste.", 1),
        StoreItem("shirt_obsidian_tank", "Sin Mangas Obsidiana", StoreCategory.CAMISETAS.name, 150, "#111315", "#FFFFFF", "Camiseta sin mangas ligera para movilidad total.", 2),
        StoreItem("shirt_gold_edition", "Camiseta Edición COD", StoreCategory.CAMISETAS.name, 350, "#1C1A12", "#FFD700", "Edición especial con acentos dorados DeFitt.", 1),

        // Pantalones
        StoreItem("pants_tech_jogger", "Jogger Técnico Negro", StoreCategory.PANTALONES.name, 160, "#14171A", "#39FF14", "Pantalón deportivo ajustado con ribete neón.", 0),
        StoreItem("pants_short_sprint", "Short Sprint Carbono", StoreCategory.PANTALONES.name, 130, "#1B1E22", "#FFFFFF", "Short corto ligero para caminata y sentadillas.", 1),
        StoreItem("pants_cargo_street", "Pantalón Cargo Urbano", StoreCategory.PANTALONES.name, 220, "#232823", "#39FF14", "Corte urbano resistente para uso diario.", 0),

        // Sudaderas
        StoreItem("hoodie_stealth", "Sudadera Stealth Neón", StoreCategory.SUDADERAS.name, 280, "#121612", "#39FF14", "Sudadera con capucha negra y detalles neón.", 0),
        StoreItem("hoodie_arctic", "Sudadera Blanco Puro", StoreCategory.SUDADERAS.name, 260, "#E5E9E5", "#141414", "Sudadera minimalista blanca de alto contraste.", 0),
        StoreItem("hoodie_gold_apex", "Sudadera Apex Dorada", StoreCategory.SUDADERAS.name, 420, "#1E1B12", "#FFD700", "Sudadera premium con acabado dorado COD.", 1),

        // Zapatos
        StoreItem("shoes_urban_black", "Zapatos Urbanos Negros", StoreCategory.ZAPATOS.name, 200, "#18181B", "#A1A1AA", "Calzado casual oscuro de suela firme.", 0),
        StoreItem("shoes_classic_white", "Zapatos Clásicos Blancos", StoreCategory.ZAPATOS.name, 220, "#EDEDED", "#27272A", "Calzado limpio para uso cotidiano.", 0),

        // Tenis deportivos
        StoreItem("sneakers_defitt_volt", "Tenis DeFitt Volt", StoreCategory.TENIS_DEPORTIVOS.name, 300, "#121412", "#39FF14", "Tenis deportivos con suela reactiva verde neón.", 0),
        StoreItem("sneakers_aero_white", "Tenis Aero Blancos", StoreCategory.TENIS_DEPORTIVOS.name, 270, "#F4F4F5", "#39FF14", "Tenis ligeros blancos con talón neón.", 1),
        StoreItem("sneakers_gold_stride", "Tenis Stride Oro", StoreCategory.TENIS_DEPORTIVOS.name, 480, "#18150E", "#FFD700", "Tenis conmemorativos para caminantes constantes.", 1),

        // Gorras
        StoreItem("cap_defitt_neon", "Gorra DeFitt Neón", StoreCategory.GORRAS.name, 110, "#121512", "#39FF14", "Gorra deportiva negra con visera perfilada.", 0),
        StoreItem("cap_white_clean", "Gorra Blanca Minimal", StoreCategory.GORRAS.name, 100, "#EAEAEA", "#111111", "Gorra ligera ventilada.", 0),
        StoreItem("cap_gold_crown", "Gorra Corona COD", StoreCategory.GORRAS.name, 250, "#1B180E", "#FFD700", "Gorra edición dorada.", 0),

        // Gafas
        StoreItem("glasses_cyber_visor", "Gafas Visor Neón", StoreCategory.GAFAS.name, 190, "#0E110E", "#39FF14", "Gafas deportivas envolventes con lente verde.", 0),
        StoreItem("glasses_obsidian", "Gafas Obsidiana Sport", StoreCategory.GAFAS.name, 160, "#111111", "#FFFFFF", "Lentes oscuros aerodinámicos.", 0),
        StoreItem("glasses_gold_tint", "Gafas Lente Dorado", StoreCategory.GAFAS.name, 290, "#1A1710", "#FFD700", "Montura oscura con cristal dorado.", 0),

        // Relojes
        StoreItem("watch_defitt_pulse", "Reloj DeFitt Pulse", StoreCategory.RELOJES.name, 240, "#0E120E", "#39FF14", "Reloj deportivo con pantalla verde neón activa.", 0),
        StoreItem("watch_cod_chrono", "Reloj Chrono Dorado", StoreCategory.RELOJES.name, 400, "#1B180F", "#FFD700", "Cronógrafo inteligente edición COD.", 0),

        // Accesorios
        StoreItem("acc_wristband_neon", "Muñequeras Neón", StoreCategory.ACCESORIOS.name, 75, "#111611", "#39FF14", "Par de muñequeras deportivas absorbentes.", 0),
        StoreItem("acc_headband_pro", "Banda Frontal Pro", StoreCategory.ACCESORIOS.name, 90, "#1A1D1A", "#FFFFFF", "Cinta frontal deportiva ligera.", 1),

        // Mochilas
        StoreItem("bag_trail_pack", "Mochila Trail Negra", StoreCategory.MOCHILAS.name, 320, "#141714", "#39FF14", "Mochila ergonómica deportiva con líneas reflectivas.", 0),
        StoreItem("bag_urban_pack", "Mochila Urbana Gris", StoreCategory.MOCHILAS.name, 280, "#2A2E2A", "#FFFFFF", "Mochila compacta para hidratación y equipo.", 0),
        StoreItem("bag_gold_vault", "Mochila Vault COD", StoreCategory.MOCHILAS.name, 520, "#1E1B11", "#FFD700", "Mochila edición especial con herrajes dorados.", 0),

        // Conjuntos deportivos
        StoreItem("suit_defitt_full", "Conjunto DeFitt Elite", StoreCategory.CONJUNTOS_DEPORTIVOS.name, 550, "#101410", "#39FF14", "Conjunto completo chaqueta + jogger con diseño DeFitt.", 0),
        StoreItem("suit_stealth_gold", "Conjunto Dorado Pro", StoreCategory.CONJUNTOS_DEPORTIVOS.name, 750, "#17140D", "#FFD700", "Conjunto deportivo de gala en negro y dorado.", 1)
    )

    fun getById(id: String): StoreItem? = allItems.find { it.id == id }

    fun getByCategory(category: StoreCategory): List<StoreItem> =
        allItems.filter { it.categoryName == category.name }
}

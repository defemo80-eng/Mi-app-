package com.example.domain.catalog

import com.example.domain.model.FoodCatalogItem

object OfflineFoodCatalog {
    val items: List<FoodCatalogItem> = listOf(
        FoodCatalogItem("apple", "Manzana", listOf("apple", "manzana", "fruit", "pomme", "produce"), 150, 52f, 0.3f, 14f, 0.2f, 10.4f, 2.4f),
        FoodCatalogItem("banana", "Plátano / Banana", listOf("banana", "platano", "plátano", "fruit"), 120, 89f, 1.1f, 22.8f, 0.3f, 12.2f, 2.6f),
        FoodCatalogItem("orange", "Naranja", listOf("orange", "naranja", "citrus", "fruit"), 140, 47f, 0.9f, 11.8f, 0.1f, 9.4f, 2.4f),
        FoodCatalogItem("strawberry", "Fresas", listOf("strawberry", "fresa", "berry", "fruit"), 150, 32f, 0.7f, 7.7f, 0.3f, 4.9f, 2.0f),
        FoodCatalogItem("avocado", "Aguacate", listOf("avocado", "aguacate", "palta", "fruit"), 100, 160f, 2.0f, 8.5f, 14.7f, 0.7f, 6.7f),
        FoodCatalogItem("salad", "Ensalada verde mixta", listOf("salad", "ensalada", "vegetable", "greens", "leaf", "lettuce"), 200, 35f, 2.1f, 5.2f, 0.8f, 2.2f, 2.8f),
        FoodCatalogItem("broccoli", "Brócoli cocido", listOf("broccoli", "brocoli", "vegetable"), 150, 35f, 2.4f, 7.2f, 0.4f, 1.4f, 3.3f),
        FoodCatalogItem("tomato", "Tomate fresco", listOf("tomato", "tomate", "vegetable"), 120, 18f, 0.9f, 3.9f, 0.2f, 2.6f, 1.2f),
        FoodCatalogItem("carrot", "Zanahoria", listOf("carrot", "zanahoria", "vegetable"), 100, 41f, 0.9f, 9.6f, 0.2f, 4.7f, 2.8f),
        FoodCatalogItem("egg", "Huevo cocido / revuelto", listOf("egg", "huevo", "omelette", "breakfast"), 100, 155f, 12.6f, 1.1f, 10.6f, 1.1f, 0f),
        FoodCatalogItem("chicken_breast", "Pechuga de pollo a la plancha", listOf("chicken", "pollo", "poultry", "meat", "grilled"), 150, 165f, 31.0f, 0f, 3.6f, 0f, 0f),
        FoodCatalogItem("beef_steak", "Carne de res magra", listOf("beef", "steak", "carne", "meat"), 150, 210f, 26.0f, 0f, 11.2f, 0f, 0f),
        FoodCatalogItem("fish_salmon", "Salmón / Pescado al horno", listOf("fish", "salmon", "pescado", "seafood"), 150, 185f, 22.0f, 0f, 10.5f, 0f, 0f),
        FoodCatalogItem("tuna", "Atún natural", listOf("tuna", "atun", "fish", "seafood"), 120, 116f, 25.5f, 0f, 0.8f, 0f, 0f),
        FoodCatalogItem("rice_white", "Arroz blanco cocido", listOf("rice", "arroz", "grain", "dish"), 180, 130f, 2.7f, 28.2f, 0.3f, 0.1f, 0.4f),
        FoodCatalogItem("rice_brown", "Arroz integral cocido", listOf("brown rice", "arroz integral", "rice", "grain"), 180, 112f, 2.6f, 23.5f, 0.9f, 0.4f, 1.8f),
        FoodCatalogItem("pasta", "Pasta cocida", listOf("pasta", "spaghetti", "noodle", "macaroni"), 200, 131f, 5.0f, 25.0f, 1.1f, 0.6f, 1.8f),
        FoodCatalogItem("bread_whole", "Pan integral", listOf("bread", "pan", "toast", "bakery", "baked"), 60, 247f, 13.0f, 41.0f, 3.4f, 6.0f, 6.8f),
        FoodCatalogItem("bread_white", "Pan blanco / Bolillo", listOf("bread", "pan", "baguette", "roll", "baked"), 60, 265f, 9.0f, 49.0f, 3.2f, 5.0f, 2.7f),
        FoodCatalogItem("oats", "Avena cocida", listOf("oatmeal", "avena", "porridge", "cereal"), 200, 71f, 2.5f, 12.0f, 1.5f, 0.5f, 1.7f),
        FoodCatalogItem("yogurt", "Yogur natural", listOf("yogurt", "yogur", "dairy"), 150, 61f, 3.5f, 4.7f, 3.3f, 4.7f, 0f),
        FoodCatalogItem("milk", "Leche semidescremada", listOf("milk", "leche", "dairy", "drink", "glass"), 250, 50f, 3.3f, 4.8f, 1.6f, 4.8f, 0f),
        FoodCatalogItem("cheese", "Queso fresco / panela", listOf("cheese", "queso", "dairy"), 50, 220f, 17.5f, 2.2f, 15.8f, 1.0f, 0f),
        FoodCatalogItem("beans", "Frijoles / Lentejas cocidas", listOf("beans", "frijoles", "lentils", "lentejas", "legume"), 160, 127f, 8.7f, 22.8f, 0.5f, 0.3f, 7.5f),
        FoodCatalogItem("potato", "Papa cocida / al horno", listOf("potato", "papa", "patata"), 160, 87f, 1.9f, 20.1f, 0.1f, 0.9f, 1.8f),
        FoodCatalogItem("sweet_potato", "Camote / Batata", listOf("sweet potato", "camote", "batata"), 160, 90f, 2.0f, 20.7f, 0.2f, 6.5f, 3.3f),
        FoodCatalogItem("almonds", "Almendras / Nueces", listOf("almond", "nut", "almendra", "nuez", "snack"), 30, 579f, 21.2f, 21.6f, 49.9f, 4.4f, 12.5f),
        FoodCatalogItem("sandwich", "Sándwich de jamón y queso", listOf("sandwich", "sub", "burger", "torta"), 180, 240f, 12.0f, 26.0f, 9.5f, 3.5f, 2.0f),
        FoodCatalogItem("taco", "Tacos caseros (pollo/carne)", listOf("taco", "tortilla", "mexican", "wrap"), 160, 215f, 11.5f, 21.0f, 9.2f, 1.5f, 2.5f),
        FoodCatalogItem("pizza", "Pizza (porción estándar)", listOf("pizza", "fast food", "cheese"), 140, 266f, 11.4f, 33.3f, 9.7f, 3.6f, 2.3f),
        FoodCatalogItem("soup", "Sopa de verduras / caldo", listOf("soup", "sopa", "caldo", "stew", "bowl"), 300, 45f, 2.2f, 6.5f, 1.1f, 1.8f, 1.4f),
        FoodCatalogItem("coffee", "Café negro sin azúcar", listOf("coffee", "cafe", "espresso", "mug", "cup"), 240, 2f, 0.3f, 0f, 0f, 0f, 0f),
        FoodCatalogItem("juice", "Jugo de naranja natural", listOf("juice", "jugo", "zumo", "beverage"), 240, 45f, 0.7f, 10.4f, 0.2f, 8.4f, 0.2f),
        FoodCatalogItem("cake_sweet", "Pan dulce / Pastelito", listOf("cake", "pastry", "dessert", "cookie", "donut", "pastel"), 80, 360f, 5.0f, 52.0f, 14.5f, 28.0f, 1.5f),
        FoodCatalogItem("water", "Agua natural", listOf("water", "agua", "bottle"), 250, 0f, 0f, 0f, 0f, 0f, 0f),
        FoodCatalogItem("custom_meal", "Plato casero mixto", listOf("food", "meal", "dish", "plate", "cuisine"), 250, 155f, 9.5f, 18.0f, 5.2f, 2.1f, 2.4f)
    )

    fun matchLabels(labels: List<String>): FoodCatalogItem {
        val normalized = labels.map { it.lowercase().trim() }
        for (label in normalized) {
            val exact = items.firstOrNull { item ->
                item.keywords.any { kw -> label.contains(kw) || kw.contains(label) }
            }
            if (exact != null && exact.id != "custom_meal") return exact
        }
        return items.last() // fallback generic plato mixto
    }

    fun search(query: String): List<FoodCatalogItem> {
        val q = query.lowercase().trim()
        if (q.isBlank()) return items
        return items.filter {
            it.name.lowercase().contains(q) || it.keywords.any { k -> k.contains(q) }
        }
    }
}

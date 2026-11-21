package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AvatarParams(
    val heightCm: Int = 172,
    val shoulderWidthRatio: Float = 1.0f, // 0.84 (narrow) .. 1.22 (broad)
    val waistRatio: Float = 1.0f,         // 0.82 (lean) .. 1.24 (wider)
    val hipWidthRatio: Float = 1.0f,      // 0.86 .. 1.18
    val limbThicknessRatio: Float = 1.0f, // 0.85 .. 1.22
    val postureAlignment: Float = 0.92f,  // 0.70 .. 1.00
    val skinToneHex: String = "#D69E7A",
    val hairColorHex: String = "#1D1B1A",
    val hairStyle: Int = 0,               // 0=Short, 1=Fade, 2=Long, 3=Buzz
    val calibratedViaCamera: Boolean = false,
    val scanCount: Int = 0,
    val lastUpdatedDate: String = ""
)

enum class StoreCategory(val displayName: String) {
    CAMISETAS("Camisetas"),
    PANTALONES("Pantalones"),
    SUDADERAS("Sudaderas"),
    ZAPATOS("Zapatos"),
    TENIS_DEPORTIVOS("Tenis deportivos"),
    GORRAS("Gorras"),
    GAFAS("Gafas"),
    RELOJES("Relojes"),
    ACCESORIOS("Accesorios"),
    MOCHILAS("Mochilas"),
    CONJUNTOS_DEPORTIVOS("Conjuntos deportivos")
}

@Serializable
data class StoreItem(
    val id: String,
    val name: String,
    val categoryName: String,
    val priceCod: Int,
    val primaryColorHex: String,
    val accentColorHex: String,
    val description: String,
    val styleVariant: Int = 0
) {
    val category: StoreCategory
        get() = StoreCategory.valueOf(categoryName)
}

enum class MealType(val label: String) {
    DESAYUNO("Desayuno"),
    ALMUERZO("Almuerzo"),
    CENA("Cena"),
    MERIENDA("Meriendas")
}

@Serializable
data class FoodCatalogItem(
    val id: String,
    val name: String,
    val keywords: List<String>,
    val defaultPortionGrams: Int,
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatsPer100g: Float,
    val sugarsPer100g: Float,
    val fiberPer100g: Float
)

enum class ExperienceLevel(val label: String) {
    PRINCIPIANTE("Principiante"),
    INTERMEDIO("Intermedio"),
    AVANZADO("Avanzado")
}

enum class ExerciseAnimType {
    PUSHUPS,
    SQUATS,
    MOBILITY,
    PLANK,
    WALKING,
    STRETCH,
    COORDINATION
}

@Serializable
data class ExerciseItem(
    val id: String,
    val name: String,
    val category: String,
    val explanation: String,
    val prescriptionLabel: String,
    val durationSeconds: Int,
    val restSeconds: Int,
    val difficulty: String,
    val animTypeName: String
) {
    val animType: ExerciseAnimType
        get() = ExerciseAnimType.valueOf(animTypeName)
}

@Serializable
data class WorkoutRoutine(
    val title: String,
    val difficulty: String,
    val estimatedMinutes: Int,
    val targetNote: String,
    val exercises: List<ExerciseItem>
)

@Serializable
data class ScanComparison(
    val hasTwoScans: Boolean,
    val previousDate: String?,
    val latestDate: String?,
    val shoulderDeltaPct: Float,
    val waistDeltaPct: Float,
    val postureDeltaPct: Float,
    val measurableChangeDetected: Boolean,
    val summaryMessage: String
)

@Serializable
data class BackupPayload(
    val formatVersion: Int = 1,
    val exportedAt: Long,
    val checksumSha256: String,
    val userJson: String,
    val dailyLogsJson: String,
    val transactionsJson: String,
    val foodsJson: String,
    val workoutsJson: String,
    val scansJson: String,
    val purchasesJson: String,
    val equippedJson: String,
    val achievementsJson: String
)

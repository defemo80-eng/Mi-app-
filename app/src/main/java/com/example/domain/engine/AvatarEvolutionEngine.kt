package com.example.domain.engine

import com.example.data.local.entity.BodyScanEntity
import com.example.domain.model.AvatarParams
import com.example.domain.model.ScanComparison
import kotlin.math.abs

object AvatarEvolutionEngine {

    private const val MIN_RELIABLE_DELTA = 0.025f // 2.5% minimum reliable geometric change

    /**
     * Compares two body scans (previous vs latest) using measurable geometric ratios.
     * Never fabricates muscles, abs, or unrealistic changes.
     */
    fun compareScans(previous: BodyScanEntity?, latest: BodyScanEntity?): ScanComparison {
        if (previous == null || latest == null || previous.id == latest.id) {
            return ScanComparison(
                hasTwoScans = false,
                previousDate = previous?.dateKey,
                latestDate = latest?.dateKey,
                shoulderDeltaPct = 0f,
                waistDeltaPct = 0f,
                postureDeltaPct = 0f,
                measurableChangeDetected = false,
                summaryMessage = "Se necesitan al menos dos escaneos semanales para comparar cambios físicos fiables."
            )
        }

        val shoulderDiff = latest.shoulderWidthRatio - previous.shoulderWidthRatio
        val waistDiff = latest.waistRatio - previous.waistRatio
        val postureDiff = latest.postureAlignmentScore - previous.postureAlignmentScore

        val measurable = abs(shoulderDiff) >= MIN_RELIABLE_DELTA ||
            abs(waistDiff) >= MIN_RELIABLE_DELTA ||
            abs(postureDiff) >= 0.03f

        val summary = if (!measurable) {
            "Sin cambios físicos comprobables entre ${previous.dateKey} y ${latest.dateKey}. El avatar conserva exactamente su apariencia actual."
        } else {
            val parts = mutableListOf<String>()
            if (abs(waistDiff) >= MIN_RELIABLE_DELTA) {
                val dir = if (waistDiff < 0) "reducción" else "variación"
                parts.add("$dir de contorno medio (${String.format("%.1f", abs(waistDiff) * 100)}%)")
            }
            if (abs(shoulderDiff) >= MIN_RELIABLE_DELTA) {
                val dir = if (shoulderDiff > 0) "amplitud" else "ajuste"
                parts.add("$dir de hombros (${String.format("%.1f", abs(shoulderDiff) * 100)}%)")
            }
            if (abs(postureDiff) >= 0.03f) {
                val dir = if (postureDiff > 0) "mejora" else "cambio"
                parts.add("$dir postural (${String.format("%.1f", abs(postureDiff) * 100)}%)")
            }
            "Cambios graduales medibles entre ${previous.dateKey} y ${latest.dateKey}: ${parts.joinToString(", ")}. Representación orientativa, no medición médica."
        }

        return ScanComparison(
            hasTwoScans = true,
            previousDate = previous.dateKey,
            latestDate = latest.dateKey,
            shoulderDeltaPct = shoulderDiff * 100f,
            waistDeltaPct = waistDiff * 100f,
            postureDeltaPct = postureDiff * 100f,
            measurableChangeDetected = measurable,
            summaryMessage = summary
        )
    }

    /**
     * Midnight daily evaluation rule:
     * Only changes physical appearance if verifiable scan calibration justifies a gradual step
     * toward the latest verified scan measurements. Never modifies body based solely on XP or workouts!
     */
    fun evaluateMidnightUpdate(
        currentAvatar: AvatarParams,
        latestScan: BodyScanEntity?,
        previousScan: BodyScanEntity?,
        dateKey: String
    ): Pair<AvatarParams, String> {
        if (latestScan == null || previousScan == null || latestScan.id == previousScan.id) {
            return currentAvatar.copy(lastUpdatedDate = dateKey) to
                "Actualización diaria ($dateKey): Sin cambios físicos comprobables; el avatar conserva su apariencia."
        }

        val comparison = compareScans(previousScan, latestScan)
        if (!comparison.measurableChangeDetected) {
            return currentAvatar.copy(lastUpdatedDate = dateKey) to
                "Actualización diaria ($dateKey): Mediciones estables; el avatar conserva su apariencia."
        }

        // Gradual interpolation (max 35% per daily step toward verified scan target)
        fun stepTowards(current: Float, target: Float): Float {
            val delta = target - current
            return if (abs(delta) < 0.005f) target else current + delta * 0.35f
        }

        val updated = currentAvatar.copy(
            shoulderWidthRatio = stepTowards(currentAvatar.shoulderWidthRatio, latestScan.shoulderWidthRatio).coerceIn(0.84f, 1.22f),
            waistRatio = stepTowards(currentAvatar.waistRatio, latestScan.waistRatio).coerceIn(0.82f, 1.24f),
            hipWidthRatio = stepTowards(currentAvatar.hipWidthRatio, latestScan.hipWidthRatio).coerceIn(0.86f, 1.18f),
            limbThicknessRatio = stepTowards(currentAvatar.limbThicknessRatio, latestScan.limbThicknessRatio).coerceIn(0.85f, 1.22f),
            postureAlignment = stepTowards(currentAvatar.postureAlignment, latestScan.postureAlignmentScore).coerceIn(0.70f, 1.00f),
            lastUpdatedDate = dateKey
        )

        return updated to "Actualización diaria ($dateKey): Ajuste gradual aplicado según comparación verificada de escaneos."
    }
}

package com.example.domain.engine

import com.example.domain.model.ExerciseAnimType
import com.example.domain.model.ExerciseItem
import com.example.domain.model.ExperienceLevel
import com.example.domain.model.WorkoutRoutine

object WorkoutGenerator {

    fun generateDailyRoutine(
        age: Int,
        experienceLevel: ExperienceLevel,
        recentSteps: Long
    ): WorkoutRoutine {
        val ageAdjustedFactor = when {
            age >= 60 -> 0.7f
            age >= 45 -> 0.85f
            age < 16 -> 0.8f
            else -> 1.0f
        }

        val level = if (age >= 65 && experienceLevel == ExperienceLevel.AVANZADO) {
            ExperienceLevel.INTERMEDIO
        } else {
            experienceLevel
        }

        val (difficultyLabel, repsScale, restScale) = when (level) {
            ExperienceLevel.PRINCIPIANTE -> Triple("Suave / Principiante", 0.75f * ageAdjustedFactor, 1.2f)
            ExperienceLevel.INTERMEDIO -> Triple("Moderado / Intermedio", 1.0f * ageAdjustedFactor, 1.0f)
            ExperienceLevel.AVANZADO -> Triple("Activo / Avanzado", 1.25f * ageAdjustedFactor, 0.85f)
        }

        val pushupVariant = when (level) {
            ExperienceLevel.PRINCIPIANTE -> ExerciseItem(
                id = "ex_pushups_adapted",
                name = "Flexiones adaptadas",
                category = "Fuerza superior",
                explanation = "Apoya tus manos contra una pared firme o en el suelo con las rodillas apoyadas. Mantén la espalda recta y baja de forma controlada sin forzar los hombros.",
                prescriptionLabel = "${(8 * repsScale).toInt().coerceAtLeast(5)} repeticiones x 2 series",
                durationSeconds = 40,
                restSeconds = (35 * restScale).toInt().coerceIn(20, 60),
                difficulty = difficultyLabel,
                animTypeName = ExerciseAnimType.PUSHUPS.name
            )
            ExperienceLevel.INTERMEDIO -> ExerciseItem(
                id = "ex_pushups_knee_std",
                name = "Flexiones adaptadas controladas",
                category = "Fuerza superior",
                explanation = "Con rodillas apoyadas o postura completa según tu comodidad, desciende lentamente en 2 segundos y empuja sin bloquear los codos.",
                prescriptionLabel = "${(10 * repsScale).toInt().coerceAtLeast(6)} repeticiones x 3 series",
                durationSeconds = 45,
                restSeconds = (30 * restScale).toInt().coerceIn(20, 50),
                difficulty = difficultyLabel,
                animTypeName = ExerciseAnimType.PUSHUPS.name
            )
            ExperienceLevel.AVANZADO -> ExerciseItem(
                id = "ex_pushups_standard",
                name = "Flexiones técnicas",
                category = "Fuerza superior",
                explanation = "Cuerpo alineado de cabeza a talones, abdomen firme y descenso estable. Prioriza técnica limpia sobre velocidad.",
                prescriptionLabel = "${(12 * repsScale).toInt().coerceAtLeast(8)} repeticiones x 3 series",
                durationSeconds = 50,
                restSeconds = (25 * restScale).toInt().coerceIn(20, 45),
                difficulty = difficultyLabel,
                animTypeName = ExerciseAnimType.PUSHUPS.name
            )
        }

        val squats = ExerciseItem(
            id = "ex_squats",
            name = if (age >= 55 || level == ExperienceLevel.PRINCIPIANTE) "Sentadillas asistidas" else "Sentadillas controladas",
            category = "Tren inferior",
            explanation = "Pies al ancho de los hombros. Lleva la cadera hacia atrás como si fueras a sentarte en una silla, cuidando que las rodillas sigan la línea de los pies.",
            prescriptionLabel = "${(12 * repsScale).toInt().coerceAtLeast(6)} repeticiones",
            durationSeconds = 45,
            restSeconds = (30 * restScale).toInt().coerceIn(20, 50),
            difficulty = difficultyLabel,
            animTypeName = ExerciseAnimType.SQUATS.name
        )

        val mobility = ExerciseItem(
            id = "ex_mobility",
            name = "Movilidad articular guiada",
            category = "Movilidad",
            explanation = "Círculos lentos de hombros hacia atrás, rotaciones suaves de cadera y extensión torácica respirando profundo.",
            prescriptionLabel = "45 segundos continuos",
            durationSeconds = 45,
            restSeconds = 15,
            difficulty = "Para todos los niveles",
            animTypeName = ExerciseAnimType.MOBILITY.name
        )

        val plank = ExerciseItem(
            id = "ex_plank",
            name = if (level == ExperienceLevel.PRINCIPIANTE) "Plancha inclinada o con rodillas" else "Plancha abdominal adaptada",
            category = "Zona media",
            explanation = "Apoya antebrazos sobre una superficie firme o elevada. Mantén el cuello neutro y evita hundir la zona lumbar.",
            prescriptionLabel = "${(25 * repsScale).toInt().coerceIn(15, 45)} segundos",
            durationSeconds = (25 * repsScale).toInt().coerceIn(15, 45),
            restSeconds = (30 * restScale).toInt().coerceIn(20, 45),
            difficulty = difficultyLabel,
            animTypeName = ExerciseAnimType.PLANK.name
        )

        val walk = ExerciseItem(
            id = "ex_walk",
            name = "Caminata activa en sitio",
            category = "Cardiovascular suave",
            explanation = if (recentSteps > 4000) {
                "Ya acumulaste buena caminata hoy. Mantén un ritmo fluido elevando rodillas suavemente para activar circulación."
            } else {
                "Marcha rítmica moviendo los brazos de forma natural. Ritmo cómodo que te permita hablar sin ahogarte."
            },
            prescriptionLabel = "60 segundos",
            durationSeconds = 60,
            restSeconds = 20,
            difficulty = "Moderado saludable",
            animTypeName = ExerciseAnimType.WALKING.name
        )

        val coordination = ExerciseItem(
            id = "ex_coordination",
            name = "Coordinación brazo-pierna alternada",
            category = "Coordinación",
            explanation = "Eleva rodilla derecha mientras tocas con la mano izquierda de forma controlada, alternando lados con equilibrio.",
            prescriptionLabel = "${(16 * repsScale).toInt().coerceAtLeast(10)} alternancias",
            durationSeconds = 40,
            restSeconds = 20,
            difficulty = difficultyLabel,
            animTypeName = ExerciseAnimType.COORDINATION.name
        )

        val stretch = ExerciseItem(
            id = "ex_stretch",
            name = "Estiramientos finales y respiración",
            category = "Recuperación",
            explanation = "Estira gemelos, cuádriceps y espalda alta sin rebotes. Inhala 4 segundos y exhala 6 segundos para relajar el pulso.",
            prescriptionLabel = "50 segundos suaves",
            durationSeconds = 50,
            restSeconds = 10,
            difficulty = "Recuperación",
            animTypeName = ExerciseAnimType.STRETCH.name
        )

        val exercises = listOf(mobility, walk, squats, pushupVariant, plank, coordination, stretch)
        val totalMinutes = ((exercises.sumOf { it.durationSeconds + it.restSeconds }) / 60).coerceAtLeast(6)

        return WorkoutRoutine(
            title = "Rutina Diaria Personalizada",
            difficulty = difficultyLabel,
            estimatedMinutes = totalMinutes,
            targetNote = "Adaptada a ${age} años • Nivel ${level.label} • Sin máquinas ni rutinas extremas",
            exercises = exercises
        )
    }
}

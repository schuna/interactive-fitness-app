package com.openai.interactivefitness.domain

enum class MuscleGroup(val label: String) {
    CHEST("가슴"),
    BACK("등"),
    SHOULDERS("어깨"),
    ARMS("팔"),
    LEGS("하체"),
    CORE("코어"),
}

enum class ExerciseEquipment(val label: String) {
    BODYWEIGHT("맨몸"),
    DUMBBELL("덤벨"),
    BARBELL("바벨"),
    MACHINE("머신"),
    CABLE("케이블"),
}

enum class ExerciseIllustration {
    VERTICAL_PULL,
    HORIZONTAL_PULL,
    HIP_HINGE,
    PRESS,
    SQUAT,
    PLANK,
    CURL,
    SHOULDER_PRESS,
}

data class ExerciseDefinition(
    val id: String,
    val name: String,
    val muscleGroups: Set<MuscleGroup>,
    val equipment: ExerciseEquipment,
    val illustration: ExerciseIllustration,
)

object ExerciseCatalog {
    val exercises: List<ExerciseDefinition> = listOf(
        ExerciseDefinition(
            "lat-pulldown", "랫 풀다운", setOf(MuscleGroup.BACK),
            ExerciseEquipment.CABLE, ExerciseIllustration.VERTICAL_PULL,
        ),
        ExerciseDefinition(
            "seated-cable-row", "시티드 케이블 로우", setOf(MuscleGroup.BACK),
            ExerciseEquipment.CABLE, ExerciseIllustration.HORIZONTAL_PULL,
        ),
        ExerciseDefinition(
            "one-arm-dumbbell-row", "원 암 덤벨 로우", setOf(MuscleGroup.BACK),
            ExerciseEquipment.DUMBBELL, ExerciseIllustration.HORIZONTAL_PULL,
        ),
        ExerciseDefinition(
            "romanian-deadlift", "루마니안 데드리프트",
            setOf(MuscleGroup.BACK, MuscleGroup.LEGS),
            ExerciseEquipment.BARBELL, ExerciseIllustration.HIP_HINGE,
        ),
        ExerciseDefinition(
            "chest-press", "체스트 프레스", setOf(MuscleGroup.CHEST),
            ExerciseEquipment.MACHINE, ExerciseIllustration.PRESS,
        ),
        ExerciseDefinition(
            "push-up", "푸시업", setOf(MuscleGroup.CHEST, MuscleGroup.ARMS),
            ExerciseEquipment.BODYWEIGHT, ExerciseIllustration.PRESS,
        ),
        ExerciseDefinition(
            "goblet-squat", "고블릿 스쿼트", setOf(MuscleGroup.LEGS),
            ExerciseEquipment.DUMBBELL, ExerciseIllustration.SQUAT,
        ),
        ExerciseDefinition(
            "back-squat", "백 스쿼트", setOf(MuscleGroup.LEGS),
            ExerciseEquipment.BARBELL, ExerciseIllustration.SQUAT,
        ),
        ExerciseDefinition(
            "plank", "플랭크", setOf(MuscleGroup.CORE),
            ExerciseEquipment.BODYWEIGHT, ExerciseIllustration.PLANK,
        ),
        ExerciseDefinition(
            "dumbbell-curl", "덤벨 컬", setOf(MuscleGroup.ARMS),
            ExerciseEquipment.DUMBBELL, ExerciseIllustration.CURL,
        ),
        ExerciseDefinition(
            "shoulder-press", "숄더 프레스", setOf(MuscleGroup.SHOULDERS),
            ExerciseEquipment.DUMBBELL, ExerciseIllustration.SHOULDER_PRESS,
        ),
        ExerciseDefinition(
            "cable-face-pull", "케이블 페이스 풀",
            setOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK),
            ExerciseEquipment.CABLE, ExerciseIllustration.HORIZONTAL_PULL,
        ),
    )

    fun filter(
        query: String = "",
        muscleGroup: MuscleGroup? = null,
        equipment: ExerciseEquipment? = null,
    ): List<ExerciseDefinition> {
        val normalized = query.trim()
        return exercises.filter { exercise ->
            (normalized.isBlank() ||
                exercise.name.contains(normalized, ignoreCase = true) ||
                exercise.muscleGroups.any { it.label.contains(normalized) } ||
                exercise.equipment.label.contains(normalized)) &&
                (muscleGroup == null || muscleGroup in exercise.muscleGroups) &&
                (equipment == null || exercise.equipment == equipment)
        }
    }
}

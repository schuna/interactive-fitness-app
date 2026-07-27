package com.openai.interactivefitness.data

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.openai.interactivefitness.domain.ConversationIntent
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

data class GeminiIntentResult(
    val intent: ConversationIntent,
    val parameters: Map<String, String>,
    val reply: String,
)

class GeminiIntentRouter {
    private val functions = listOf(
        FunctionDeclaration(
            "open_manual_workout_log",
            "운동 기록을 사용자가 직접 입력하는 화면을 연다.",
            mapOf(
                "workoutType" to Schema.string(
                    "운동 종류: STRENGTH, RUNNING, CYCLING, RECOVERY 중 하나",
                ),
            ),
            optionalParameters = listOf("workoutType"),
        ),
        FunctionDeclaration(
            "show_workout_history",
            "저장된 운동 기록이나 운동 내역을 보여준다.",
            mapOf(
                "period" to Schema.string("조회 기간: WEEK, MONTH, ALL 중 하나"),
            ),
            optionalParameters = listOf("period"),
        ),
        FunctionDeclaration(
            "show_trend_analysis",
            "주간 또는 월간 운동 트렌드와 통계를 보여준다.",
            mapOf(
                "period" to Schema.string("분석 기간: WEEK 또는 MONTH"),
                "workoutType" to Schema.string("선택적인 운동 종류"),
            ),
            optionalParameters = listOf("period", "workoutType"),
        ),
        FunctionDeclaration(
            "get_recommended_workout",
            "오늘의 컨디션과 최근 기록에 따른 추천 운동을 보여준다.",
            mapOf(
                "availableMinutes" to Schema.integer("사용 가능한 운동 시간(분)"),
                "focus" to Schema.string("원하는 운동 부위 또는 목표"),
            ),
            optionalParameters = listOf("availableMinutes", "focus"),
        ),
        FunctionDeclaration(
            "open_custom_plan_builder",
            "운동 종목을 직접 선택하는 커스텀 운동 계획 화면을 연다.",
            mapOf(
                "muscleGroup" to Schema.string("운동 부위"),
                "equipment" to Schema.string("사용할 장비"),
                "durationMinutes" to Schema.integer("계획 시간(분)"),
            ),
            optionalParameters = listOf("muscleGroup", "equipment", "durationMinutes"),
        ),
        FunctionDeclaration(
            "update_daily_condition",
            "피로도, 근육통, 통증 등 오늘의 컨디션 입력 화면을 연다.",
            emptyMap(),
        ),
    )

    private val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = MODEL_NAME,
        tools = listOf(Tool.functionDeclarations(functions)),
    )

    suspend fun route(text: String): GeminiIntentResult? {
        val prompt = """
            사용자의 한국어 문장을 분석해 가장 적절한 앱 함수 하나를 선택하세요.
            메뉴와 관련 없는 일반 대화라면 함수 호출 없이 짧게 답하세요.
            사용자 문장: $text
        """.trimIndent()
        val response = model.generateContent(prompt)
        val call = response.functionCalls.firstOrNull() ?: return null
        val parameters = call.args.mapValues { (_, value) -> value.asText() }
        return mapGeminiFunction(call.name, parameters)
    }

    private fun JsonElement.asText(): String =
        runCatching { jsonPrimitive.content }.getOrElse { toString() }

    private companion object {
        const val MODEL_NAME = "gemini-3.5-flash-lite"
    }
}

internal fun mapGeminiFunction(
    name: String,
    parameters: Map<String, String> = emptyMap(),
): GeminiIntentResult? =
    when (name) {
            "open_manual_workout_log" -> GeminiIntentResult(
                ConversationIntent.ManualLog,
                parameters,
                "운동 내용을 직접 입력해 저장할까요?",
            )
            "show_workout_history" -> GeminiIntentResult(
                ConversationIntent.ShowHistory,
                parameters,
                "운동 기록을 확인할까요?",
            )
            "show_trend_analysis" -> GeminiIntentResult(
                ConversationIntent.ShowDashboard,
                parameters,
                "운동 트렌드 분석을 확인할까요?",
            )
            "get_recommended_workout" -> GeminiIntentResult(
                ConversationIntent.RecommendToday,
                parameters,
                "컨디션과 최근 기록을 반영한 추천 운동을 확인할까요?",
            )
            "open_custom_plan_builder" -> GeminiIntentResult(
                ConversationIntent.CustomWorkoutPlan,
                parameters,
                "운동 종목을 선택해 커스텀 계획을 만들까요?",
            )
            "update_daily_condition" -> GeminiIntentResult(
                ConversationIntent.UpdateCondition,
                parameters,
                "오늘의 컨디션을 다시 입력할까요?",
            )
            else -> null
        }

package com.openai.interactivefitness.domain

data class MenuCandidate(
    val command: String,
    val keywords: Set<String>,
)

/**
 * Small, offline intent ranker. It is deterministic and keeps execution behind
 * the existing ConversationEngine validation instead of executing arbitrary text.
 */
class MenuSuggestionEngine {
    fun suggest(text: String, candidates: List<MenuCandidate>): String? {
        val normalized = text.trim().lowercase()
        if (normalized.length < 2) return null
        return candidates
            .map { candidate ->
                val score: Int = candidate.keywords.fold(0) { total, keyword ->
                    total + when {
                        normalized.contains(keyword) -> 3
                        normalized.split(Regex("\\s+")).any {
                            it.length >= 2 && keyword.contains(it)
                        } -> 1
                        else -> 0
                    }
                }
                candidate.command to score
            }
            .filter { it.second > 0 }
            .maxWithOrNull(compareBy<Pair<String, Int>> { it.second }.thenBy { it.first })
            ?.first
    }
}

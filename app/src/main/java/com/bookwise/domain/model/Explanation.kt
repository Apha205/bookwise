package com.bookwise.domain.model

data class Explanation(
    val id: String,
    val bookId: String,
    val locatorJson: String?,
    val selectedText: String,
    val action: TutorAction,
    val prompt: String?,
    val response: String,
    val createdAtMillis: Long
)

enum class TutorAction(val label: String) {
    EXPLAIN("Explain"),
    TRANSLATE("Translate"),
    DEFINE("Define"),
    SUMMARIZE("Summarize"),
    ASK("Ask")
}

package dev.eftrainer.domain.learning

data class TaskRun(
    val passed: Map<String, List<String>> = emptyMap(),
    val missed: Set<String> = emptySet(),
    val skipped: Set<String> = emptySet(),
)

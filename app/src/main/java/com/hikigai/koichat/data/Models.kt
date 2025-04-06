package com.hikigai.koichat.data

data class DiagnosisResponse(
    val name: String,
    val description: String? = null
)

data class APIResponse(
    val data: List<DiagnosisResponse>,
    val message: String,
    val status: String
)

data class PromptLimit(
    val daily: LimitData,
    val monthly: LimitData
)

data class LimitData(
    val time: String,
    val count: Int
)

object APIConfig {
    const val BASE_URL = "https://smarthostdev.hikigaidemo.com"
    const val DIAGNOSIS_ENDPOINT = "/api/gen/v2/koichat/suggested_diagnosis/notes_based"
    const val FEEDBACK_ENDPOINT = "/gen/v1/diagnosis_feedback"
    
    val HEADERS = mapOf(
        "Authorization" to "org_d9e84eda44b5efec3d26750c7d1291b3ef352acc8ef32a4c80c9d3e446d71bc059cb377f736e3b163fa469",
        "x-bland-org-id" to "54cdc6f0-1087-4641-9e16-e7fd0b1b6303",
        "Content-Type" to "application/json"
    )
}

object ChatExample {
    val examples = listOf(
        "A 45 year old female come with high fever, cough and stuffy nose",
        "Patient reports abdominal pain, bloating, and irregular periods",
        "A 35-year-old male comes in with a persistent sore throat, swollen glands, and difficulty swallowing"
    )
}

enum class FeedbackType(val value: Int, val apiValue: String) {
    DISLIKE(0, "dislike"),
    LIKE(1, "like")
} 
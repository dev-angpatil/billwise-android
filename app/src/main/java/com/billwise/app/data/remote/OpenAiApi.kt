package com.billwise.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun categorizeTransaction(
        @Header("Authorization") authorization: String,
        @Body request: OpenAiRequest
    ): OpenAiResponse
}

data class OpenAiRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>,
    val temperature: Double = 0.3
)

data class Message(
    val role: String,
    val content: String
)

data class OpenAiResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

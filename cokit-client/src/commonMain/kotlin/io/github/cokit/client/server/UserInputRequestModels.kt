package io.github.cokit.client.server

import io.github.cokit.client.ItemId
import io.github.cokit.client.ThreadId
import io.github.cokit.client.TurnId
import kotlinx.serialization.Serializable

@Serializable
data class UserInputRequest(
    val threadId: ThreadId,
    val turnId: TurnId,
    val itemId: ItemId,
    val questions: List<UserInputQuestion>,
    val autoResolutionMs: Long? = null,
)

@Serializable
data class UserInputQuestion(
    val id: UserInputQuestionId,
    val header: String,
    val question: String,
    val options: List<UserInputOption>? = null,
    val isOther: Boolean = false,
    val isSecret: Boolean = false,
)

@Serializable
@JvmInline
value class UserInputQuestionId(val value: String)

@Serializable
data class UserInputOption(
    val label: String,
    val description: String,
)

@Serializable
data class UserInputAnswer(
    val answers: List<String>,
) {
    companion object {
        fun choice(label: String): UserInputAnswer = UserInputAnswer(listOf(label))

        fun freeForm(text: String): UserInputAnswer = UserInputAnswer(listOf(text))
    }
}

sealed interface UserInputResponse {
    data object Cancel : UserInputResponse

    data class Answers(
        val answers: Map<UserInputQuestionId, UserInputAnswer>,
    ) : UserInputResponse
}

fun interface UserInputRequestHandler {
    suspend fun respond(request: UserInputRequest): UserInputResponse
}

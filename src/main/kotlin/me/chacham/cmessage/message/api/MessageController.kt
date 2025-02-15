package me.chacham.cmessage.message.api

import me.chacham.cmessage.message.domain.Message
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/v1/messages")
class MessageController(@Value("\${app.base-url}") private val baseUrl: String) {
    @PostMapping
    suspend fun sendMessage(
        @RequestBody request: SendMessageRequest,
    ): ResponseEntity<Unit> {
        println("Message: $request")
        return ResponseEntity.created(URI.create("${baseUrl}/testId")).build()
    }

    @GetMapping
    suspend fun getMessages(
        @RequestParam("senderId") senderId: String?,
        @RequestParam("receiverId") receiverId: String?,
    ): ResponseEntity<List<Message>> {
        return ResponseEntity.ok(
            listOf(
                Message("testId", "senderId", "receiverId", "content"),
                Message("testId2", "senderId", "receiverId", "content"),
            )
        )
    }

    @GetMapping("/{messageId}")
    suspend fun getMessage(
        @PathVariable messageId: String,
    ): ResponseEntity<Message> {
        return ResponseEntity.ok(Message(messageId, "senderId", "receiverId", "content"))
    }
}

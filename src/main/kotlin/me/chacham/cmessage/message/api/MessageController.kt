package me.chacham.cmessage.message.api

import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.domain.MessageId
import me.chacham.cmessage.message.repository.MessageRepository
import me.chacham.cmessage.user.domain.UserId
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/v1/messages")
class MessageController(
    @Value("\${app.base-url}") private val baseUrl: String,
    private val messageRepository: MessageRepository,
) {
    @PostMapping
    suspend fun sendMessage(
        @RequestBody request: SendMessageRequest,
    ): ResponseEntity<SendMessageResponse> {
        val messageId = messageRepository.saveMessage(request.senderId, request.receiverId, request.content)
        return ResponseEntity.created(URI.create("${baseUrl}/api/v1/messages/${messageId}"))
            .body(SendMessageResponse(messageId))
    }

    @GetMapping
    suspend fun getMessages(
        @RequestParam("senderId") senderId: UserId?,
        @RequestParam("receiverId") receiverId: UserId?,
    ): ResponseEntity<List<Message>> {
        val messages = messageRepository.findMessages(senderId, receiverId)
        return ResponseEntity.ok(messages)
    }

    @GetMapping("/{messageId}")
    suspend fun getMessage(
        @PathVariable messageId: MessageId,
    ): ResponseEntity<Message> {
        val message = messageRepository.findMessage(messageId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(message)
    }
}

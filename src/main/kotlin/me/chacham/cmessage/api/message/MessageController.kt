package me.chacham.cmessage.api.message

import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.message.domain.Message
import me.chacham.cmessage.message.domain.MessageId
import me.chacham.cmessage.message.service.MessageService
import me.chacham.cmessage.user.domain.UserId
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/v1/messages")
class MessageController(
    @Value("\${app.base-url}") private val baseUrl: String,
    private val messageService: MessageService,
) {
    @PostMapping
    suspend fun sendMessage(
        @Validated @RequestBody request: SendMessageRequest,
    ): ResponseEntity<SendMessageResponse> {
        if (request.receiverId != null && request.groupId != null) {
            // both receiverId and groupId are present
            return ResponseEntity.badRequest().build()
        }
        if (request.receiverId != null) {
            val senderId = request.senderId
            val receiverId = request.receiverId
            val messageId = messageService.sendUserMessage(senderId, receiverId, request.content)
            return ResponseEntity.created(URI.create("${baseUrl}/api/v1/messages/${messageId}"))
                .body(SendMessageResponse(messageId))
        }
        if (request.groupId != null) {
            val senderId = request.senderId
            val groupId = request.groupId
            val messageId = messageService.sendGroupMessage(senderId, groupId, request.content)
            return ResponseEntity.created(URI.create("${baseUrl}/api/v1/messages/${messageId}"))
                .body(SendMessageResponse(messageId))
        }
        // neither receiverId nor groupId is present
        return ResponseEntity.badRequest().build()
    }

    @GetMapping
    suspend fun getMessages(
        @RequestParam("senderId") senderId: UserId?,
        @RequestParam("receiverId") receiverId: UserId?,
        @RequestParam("userIdPair") userIdPair: UserIdPair?,
        @RequestParam("groupId") groupId: GroupId?,
    ): ResponseEntity<List<Message>> {
        if (listOfNotNull(senderId, receiverId, userIdPair, groupId).size != 1) {
            return ResponseEntity.badRequest().build()
        }
        if (senderId != null) {
            val messages = messageService.findSenderMessages(senderId)
            return ResponseEntity.ok(messages)
        }
        if (receiverId != null) {
            val messages = messageService.findReceiverMessages(receiverId)
            return ResponseEntity.ok(messages)
        }
        if (userIdPair != null) {
            val messages = messageService.findSenderReceiverMessages(userIdPair.userId1, userIdPair.userId2)
            return ResponseEntity.ok(messages)
        }
        if (groupId != null) {
            val messages = messageService.findGroupMessages(groupId)
            return ResponseEntity.ok(messages)
        }
        return ResponseEntity.badRequest().build()
    }

    @GetMapping("/{messageId}")
    suspend fun getMessage(
        @PathVariable messageId: MessageId,
    ): ResponseEntity<Message> {
        val message = messageService.findMessage(messageId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(message)
    }
}

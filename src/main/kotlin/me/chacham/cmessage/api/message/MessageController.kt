package me.chacham.cmessage.api.message

import me.chacham.cmessage.address.domain.Address
import me.chacham.cmessage.address.domain.GroupAddress
import me.chacham.cmessage.address.domain.UserAddress
import me.chacham.cmessage.group.domain.GroupId
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
        val messageId = messageRepository.saveMessage(request.senderAddress, request.receiverAddress, request.content)
        return ResponseEntity.created(URI.create("${baseUrl}/api/v1/messages/${messageId}"))
            .body(SendMessageResponse(messageId))
    }

    @GetMapping
    suspend fun getMessages(
        @RequestParam("senderId") senderId: UserId?,
        @RequestParam("senderGroupId") senderGroupId: GroupId?,
        @RequestParam("senderAddress") senderAddress: Address?,
        @RequestParam("receiverId") receiverId: UserId?,
        @RequestParam("receiverGroupId") receiverGroupId: GroupId?,
        @RequestParam("receiverAddress") receiverAddress: Address?,
    ): ResponseEntity<List<Message>> {
        if (listOfNotNull(senderId, senderGroupId, senderAddress).size > 1) {
            return ResponseEntity.badRequest().build()
        }
        val senderAddr = senderAddress ?: senderId?.let { UserAddress(it) } ?: senderGroupId?.let { GroupAddress(it) }
        if (listOfNotNull(receiverId, receiverGroupId, receiverAddress).size > 1) {
            return ResponseEntity.badRequest().build()
        }
        val receiverAddr =
            receiverAddress ?: receiverId?.let { UserAddress(it) } ?: receiverGroupId?.let { GroupAddress(it) }
        val messages = messageRepository.findMessages(senderAddr, receiverAddr)
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

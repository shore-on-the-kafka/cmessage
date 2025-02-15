package me.chacham.cmessage.message.api

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/messages")
class MessageController {
    @PostMapping
    suspend fun sendMessage(
        @RequestBody message: String,
    ) {
        println("Message: $message")
    }

    @GetMapping
    suspend fun getMessages(
        @RequestParam("from") from: String?,
        @RequestParam("to") to: String?,
    ): List<String> {
        return listOf(
            "Hello from $from",
            "World to $to"
        )
    }
}

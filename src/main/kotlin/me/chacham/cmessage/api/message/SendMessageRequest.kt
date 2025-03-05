package me.chacham.cmessage.api.message

import me.chacham.cmessage.address.domain.Address

data class SendMessageRequest(val senderAddress: Address, val receiverAddress: Address, val content: String)

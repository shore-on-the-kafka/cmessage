package me.chacham.cmessage.address.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import me.chacham.cmessage.address.infra.AddressDeserializer
import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.user.domain.UserId

@JsonDeserialize(using = AddressDeserializer::class)
sealed interface Address {
    fun getUserId(): UserId?
    fun getGroupId(): GroupId?
}

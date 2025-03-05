package me.chacham.cmessage.address.infra

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import me.chacham.cmessage.address.domain.Address
import me.chacham.cmessage.address.domain.GroupAddress
import me.chacham.cmessage.address.domain.UserAddress
import me.chacham.cmessage.address.domain.UserAtGroupAddress
import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.user.domain.UserId

class AddressDeserializer : JsonDeserializer<Address>() {
    companion object {
        private val ADDRESS_REGEX = Regex("^(.+)(@(.+))$")
    }

    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Address? {
        val source = parser.text
        val matchResult = ADDRESS_REGEX.find(source)
        val userIdValue = matchResult?.groupValues?.get(1)
        val groupIdValue = matchResult?.groupValues?.get(2)
        val groupId2Value = matchResult?.groupValues?.get(3)
        println("HOLOLO $source $userIdValue $groupIdValue $groupId2Value")
        return if (groupIdValue.isNullOrBlank()) {
            if (userIdValue.isNullOrBlank()) {
                null
            } else {
                UserAddress(UserId(userIdValue))
            }
        } else {
            if (userIdValue.isNullOrBlank()) {
                GroupAddress(GroupId(groupIdValue))
            } else {
                UserAtGroupAddress(UserId(userIdValue), GroupId(groupIdValue))
            }
        }
    }
}

package me.chacham.cmessage.address.infra

import me.chacham.cmessage.address.domain.Address
import me.chacham.cmessage.address.domain.GroupAddress
import me.chacham.cmessage.address.domain.UserAddress
import me.chacham.cmessage.address.domain.UserAtGroupAddress
import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.user.domain.UserId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class StringToAddressConverter : Converter<String, Address> {
    companion object {
        private val ADDRESS_REGEX = Regex("^(.+)@(.+)$")
    }
    override fun convert(source: String): Address? {
        val matchResult = ADDRESS_REGEX.find(source)
        val userIdValue = matchResult?.groupValues?.get(1)
        val groupIdValue = matchResult?.groupValues?.get(2)
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

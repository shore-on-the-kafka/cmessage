package me.chacham.cmessage.api.util

import me.chacham.cmessage.api.message.UserIdPair
import me.chacham.cmessage.user.domain.UserId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class StringToUserIdPairConverter : Converter<String, UserIdPair> {
    override fun convert(source: String): UserIdPair? {
        val userIds = source.split(" ")
        if (userIds.size != 2) {
            return null
        }
        return UserIdPair(UserId(userIds[0]), UserId(userIds[1]))
    }
}

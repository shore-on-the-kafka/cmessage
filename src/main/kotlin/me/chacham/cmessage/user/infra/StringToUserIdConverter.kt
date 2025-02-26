package me.chacham.cmessage.user.infra

import me.chacham.cmessage.user.domain.UserId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class StringToUserIdConverter : Converter<String, UserId> {
    override fun convert(source: String): UserId? {
        return if (source.isBlank()) {
            null
        } else {
            UserId(source)
        }
    }
}

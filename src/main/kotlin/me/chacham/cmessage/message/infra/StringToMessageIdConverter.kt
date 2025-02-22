package me.chacham.cmessage.message.infra

import me.chacham.cmessage.message.domain.MessageId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class StringToMessageIdConverter : Converter<String, MessageId> {
    override fun convert(source: String): MessageId? {
        return if (source.isBlank()) {
            null
        } else {
            MessageId(source)
        }
    }
}

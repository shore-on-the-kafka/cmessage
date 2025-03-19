package me.chacham.cmessage.group.infra

import me.chacham.cmessage.group.domain.GroupId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class StringToGroupIdConverter : Converter<String, GroupId> {
    override fun convert(source: String): GroupId? {
        return if (source.isBlank()) {
            null
        } else {
            GroupId(source)
        }
    }
}

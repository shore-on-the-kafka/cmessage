package me.chacham.cmessage.api.user

import me.chacham.cmessage.group.domain.Group
import me.chacham.cmessage.group.repository.GroupRepository
import me.chacham.cmessage.user.domain.UserId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(private val groupRepository: GroupRepository) {
    @GetMapping("/me/groups")
    suspend fun getMyGroups(
        @RequestHeader("X-User-Id") userId: UserId,
    ): ResponseEntity<List<Group>> {
        val groups = groupRepository.findGroupsOfUser(userId)
        return ResponseEntity.ok(groups)
    }
}

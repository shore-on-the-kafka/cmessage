package me.chacham.cmessage.api.user

import me.chacham.cmessage.api.viewmodels.UserView
import me.chacham.cmessage.group.domain.Group
import me.chacham.cmessage.group.repository.GroupRepository
import me.chacham.cmessage.user.domain.User
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(private val groupRepository: GroupRepository) {
    @GetMapping("/me")
    suspend fun getMe(
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<UserView> {
        return ResponseEntity.ok(UserView.of(user))
    }

    @GetMapping("/me/groups")
    suspend fun getMyGroups(
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<List<Group>> {
        val groups = groupRepository.findGroupsOfUser(user.id)
        return ResponseEntity.ok(groups)
    }
}

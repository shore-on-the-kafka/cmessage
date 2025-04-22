package me.chacham.cmessage.api.group

import me.chacham.cmessage.group.domain.Group
import me.chacham.cmessage.group.domain.GroupId
import me.chacham.cmessage.group.repository.GroupRepository
import me.chacham.cmessage.user.domain.User
import me.chacham.cmessage.user.domain.UserId
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/v1/groups")
class GroupController(
    @Value("\${app.base-url}") private val baseUrl: String,
    private val groupRepository: GroupRepository,
) {
    @PostMapping
    suspend fun createGroup(
        @AuthenticationPrincipal user: User,
        @RequestBody request: CreateGroupRequest,
    ): ResponseEntity<CreateGroupResponse> {
        // TODO: Add validation for duplicated group name
        val groupId = groupRepository.saveGroup(request.name, listOf(user.id) + request.members)
        return ResponseEntity.created(URI.create("${baseUrl}/api/v1/groups/${groupId.id}"))
            .body(CreateGroupResponse(groupId))
    }

    @GetMapping("/{groupId}")
    suspend fun getGroup(
        @PathVariable("groupId") groupId: GroupId,
    ): ResponseEntity<Group> {
        val group = groupRepository.findGroup(groupId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(group)
    }

    @PostMapping("/{groupId}/members")
    suspend fun addMembers(
        @PathVariable("groupId") groupId: GroupId,
        @RequestBody request: AddMembersRequest,
    ): ResponseEntity<AddMembersResponse> {
        // TODO: Add validation for group existence
        groupRepository.addMembers(groupId, request.users)
        return ResponseEntity.created(URI.create("${baseUrl}/api/v1/groups/${groupId.id}/members"))
            .body(AddMembersResponse(groupId, request.users))
    }

    @GetMapping("/{groupId}/members")
    suspend fun getMembers(
        @PathVariable("groupId") groupId: GroupId,
    ): ResponseEntity<List<UserId>> {
        val group = groupRepository.findGroup(groupId)
        return if (group != null) {
            ResponseEntity.ok(group.members)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

package coach.spongecoach.club.adapter.in.web;

import coach.spongecoach.auth.adapter.in.web.CurrentUserContext;
import coach.spongecoach.club.application.ClubMembershipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clubs/{clubId}/members")
class ClubMembershipController {

    private final ClubMembershipService membershipService;
    private final CurrentUserContext auth;

    ClubMembershipController(ClubMembershipService membershipService, CurrentUserContext auth) {
        this.membershipService = membershipService;
        this.auth = auth;
    }

    @GetMapping
    List<ClubMembershipResponse> list(@PathVariable UUID clubId) {
        auth.requireClubMember(clubId);
        return membershipService.listMembers(clubId).stream().map(ClubMembershipResponse::from).toList();
    }

    @PutMapping("/{personId}")
    ClubMembershipResponse updateRole(
            @PathVariable UUID clubId,
            @PathVariable UUID personId,
            @Valid @RequestBody UpdateClubMemberRoleRequest request) {
        auth.requireClubAdmin(clubId);
        return ClubMembershipResponse.from(membershipService.updateRole(clubId, personId, request.role()));
    }

    @DeleteMapping("/{personId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable UUID clubId, @PathVariable UUID personId) {
        auth.requireClubAdmin(clubId);
        membershipService.removeMember(clubId, personId);
    }
}

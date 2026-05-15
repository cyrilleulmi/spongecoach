package coach.spongecoach.team.adapter.in.web;

import coach.spongecoach.auth.adapter.in.web.CurrentUserContext;
import coach.spongecoach.team.application.TeamMembershipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams/{teamId}/members")
class TeamMembershipController {

    private final TeamMembershipService membershipService;
    private final CurrentUserContext auth;

    TeamMembershipController(TeamMembershipService membershipService, CurrentUserContext auth) {
        this.membershipService = membershipService;
        this.auth = auth;
    }

    @GetMapping
    List<TeamMembershipResponse> list(@PathVariable UUID teamId) {
        auth.requireAuthenticated();
        return membershipService.listMembers(teamId).stream().map(TeamMembershipResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TeamMembershipResponse add(@PathVariable UUID teamId, @Valid @RequestBody AddMemberRequest request) {
        auth.requireAdmin();
        return TeamMembershipResponse.from(membershipService.addMember(teamId, request.personId()));
    }

    @DeleteMapping("/{personId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable UUID teamId, @PathVariable UUID personId) {
        auth.requireAdmin();
        membershipService.removeMember(teamId, personId);
    }
}

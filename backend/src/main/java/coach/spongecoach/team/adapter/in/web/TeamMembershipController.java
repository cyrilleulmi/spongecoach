package coach.spongecoach.team.adapter.in.web;

import coach.spongecoach.auth.adapter.in.web.CurrentUserContext;
import coach.spongecoach.person.application.PersonService;
import coach.spongecoach.team.application.TeamMembershipService;
import coach.spongecoach.team.application.TeamService;
import coach.spongecoach.team.domain.model.Team;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams/{teamId}/members")
class TeamMembershipController {

    private final TeamMembershipService membershipService;
    private final TeamService teamService;
    private final PersonService personService;
    private final CurrentUserContext auth;

    TeamMembershipController(TeamMembershipService membershipService, TeamService teamService,
                             PersonService personService, CurrentUserContext auth) {
        this.membershipService = membershipService;
        this.teamService = teamService;
        this.personService = personService;
        this.auth = auth;
    }

    @GetMapping
    List<TeamMemberResponse> list(@PathVariable UUID teamId) {
        Team team = teamService.getTeam(teamId);
        auth.requireClubMember(team.clubId());
        return membershipService.listMembers(teamId).stream()
                .map(m -> TeamMemberResponse.from(m, personService.getPerson(m.personId())))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TeamMemberResponse add(@PathVariable UUID teamId, @Valid @RequestBody AddMemberRequest request) {
        Team team = teamService.getTeam(teamId);
        auth.requireClubAdmin(team.clubId());
        var membership = membershipService.addMember(teamId, request.personId());
        return TeamMemberResponse.from(membership, personService.getPerson(membership.personId()));
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    List<TeamMemberResponse> addBulk(@PathVariable UUID teamId, @Valid @RequestBody AddMembersRequest request) {
        Team team = teamService.getTeam(teamId);
        auth.requireClubAdmin(team.clubId());
        return membershipService.addMembers(teamId, request.personIds()).stream()
                .map(m -> TeamMemberResponse.from(m, personService.getPerson(m.personId())))
                .toList();
    }

    @DeleteMapping("/{personId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable UUID teamId, @PathVariable UUID personId) {
        Team team = teamService.getTeam(teamId);
        auth.requireClubAdmin(team.clubId());
        membershipService.removeMember(teamId, personId);
    }
}

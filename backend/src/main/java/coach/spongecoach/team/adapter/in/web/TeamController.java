package coach.spongecoach.team.adapter.in.web;

import coach.spongecoach.auth.adapter.in.web.CurrentUserContext;
import coach.spongecoach.team.application.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
class TeamController {

    private final TeamService teamService;
    private final CurrentUserContext auth;

    TeamController(TeamService teamService, CurrentUserContext auth) {
        this.teamService = teamService;
        this.auth = auth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TeamResponse create(@Valid @RequestBody CreateTeamRequest request) {
        auth.requireAdmin();
        return TeamResponse.from(teamService.createTeam(request.name(), request.clubId()));
    }

    @GetMapping
    List<TeamResponse> list(@RequestParam(required = false) UUID clubId) {
        auth.requireAuthenticated();
        if (clubId != null) {
            return teamService.listTeamsByClub(clubId).stream().map(TeamResponse::from).toList();
        }
        return teamService.listTeams().stream().map(TeamResponse::from).toList();
    }

    @GetMapping("/{id}")
    TeamResponse get(@PathVariable UUID id) {
        auth.requireAuthenticated();
        return TeamResponse.from(teamService.getTeam(id));
    }

    @PutMapping("/{id}")
    TeamResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTeamRequest request) {
        auth.requireAdmin();
        return TeamResponse.from(teamService.updateTeam(id, request.name()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        auth.requireAdmin();
        teamService.deleteTeam(id);
    }
}

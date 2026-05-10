package coach.spongecoach.team.adapter.in.web;

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

    TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TeamResponse create(@Valid @RequestBody CreateTeamRequest request) {
        return TeamResponse.from(teamService.createTeam(request.name(), request.clubId()));
    }

    @GetMapping
    List<TeamResponse> list(@RequestParam(required = false) UUID clubId) {
        if (clubId != null) {
            return teamService.listTeamsByClub(clubId).stream().map(TeamResponse::from).toList();
        }
        return teamService.listTeams().stream().map(TeamResponse::from).toList();
    }

    @GetMapping("/{id}")
    TeamResponse get(@PathVariable UUID id) {
        return TeamResponse.from(teamService.getTeam(id));
    }

    @PutMapping("/{id}")
    TeamResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTeamRequest request) {
        return TeamResponse.from(teamService.updateTeam(id, request.name()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        teamService.deleteTeam(id);
    }
}

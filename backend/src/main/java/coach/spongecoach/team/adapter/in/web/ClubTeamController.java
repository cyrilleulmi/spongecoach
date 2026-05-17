package coach.spongecoach.team.adapter.in.web;

import coach.spongecoach.auth.adapter.in.web.CurrentUserContext;
import coach.spongecoach.team.application.TeamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clubs/{clubId}/teams")
class ClubTeamController {

    private final TeamService teamService;
    private final CurrentUserContext auth;

    ClubTeamController(TeamService teamService, CurrentUserContext auth) {
        this.teamService = teamService;
        this.auth = auth;
    }

    @GetMapping
    List<TeamResponse> list(@PathVariable UUID clubId) {
        auth.requireClubMember(clubId);
        return teamService.listTeamsByClub(clubId).stream().map(TeamResponse::from).toList();
    }
}

package coach.spongecoach.club.adapter.in.web;

import coach.spongecoach.auth.adapter.in.web.CurrentUserContext;
import coach.spongecoach.club.application.ClubService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clubs")
class ClubController {

    private final ClubService clubService;
    private final CurrentUserContext auth;

    ClubController(ClubService clubService, CurrentUserContext auth) {
        this.clubService = clubService;
        this.auth = auth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClubResponse create(@Valid @RequestBody CreateClubRequest request) {
        auth.requireAdmin();
        return ClubResponse.from(clubService.createClub(request.name(), request.location()));
    }

    @GetMapping
    List<ClubResponse> list() {
        auth.requireAuthenticated();
        return clubService.listClubs().stream().map(ClubResponse::from).toList();
    }

    @GetMapping("/{id}")
    ClubResponse get(@PathVariable UUID id) {
        auth.requireAuthenticated();
        return ClubResponse.from(clubService.getClub(id));
    }

    @PutMapping("/{id}")
    ClubResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateClubRequest request) {
        auth.requireAdmin();
        return ClubResponse.from(clubService.updateClub(id, request.name(), request.location()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        auth.requireAdmin();
        clubService.deleteClub(id);
    }
}

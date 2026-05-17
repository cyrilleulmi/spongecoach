package coach.spongecoach.club.adapter.in.web;

import coach.spongecoach.auth.adapter.in.web.CurrentUserContext;
import coach.spongecoach.auth.domain.model.AuthenticatedUser;
import coach.spongecoach.auth.domain.model.ClubMembership;
import coach.spongecoach.club.application.ClubService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.MethodNotAllowedException;

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
        AuthenticatedUser user = auth.requireAuthenticated();
        return ClubResponse.from(clubService.createClub(request.name(), request.location(), user.personId()));
    }

    @GetMapping
    List<ClubResponse> list() {
        AuthenticatedUser user = auth.requireAuthenticated();
        List<UUID> clubIds = user.clubMemberships().stream().map(ClubMembership::clubId).toList();
        return clubIds.stream()
                .map(clubService::getClub)
                .map(ClubResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    ClubResponse get(@PathVariable UUID id) {
        auth.requireClubMember(id);
        return ClubResponse.from(clubService.getClub(id));
    }

    @PutMapping("/{id}")
    ClubResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateClubRequest request) {
        auth.requireClubAdmin(id);
        return ClubResponse.from(clubService.updateClub(id, request.name(), request.location()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    void delete(@PathVariable UUID id) {
        throw new MethodNotAllowedException("DELETE", null);
    }
}

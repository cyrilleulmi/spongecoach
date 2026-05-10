package coach.spongecoach.club.adapter.in.web;

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

    ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClubResponse create(@Valid @RequestBody CreateClubRequest request) {
        return ClubResponse.from(clubService.createClub(request.name(), request.location()));
    }

    @GetMapping
    List<ClubResponse> list() {
        return clubService.listClubs().stream().map(ClubResponse::from).toList();
    }

    @GetMapping("/{id}")
    ClubResponse get(@PathVariable UUID id) {
        return ClubResponse.from(clubService.getClub(id));
    }

    @PutMapping("/{id}")
    ClubResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateClubRequest request) {
        return ClubResponse.from(clubService.updateClub(id, request.name(), request.location()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        clubService.deleteClub(id);
    }
}

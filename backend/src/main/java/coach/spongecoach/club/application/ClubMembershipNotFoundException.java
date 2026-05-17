package coach.spongecoach.club.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ClubMembershipNotFoundException extends RuntimeException {
    public ClubMembershipNotFoundException(UUID clubId, UUID personId) {
        super("No membership found for person " + personId + " in club " + clubId);
    }
}

package coach.spongecoach.club.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ClubNotFoundException extends RuntimeException {
    public ClubNotFoundException(UUID id) {
        super("Club not found: " + id);
    }
}

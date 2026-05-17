package coach.spongecoach.club.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class NotAClubMemberException extends RuntimeException {
    public NotAClubMemberException(UUID personId, UUID clubId) {
        super("Person " + personId + " is not a member of club " + clubId);
    }
}

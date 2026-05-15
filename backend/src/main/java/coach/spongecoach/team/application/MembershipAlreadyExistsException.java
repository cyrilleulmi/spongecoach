package coach.spongecoach.team.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.CONFLICT)
public class MembershipAlreadyExistsException extends RuntimeException {
    public MembershipAlreadyExistsException(UUID teamId, UUID personId) {
        super("Person " + personId + " is already a member of team " + teamId);
    }
}

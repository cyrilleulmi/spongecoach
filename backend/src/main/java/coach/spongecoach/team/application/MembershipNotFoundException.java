package coach.spongecoach.team.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class MembershipNotFoundException extends RuntimeException {
    public MembershipNotFoundException(UUID teamId, UUID personId) {
        super("Person " + personId + " is not a member of team " + teamId);
    }
}

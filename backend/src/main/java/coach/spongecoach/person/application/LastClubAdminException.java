package coach.spongecoach.person.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@ResponseStatus(HttpStatus.CONFLICT)

public class LastClubAdminException extends RuntimeException {

    private final List<String> clubNames;

    public LastClubAdminException(List<String> clubNames) {
        super("Cannot delete account: last admin of club(s): " + String.join(", ", clubNames));
        this.clubNames = clubNames;
    }

    public List<String> clubNames() {
        return clubNames;
    }
}

package coach.spongecoach.person.domain;

import java.util.List;
import java.util.UUID;

/** Port: returns names of clubs where the given person is the sole admin, blocking deletion. */
public interface DeletePersonGuard {
    List<String> findBlockingClubs(UUID personId);
}

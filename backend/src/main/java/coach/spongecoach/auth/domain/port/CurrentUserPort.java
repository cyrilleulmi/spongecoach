package coach.spongecoach.auth.domain.port;

import coach.spongecoach.auth.domain.model.AuthenticatedUser;

import java.util.Optional;

public interface CurrentUserPort {
    Optional<AuthenticatedUser> resolve(String email);
}

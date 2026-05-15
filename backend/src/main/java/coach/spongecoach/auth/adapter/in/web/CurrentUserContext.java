package coach.spongecoach.auth.adapter.in.web;

import coach.spongecoach.auth.application.ForbiddenException;
import coach.spongecoach.auth.application.UnauthorizedException;
import coach.spongecoach.auth.domain.model.AuthenticatedUser;
import coach.spongecoach.auth.domain.model.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CurrentUserContext {

    private final HttpServletRequest request;

    CurrentUserContext(HttpServletRequest request) {
        this.request = request;
    }

    public Optional<AuthenticatedUser> currentUser() {
        return Optional.ofNullable((AuthenticatedUser) request.getAttribute(AuthFilter.ATTRIBUTE));
    }

    public AuthenticatedUser requireAuthenticated() {
        return currentUser().orElseThrow(UnauthorizedException::new);
    }

    public AuthenticatedUser requireAdmin() {
        AuthenticatedUser user = requireAuthenticated();
        if (user.role() != Role.ADMIN) {
            throw new ForbiddenException();
        }
        return user;
    }

    public AuthenticatedUser requireSelfOrAdmin(UUID personId) {
        AuthenticatedUser user = requireAuthenticated();
        if (user.role() == Role.ADMIN || user.personId().equals(personId)) {
            return user;
        }
        throw new ForbiddenException();
    }
}

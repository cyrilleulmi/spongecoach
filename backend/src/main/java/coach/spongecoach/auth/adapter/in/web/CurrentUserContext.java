package coach.spongecoach.auth.adapter.in.web;

import coach.spongecoach.auth.application.ForbiddenException;
import coach.spongecoach.auth.application.UnauthorizedException;
import coach.spongecoach.auth.domain.model.AuthenticatedUser;
import coach.spongecoach.auth.domain.model.ClubRole;
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

    public AuthenticatedUser requireClubMember(UUID clubId) {
        AuthenticatedUser user = requireAuthenticated();
        boolean isMember = user.clubMemberships().stream()
                .anyMatch(m -> m.clubId().equals(clubId));
        if (!isMember) throw new ForbiddenException();
        return user;
    }

    public AuthenticatedUser requireClubAdmin(UUID clubId) {
        AuthenticatedUser user = requireAuthenticated();
        boolean isAdmin = user.clubMemberships().stream()
                .anyMatch(m -> m.clubId().equals(clubId) && m.role() == ClubRole.ADMIN);
        if (!isAdmin) throw new ForbiddenException();
        return user;
    }
}

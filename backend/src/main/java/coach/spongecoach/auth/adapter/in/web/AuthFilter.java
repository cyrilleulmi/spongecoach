package coach.spongecoach.auth.adapter.in.web;

import coach.spongecoach.auth.domain.port.CurrentUserPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Mock-User-Id";
    static final String ATTRIBUTE = "currentUser";

    private final CurrentUserPort currentUserPort;

    AuthFilter(CurrentUserPort currentUserPort) {
        this.currentUserPort = currentUserPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String email = request.getHeader(HEADER);
        if (email != null && !email.isBlank()) {
            currentUserPort.resolve(email)
                    .ifPresent(user -> request.setAttribute(ATTRIBUTE, user));
        }
        chain.doFilter(request, response);
    }
}

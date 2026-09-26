package com.spongecoach.auth;

import com.spongecoach.api.dto.ErrorResponse;
import com.spongecoach.domain.AppUser;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;

/**
 * Stand-in for authentication (ADR-0017): the User is whoever the {@value #COOKIE} cookie names,
 * as picked in the frontend's user dropdown. A cookie rather than a header so that {@code <img>}
 * Avatar requests carry it too. Every {@code /api} call needs a known User — except listing the
 * Users, which is what the dropdown is filled from. Real authentication later replaces this class
 * only; {@link Access} keeps deciding what the User may do.
 */
@Provider
public class UserCookieFilter implements ContainerRequestFilter {

    public static final String COOKIE = "spongecoach-user";

    @Inject
    CurrentUser currentUser;

    @Override
    public void filter(ContainerRequestContext request) {
        String path = request.getUriInfo().getPath();
        if (!path.startsWith("/api/") || isUserList(request, path)) {
            return;
        }
        AppUser user = find(request.getCookies().get(COOKIE));
        if (user == null) {
            request.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorResponse("unauthenticated", "choose a user first"))
                    .build());
            return;
        }
        currentUser.set(user);
    }

    private static boolean isUserList(ContainerRequestContext request, String path) {
        return "GET".equals(request.getMethod()) && path.equals("/api/users");
    }

    private static AppUser find(Cookie cookie) {
        if (cookie == null) {
            return null;
        }
        try {
            return AppUser.findById(UUID.fromString(cookie.getValue()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

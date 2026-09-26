package com.spongecoach.api;

import com.spongecoach.api.dto.CurrentUserDto;
import com.spongecoach.api.dto.UserDto;
import com.spongecoach.auth.CurrentUser;
import com.spongecoach.domain.AppUser;
import com.spongecoach.domain.Player;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

/**
 * The seeded Users (ADR-0017). {@code GET /api/users} is the one call that needs no User, since it
 * fills the dropdown a User is picked from; it goes away once real authentication arrives.
 */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    CurrentUser currentUser;

    @GET
    @Path("/users")
    public List<UserDto> list() {
        return AppUser.listAllOrdered().stream().map(UserDto::from).toList();
    }

    /** Who the request acts as, with the active Lines their Player is on — what the UI gates on. */
    @GET
    @Path("/me")
    @Transactional
    public CurrentUserDto me() {
        AppUser user = AppUser.findById(currentUser.id());
        List<UUID> lineIds = List.of();
        if (user.playerId != null) {
            Player player = Player.findById(user.playerId);
            lineIds = player.activeLines().stream().map(line -> line.id).toList();
        }
        return CurrentUserDto.from(user, lineIds);
    }
}

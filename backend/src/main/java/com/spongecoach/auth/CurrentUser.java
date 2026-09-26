package com.spongecoach.auth;

import com.spongecoach.domain.AppUser;
import com.spongecoach.domain.Role;
import jakarta.enterprise.context.RequestScoped;

import java.util.UUID;

/**
 * Who this request acts as, resolved once by {@link UserCookieFilter}. Holds plain values, not the
 * entity, so it stays valid across the resource's own transaction.
 */
@RequestScoped
public class CurrentUser {

    private UUID id;
    private Role role;
    private UUID teamId;
    private UUID playerId;

    void set(AppUser user) {
        this.id = user.id;
        this.role = user.role;
        this.teamId = user.teamId;
        this.playerId = user.playerId;
    }

    public UUID id() {
        return id;
    }

    public Role role() {
        return role;
    }

    public UUID teamId() {
        return teamId;
    }

    /** The Player this User acts as, or null. */
    public UUID playerId() {
        return playerId;
    }
}

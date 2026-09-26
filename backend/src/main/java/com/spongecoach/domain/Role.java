package com.spongecoach.domain;

/** What an {@link AppUser} may do (ADR-0017). */
public enum Role {
    /** Everything, in every Team — including support endpoints no one else may call. */
    SYS_ADMIN,
    /** Almost everything inside their own Team. */
    COACH,
    /** Their own Player, their own Lines, and their own attendance. */
    PLAYER
}

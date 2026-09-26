package com.spongecoach.auth;

import com.spongecoach.api.AccessDeniedException;
import com.spongecoach.domain.Event;
import com.spongecoach.domain.Line;
import com.spongecoach.domain.Player;
import com.spongecoach.domain.Role;
import com.spongecoach.domain.Team;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Decides whether the current User may make a write (ADR-0017). Reads are open to every User.
 *
 * <ul>
 *   <li>A SysAdmin may do everything.
 *   <li>A Coach may do everything inside their own Team.
 *   <li>A Player may edit their own Player, the Lines they are on (by current roster), those Lines'
 *       Focus per Event, and their own attendance — the last two not on a done Event.
 * </ul>
 *
 * Each check throws {@link AccessDeniedException}; call it after the row is found, so an unknown id
 * is still a 404.
 */
@RequestScoped
public class Access {

    @Inject
    CurrentUser user;

    public boolean isCoachOf(UUID teamId) {
        return user.role() == Role.SYS_ADMIN
                || (user.role() == Role.COACH && teamId != null && teamId.equals(user.teamId()));
    }

    /** Team-level management: creating, deleting and restoring Lines; Iterations and Events. */
    public void requireCoach(Team team) {
        if (!isCoachOf(team.id)) {
            throw new AccessDeniedException("only a coach of this team may do this");
        }
    }

    /** Editing a Line: its name, roster, Skill ratings and Development goals. */
    public void requireLineMember(Line line) {
        if (!isCoachOf(line.team.id) && !isOnLine(line)) {
            throw new AccessDeniedException("only a coach or a player on line " + line.id + " may edit it");
        }
    }

    /** Editing a Player's profile: Player ratings, Player development goals, Avatar. */
    public void requireSelf(Player player) {
        if (!isCoachOf(player.team.id) && !player.id.equals(user.playerId())) {
            throw new AccessDeniedException("only a coach or player " + player.id + " themselves may edit this player");
        }
    }

    /** Setting one Line's Focus for an Event. */
    public void requireFocus(Event event, Line line) {
        if (isCoachOf(event.iteration.team.id)) {
            return;
        }
        if (!isOnLine(line)) {
            throw new AccessDeniedException("only a coach or a player on line " + line.id + " may set its focus");
        }
        requireNotDone(event);
    }

    /** Answering attendance for a Player on an Event. */
    public void requireAttendance(Event event, UUID playerId) {
        if (isCoachOf(event.iteration.team.id)) {
            return;
        }
        if (!playerId.equals(user.playerId())) {
            throw new AccessDeniedException("a player may only answer for themselves");
        }
        requireNotDone(event);
    }

    private boolean isOnLine(Line line) {
        UUID playerId = user.playerId();
        return playerId != null
                && line.deletedAt == null
                && line.players.stream().anyMatch(p -> p.id.equals(playerId));
    }

    /** A done Event is history: only a coach may still change it (ADR-0012, ADR-0017). */
    private void requireNotDone(Event event) {
        if (event.scheduledOn.isBefore(LocalDateTime.now())) {
            throw new AccessDeniedException("event " + event.id + " is done; only a coach may change it");
        }
    }
}

package coach.spongecoach.club.domain;

import java.util.UUID;

/** Port: removes a person from all teams within a given club. */
public interface TeamMembershipCleaner {
    void removePersonFromClubTeams(UUID clubId, UUID personId);
}

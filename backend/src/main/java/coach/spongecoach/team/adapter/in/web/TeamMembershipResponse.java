package coach.spongecoach.team.adapter.in.web;

import coach.spongecoach.team.domain.model.TeamMembership;

import java.util.UUID;

record TeamMembershipResponse(UUID teamId, UUID personId) {
    static TeamMembershipResponse from(TeamMembership membership) {
        return new TeamMembershipResponse(membership.teamId(), membership.personId());
    }
}

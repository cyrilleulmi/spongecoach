package coach.spongecoach.club.adapter.in.web;

import coach.spongecoach.auth.domain.model.ClubMembership;
import coach.spongecoach.auth.domain.model.ClubRole;

import java.util.UUID;

record ClubMembershipResponse(UUID clubId, UUID personId, ClubRole role) {

    static ClubMembershipResponse from(ClubMembership membership) {
        return new ClubMembershipResponse(membership.clubId(), membership.personId(), membership.role());
    }
}

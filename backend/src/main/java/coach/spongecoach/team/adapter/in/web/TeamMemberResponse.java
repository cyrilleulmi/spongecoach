package coach.spongecoach.team.adapter.in.web;

import coach.spongecoach.person.domain.model.Person;
import coach.spongecoach.team.domain.model.TeamMembership;

import java.util.UUID;

record TeamMemberResponse(UUID teamId, UUID personId, String firstName, String lastName, String email) {

    static TeamMemberResponse from(TeamMembership membership, Person person) {
        return new TeamMemberResponse(
                membership.teamId(),
                membership.personId(),
                person.firstName(),
                person.lastName(),
                person.email()
        );
    }
}

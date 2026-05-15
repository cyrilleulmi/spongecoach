package coach.spongecoach.team.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "team_membership")
class TeamMembershipJpaEntity {

    @EmbeddedId
    private TeamMembershipId id;

    protected TeamMembershipJpaEntity() {}

    TeamMembershipJpaEntity(UUID teamId, UUID personId) {
        this.id = new TeamMembershipId(teamId, personId);
    }

    UUID getTeamId() { return id.teamId; }
    UUID getPersonId() { return id.personId; }

    @Embeddable
    static class TeamMembershipId implements Serializable {

        @Column(name = "team_id", columnDefinition = "uuid")
        private UUID teamId;

        @Column(name = "person_id", columnDefinition = "uuid")
        private UUID personId;

        protected TeamMembershipId() {}

        TeamMembershipId(UUID teamId, UUID personId) {
            this.teamId = teamId;
            this.personId = personId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TeamMembershipId that)) return false;
            return Objects.equals(teamId, that.teamId) && Objects.equals(personId, that.personId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(teamId, personId);
        }
    }
}

package coach.spongecoach.club.adapter.out.persistence;

import coach.spongecoach.auth.domain.model.ClubRole;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "club_membership")
class ClubMembershipJpaEntity {

    @EmbeddedId
    private ClubMembershipId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubRole role;

    protected ClubMembershipJpaEntity() {}

    ClubMembershipJpaEntity(UUID clubId, UUID personId, ClubRole role) {
        this.id = new ClubMembershipId(clubId, personId);
        this.role = role;
    }

    UUID getClubId() { return id.clubId; }
    UUID getPersonId() { return id.personId; }
    ClubRole getRole() { return role; }
    void setRole(ClubRole role) { this.role = role; }

    @Embeddable
    static class ClubMembershipId implements Serializable {

        @Column(name = "club_id", columnDefinition = "uuid")
        private UUID clubId;

        @Column(name = "person_id", columnDefinition = "uuid")
        private UUID personId;

        protected ClubMembershipId() {}

        ClubMembershipId(UUID clubId, UUID personId) {
            this.clubId = clubId;
            this.personId = personId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClubMembershipId that)) return false;
            return Objects.equals(clubId, that.clubId) && Objects.equals(personId, that.personId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clubId, personId);
        }
    }
}

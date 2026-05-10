package coach.spongecoach.team.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "team")
class TeamJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "club_id", nullable = false, columnDefinition = "uuid")
    private UUID clubId;

    protected TeamJpaEntity() {}

    TeamJpaEntity(UUID id, String name, UUID clubId) {
        this.id = id;
        this.name = name;
        this.clubId = clubId;
    }

    UUID getId() { return id; }
    String getName() { return name; }
    UUID getClubId() { return clubId; }
    void setName(String name) { this.name = name; }
}

package coach.spongecoach.club.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "club")
class ClubJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    protected ClubJpaEntity() {}

    ClubJpaEntity(UUID id, String name, String location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }

    UUID getId() { return id; }
    String getName() { return name; }
    String getLocation() { return location; }
    void setName(String name) { this.name = name; }
    void setLocation(String location) { this.location = location; }
}

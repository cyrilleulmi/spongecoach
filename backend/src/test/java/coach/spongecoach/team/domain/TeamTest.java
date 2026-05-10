package coach.spongecoach.team.domain;

import coach.spongecoach.team.domain.model.Team;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TeamTest {

    @Test
    void create_assignsRandomId() {
        UUID clubId = UUID.randomUUID();
        Team a = Team.create("U17", clubId);
        Team b = Team.create("U17", clubId);
        assertThat(a.id()).isNotNull();
        assertThat(a.id()).isNotEqualTo(b.id());
    }

    @Test
    void withName_returnsNewInstanceWithUpdatedName() {
        UUID clubId = UUID.randomUUID();
        Team original = Team.create("Old", clubId);
        Team updated = original.withName("New");
        assertThat(updated.name()).isEqualTo("New");
        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.clubId()).isEqualTo(clubId);
    }
}

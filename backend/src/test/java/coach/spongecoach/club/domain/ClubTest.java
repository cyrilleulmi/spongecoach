package coach.spongecoach.club.domain;

import coach.spongecoach.club.domain.model.Club;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClubTest {

    @Test
    void create_assignsRandomId() {
        Club a = Club.create("UHC Test", "Malters");
        Club b = Club.create("UHC Test", "Malters");
        assertThat(a.id()).isNotNull();
        assertThat(a.id()).isNotEqualTo(b.id());
    }

    @Test
    void withName_returnsNewInstanceWithUpdatedName() {
        Club original = Club.create("Old Name", "Köniz");
        Club updated = original.withName("New Name");
        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.location()).isEqualTo(original.location());
    }

    @Test
    void withLocation_returnsNewInstanceWithUpdatedLocation() {
        Club original = Club.create("UHC Test", "Bern");
        Club updated = original.withLocation("Basel");
        assertThat(updated.location()).isEqualTo("Basel");
        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.name()).isEqualTo(original.name());
    }
}

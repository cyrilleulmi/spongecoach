package coach.spongecoach.club.application;

import coach.spongecoach.club.domain.ClubRepository;
import coach.spongecoach.club.domain.model.Club;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClubServiceTest {

    private ClubService service;

    @BeforeEach
    void setUp() {
        service = new ClubService(new InMemoryClubRepository());
    }

    @Test
    void createClub_persistsAndReturnsClub() {
        Club club = service.createClub("UHC Test", "Zurich");
        assertThat(club.id()).isNotNull();
        assertThat(club.name()).isEqualTo("UHC Test");
        assertThat(club.location()).isEqualTo("Malters");
    }

    @Test
    void getClub_returnsExistingClub() {
        Club created = service.createClub("UHC Test", "Malters");
        Club found = service.getClub(created.id());
        assertThat(found).isEqualTo(created);
    }

    @Test
    void getClub_throwsWhenNotFound() {
        UUID unknown = UUID.randomUUID();
        assertThatThrownBy(() -> service.getClub(unknown))
                .isInstanceOf(ClubNotFoundException.class);
    }

    @Test
    void listClubs_returnsAllClubs() {
        service.createClub("UHC Malters", "Malters");
        service.createClub("UHC Köniz", "Köniz");
        assertThat(service.listClubs()).hasSize(2);
    }

    @Test
    void updateClub_updatesNameAndLocation() {
        Club original = service.createClub("Old", "Old City");
        Club updated = service.updateClub(original.id(), "New", "New City");
        assertThat(updated.name()).isEqualTo("New");
        assertThat(updated.location()).isEqualTo("New City");
        assertThat(updated.id()).isEqualTo(original.id());
    }

    @Test
    void updateClub_throwsWhenNotFound() {
        assertThatThrownBy(() -> service.updateClub(UUID.randomUUID(), "X", "Y"))
                .isInstanceOf(ClubNotFoundException.class);
    }

    @Test
    void deleteClub_removesClub() {
        Club club = service.createClub("UHC Delete", "Lausanne");
        service.deleteClub(club.id());
        assertThatThrownBy(() -> service.getClub(club.id()))
                .isInstanceOf(ClubNotFoundException.class);
    }

    @Test
    void deleteClub_throwsWhenNotFound() {
        assertThatThrownBy(() -> service.deleteClub(UUID.randomUUID()))
                .isInstanceOf(ClubNotFoundException.class);
    }

    static class InMemoryClubRepository implements ClubRepository {
        private final Map<UUID, Club> store = new HashMap<>();

        @Override
        public Club save(Club club) {
            store.put(club.id(), club);
            return club;
        }

        @Override
        public Optional<Club> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Club> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public boolean existsById(UUID id) {
            return store.containsKey(id);
        }

        @Override
        public void delete(UUID id) {
            store.remove(id);
        }
    }
}

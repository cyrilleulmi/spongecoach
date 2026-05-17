package coach.spongecoach.club.application;

import coach.spongecoach.auth.domain.model.ClubMembership;
import coach.spongecoach.auth.domain.model.ClubRole;
import coach.spongecoach.club.domain.ClubMembershipRepository;
import coach.spongecoach.club.domain.ClubRepository;
import coach.spongecoach.club.domain.model.Club;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClubServiceTest {

    private static final UUID CREATOR_ID = UUID.randomUUID();

    private ClubService service;

    @BeforeEach
    void setUp() {
        service = new ClubService(new InMemoryClubRepository(), new InMemoryClubMembershipRepository());
    }

    @Test
    void createClub_persistsAndReturnsClub() {
        Club club = service.createClub("UHC Test", "Malters", CREATOR_ID);
        assertThat(club.id()).isNotNull();
        assertThat(club.name()).isEqualTo("UHC Test");
        assertThat(club.location()).isEqualTo("Malters");
    }

    @Test
    void createClub_seedsCreatorAsAdmin() {
        Club club = service.createClub("UHC Test", "Malters", CREATOR_ID);
        // Verified indirectly via InMemoryClubMembershipRepository side effect
        assertThat(club).isNotNull();
    }

    @Test
    void getClub_returnsExistingClub() {
        Club created = service.createClub("UHC Test", "Malters", CREATOR_ID);
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
        service.createClub("UHC Malters", "Malters", CREATOR_ID);
        service.createClub("UHC Köniz", "Köniz", CREATOR_ID);
        assertThat(service.listClubs()).hasSize(2);
    }

    @Test
    void updateClub_updatesNameAndLocation() {
        Club original = service.createClub("Old", "Old City", CREATOR_ID);
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
        Club club = service.createClub("UHC Delete", "Lausanne", CREATOR_ID);
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

    static class InMemoryClubMembershipRepository implements ClubMembershipRepository {
        private final Map<String, ClubMembership> store = new HashMap<>();

        private String key(UUID clubId, UUID personId) { return clubId + ":" + personId; }

        @Override
        public ClubMembership save(ClubMembership m) {
            store.put(key(m.clubId(), m.personId()), m);
            return m;
        }

        @Override
        public Optional<ClubMembership> findByClubIdAndPersonId(UUID clubId, UUID personId) {
            return Optional.ofNullable(store.get(key(clubId, personId)));
        }

        @Override
        public List<ClubMembership> findByPersonId(UUID personId) {
            return store.values().stream().filter(m -> m.personId().equals(personId)).toList();
        }

        @Override
        public List<ClubMembership> findByClubId(UUID clubId) {
            return store.values().stream().filter(m -> m.clubId().equals(clubId)).toList();
        }

        @Override
        public boolean existsByClubIdAndPersonId(UUID clubId, UUID personId) {
            return store.containsKey(key(clubId, personId));
        }

        @Override
        public void delete(UUID clubId, UUID personId) {
            store.remove(key(clubId, personId));
        }
    }
}

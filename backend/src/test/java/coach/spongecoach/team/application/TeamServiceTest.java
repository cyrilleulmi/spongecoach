package coach.spongecoach.team.application;

import coach.spongecoach.club.application.ClubNotFoundException;
import coach.spongecoach.club.domain.ClubRepository;
import coach.spongecoach.club.domain.model.Club;
import coach.spongecoach.team.domain.TeamRepository;
import coach.spongecoach.team.domain.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamServiceTest {

    private TeamService service;
    private InMemoryClubRepository clubRepo;

    @BeforeEach
    void setUp() {
        clubRepo = new InMemoryClubRepository();
        service = new TeamService(new InMemoryTeamRepository(), clubRepo);
    }

    @Test
    void createTeam_persistsAndReturnsTeam() {
        Club club = clubRepo.save(Club.create("UHC Test", "Malters"));
        Team team = service.createTeam("Herren 1", club.id());
        assertThat(team.id()).isNotNull();
        assertThat(team.name()).isEqualTo("Herren 1");
        assertThat(team.clubId()).isEqualTo(club.id());
    }

    @Test
    void createTeam_throwsWhenClubNotFound() {
        assertThatThrownBy(() -> service.createTeam("Herren 1", UUID.randomUUID()))
                .isInstanceOf(ClubNotFoundException.class);
    }

    @Test
    void getTeam_throwsWhenNotFound() {
        assertThatThrownBy(() -> service.getTeam(UUID.randomUUID()))
                .isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    void updateTeam_updatesName() {
        Club club = clubRepo.save(Club.create("UHC Test", "Malters"));
        Team original = service.createTeam("Old", club.id());
        Team updated = service.updateTeam(original.id(), "New");
        assertThat(updated.name()).isEqualTo("New");
        assertThat(updated.id()).isEqualTo(original.id());
    }

    static class InMemoryTeamRepository implements TeamRepository {
        private final Map<UUID, Team> store = new HashMap<>();

        @Override
        public Team save(Team team) { store.put(team.id(), team); return team; }

        @Override
        public Optional<Team> findById(UUID id) { return Optional.ofNullable(store.get(id)); }

        @Override
        public List<Team> findAll() { return List.copyOf(store.values()); }

        @Override
        public List<Team> findByClubId(UUID clubId) {
            return store.values().stream().filter(t -> t.clubId().equals(clubId)).toList();
        }

        @Override
        public List<Team> findByMemberId(UUID personId) { return List.of(); }

        @Override
        public boolean existsById(UUID id) { return store.containsKey(id); }

        @Override
        public void delete(UUID id) { store.remove(id); }
    }

    static class InMemoryClubRepository implements ClubRepository {
        private final Map<UUID, Club> store = new HashMap<>();

        @Override
        public Club save(Club club) { store.put(club.id(), club); return club; }

        @Override
        public Optional<Club> findById(UUID id) { return Optional.ofNullable(store.get(id)); }

        @Override
        public List<Club> findAll() { return List.copyOf(store.values()); }

        @Override
        public boolean existsById(UUID id) { return store.containsKey(id); }

        @Override
        public void delete(UUID id) { store.remove(id); }
    }
}

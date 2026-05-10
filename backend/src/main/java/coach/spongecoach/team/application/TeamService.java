package coach.spongecoach.team.application;

import coach.spongecoach.club.domain.ClubRepository;
import coach.spongecoach.club.application.ClubNotFoundException;
import coach.spongecoach.team.domain.TeamRepository;
import coach.spongecoach.team.domain.model.Team;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final ClubRepository clubRepository;

    public TeamService(TeamRepository teamRepository, ClubRepository clubRepository) {
        this.teamRepository = teamRepository;
        this.clubRepository = clubRepository;
    }

    public Team createTeam(String name, UUID clubId) {
        if (!clubRepository.existsById(clubId)) {
            throw new ClubNotFoundException(clubId);
        }
        return teamRepository.save(Team.create(name, clubId));
    }

    @Transactional(readOnly = true)
    public Team getTeam(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Team> listTeams() {
        return teamRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Team> listTeamsByClub(UUID clubId) {
        return teamRepository.findByClubId(clubId);
    }

    public Team updateTeam(UUID id, String name) {
        Team existing = teamRepository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));
        return teamRepository.save(existing.withName(name));
    }

    public void deleteTeam(UUID id) {
        teamRepository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));
        teamRepository.delete(id);
    }
}

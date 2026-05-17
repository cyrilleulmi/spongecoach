package coach.spongecoach.team.adapter.in.web;

import coach.spongecoach.auth.domain.model.AuthenticatedUser;
import coach.spongecoach.auth.domain.model.ClubMembership;
import coach.spongecoach.auth.domain.model.ClubRole;
import coach.spongecoach.auth.domain.port.CurrentUserPort;
import coach.spongecoach.team.application.TeamNotFoundException;
import coach.spongecoach.team.application.TeamService;
import coach.spongecoach.team.domain.model.Team;
import coach.spongecoach.auth.adapter.in.web.CurrentUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeamController.class)
@Import(CurrentUserContext.class)
class TeamControllerTest {

    static final String ADMIN_EMAIL = "admin@test.ch";
    static final String USER_EMAIL = "user@test.ch";
    static final UUID CLUB_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    static final UUID ADMIN_PERSON_ID = UUID.fromString("a0000000-0000-0000-0000-000000000002");
    static final UUID USER_PERSON_ID = UUID.fromString("a0000000-0000-0000-0000-000000000003");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TeamService teamService;
    @MockitoBean CurrentUserPort currentUserPort;

    @BeforeEach
    void setUpAuth() {
        when(currentUserPort.resolve(ADMIN_EMAIL))
                .thenReturn(Optional.of(new AuthenticatedUser(ADMIN_PERSON_ID, ADMIN_EMAIL,
                        List.of(new ClubMembership(CLUB_ID, ADMIN_PERSON_ID, ClubRole.ADMIN)))));
        when(currentUserPort.resolve(USER_EMAIL))
                .thenReturn(Optional.of(new AuthenticatedUser(USER_PERSON_ID, USER_EMAIL,
                        List.of(new ClubMembership(CLUB_ID, USER_PERSON_ID, ClubRole.MEMBER)))));
    }

    @Test
    void POST_teams_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(teamService.createTeam("Herren 1", CLUB_ID))
                .thenReturn(new Team(id, "Herren 1", CLUB_ID));

        mockMvc.perform(post("/api/teams")
                        .header("X-Mock-User-Id", ADMIN_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTeamRequest("Herren 1", CLUB_ID))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Herren 1"))
                .andExpect(jsonPath("$.clubId").value(CLUB_ID.toString()));
    }

    @Test
    void POST_teams_returns401WithoutAuth() throws Exception {
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTeamRequest("Herren 1", CLUB_ID))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void POST_teams_returns403ForMember() throws Exception {
        mockMvc.perform(post("/api/teams")
                        .header("X-Mock-User-Id", USER_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTeamRequest("Herren 1", CLUB_ID))))
                .andExpect(status().isForbidden());
    }

    @Test
    void GET_teams_returnsUserTeams() throws Exception {
        when(teamService.listTeamsByMember(USER_PERSON_ID)).thenReturn(List.of(
                new Team(UUID.randomUUID(), "Herren 1", CLUB_ID)
        ));

        mockMvc.perform(get("/api/teams")
                        .header("X-Mock-User-Id", USER_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void GET_teams_returns401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_team_returns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(teamService.getTeam(id)).thenThrow(new TeamNotFoundException(id));

        mockMvc.perform(get("/api/teams/{id}", id)
                        .header("X-Mock-User-Id", USER_EMAIL))
                .andExpect(status().isNotFound());
    }
}

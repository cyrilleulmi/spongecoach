package coach.spongecoach.team.adapter.in.web;

import coach.spongecoach.team.application.TeamNotFoundException;
import coach.spongecoach.team.application.TeamService;
import coach.spongecoach.team.domain.model.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeamController.class)
class TeamControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TeamService teamService;

    @Test
    void POST_teams_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        when(teamService.createTeam("Herren 1", clubId))
                .thenReturn(new Team(id, "Herren 1", clubId));

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTeamRequest("Herren 1", clubId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Herren 1"))
                .andExpect(jsonPath("$.clubId").value(clubId.toString()));
    }

    @Test
    void GET_teams_returnsAll() throws Exception {
        UUID clubId = UUID.randomUUID();
        when(teamService.listTeams()).thenReturn(List.of(
                new Team(UUID.randomUUID(), "Herren 1", clubId)
        ));

        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void GET_team_returns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(teamService.getTeam(id)).thenThrow(new TeamNotFoundException(id));

        mockMvc.perform(get("/api/teams/{id}", id))
                .andExpect(status().isNotFound());
    }
}

package coach.spongecoach.team;

import coach.spongecoach.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TeamIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void createTeam_requiresExistingClub() throws Exception {
        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Herren 1\",\"clubId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void fullCrudLifecycle() throws Exception {
        // Create a club first
        MvcResult clubResult = mockMvc.perform(post("/api/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"UHC Teams Test\",\"location\":\"Malters\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String clubId = clubResult.getResponse().getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        // Create team
        MvcResult teamResult = mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Herren 1\",\"clubId\":\"" + clubId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clubId").value(clubId))
                .andReturn();

        String teamId = teamResult.getResponse().getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        // List teams by club
        mockMvc.perform(get("/api/teams?clubId=" + clubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Delete team
        mockMvc.perform(delete("/api/teams/" + teamId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/teams/" + teamId))
                .andExpect(status().isNotFound());
    }
}

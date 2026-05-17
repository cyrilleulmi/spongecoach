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

    static final String ADMIN_USER = "cyrille.ulmi@eintracht-beromunster.ch";
    static final String BEROMUNSTER_ID = "a1b2c3d4-0001-0001-0001-000000000001";

    @Autowired MockMvc mockMvc;

    @Test
    void fullCrudLifecycle() throws Exception {
        // Create team in Eintracht Beromünster (Cyrille is admin)
        MvcResult teamResult = mockMvc.perform(post("/api/teams")
                        .header("X-Mock-User-Id", ADMIN_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Herren Integration\",\"clubId\":\"" + BEROMUNSTER_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clubId").value(BEROMUNSTER_ID))
                .andReturn();

        String teamId = teamResult.getResponse().getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        // List teams for this club
        mockMvc.perform(get("/api/clubs/" + BEROMUNSTER_ID + "/teams")
                        .header("X-Mock-User-Id", ADMIN_USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Delete team
        mockMvc.perform(delete("/api/teams/" + teamId)
                        .header("X-Mock-User-Id", ADMIN_USER))
                .andExpect(status().isNoContent());
    }

    @Test
    void createTeam_returns403ForNonMemberClub() throws Exception {
        mockMvc.perform(post("/api/teams")
                        .header("X-Mock-User-Id", ADMIN_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Herren 1\",\"clubId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void GET_teams_returns401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isUnauthorized());
    }
}

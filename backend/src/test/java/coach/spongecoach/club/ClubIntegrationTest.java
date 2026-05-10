package coach.spongecoach.club;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ClubIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void fullCrudLifecycle() throws Exception {
        // Create
        MvcResult createResult = mockMvc.perform(post("/api/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"UHC Integration\",\"location\":\"Malters\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("UHC Integration"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String id = responseBody.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        // Read
        mockMvc.perform(get("/api/clubs/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Malters"));

        // List
        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Update
        mockMvc.perform(put("/api/clubs/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"UHC Updated\",\"location\":\"Köniz\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("UHC Updated"));

        // Delete
        mockMvc.perform(delete("/api/clubs/" + id))
                .andExpect(status().isNoContent());

        // Verify 404 after deletion
        mockMvc.perform(get("/api/clubs/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_unknownClub_returns404() throws Exception {
        mockMvc.perform(get("/api/clubs/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

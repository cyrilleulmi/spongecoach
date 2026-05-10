package coach.spongecoach.person;

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
class PersonIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void fullCrudLifecycle() throws Exception {
        // Create
        MvcResult createResult = mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"Smith\",\"email\":\"alice.inttest@test.ch\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice.inttest@test.ch"))
                .andReturn();

        String id = createResult.getResponse().getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        // Duplicate email returns 409
        mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"Smith\",\"email\":\"alice.inttest@test.ch\"}"))
                .andExpect(status().isConflict());

        // Delete
        mockMvc.perform(delete("/api/persons/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/persons/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_unknownPerson_returns404() throws Exception {
        mockMvc.perform(get("/api/persons/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

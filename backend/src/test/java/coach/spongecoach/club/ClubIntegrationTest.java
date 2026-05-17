package coach.spongecoach.club;

import coach.spongecoach.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ClubIntegrationTest {

    static final String ADMIN_USER = "cyrille.ulmi@eintracht-beromunster.ch";
    static final String MEMBER_USER = "seraina.esposito@eintracht-beromunster.ch";
    static final String BEROMUNSTER_ID = "a1b2c3d4-0001-0001-0001-000000000001";

    @Autowired MockMvc mockMvc;

    @Test
    void GET_clubs_returnsUserClubs() throws Exception {
        mockMvc.perform(get("/api/clubs")
                        .header("X-Mock-User-Id", ADMIN_USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(BEROMUNSTER_ID));
    }

    @Test
    void GET_club_returns200ForMember() throws Exception {
        mockMvc.perform(get("/api/clubs/" + BEROMUNSTER_ID)
                        .header("X-Mock-User-Id", MEMBER_USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Eintracht Beromünster"));
    }

    @Test
    void PUT_club_returns200ForAdmin() throws Exception {
        mockMvc.perform(put("/api/clubs/" + BEROMUNSTER_ID)
                        .header("X-Mock-User-Id", ADMIN_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Eintracht Beromünster\",\"location\":\"Beromünster\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void PUT_club_returns403ForMember() throws Exception {
        mockMvc.perform(put("/api/clubs/" + BEROMUNSTER_ID)
                        .header("X-Mock-User-Id", MEMBER_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"UHC Updated\",\"location\":\"Köniz\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void DELETE_club_returns405() throws Exception {
        mockMvc.perform(delete("/api/clubs/" + BEROMUNSTER_ID)
                        .header("X-Mock-User-Id", ADMIN_USER))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void POST_club_returns201ForAnyAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/clubs")
                        .header("X-Mock-User-Id", MEMBER_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"UHC Integration\",\"location\":\"Malters\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("UHC Integration"));
    }

    @Test
    void GET_clubs_returns401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_unknownClub_returns403ForNonMember() throws Exception {
        mockMvc.perform(get("/api/clubs/" + UUID.randomUUID())
                        .header("X-Mock-User-Id", ADMIN_USER))
                .andExpect(status().isForbidden());
    }

    @Test
    void GET_unknownClub_returns401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/clubs/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}

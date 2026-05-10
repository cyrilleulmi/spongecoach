package coach.spongecoach.club.adapter.in.web;

import coach.spongecoach.club.application.ClubNotFoundException;
import coach.spongecoach.club.application.ClubService;
import coach.spongecoach.club.domain.model.Club;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClubController.class)
class ClubControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ClubService clubService;

    @Test
    void POST_clubs_returns201WithBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(clubService.createClub("UHC Test", "Malters"))
                .thenReturn(new Club(id, "UHC Test", "Malters"));

        mockMvc.perform(post("/api/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClubRequest("UHC Test", "Malters"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("UHC Test"));
    }

    @Test
    void POST_clubs_returns400WhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClubRequest("", "Malters"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_clubs_returnsAll() throws Exception {
        when(clubService.listClubs()).thenReturn(List.of(
                new Club(UUID.randomUUID(), "UHC Malters", "Malters"),
                new Club(UUID.randomUUID(), "UHC Köniz", "Köniz")
        ));

        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void GET_club_returns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(clubService.getClub(id)).thenThrow(new ClubNotFoundException(id));

        mockMvc.perform(get("/api/clubs/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void DELETE_club_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/clubs/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void DELETE_club_returns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ClubNotFoundException(id)).when(clubService).deleteClub(id);

        mockMvc.perform(delete("/api/clubs/{id}", id))
                .andExpect(status().isNotFound());
    }
}

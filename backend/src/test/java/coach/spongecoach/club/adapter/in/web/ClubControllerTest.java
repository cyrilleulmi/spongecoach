package coach.spongecoach.club.adapter.in.web;

import coach.spongecoach.auth.domain.model.AuthenticatedUser;
import coach.spongecoach.auth.domain.model.ClubMembership;
import coach.spongecoach.auth.domain.model.ClubRole;
import coach.spongecoach.auth.domain.port.CurrentUserPort;
import coach.spongecoach.club.application.ClubNotFoundException;
import coach.spongecoach.club.application.ClubService;
import coach.spongecoach.club.domain.model.Club;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClubController.class)
@Import(CurrentUserContext.class)
class ClubControllerTest {

    static final String ADMIN_EMAIL = "admin@test.ch";
    static final String USER_EMAIL = "user@test.ch";
    static final UUID CLUB_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    static final UUID ADMIN_PERSON_ID = UUID.fromString("a0000000-0000-0000-0000-000000000002");
    static final UUID USER_PERSON_ID = UUID.fromString("a0000000-0000-0000-0000-000000000003");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ClubService clubService;
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
    void POST_clubs_returns201WithBody() throws Exception {
        when(clubService.createClub(eq("UHC Test"), eq("Malters"), any()))
                .thenReturn(new Club(CLUB_ID, "UHC Test", "Malters"));

        mockMvc.perform(post("/api/clubs")
                        .header("X-Mock-User-Id", ADMIN_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClubRequest("UHC Test", "Malters"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CLUB_ID.toString()))
                .andExpect(jsonPath("$.name").value("UHC Test"));
    }

    @Test
    void POST_clubs_returns201ForMember() throws Exception {
        when(clubService.createClub(eq("UHC Test"), eq("Malters"), any()))
                .thenReturn(new Club(CLUB_ID, "UHC Test", "Malters"));

        mockMvc.perform(post("/api/clubs")
                        .header("X-Mock-User-Id", USER_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClubRequest("UHC Test", "Malters"))))
                .andExpect(status().isCreated());
    }

    @Test
    void POST_clubs_returns401WithoutAuth() throws Exception {
        mockMvc.perform(post("/api/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClubRequest("UHC Test", "Malters"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void POST_clubs_returns400WhenNameBlank() throws Exception {
        mockMvc.perform(post("/api/clubs")
                        .header("X-Mock-User-Id", ADMIN_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateClubRequest("", "Malters"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_clubs_returnsOnlyUserClubs() throws Exception {
        when(clubService.getClub(CLUB_ID))
                .thenReturn(new Club(CLUB_ID, "UHC Malters", "Malters"));

        mockMvc.perform(get("/api/clubs")
                        .header("X-Mock-User-Id", USER_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(CLUB_ID.toString()));
    }

    @Test
    void GET_clubs_returns401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_club_returns200ForMember() throws Exception {
        when(clubService.getClub(CLUB_ID)).thenReturn(new Club(CLUB_ID, "UHC Test", "Malters"));

        mockMvc.perform(get("/api/clubs/{id}", CLUB_ID)
                        .header("X-Mock-User-Id", USER_EMAIL))
                .andExpect(status().isOk());
    }

    @Test
    void GET_club_returns403ForNonMember() throws Exception {
        UUID otherClub = UUID.randomUUID();

        mockMvc.perform(get("/api/clubs/{id}", otherClub)
                        .header("X-Mock-User-Id", USER_EMAIL))
                .andExpect(status().isForbidden());
    }

    @Test
    void GET_club_returns404WhenMissing() throws Exception {
        when(clubService.getClub(CLUB_ID)).thenThrow(new ClubNotFoundException(CLUB_ID));

        mockMvc.perform(get("/api/clubs/{id}", CLUB_ID)
                        .header("X-Mock-User-Id", ADMIN_EMAIL))
                .andExpect(status().isNotFound());
    }

    @Test
    void PUT_club_returns200ForAdmin() throws Exception {
        when(clubService.updateClub(eq(CLUB_ID), eq("New"), eq("New City")))
                .thenReturn(new Club(CLUB_ID, "New", "New City"));

        mockMvc.perform(put("/api/clubs/{id}", CLUB_ID)
                        .header("X-Mock-User-Id", ADMIN_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateClubRequest("New", "New City"))))
                .andExpect(status().isOk());
    }

    @Test
    void PUT_club_returns403ForMember() throws Exception {
        mockMvc.perform(put("/api/clubs/{id}", CLUB_ID)
                        .header("X-Mock-User-Id", USER_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateClubRequest("New", "New City"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void DELETE_club_returns405() throws Exception {
        mockMvc.perform(delete("/api/clubs/{id}", CLUB_ID)
                        .header("X-Mock-User-Id", ADMIN_EMAIL))
                .andExpect(status().isMethodNotAllowed());
    }
}

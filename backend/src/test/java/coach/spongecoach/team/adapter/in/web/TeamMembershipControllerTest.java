package coach.spongecoach.team.adapter.in.web;

import coach.spongecoach.auth.adapter.in.web.CurrentUserContext;
import coach.spongecoach.auth.domain.model.AuthenticatedUser;
import coach.spongecoach.auth.domain.model.ClubMembership;
import coach.spongecoach.auth.domain.model.ClubRole;
import coach.spongecoach.auth.domain.port.CurrentUserPort;
import coach.spongecoach.person.application.PersonService;
import coach.spongecoach.person.domain.model.Person;
import coach.spongecoach.team.application.TeamMembershipService;
import coach.spongecoach.team.application.TeamService;
import coach.spongecoach.team.domain.model.Team;
import coach.spongecoach.team.domain.model.TeamMembership;
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

@WebMvcTest(TeamMembershipController.class)
@Import(CurrentUserContext.class)
class TeamMembershipControllerTest {

    static final String ADMIN_EMAIL = "admin@test.ch";
    static final String MEMBER_EMAIL = "member@test.ch";
    static final UUID CLUB_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");
    static final UUID TEAM_ID = UUID.fromString("b0000000-0000-0000-0000-000000000002");
    static final UUID ADMIN_PERSON_ID = UUID.fromString("b0000000-0000-0000-0000-000000000003");
    static final UUID MEMBER_PERSON_ID = UUID.fromString("b0000000-0000-0000-0000-000000000004");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TeamMembershipService membershipService;
    @MockitoBean TeamService teamService;
    @MockitoBean PersonService personService;
    @MockitoBean CurrentUserPort currentUserPort;

    @BeforeEach
    void setUp() {
        when(currentUserPort.resolve(ADMIN_EMAIL))
                .thenReturn(Optional.of(new AuthenticatedUser(ADMIN_PERSON_ID, ADMIN_EMAIL,
                        List.of(new ClubMembership(CLUB_ID, ADMIN_PERSON_ID, ClubRole.ADMIN)))));
        when(currentUserPort.resolve(MEMBER_EMAIL))
                .thenReturn(Optional.of(new AuthenticatedUser(MEMBER_PERSON_ID, MEMBER_EMAIL,
                        List.of(new ClubMembership(CLUB_ID, MEMBER_PERSON_ID, ClubRole.MEMBER)))));
        when(teamService.getTeam(TEAM_ID))
                .thenReturn(new Team(TEAM_ID, "Herren 1", CLUB_ID));
    }

    @Test
    void GET_members_returnsPersonDetails() throws Exception {
        when(membershipService.listMembers(TEAM_ID))
                .thenReturn(List.of(new TeamMembership(TEAM_ID, MEMBER_PERSON_ID)));
        when(personService.getPerson(MEMBER_PERSON_ID))
                .thenReturn(new Person(MEMBER_PERSON_ID, "Alice", "Smith", MEMBER_EMAIL));

        mockMvc.perform(get("/api/teams/{teamId}/members", TEAM_ID)
                        .header("X-Mock-User-Id", MEMBER_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].personId").value(MEMBER_PERSON_ID.toString()))
                .andExpect(jsonPath("$[0].firstName").value("Alice"))
                .andExpect(jsonPath("$[0].lastName").value("Smith"))
                .andExpect(jsonPath("$[0].email").value(MEMBER_EMAIL));
    }

    @Test
    void GET_members_returns401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/teams/{teamId}/members", TEAM_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_members_returns403ForNonMember() throws Exception {
        String outsiderEmail = "outsider@test.ch";
        UUID outsiderId = UUID.randomUUID();
        when(currentUserPort.resolve(outsiderEmail))
                .thenReturn(Optional.of(new AuthenticatedUser(outsiderId, outsiderEmail, List.of())));

        mockMvc.perform(get("/api/teams/{teamId}/members", TEAM_ID)
                        .header("X-Mock-User-Id", outsiderEmail))
                .andExpect(status().isForbidden());
    }

    @Test
    void POST_member_returns201ForAdmin() throws Exception {
        when(membershipService.addMember(TEAM_ID, MEMBER_PERSON_ID))
                .thenReturn(new TeamMembership(TEAM_ID, MEMBER_PERSON_ID));
        when(personService.getPerson(MEMBER_PERSON_ID))
                .thenReturn(new Person(MEMBER_PERSON_ID, "Alice", "Smith", MEMBER_EMAIL));

        mockMvc.perform(post("/api/teams/{teamId}/members", TEAM_ID)
                        .header("X-Mock-User-Id", ADMIN_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddMemberRequest(MEMBER_PERSON_ID))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personId").value(MEMBER_PERSON_ID.toString()))
                .andExpect(jsonPath("$.firstName").value("Alice"));
    }

    @Test
    void POST_member_returns403ForMember() throws Exception {
        mockMvc.perform(post("/api/teams/{teamId}/members", TEAM_ID)
                        .header("X-Mock-User-Id", MEMBER_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddMemberRequest(ADMIN_PERSON_ID))))
                .andExpect(status().isForbidden());
    }

    @Test
    void DELETE_member_returns204ForAdmin() throws Exception {
        mockMvc.perform(delete("/api/teams/{teamId}/members/{personId}", TEAM_ID, MEMBER_PERSON_ID)
                        .header("X-Mock-User-Id", ADMIN_EMAIL))
                .andExpect(status().isNoContent());
    }

    @Test
    void DELETE_member_returns403ForMember() throws Exception {
        mockMvc.perform(delete("/api/teams/{teamId}/members/{personId}", TEAM_ID, MEMBER_PERSON_ID)
                        .header("X-Mock-User-Id", MEMBER_EMAIL))
                .andExpect(status().isForbidden());
    }
}

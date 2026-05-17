package coach.spongecoach.person.adapter.in.web;

import coach.spongecoach.auth.domain.model.AuthenticatedUser;
import coach.spongecoach.auth.domain.model.ClubMembership;
import coach.spongecoach.auth.domain.model.ClubRole;
import coach.spongecoach.auth.domain.port.CurrentUserPort;
import coach.spongecoach.person.application.PersonNotFoundException;
import coach.spongecoach.person.application.PersonService;
import coach.spongecoach.person.domain.model.Person;
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

@WebMvcTest(PersonController.class)
@Import(CurrentUserContext.class)
class PersonControllerTest {

    static final String ADMIN_EMAIL = "admin@test.ch";
    static final String USER_EMAIL = "user@test.ch";
    static final UUID CLUB_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    static final UUID ADMIN_PERSON_ID = UUID.fromString("a0000000-0000-0000-0000-000000000002");
    static final UUID USER_PERSON_ID = UUID.fromString("a0000000-0000-0000-0000-000000000003");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean PersonService personService;
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
    void POST_persons_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(personService.createPerson("Alice", "Smith", "alice@test.ch"))
                .thenReturn(new Person(id, "Alice", "Smith", "alice@test.ch"));

        mockMvc.perform(post("/api/persons")
                        .header("X-Mock-User-Id", ADMIN_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePersonRequest("Alice", "Smith", "alice@test.ch"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("alice@test.ch"));
    }

    @Test
    void POST_persons_returns201ForAnyAuthenticatedUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(personService.createPerson("Alice", "Smith", "alice@test.ch"))
                .thenReturn(new Person(id, "Alice", "Smith", "alice@test.ch"));

        mockMvc.perform(post("/api/persons")
                        .header("X-Mock-User-Id", USER_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePersonRequest("Alice", "Smith", "alice@test.ch"))))
                .andExpect(status().isCreated());
    }

    @Test
    void POST_persons_returns401WithoutAuth() throws Exception {
        mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePersonRequest("Alice", "Smith", "alice@test.ch"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void POST_persons_returns400OnInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/persons")
                        .header("X-Mock-User-Id", ADMIN_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePersonRequest("Alice", "Smith", "not-an-email"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_person_returns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(personService.getPerson(id)).thenThrow(new PersonNotFoundException(id));

        mockMvc.perform(get("/api/persons/{id}", id)
                        .header("X-Mock-User-Id", USER_EMAIL))
                .andExpect(status().isNotFound());
    }

    @Test
    void PUT_person_returns200ForSelf() throws Exception {
        when(personService.updatePerson(USER_PERSON_ID, "Bob", "Smith", USER_EMAIL))
                .thenReturn(new Person(USER_PERSON_ID, "Bob", "Smith", USER_EMAIL));

        mockMvc.perform(put("/api/persons/{id}", USER_PERSON_ID)
                        .header("X-Mock-User-Id", USER_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdatePersonRequest("Bob", "Smith", USER_EMAIL))))
                .andExpect(status().isOk());
    }

    @Test
    void PUT_person_returns403WhenNotSelf() throws Exception {
        UUID otherId = UUID.randomUUID();
        mockMvc.perform(put("/api/persons/{id}", otherId)
                        .header("X-Mock-User-Id", USER_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdatePersonRequest("Bob", "Smith", "other@test.ch"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void DELETE_person_returns204ForSelf() throws Exception {
        mockMvc.perform(delete("/api/persons/{id}", USER_PERSON_ID)
                        .header("X-Mock-User-Id", USER_EMAIL))
                .andExpect(status().isNoContent());
    }

    @Test
    void DELETE_person_returns403WhenNotSelf() throws Exception {
        mockMvc.perform(delete("/api/persons/{id}", ADMIN_PERSON_ID)
                        .header("X-Mock-User-Id", USER_EMAIL))
                .andExpect(status().isForbidden());
    }

    @Test
    void DELETE_person_returns401WithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/persons/{id}", USER_PERSON_ID))
                .andExpect(status().isUnauthorized());
    }
}

package coach.spongecoach.person.adapter.in.web;

import coach.spongecoach.person.application.PersonNotFoundException;
import coach.spongecoach.person.application.PersonService;
import coach.spongecoach.person.domain.model.Person;
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

@WebMvcTest(PersonController.class)
class PersonControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean PersonService personService;

    @Test
    void POST_persons_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(personService.createPerson("Alice", "Smith", "alice@test.ch"))
                .thenReturn(new Person(id, "Alice", "Smith", "alice@test.ch"));

        mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePersonRequest("Alice", "Smith", "alice@test.ch"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("alice@test.ch"));
    }

    @Test
    void POST_persons_returns400OnInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePersonRequest("Alice", "Smith", "not-an-email"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_persons_returnsAll() throws Exception {
        when(personService.listPersons()).thenReturn(List.of(
                new Person(UUID.randomUUID(), "Alice", "Smith", "alice@test.ch")
        ));

        mockMvc.perform(get("/api/persons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void GET_person_returns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(personService.getPerson(id)).thenThrow(new PersonNotFoundException(id));

        mockMvc.perform(get("/api/persons/{id}", id))
                .andExpect(status().isNotFound());
    }
}

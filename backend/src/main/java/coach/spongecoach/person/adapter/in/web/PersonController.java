package coach.spongecoach.person.adapter.in.web;

import coach.spongecoach.person.application.PersonService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/persons")
class PersonController {

    private final PersonService personService;

    PersonController(PersonService personService) {
        this.personService = personService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PersonResponse create(@Valid @RequestBody CreatePersonRequest request) {
        return PersonResponse.from(personService.createPerson(request.firstName(), request.lastName(), request.email()));
    }

    @GetMapping
    List<PersonResponse> list() {
        return personService.listPersons().stream().map(PersonResponse::from).toList();
    }

    @GetMapping("/{id}")
    PersonResponse get(@PathVariable UUID id) {
        return PersonResponse.from(personService.getPerson(id));
    }

    @PutMapping("/{id}")
    PersonResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePersonRequest request) {
        return PersonResponse.from(personService.updatePerson(id, request.firstName(), request.lastName(), request.email()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        personService.deletePerson(id);
    }
}

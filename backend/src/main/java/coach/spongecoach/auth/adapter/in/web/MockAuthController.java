package coach.spongecoach.auth.adapter.in.web;

import coach.spongecoach.auth.adapter.out.mock.MockCurrentUserAdapter;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/mock")
@Profile("mock")
class MockAuthController {

    private final MockCurrentUserAdapter mockUserAdapter;

    MockAuthController(MockCurrentUserAdapter mockUserAdapter) {
        this.mockUserAdapter = mockUserAdapter;
    }

    @GetMapping("/users")
    List<MockUserResponse> listUsers() {
        return mockUserAdapter.listAll().stream().map(MockUserResponse::from).toList();
    }
}

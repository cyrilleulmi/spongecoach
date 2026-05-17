package coach.spongecoach.auth.adapter.in.web;

import coach.spongecoach.auth.adapter.out.mock.MockCurrentUserAdapter;
import coach.spongecoach.auth.domain.model.AuthenticatedUser;
import coach.spongecoach.auth.domain.model.ClubMembership;
import coach.spongecoach.auth.domain.model.ClubRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MockAuthController.class)
@Import(CurrentUserContext.class)
@ActiveProfiles("mock")
class MockAuthControllerTest {

    static final UUID CLUB_ID = UUID.fromString("c1000000-0000-0000-0000-000000000001");

    @Autowired MockMvc mockMvc;
    // MockCurrentUserAdapter implements CurrentUserPort — one mock covers both injection points
    @MockitoBean MockCurrentUserAdapter mockUserAdapter;

    @Test
    void listUsers_includesRoleAdminForClubAdmin() throws Exception {
        UUID adminId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        when(mockUserAdapter.listAll()).thenReturn(List.of(
                new AuthenticatedUser(adminId, "admin@test.ch",
                        List.of(new ClubMembership(CLUB_ID, adminId, ClubRole.ADMIN)))
        ));

        mockMvc.perform(get("/api/auth/mock/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    void listUsers_includesRoleUserForClubMember() throws Exception {
        UUID userId = UUID.fromString("a0000000-0000-0000-0000-000000000002");
        when(mockUserAdapter.listAll()).thenReturn(List.of(
                new AuthenticatedUser(userId, "member@test.ch",
                        List.of(new ClubMembership(CLUB_ID, userId, ClubRole.MEMBER)))
        ));

        mockMvc.perform(get("/api/auth/mock/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("USER"));
    }

    @Test
    void listUsers_includesRoleUserForNoMemberships() throws Exception {
        UUID userId = UUID.fromString("a0000000-0000-0000-0000-000000000003");
        when(mockUserAdapter.listAll()).thenReturn(List.of(
                new AuthenticatedUser(userId, "nomember@test.ch", List.of())
        ));

        mockMvc.perform(get("/api/auth/mock/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("USER"));
    }
}

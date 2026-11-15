package com.spongecoach.api;

import com.spongecoach.api.dto.PlayerUpdateRequest;
import com.spongecoach.auth.Access;
import com.spongecoach.domain.Player;
import com.spongecoach.domain.Team;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** [TEST-MR] SmartCodeReview test data - do not merge. */
@QuarkusTest
class JerseyNumberValidationTest {

    @Inject
    PlayerResource resource;

    @InjectMock
    Access access;

    @Test
    @TestTransaction
    void asksAccessBeforeSavingTheNumber() {
        Player carmela = newPlayer("Carmela");
        resource.update(carmela.id, new PlayerUpdateRequest(null, 12));
        verify(access).requireSelf(carmela);
        assertEquals(12, Player.<Player>findById(carmela.id).jerseyNumber);
    }

    @Test
    @TestTransaction
    void keepsTheNumberWhenAccessIsDenied() {
        Player carmela = newPlayer("Carmela");
        when(access.isCoachOf(any())).thenReturn(false);
        doThrow(new AccessDeniedException("not yours")).when(access).requireSelf(carmela);

        assertThrows(AccessDeniedException.class,
                () -> resource.update(carmela.id, new PlayerUpdateRequest(null, 12)));
        assertNull(Player.<Player>findById(carmela.id).jerseyNumber);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100, 150})
    @TestTransaction
    void rejectsNumbersOutsideTheRange(int jerseyNumber) {
        Player carmela = newPlayer("Carmela");
        BadRequestException rejected = assertThrows(BadRequestException.class,
                () -> resource.update(carmela.id, new PlayerUpdateRequest(null, jerseyNumber)));
        assertEquals("jersey number must be between 1 and 99", rejected.getMessage());
    }

    private static Player newPlayer(String name) {
        Player player = new Player();
        player.name = name;
        player.team = Team.findAll().firstResult();
        player.persist();
        return player;
    }
}

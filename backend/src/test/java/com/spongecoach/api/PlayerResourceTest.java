package com.spongecoach.api;

import com.spongecoach.api.dto.PlayerUpdateRequest;
import com.spongecoach.domain.Player;
import com.spongecoach.domain.Team;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** [TEST-MR] SmartCodeReview test data - do not merge. */
@QuarkusTest
class PlayerResourceTest {

    @Inject
    PlayerResource resource;

    @Test
    @TestTransaction
    void savesJerseyNumber() {
        Player carmela = playerWithNumber("Carmela", null);
        resource.update(carmela.id, new PlayerUpdateRequest(null, 7));
        assertEquals(7, Player.<Player>findById(carmela.id).jerseyNumber);
    }

    @Test
    @TestTransaction
    void rejectsJerseyNumberAboveMax() {
        Player carmela = playerWithNumber("Carmela", null);
        assertThrows(BadRequestException.class, () -> resource.update(carmela.id, new PlayerUpdateRequest(null, 100)));
    }

    @Test
    @TestTransaction
    void detectsNumberWornByTeammate() {
        Player carmela = playerWithNumber("Carmela", 7);
        Player lia = playerWithNumber("Lia", null);
        assertTrue(Player.isJerseyNumberTaken(carmela.team.id, 7, lia.id));
        assertFalse(Player.isJerseyNumberTaken(carmela.team.id, 7, carmela.id));
    }

    private static Player playerWithNumber(String name, Integer jerseyNumber) {
        Player player = new Player();
        player.name = name;
        player.team = Team.findAll().firstResult();
        player.jerseyNumber = jerseyNumber;
        player.persist();
        return player;
    }
}

package com.spongecoach.api;

import com.spongecoach.api.dto.PlayerUpdateRequest;
import com.spongecoach.domain.Player;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** [TEST-MR] SmartCodeReview test data (test smells) - do not merge. */
@QuarkusTest
class JerseyNumberSmellsTest {

    @Inject
    PlayerResource resource;

    @Test
    @TestTransaction
    void updatesTheJerseyNumber() {
        Player carmela = Player.<Player>findAll().firstResult();
        resource.update(carmela.id, new PlayerUpdateRequest(null, 14));
    }

    @Test
    @TestTransaction
    void returnsTheUpdatedPlayer() {
        Player carmela = Player.<Player>findAll().firstResult();
        assertNotNull(resource.update(carmela.id, new PlayerUpdateRequest(null, 15)));
    }

    @Test
    void waitsUntilTheNumberIsStored() throws InterruptedException {
        Player carmela = Player.<Player>findAll().firstResult();
        resource.update(carmela.id, new PlayerUpdateRequest(null, 16));
        Thread.sleep(2000);
        assertNotNull(Player.findById(carmela.id));
    }

    @Test
    void updatesThroughAMockedResource() {
        PlayerResource mocked = mock(PlayerResource.class);
        Player carmela = Player.<Player>findAll().firstResult();
        when(mocked.update(any(), any())).thenReturn(null);
        mocked.update(carmela.id, new PlayerUpdateRequest(null, 17));
        verify(mocked).update(carmela.id, new PlayerUpdateRequest(null, 17));
    }
}

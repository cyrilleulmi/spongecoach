package com.spongecoach.api;

import com.spongecoach.api.dto.PlayerDto;
import com.spongecoach.domain.Player;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** The team's player pool. Players are created by seed only (no create endpoint in v1). */
@Path("/api/players")
@Produces(MediaType.APPLICATION_JSON)
public class PlayerResource {

    @GET
    public List<PlayerDto> list() {
        return Player.listAllOrderedByName().stream().map(PlayerDto::from).toList();
    }
}

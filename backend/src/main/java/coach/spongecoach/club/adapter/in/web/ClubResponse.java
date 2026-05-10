package coach.spongecoach.club.adapter.in.web;

import coach.spongecoach.club.domain.model.Club;

import java.util.UUID;

record ClubResponse(UUID id, String name, String location) {

    static ClubResponse from(Club club) {
        return new ClubResponse(club.id(), club.name(), club.location());
    }
}

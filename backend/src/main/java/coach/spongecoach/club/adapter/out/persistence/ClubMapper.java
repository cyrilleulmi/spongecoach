package coach.spongecoach.club.adapter.out.persistence;

import coach.spongecoach.club.domain.model.Club;
import org.springframework.stereotype.Component;

@Component
class ClubMapper {

    Club toDomain(ClubJpaEntity entity) {
        return new Club(entity.getId(), entity.getName(), entity.getLocation());
    }

    ClubJpaEntity toEntity(Club club) {
        return new ClubJpaEntity(club.id(), club.name(), club.location());
    }
}

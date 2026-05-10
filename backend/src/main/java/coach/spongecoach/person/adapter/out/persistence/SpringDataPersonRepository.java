package coach.spongecoach.person.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataPersonRepository extends JpaRepository<PersonJpaEntity, UUID> {
    boolean existsByEmail(String email);
}

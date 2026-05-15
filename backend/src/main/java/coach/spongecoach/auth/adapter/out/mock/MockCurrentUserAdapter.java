package coach.spongecoach.auth.adapter.out.mock;

import coach.spongecoach.auth.domain.model.AuthenticatedUser;
import coach.spongecoach.auth.domain.model.Role;
import coach.spongecoach.auth.domain.port.CurrentUserPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Profile("mock")
public class MockCurrentUserAdapter implements CurrentUserPort {

    private final Map<String, AuthenticatedUser> usersByEmail;

    MockCurrentUserAdapter() {
        List<AuthenticatedUser> users = List.of(
            // Eintracht Beromünster — admins
            new AuthenticatedUser(UUID.fromString("c0000000-0000-0000-0000-000000000001"), "cyrille.ulmi@eintracht-beromunster.ch", Role.ADMIN),
            new AuthenticatedUser(UUID.fromString("c0000000-0000-0000-0000-000000000002"), "jan.milczarek@eintracht-beromunster.ch", Role.ADMIN),
            new AuthenticatedUser(UUID.fromString("c0000000-0000-0000-0000-000000000016"), "lukas.holdener@eintracht-beromunster.ch", Role.ADMIN),
            // Eintracht Beromünster — players
            new AuthenticatedUser(UUID.fromString("c0000000-0000-0000-0000-000000000003"), "seraina.esposito@eintracht-beromunster.ch", Role.USER),
            new AuthenticatedUser(UUID.fromString("c0000000-0000-0000-0000-000000000006"), "ramon.wandeler@eintracht-beromunster.ch", Role.USER),
            new AuthenticatedUser(UUID.fromString("c0000000-0000-0000-0000-000000000010"), "aleksandar.jarakovic@eintracht-beromunster.ch", Role.USER),
            // UHC Lok Reinach — admin (coach, no connection to Beromünster)
            new AuthenticatedUser(UUID.fromString("c0000000-0000-0000-0000-000000000023"), "daniel.schiess@lok-reinach.ch", Role.ADMIN),
            // UHC Lok Reinach — players
            new AuthenticatedUser(UUID.fromString("c0000000-0000-0000-0000-000000000024"), "andreas.frey@lok-reinach.ch", Role.USER)
        );
        usersByEmail = users.stream().collect(Collectors.toMap(AuthenticatedUser::email, Function.identity()));
    }

    @Override
    public Optional<AuthenticatedUser> resolve(String email) {
        return Optional.ofNullable(usersByEmail.get(email));
    }

    public List<AuthenticatedUser> listAll() {
        return List.copyOf(usersByEmail.values());
    }
}

package coach.spongecoach.auth.adapter.out.mock;

import coach.spongecoach.auth.domain.model.AuthenticatedUser;
import coach.spongecoach.auth.domain.port.CurrentUserPort;
import coach.spongecoach.auth.domain.model.ClubMembership;
import coach.spongecoach.auth.domain.model.ClubRole;
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

    private static final UUID BEROMUNSTER = UUID.fromString("a1b2c3d4-0001-0001-0001-000000000001");
    private static final UUID LOK_REINACH = UUID.fromString("a1b2c3d4-0001-0001-0001-000000000002");

    private final Map<String, AuthenticatedUser> usersByEmail;

    MockCurrentUserAdapter() {
        List<AuthenticatedUser> users = List.of(
            // Eintracht Beromünster — admins
            new AuthenticatedUser(
                UUID.fromString("c0000000-0000-0000-0000-000000000001"),
                "cyrille.ulmi@eintracht-beromunster.ch",
                List.of(new ClubMembership(BEROMUNSTER, UUID.fromString("c0000000-0000-0000-0000-000000000001"), ClubRole.ADMIN))
            ),
            new AuthenticatedUser(
                UUID.fromString("c0000000-0000-0000-0000-000000000002"),
                "jan.milczarek@eintracht-beromunster.ch",
                List.of(new ClubMembership(BEROMUNSTER, UUID.fromString("c0000000-0000-0000-0000-000000000002"), ClubRole.ADMIN))
            ),
            new AuthenticatedUser(
                UUID.fromString("c0000000-0000-0000-0000-000000000016"),
                "lukas.holdener@eintracht-beromunster.ch",
                List.of(
                    new ClubMembership(BEROMUNSTER, UUID.fromString("c0000000-0000-0000-0000-000000000016"), ClubRole.ADMIN),
                    new ClubMembership(LOK_REINACH, UUID.fromString("c0000000-0000-0000-0000-000000000016"), ClubRole.MEMBER)
                )
            ),
            // Eintracht Beromünster — players
            new AuthenticatedUser(
                UUID.fromString("c0000000-0000-0000-0000-000000000003"),
                "seraina.esposito@eintracht-beromunster.ch",
                List.of(new ClubMembership(BEROMUNSTER, UUID.fromString("c0000000-0000-0000-0000-000000000003"), ClubRole.MEMBER))
            ),
            new AuthenticatedUser(
                UUID.fromString("c0000000-0000-0000-0000-000000000006"),
                "ramon.wandeler@eintracht-beromunster.ch",
                List.of(new ClubMembership(BEROMUNSTER, UUID.fromString("c0000000-0000-0000-0000-000000000006"), ClubRole.MEMBER))
            ),
            new AuthenticatedUser(
                UUID.fromString("c0000000-0000-0000-0000-000000000010"),
                "aleksandar.jarakovic@eintracht-beromunster.ch",
                List.of(new ClubMembership(BEROMUNSTER, UUID.fromString("c0000000-0000-0000-0000-000000000010"), ClubRole.MEMBER))
            ),
            // UHC Lok Reinach — admin
            new AuthenticatedUser(
                UUID.fromString("c0000000-0000-0000-0000-000000000023"),
                "daniel.schiess@lok-reinach.ch",
                List.of(new ClubMembership(LOK_REINACH, UUID.fromString("c0000000-0000-0000-0000-000000000023"), ClubRole.ADMIN))
            ),
            // UHC Lok Reinach — player
            new AuthenticatedUser(
                UUID.fromString("c0000000-0000-0000-0000-000000000024"),
                "andreas.frey@lok-reinach.ch",
                List.of(new ClubMembership(LOK_REINACH, UUID.fromString("c0000000-0000-0000-0000-000000000024"), ClubRole.MEMBER))
            )
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

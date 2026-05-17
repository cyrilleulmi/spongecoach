package coach.spongecoach.team.adapter.in.web;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

record AddMembersRequest(@NotNull @NotEmpty List<@NotNull UUID> personIds) {
}

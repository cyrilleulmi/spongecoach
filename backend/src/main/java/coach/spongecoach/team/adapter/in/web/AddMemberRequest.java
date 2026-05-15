package coach.spongecoach.team.adapter.in.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record AddMemberRequest(@NotNull UUID personId) {
}

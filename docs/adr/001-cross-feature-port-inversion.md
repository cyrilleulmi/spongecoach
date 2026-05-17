# ADR 001 — Cross-feature port inversion to prevent dependency cycles

**Status:** Accepted  
**Date:** 2026-05-16

## Context

The backend follows a strict hexagonal / feature-slice layout enforced by ArchUnit. The allowed dependency direction between feature slices is:

```
auth ← club ← team
```

Two cases required a club-slice operation to trigger behaviour in the team slice (which already depends on club), and a person-slice operation to trigger behaviour in the club slice (which already depends on person):

1. **Removing a club member** must cascade: remove that person from all teams within the club.  
   Direct call `club.application → team.application` would close the `club ↔ team` cycle → ArchUnit failure.

2. **Deleting a person** must be blocked if they are the last admin of any club.  
   Direct call `person.application → club.application` would close the `person ↔ club` cycle → ArchUnit failure.

## Decision

Apply **port inversion**: define a narrow port interface in the *calling* slice's domain package, and implement it in the *called* slice's adapter package.

| Port | Location | Implemented by |
|---|---|---|
| `TeamMembershipCleaner` | `club.domain` | `team.adapter.out.persistence.TeamMembershipCleanerAdapter` |
| `DeletePersonGuard` | `person.domain` | `club.adapter.out.persistence.DeletePersonGuardAdapter` |

The calling service injects the port interface (resolved by Spring at runtime). ArchUnit sees only the dependency on the port interface in the correct direction — no cycle.

## Consequences

- **Positive:** No ArchUnit violation. Each slice stays independently testable — the port can be mocked in unit tests.
- **Positive:** The port contract makes the cross-feature coupling explicit and narrow (one method each).
- **Negative:** A new operation that needs to cross feature boundaries in the "wrong" direction requires the same ceremony: add a port interface, write an adapter implementation, wire via DI.
- **Rule to follow:** When feature A needs to trigger behaviour in feature B, but B already depends on A, define the port in A's domain and implement it in B's adapter. Never add a direct cross-feature application-layer dependency that would close a cycle.

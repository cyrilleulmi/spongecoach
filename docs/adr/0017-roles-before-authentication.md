---
status: accepted
---

# Roles are enforced before there is authentication: a User is picked, not logged in

The app is about to be used by the whole team, not just the coach, so writes need to be limited by
who is making them. Authentication (real logins) is deliberately deferred. We want the permission
model in place and testable now, and to swap in authentication later without touching it.

**Users and Roles.** A new `app_user` table holds seeded Users, each with exactly one Role:

- **SysAdmin** — may do everything, in every Team. Reserved for support endpoints no one else may
  call; there are none yet.
- **Coach** — may do everything inside their own Team: manage Lines, Iterations and Events, edit
  every Player, answer attendance for anyone, and still change a done Event. When in doubt a Coach
  gets the permission.
- **Player** — acts as exactly one Player. May edit that Player (ratings, goals, Avatar), the Lines
  that Player is on by *current* roster (name, roster, Skill ratings, Development goals), those
  Lines' Focus per Event, and their own attendance. The last two only while the Event has not
  passed — the "read-only once done" default of ADR-0012 is a hard rule for Players, and a
  frontend-only default for Coaches. A Player may also add to and recolor the shared Skill and
  Development goal lists, since creating them from the line and player screens (ADR-0009) is part
  of editing one's own Line or Player.

A Coach may also be a Player (the seed makes Anita both): the Coach Role already covers everything
the Player could do, so it is one User with a `player_id`, not two Users.

Every User may read everything. Only writes are checked.

**Who is asking.** The frontend shows a dropdown of every User in the header and stores the choice
in a `spongecoach-user` cookie; the backend's `UserCookieFilter` resolves it into a request-scoped
`CurrentUser`. A cookie rather than a header because Avatar images are loaded by `<img>` tags,
which cannot send headers. Every `/api` request needs a known User (401 `unauthenticated`
otherwise), except `GET /api/users`, which fills the dropdown. The first visit picks the SysAdmin.

**Where the rules live.** The per-endpoint matrix is `docs/permissions.md`. One `Access` bean holds every rule, and each write endpoint calls it after
loading the row, so an unknown id is still a 404 and a refused write is a 403 `forbidden`. We did not
use `quarkus-security` and `@RolesAllowed`: almost every rule depends on the row (is this Player on
this Line? is this Event done?), so annotations would cover only the easy half, and the rest would
still need code. When authentication arrives, only `UserCookieFilter` changes. It will map the
authenticated identity to an `app_user` row, and `Access` stays as it is.

**Focus per Line.** `PUT /api/events/{id}` replaces an Event's whole Focus set, which a Player can't
be allowed to do without touching other Lines. `PUT /api/events/{id}/focus/{lineId}` sets or clears
one Line's Focus. The frontend now uses it for everyone, and the whole-set write is Coach-only.

**Consequences.** The UI hides what the backend would refuse, but the backend is the authority. Until
authentication exists, anyone who can reach the app can pick any User, so this is about preventing
mistakes, not about security. `GET /api/users` goes away with the dropdown.

# Permissions matrix

Who may call what (ADR-0017). This is the single reference for authorization: `Access`
(`backend/.../auth/Access.java`) enforces it, `CurrentUserService` in the frontend mirrors it to hide
controls, and `docs/spec/authorization.feature` proves it.

**Every new or changed endpoint must be added here, with a decision for every Role, in the same
change.** If the right answer isn't obvious, ask. The default is: a Coach may do it within their Team,
and a SysAdmin may do everything.

## Roles

- **SysAdmin** — everything, in every Team. Support endpoints that no one else may call are
  SysAdmin-only (none exist yet).
- **Coach** — everything inside their own Team, including done Events. When in doubt a Coach gets
  the permission.
- **Player** — acts as one Player. Changes only that Player, the Lines they are on, those Lines'
  Focus, and their own attendance.

## Terms

- **own Line** — a Line the User's Player is on by *current* roster (`line_player`), and not
  deleted. Not the Event's snapshot roster.
- **self** — the User's own Player (`app_user.player_id`).
- **not done** — the Event's `scheduledOn` has not passed. A Player's write to a done Event is
  refused; a Coach's is allowed (the frontend still locks it until "Trotzdem bearbeiten").
- A refused write is **403 `forbidden`**. No known User is **401 `unauthenticated`**. An unknown
  row is still **404**: the check runs after the row is loaded.

## Matrix

✓ allowed · ✗ refused (403) · *condition* allowed only then

| Endpoint | What | SysAdmin | Coach | Player | `Access` check |
|---|---|---|---|---|---|
| `GET /api/users` | list Users for the dropdown | ✓ | ✓ | ✓ | none; needs no User at all |
| `GET /api/me` | the current User | ✓ | ✓ | ✓ | User required |
| every other `GET` | reads | ✓ | ✓ | ✓ | User required |
| `POST /api/lines` | create Line | ✓ | ✓ | ✗ | `requireCoach` |
| `DELETE /api/lines/{id}` | delete Line | ✓ | ✓ | ✗ | `requireCoach` |
| `POST /api/lines/{id}/restore` | restore Line | ✓ | ✓ | ✗ | `requireCoach` |
| `PUT /api/lines/{id}` | name, roster, Development goals | ✓ | ✓ | *own Line* | `requireLineMember` |
| `PUT/DELETE /api/lines/{id}/skills/{skillId}` | Line Skill rating | ✓ | ✓ | *own Line* | `requireLineMember` |
| `POST/PUT /api/skills`, `/api/development-goals` | Line catalogs: create, rename, recolor | ✓ | ✓ | ✓ | none |
| `POST/PUT /api/player-skills`, `/api/player-development-goals` | Player lists: create, rename, recolor | ✓ | ✓ | ✓ | none |
| `PUT /api/players/{id}` | Player development goals | ✓ | ✓ | *self* | `requireSelf` |
| `PUT/DELETE /api/players/{id}/skills/{skillId}` | Player rating | ✓ | ✓ | *self* | `requireSelf` |
| `PUT/DELETE /api/players/{id}/avatar` | paint / remove Avatar | ✓ | ✓ | *self* | `requireSelf` |
| `POST /api/iterations` | create Iteration (with Events) | ✓ | ✓ | ✗ | `requireCoach` |
| `PUT/DELETE /api/iterations/{id}` | rename, reorder, delete Iteration | ✓ | ✓ | ✗ | `requireCoach` |
| `POST /api/iterations/{id}/events` | add Event | ✓ | ✓ | ✗ | `requireCoach` |
| `PUT/DELETE /api/iterations/{it}/events/{ev}` | reschedule, rename, delete Event | ✓ | ✓ | ✗ | `requireCoach` |
| `PUT /api/events/{id}` | Event fields, whole Focus set | ✓ | ✓ | ✗ | `requireCoach` |
| `PUT /api/events/{id}/focus/{lineId}` | one Line's Focus | ✓ | ✓ | *own Line, not done* | `requireFocus` |
| `PUT /api/events/{id}/attendance/{playerId}` | an answer | ✓ | ✓ | *self, not done* | `requireAttendance` |
| `POST /api/drills` | upload a Drill (starts an interpreter job) | ✓ | ✓ | ✗ | `requireCoach` |
| `POST /api/drills/drawn` | create a Drill drawn by hand (no interpreter) | ✓ | ✓ | ✓ | `requireTeamMember` |
| `PUT /api/drills/{id}` | rename, retag a Drill | ✓ | ✓ | ✓ | `requireTeamMember` |
| `POST /api/drills/{id}/answers` | answer Clarifying questions (starts a job) | ✓ | ✓ | ✗ | `requireCoach` |
| `POST /api/drills/{id}/chat` | send a correction (starts a job) | ✓ | ✓ | ✗ | `requireCoach` |
| `POST /api/drills/{id}/retry` | interpret again (starts a job) | ✓ | ✓ | ✗ | `requireCoach` |
| `PUT /api/drills/{id}/script` | save a hand-edited script | ✓ | ✓ | ✓ | `requireTeamMember` |
| `POST /api/drills/{id}/revert/{version}` | make an earlier version current | ✓ | ✓ | ✓ | `requireTeamMember` |
| `POST /api/drills/{id}/sketches` | add photos (starts no job) | ✓ | ✓ | ✓ | `requireTeamMember` |
| `DELETE /api/drills/{id}/sketches/{n}` | remove a photo (soft) | ✓ | ✓ | ✓ | `requireTeamMember` |
| `POST /api/drills/{id}/sketches/{n}/restore` | restore a removed photo | ✓ | ✓ | ✓ | `requireTeamMember` |
| `DELETE /api/drills/{id}` | delete a Drill (soft) | ✓ | ✓ | ✓ | `requireTeamMember` |
| `POST /api/drills/{id}/restore` | restore a deleted Drill | ✓ | ✓ | ✓ | `requireTeamMember` |

"Coach ✓" always means *within their own Team*. With one hardcoded Team, that is every row today.

Only the Drill writes that start a paid interpreter job are Coach-only: uploading photos, answers,
chat and retry (ADR-0018, ADR-0019). Everything that costs nothing (drawing, editing the script,
photos, deleting and restoring) is any team member's, Player included (ADR-0020). "Team member"
means SysAdmin, or a Coach or Player whose `team_id` is the Drill's Team. With one Team there is no
other Team's Player to prove the refusal with; the scenarios cover the Role split instead.

## Frontend mirror

`CurrentUserService` answers the same questions: `isCoach`, `canEditLine(lineId)`,
`canEditPlayer(playerId)`, `canEditDrills()` (any team member: draw, edit, photos, delete, restore).
The drill screens use `isCoach` only for the controls that start an interpreter job (photo upload
with Claude, answers, chat, retry) and `canEditDrills` for the rest. `EventDetail` adds the done-Event rule. A control the User can't use is
hidden, or shown as plain text. The frontend never grants what the backend refuses.

## Changing this

1. Add or adjust the row above, with a decision for every Role.
2. Enforce it in the resource through `Access`, adding a check there if none fits.
3. Add a scenario to `docs/spec/authorization.feature`: at least one allowed and one refused case.
4. Mirror it in `CurrentUserService` / the screen, and add a `ui.feature` scenario if the UI changes.

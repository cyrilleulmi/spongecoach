---
status: accepted
---

# Player is team-scoped; a Line's roster is an n:n selection of Team Players

Originally a Player belonged to exactly one Line (`player.line_id NOT NULL`, "Core domain model & schema"), and the per-Line overview created new Players inline via a free-text field. We've reversed both:

- **Player is now team-scoped** (`player.team_id`), like the Skill / Development goal / Focus catalogs (ADR-0006). A Player exists independently of any Line.
- **A Line's roster is an n:n association** (`line_player` join table). The same Player can be rostered on more than one Line at once — confirmed as the intended behaviour, mirroring how a Line links to Skills/Goals/Focuses.
- **Players are created by seed only in v1.** There is no create-Player endpoint or UI; the line view can only associate/disassociate existing Team Players (a dialog with the same chip picker used for Goals/Focuses). This matches the scope call in "Implement per-line overview page" that kept catalog *creation* out of v1.

API impact: `POST /api/lines/{id}/players` and `DELETE /api/lines/{id}/players/{playerId}` are removed; roster membership is edited through `playerIds` on the full-resource `PUT /api/lines/{id}` (same shape as `developmentGoalIds` / `focusIds`), and `GET /api/players` lists the Team pool.

This is hard to reverse once real data exists — collapsing the n:n back to one-line-per-player means choosing which Line "keeps" each shared player. Accepted because it removes accidental player duplication (the same person entered separately on two lines) and makes the roster behave consistently with every other Line association in the app.

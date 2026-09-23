---
status: accepted
---

# Players get their own Skills, Ratings and Development goals, separate from the Line ones

Until now a Rating was explicitly "not tied to an individual Player" (ADR-0006): Skills and
Development goals lived only on Lines. Coaches want a player screen that says something about the
Player themselves — what they're good at and what they're working on — so a Player now carries
their own Ratings and Development goals.

We made these **separate lists**, not a reuse of the Line catalogs: `player_skill` and
`player_development_goal`, with `player_skill_rating` (0-100, overwritten, no history — same model
as a Line Rating) and `player_player_development_goal`. A Line Skill can't be rated on a Player and
vice versa; a Line's Ratings don't roll up into its Players, and Player Ratings don't average into
their Line. The two vocabularies differ in practice (a goalie's "Butterfly-Technik" has no Line
counterpart, a Line's "Umschalttempo" is about the unit, not a person), and sharing rows would mean
a coach renaming a Line Skill quietly relabels every Player rated on it.

The cost is two near-identical catalog stacks (entity, resource, DTO factory) and two lists a coach
maintains. We accept it: the lists stay small, and the UI calls both "Skill"/"Ziel" since a coach
only ever sees one of them at a time. Player lists follow ADR-0009 — created from the player
screen, each with a palette color — and, like the Line lists, have no delete endpoint in v1, so no
deletion-blocking rule is needed yet.

Players are still seed-only and have no photo; the avatar is the name's initials.

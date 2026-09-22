Feature: Lines
  A Line is a group of Players who train and play together. Its roster is a selection of the Team's
  Players, it self-rates Skills, and it associates with Development goals and Focuses from the
  shared catalogs. Lines are never hard-deleted (ADR-0004) so that Events they attended keep
  showing them.

  Background:
    Given the single seeded Team

  Rule: A Line's identity

    @spec:lines.create
    Scenario: Creating a Line
      When the coach creates a Line named "Kiwi"
      Then it appears in the Line list with a player count of 0
      And it is assigned a color from the fixed dial palette, kept even once it is deleted

    @spec:lines.create-requires-name
    Scenario: A Line needs a name
      When the coach creates a Line with a blank name
      Then the request is rejected as a bad request

    @spec:lines.list-with-player-count
    Scenario: The Line list carries each Line's roster size
      Given a Line "Kiwi" rostering "Carmela", "Debi", "Gina" and "Nives"
      When the Line list is read
      Then "Kiwi" is listed with a player count of 4

    @spec:lines.detail-embeds-associations
    Scenario: A Line's detail embeds everything the line screen needs in one read
      Given a Line "Bäri" rostering "Rahel"
      And "Bäri" rates the Skill "Passgenauigkeit" at 72
      And "Bäri" is associated with the Development goal "Ballverluste im eigenen Drittel reduzieren"
      And "Bäri" is associated with the Focus "Cross-Pässe unter Druck"
      When "Bäri" is read by id
      Then its roster, its Skill ratings, its Development goals and its Focuses come back inline

    @spec:lines.unknown-is-not-found
    Scenario: An unknown Line
      When a Line that does not exist is read
      Then the response is a not-found error envelope

  Rule: Soft deletion preserves history (ADR-0004)

    @spec:lines.delete-is-soft
    Scenario: Deleting a Line hides it but keeps its history
      Given a Line "Kiwi" rostering "Sabrina"
      When the coach deletes it
      Then it no longer appears in the Line list, and reading it by id is not found
      But its row and its roster still exist, so Events it attended still show it

    @spec:lines.delete-twice
    Scenario: Deleting an already deleted Line
      Given a deleted Line "Kiwi"
      When the coach deletes it again
      Then the response is not found

    @spec:lines.list-deleted
    Scenario: Deleted Lines can be listed for restoring
      Given a Line "Kiwi" rostering "Nives"
      And the coach has deleted "Kiwi"
      When the deleted-Line list is read
      Then "Kiwi" is listed there with its name and a player count of 1

    @spec:lines.restore
    Scenario: Restoring a deleted Line
      Given a Line "Kiwi" rostering "Carmela"
      And the coach has deleted "Kiwi"
      When the coach restores it
      Then it appears in the Line list again with its roster intact, and no longer among the deleted

    @spec:lines.restore-only-deleted
    Scenario: Restoring a Line that was never deleted
      Given a Line "Kiwi"
      When the coach restores it
      Then the response is not found

  Rule: The roster is a selection of the Team's Players (ADR-0008)

    @spec:lines.roster-replace
    Scenario: Setting the roster replaces it wholesale
      Given a Line "Kiwi" rostering "Carmela" and "Debi"
      And the Team Player "Gina", on no Line
      When the coach sets "Kiwi"'s roster to "Carmela" and "Gina"
      Then its roster is "Carmela" and "Gina", and "Debi" has left it

    @spec:lines.roster-associates-existing-players
    Scenario: Rostering existing Team Players
      Given a Line "Lama"
      And the Team Players "Anita" and "Samira", on no Line
      When the coach sets "Lama"'s roster to "Anita" and "Samira"
      Then both "Anita" and "Samira" appear in its roster

    @spec:lines.player-on-several-lines
    Scenario: The same Player may be on more than one Line
      Given a Line "Kiwi" rostering "Sophie"
      And a Line "Bäri"
      When the coach sets "Bäri"'s roster to "Sophie"
      Then "Sophie" appears in both "Kiwi"'s and "Bäri"'s rosters

    @spec:lines.roster-unknown-player
    Scenario: Rostering a Player who does not exist
      Given a Line "Kiwi"
      When the coach sets its roster to a player id that does not exist
      Then the response is not found

    @spec:lines.players-are-team-scoped
    Scenario: The Player pool is the Team's, not a Line's
      Given a Line "Kiwi" rostering "Gina"
      And the Team Player "Nives", on no Line
      When the Team's Players are listed
      Then both "Gina" and "Nives" are returned

  Rule: A Rating is a single current value, never a history (ADR-0006)

    @spec:lines.rating-first-time
    Scenario: Rating a Skill for the first time
      Given a Line "Kiwi" not yet associated with the Skill "Passgenauigkeit"
      When the coach rates it 60
      Then the Line-Skill association is created carrying 60

    @spec:lines.rating-overwrites
    Scenario: Re-rating a Skill overwrites the previous value
      Given a Line "Kiwi" not yet associated with the Skill "Passgenauigkeit"
      And the coach has rated it 60
      When the coach rates it 80
      Then the association carries 80 and no earlier value is kept

    @spec:lines.rating-range
    Scenario Outline: A Rating is a 0-100 scalar
      Given a Line "Kiwi" not yet associated with the Skill "Passgenauigkeit"
      When the coach rates it <rating>
      Then the request is rejected as a bad request

      Examples:
        | rating |
        | -1     |
        | 101    |

    @spec:lines.skill-disassociate
    Scenario: Dropping a Skill from a Line
      Given a Line "Bäri" not yet associated with the Skill "Bully-Kontrolle"
      And the coach has rated it 50
      When the coach removes that association
      Then the Skill no longer appears on the Line, and the catalog Skill itself is untouched

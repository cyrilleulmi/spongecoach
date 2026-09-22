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
      And it is assigned a color from the fixed dial palette, kept for the rest of its life

    @spec:lines.create-requires-name
    Scenario: A Line needs a name
      When the coach creates a Line with a blank name
      Then the request is rejected as a bad request

    @spec:lines.list-with-player-count
    Scenario: The Line list carries each Line's roster size
      Given a Line "Kiwi" with 4 rostered Players
      When the Line list is read
      Then "Kiwi" is listed with a player count of 4

    @spec:lines.detail-embeds-associations
    Scenario: A Line's detail embeds everything the line screen needs in one read
      Given a Line "Kiwi" with rostered Players, rated Skills, Development goals and Focuses
      When that Line is read by id
      Then its roster, its Skill ratings, its Development goals and its Focuses come back inline

    @spec:lines.unknown-is-not-found
    Scenario: An unknown Line
      When a Line that does not exist is read
      Then the response is a not-found error envelope

  Rule: Soft deletion preserves history (ADR-0004)

    @spec:lines.delete-is-soft
    Scenario: Deleting a Line hides it but keeps its history
      Given a Line "Kiwi" with a roster
      When the coach deletes it
      Then it no longer appears in the Line list
      But its row and its roster still exist, so Events it attended still show it

    @spec:lines.delete-twice
    Scenario: Deleting an already deleted Line
      Given a deleted Line
      When the coach deletes it again
      Then the response is not found

    @spec:lines.list-deleted
    Scenario: Deleted Lines can be listed for restoring
      Given a deleted Line "Kiwi"
      When the deleted-Line list is read
      Then "Kiwi" is listed with its name and player count, most recently deleted first

    @spec:lines.restore
    Scenario: Restoring a deleted Line
      Given a deleted Line "Kiwi"
      When the coach restores it
      Then it appears in the Line list again with its roster intact

    @spec:lines.restore-only-deleted
    Scenario: Restoring a Line that was never deleted
      Given an active Line
      When the coach restores it
      Then the response is not found

  Rule: The roster is a selection of the Team's Players (ADR-0008)

    @spec:lines.roster-replace
    Scenario: Setting the roster replaces it wholesale
      Given a Line "Kiwi" rostering Carmela and Debi
      When the coach sets its roster to Carmela and Gina
      Then its roster is Carmela and Gina, and Debi has left it

    @spec:lines.roster-associates-existing-players
    Scenario: Rostering existing Team Players
      Given the Team Players Carmela and Debi, on no Line
      When the coach adds both to "Kiwi"
      Then both appear in "Kiwi"'s roster

    @spec:lines.player-on-several-lines
    Scenario: The same Player may be on more than one Line
      Given a Player Carmela rostered on "Kiwi"
      When the coach also rosters her on "Bäri"
      Then she appears in both rosters

    @spec:lines.roster-unknown-player
    Scenario: Rostering a Player who does not exist
      When the coach sets a roster containing an unknown player id
      Then the response is not found

    @spec:lines.players-are-team-scoped
    Scenario: The Player pool is the Team's, not a Line's
      When the Team's Players are listed
      Then every Player is returned regardless of which Lines they are on

  Rule: A Rating is a single current value, never a history (ADR-0006)

    @spec:lines.rating-first-time
    Scenario: Rating a Skill for the first time
      Given a Line "Kiwi" not yet associated with the Skill "Passgenauigkeit"
      When the coach rates it 60
      Then the Line-Skill association is created carrying 60

    @spec:lines.rating-overwrites
    Scenario: Re-rating a Skill overwrites the previous value
      Given "Kiwi" rates "Passgenauigkeit" at 60
      When the coach rates it 80
      Then the association carries 80 and no earlier value is kept

    @spec:lines.rating-range
    Scenario Outline: A Rating is a 0-100 scalar
      When the coach rates a Skill <rating>
      Then the request is rejected as a bad request

      Examples:
        | rating |
        | -1     |
        | 101    |

    @spec:lines.skill-disassociate
    Scenario: Dropping a Skill from a Line
      Given "Kiwi" is associated with "Passgenauigkeit"
      When the coach removes that association
      Then the Skill no longer appears on the Line, and the catalog Skill itself is untouched

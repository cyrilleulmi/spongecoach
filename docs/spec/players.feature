Feature: Players
  A Player is a member of the Team, on zero or more Lines. Each Player has their own Ratings on
  Player skills and their own Player development goals — separate lists from the Line Skill and
  Development goal lists, sharing no rows with them (ADR-0015). Players are created by seed only.

  Background:
    Given the single seeded Team

  Rule: A Player is browsed with the Lines they are on

    @spec:players.list-with-lines
    Scenario: The Player list carries each Player's Lines
      Given a Line "Kiwi" rostering "Carmela"
      And the Team Player "Nives", on no Line
      When the Team's Players are listed
      Then "Carmela" is listed with the Line "Kiwi", and "Nives" with no Line

    @spec:players.list-hides-deleted-lines
    Scenario: A deleted Line is not shown on its Players
      Given a Line "Kiwi" rostering "Carmela"
      And the coach has deleted "Kiwi"
      When the Team's Players are listed
      Then "Carmela" is listed with no Line

    @spec:players.detail-embeds-profile
    Scenario: A Player's detail embeds everything the player screen needs in one read
      Given a Line "Bäri" rostering "Rahel"
      And "Rahel" is rated 72 on the Player skill "Ausdauer"
      And "Rahel" has the Player development goal "Mehr Abschlüsse suchen"
      When the Player "Rahel" is read by id
      Then their Lines, Player skill Ratings and Player development goals come back inline

    @spec:players.unknown-is-not-found
    Scenario: An unknown Player
      When a Player that does not exist is read
      Then the response is a not-found error envelope

  Rule: A Player rating is a single current value, never a history

    @spec:players.rating-first-time
    Scenario: Rating a Player skill for the first time
      Given the Team Player "Carmela", on no Line
      And the Player skill "Schusstechnik"
      When the coach rates "Carmela" 60 on that Player skill
      Then "Carmela" carries 60 on that Player skill

    @spec:players.rating-overwrites
    Scenario: Re-rating a Player skill overwrites the previous value
      Given the Team Player "Carmela", on no Line
      And "Carmela" is rated 60 on the Player skill "Schusstechnik"
      When the coach rates "Carmela" 85 on that Player skill
      Then "Carmela" carries 85 on that Player skill, and no earlier value is kept

    @spec:players.rating-range
    Scenario Outline: A Player rating is a 0-100 scalar
      Given the Team Player "Carmela", on no Line
      And the Player skill "Schusstechnik"
      When the coach rates "Carmela" <rating> on that Player skill
      Then the request is rejected as a bad request

      Examples:
        | rating |
        | -1     |
        | 101    |

    @spec:players.skill-remove
    Scenario: Dropping a Player skill from a Player
      Given the Team Player "Carmela", on no Line
      And "Carmela" is rated 60 on the Player skill "Schusstechnik"
      When the coach removes that Player skill from "Carmela"
      Then "Carmela" no longer carries it, and the Player skill itself is still listed

  Rule: Player lists are separate from the Line lists (ADR-0015)

    @spec:players.line-skill-is-not-a-player-skill
    Scenario: A Line Skill cannot be rated on a Player
      Given a Line "Kiwi" rostering "Carmela"
      And "Kiwi" rates the Skill "Passgenauigkeit" at 70
      When the coach rates "Carmela" 60 on the Line Skill "Passgenauigkeit"
      Then the response is not found

    @spec:players.line-ratings-stay-on-the-line
    Scenario: A Line's Ratings do not show on its Players
      Given a Line "Kiwi" rostering "Carmela"
      And "Kiwi" rates the Skill "Passgenauigkeit" at 70
      When the Player "Carmela" is read by id
      Then they carry no Player skill Ratings

    @spec:players.create-skill
    Scenario: Creating a Player skill from the player screen
      When the coach creates the Player skill "Kommunikation" with the color "#8a7a2e"
      Then it is listed among the Player skills with that color, and not among the Line Skills

    @spec:players.create-goal
    Scenario: Creating a Player development goal from the player screen
      When the coach creates the Player development goal "Konstanz über 60 Minuten" with the color "#4f7a3f"
      Then it is listed among the Player development goals with that color, and not among the Line Development goals

    @spec:players.create-requires-name
    Scenario: A Player skill needs a name
      When the coach creates a Player skill with a blank name
      Then the request is rejected as a bad request

  Rule: Setting a Player's Development goals replaces them wholesale

    @spec:players.goals-replace
    Scenario: Setting the Player development goals replaces the whole set
      Given the Team Player "Debi", on no Line
      And "Debi" has the Player development goal "Mehr Abschlüsse suchen"
      And the Player development goal "Mehr Pässe in die Tiefe"
      When the coach sets "Debi"'s Player development goals to "Mehr Pässe in die Tiefe"
      Then "Debi" has "Mehr Pässe in die Tiefe" but no longer "Mehr Abschlüsse suchen"

    @spec:players.goal-unknown
    Scenario: Associating a Player development goal that does not exist
      Given the Team Player "Debi", on no Line
      When the coach sets "Debi"'s Player development goals to an id that does not exist
      Then the response is not found

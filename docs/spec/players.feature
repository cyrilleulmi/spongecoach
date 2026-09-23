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

  Rule: A Player's Avatar is a single current painted image, never a history (ADR-0016)

    @spec:players.avatar-set
    Scenario: Saving a painted Avatar
      Given the Team Player "Carmela", on no Line
      When the coach saves a painted Avatar for "Carmela"
      Then "Carmela"'s Avatar is that image, and the Player list and detail carry its version

    @spec:players.avatar-overwrites
    Scenario: Saving again overwrites the previous Avatar
      Given the Team Player "Carmela", on no Line
      And "Carmela" has a painted Avatar
      When the coach saves a painted Avatar for "Carmela"
      Then "Carmela"'s Avatar is the new image, under a new version

    @spec:players.avatar-none-by-default
    Scenario: A Player without an Avatar shows initials
      Given the Team Player "Nives", on no Line
      When "Nives"'s Avatar is read
      Then the response is a not-found error envelope

    @spec:players.avatar-remove
    Scenario: Removing an Avatar goes back to initials
      Given the Team Player "Carmela", on no Line
      And "Carmela" has a painted Avatar
      When the coach removes "Carmela"'s Avatar
      Then "Carmela" carries no Avatar version, and their Avatar is not found

    @spec:players.avatar-must-be-small-square-png
    Scenario Outline: An Avatar is a square PNG of at most 512 px and 200 KB
      Given the Team Player "Carmela", on no Line
      When the coach saves <image> as "Carmela"'s Avatar
      Then the request is rejected as a bad request

      Examples:
        | image                   |
        | a text file             |
        | a 300x200 PNG           |
        | a 1024x1024 PNG         |
        | a noisy PNG over 200 KB |

    @spec:players.avatar-unknown-player
    Scenario: Saving an Avatar for a Player that does not exist
      When the coach saves a painted Avatar for a Player that does not exist
      Then the response is a not-found error envelope

    @spec:players.avatar-seeded
    Scenario: The seeded Players start with a painted Avatar
      When the seeded Player "Carmela" is read by id
      Then they carry an Avatar version, and their Avatar is a 512 px PNG

    @spec:players.avatar-version-on-roster
    Scenario: A Line's roster carries each Player's Avatar version
      Given a Line "Kiwi" rostering "Carmela"
      And "Carmela" has a painted Avatar
      When "Kiwi" is read by id
      Then "Carmela" is on the roster with their Avatar version

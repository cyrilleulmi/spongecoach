Feature: Who may change what
  Every request acts as a User, picked in the app's user dropdown until real authentication arrives
  (ADR-0017). A User has one Role. Every User may read everything; what differs is what they may
  change. A SysAdmin may change everything. A Coach may change everything in their own Team. A
  Player may change their own Player, the Lines they are on, those Lines' Focus per Event, and their
  own attendance — the last two only while the Event has not passed. A refused write is 403
  "forbidden"; a request with no known User is 401 "unauthenticated".

  Background:
    Given a Line "Kiwi" rostering "Carmela"
    And a Line "Bäri" rostering "Rahel"

  Rule: Every request needs a User, except listing the Users to pick one

    @spec:auth.no-user
    Scenario: A request with no User is refused
      When the Line list is read with no User chosen
      Then the request is refused as unauthenticated

    @spec:auth.unknown-user
    Scenario: A request naming a User that does not exist is refused
      When the Line list is read as a User that does not exist
      Then the request is refused as unauthenticated

    @spec:auth.users-listed-without-user
    Scenario: The Users can be listed without a User, to pick one
      When the Users are listed with no User chosen
      Then the seeded SysAdmin "Admin" and the seeded Coaches "Cyrille", "Jan", "Samuel" and "Anita" are among them

    @spec:auth.me
    Scenario: The current User comes back with the Lines their Player is on
      Given "Carmela" is using the app as a Player
      When the current User is read
      Then it is "Carmela" as a Player, on the Line "Kiwi"

  Rule: Every User may read everything

    @spec:auth.player-reads-everything
    Scenario: A Player reads another Line
      Given "Carmela" is using the app as a Player
      When they read the Line "Bäri"
      Then the request succeeds

  Rule: A SysAdmin and a Coach may change everything in the Team

    @spec:auth.sysadmin-manages
    Scenario: The SysAdmin creates an Iteration
      Given the SysAdmin is using the app
      When they create an Iteration "Vorbereitung"
      Then the request succeeds

    @spec:auth.coach-manages
    Scenario: A Coach creates a Line
      Given a Coach "Cyrille" is using the app
      When they create a Line "Lama"
      Then the request succeeds

    @spec:auth.coach-edits-any-player
    Scenario: A Coach answers for a Player and edits their profile
      Given an upcoming Event "Einheit 1"
      And the Player skill "Ausdauer"
      And a Coach "Cyrille" is using the app
      When they set "Rahel" to "ATTENDING" on "Einheit 1"
      Then the request succeeds
      When they rate "Rahel" 60 on the Player skill "Ausdauer"
      Then the request succeeds

    @spec:auth.coach-edits-done-event
    Scenario: A Coach still changes a done Event
      Given a done Event "Einheit 0"
      And a Coach "Cyrille" is using the app
      When they set "Bäri"'s Focus on "Einheit 0" to "Cross-Pässe unter Druck"
      Then the request succeeds

  Rule: A Player changes the Lines they are on, not the others

    @spec:auth.player-edits-own-line
    Scenario: A Player rates and renames their own Line
      Given the Skill "Passgenauigkeit"
      And "Carmela" is using the app as a Player
      When they rate "Kiwi" 80 on the Skill "Passgenauigkeit"
      Then the request succeeds
      When they rename the Line "Kiwi" to "Kiwi 2"
      Then the request succeeds

    @spec:auth.player-cannot-edit-other-line
    Scenario: A Player cannot rate a Line they are not on
      Given the Skill "Passgenauigkeit"
      And "Carmela" is using the app as a Player
      When they rate "Bäri" 80 on the Skill "Passgenauigkeit"
      Then the request is refused as forbidden

    @spec:auth.player-cannot-create-line
    Scenario: A Player cannot create or delete Lines
      Given "Carmela" is using the app as a Player
      When they create a Line "Lama"
      Then the request is refused as forbidden
      When they delete the Line "Kiwi"
      Then the request is refused as forbidden

    @spec:auth.player-creates-catalog-entry
    Scenario: A Player may add to the Skill list
      Given "Carmela" is using the app as a Player
      When they create the Skill "Schusshärte"
      Then the request succeeds

  Rule: A Player changes only their own Player

    @spec:auth.player-edits-self
    Scenario: A Player rates themselves
      Given the Player skill "Ausdauer"
      And "Carmela" is using the app as a Player
      When they rate "Carmela" 70 on the Player skill "Ausdauer"
      Then the request succeeds

    @spec:auth.player-cannot-edit-other-player
    Scenario: A Player cannot rate another Player
      Given the Player skill "Ausdauer"
      And "Carmela" is using the app as a Player
      When they rate "Rahel" 70 on the Player skill "Ausdauer"
      Then the request is refused as forbidden

  Rule: A Player answers only for themselves, and only before the Event

    @spec:auth.player-answers-for-self
    Scenario: A Player answers for themselves
      Given an upcoming Event "Einheit 1"
      And "Carmela" is using the app as a Player
      When they set "Carmela" to "DECLINED" on "Einheit 1"
      Then the request succeeds

    @spec:auth.player-cannot-answer-for-other
    Scenario: A Player cannot answer for someone else
      Given an upcoming Event "Einheit 1"
      And "Carmela" is using the app as a Player
      When they set "Rahel" to "ATTENDING" on "Einheit 1"
      Then the request is refused as forbidden

    @spec:auth.player-done-event-locked
    Scenario: A Player cannot answer once the Event has passed
      Given a done Event "Einheit 0"
      And "Carmela" is using the app as a Player
      When they set "Carmela" to "ATTENDING" on "Einheit 0"
      Then the request is refused as forbidden

  Rule: A Player sets the Focus of their own Lines only, and only before the Event

    @spec:auth.player-sets-own-line-focus
    Scenario: A Player sets their own Line's Focus
      Given an upcoming Event "Einheit 1"
      And "Carmela" is using the app as a Player
      When they set "Kiwi"'s Focus on "Einheit 1" to "Spielaufbau aus der tiefen Zone"
      Then the request succeeds

    @spec:auth.player-cannot-set-other-line-focus
    Scenario: A Player cannot set another Line's Focus
      Given an upcoming Event "Einheit 1"
      And "Carmela" is using the app as a Player
      When they set "Bäri"'s Focus on "Einheit 1" to "Cross-Pässe unter Druck"
      Then the request is refused as forbidden

    @spec:auth.player-cannot-replace-focus-set
    Scenario: A Player cannot replace an Event's whole Focus set
      Given an upcoming Event "Einheit 1"
      And "Carmela" is using the app as a Player
      When they replace "Einheit 1"'s Focus set with "Spielaufbau aus der tiefen Zone" for "Kiwi"
      Then the request is refused as forbidden

    @spec:auth.player-focus-locked-when-done
    Scenario: A Player cannot set their Line's Focus once the Event has passed
      Given a done Event "Einheit 0"
      And "Carmela" is using the app as a Player
      When they set "Kiwi"'s Focus on "Einheit 0" to "Spielaufbau aus der tiefen Zone"
      Then the request is refused as forbidden

  Rule: A Player cannot plan the timeline

    @spec:auth.player-cannot-manage-timeline
    Scenario: A Player cannot create an Iteration
      Given "Carmela" is using the app as a Player
      When they create an Iteration "Vorbereitung"
      Then the request is refused as forbidden

  Rule: Only a Coach starts the interpreter; every team member draws and edits by hand

    Uploading photos, answering, chatting and retrying can start a paid interpreter job, so they are
    a Coach's. Everything else, drawing, editing the script, photos, deleting and restoring, costs
    nothing and is any team member's (ADR-0018, ADR-0020). With one Team, every Player is a member.

    @spec:auth.coach-changes-drills
    Scenario Outline: A Coach changes a Drill
      Given a Coach "Cyrille" is using the app
      And a Drill "Bresil" waiting for answers
      When they <change> the Drill "Bresil"
      Then the request succeeds

      Examples:
        | change                        |
        | upload another drill like     |
        | answer the questions of       |
        | send a correction for         |
        | retry                         |
        | rename                        |
        | save a hand-edited script for |
        | revert                        |
        | add a sketch to               |
        | remove the first sketch of    |
        | draw a copy of                |
        | delete                        |

    @spec:auth.player-changes-drills-by-hand
    Scenario Outline: A Player draws and edits a Drill by hand
      Given "Carmela" is using the app as a Player
      And a Drill "Bresil" waiting for answers
      When they <change> the Drill "Bresil"
      Then the request succeeds

      Examples:
        | change                        |
        | rename                        |
        | save a hand-edited script for |
        | revert                        |
        | add a sketch to               |
        | remove the first sketch of    |
        | draw a copy of                |
        | delete                        |

    @spec:auth.player-cannot-start-the-interpreter
    Scenario Outline: A Player cannot start the interpreter
      Given "Carmela" is using the app as a Player
      And a Drill "Bresil" waiting for answers
      When they <change> the Drill "Bresil"
      Then the request is refused as forbidden

      Examples:
        | change                    |
        | upload another drill like |
        | answer the questions of   |
        | send a correction for     |
        | retry                     |

    @spec:auth.coach-restores-drills
    Scenario Outline: A Coach restores a Drill or a sketch
      Given a Coach "Cyrille" is using the app
      And <starting point>
      When they <restore>
      Then the request succeeds

      Examples:
        | starting point                                 | restore                                     |
        | a deleted Drill "Bresil"                       | restore the Drill "Bresil"                  |
        | a Drill "Bresil" whose first sketch was removed | restore the first sketch of the Drill "Bresil" |

    @spec:auth.player-restores-drills
    Scenario Outline: A Player restores a Drill or a sketch
      Given "Carmela" is using the app as a Player
      And <starting point>
      When they <restore>
      Then the request succeeds

      Examples:
        | starting point                                 | restore                                     |
        | a deleted Drill "Bresil"                       | restore the Drill "Bresil"                  |
        | a Drill "Bresil" whose first sketch was removed | restore the first sketch of the Drill "Bresil" |

    @spec:auth.player-watches-drills
    Scenario: A Player reads a Drill
      Given "Carmela" is using the app as a Player
      And a Drill "Bresil" waiting for answers
      When they read the Drill "Bresil"
      Then the request succeeds

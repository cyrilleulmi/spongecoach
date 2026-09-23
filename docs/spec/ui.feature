Feature: What the coach sees
  Three areas: "Team-Übersicht" (`/team`, the landing screen), the Iteration timeline where Events
  — and each Line's Focus text per Event — are planned; "Blöcke" (`/lines`), where a Line's roster,
  ratings and Development goals are managed; and "Spieler" (`/players`, `/players/:id`), the Player
  list and each Player's own Ratings and Development goals. UI copy is German; domain terms in code
  and API stay English (docs/glossary.md).

  Rule: The line screen edits one Line at a time

    @spec:ui.first-line-selected
    Scenario: The first Line is selected on arrival
      When the coach opens the line screen
      Then the first Line is selected and its roster, Skills and Development goals are shown

    @spec:ui.switch-line-refetches
    Scenario: Switching Line loads that Line's detail
      When the coach picks another Line
      Then that Line's detail is fetched and shown

    @spec:ui.rating-is-optimistic
    Scenario: A Rating click lands immediately
      When the coach clicks a rating segment
      Then the bar updates before the server answers, and the new Rating is saved without a refetch

    @spec:ui.rating-rolls-back
    Scenario: A failed Rating save is undone
      Given a rating save that fails
      When the coach clicks a rating segment
      Then the bar returns to its previous value

    @spec:ui.roster-dialog-commits-once
    Scenario: The roster dialog saves the whole roster on confirm
      When the coach toggles Players in the roster dialog and confirms
      Then the Line's full Player list is saved in one call

    @spec:ui.goal-toggle-saves-full-set
    Scenario: Toggling a Development goal saves the whole association set
      When the coach toggles a Development goal chip
      Then the Line's full Development goal list is saved

    @spec:ui.create-skill-auto-associates
    Scenario: A Skill created from the line screen is associated straight away
      When the coach creates a Skill with a chosen color
      Then it is added to the catalog and associated with the selected Line

    @spec:ui.recolor-from-line-screen
    Scenario: Recoloring a Skill from its swatch
      When the coach picks a new color for a Skill
      Then the catalog Skill is recolored

    @spec:ui.line-lifecycle-menu
    Scenario: Creating, deleting and restoring a Line from the overflow menu
      When the coach creates a Line, deletes one after confirming, and restores a deleted one
      Then the Line list reflects each step, and deleting selects the first remaining Line

  Rule: The timeline shows one Iteration at a time

    @spec:ui.timeline-renders-iteration
    Scenario: The timeline shows the current Iteration's Events as dials
      When the coach opens the team overview
      Then the first Iteration's Events are drawn in datetime order

    @spec:ui.next-event-marked
    Scenario: The next Event is marked
      Then the first Event whose datetime has not passed, in Iteration then datetime order, is
      marked "NÄCHSTES" and selected by default

    @spec:ui.dial-shows-planning-progress
    Scenario: A dial slice per attending Line, lit when that Line has a Focus
      Then each Event's dial has one equal slice per attending Line from that Event's own snapshot,
      lit in the Line's color only where that Line has a Focus set

    @spec:ui.roster-icons-show-answers
    Scenario: An icon per rostered Player shows their answer
      Then each attending Line's Players appear as icons: filled when answered, hollow while
      pending, and struck through when declined

    @spec:ui.iteration-navigation
    Scenario: Moving between Iterations
      When the coach uses the Iteration selector
      Then the timeline switches Iteration, and the arrows stop at the ends

  Rule: A done Event is read-only by default (ADR-0012)

    @spec:ui.event-readonly-when-done
    Scenario: An Event whose datetime has passed cannot be edited
      When the coach selects a done Event
      Then its name, datetime, Focus text fields, attendance controls and delete button are all disabled

    @spec:ui.edit-anyway
    Scenario: The coach can lift the lock per Event
      Given a done Event
      When the coach chooses "Trotzdem bearbeiten"
      Then editing is enabled again for that Event only, and re-locks on the next selection

    @spec:ui.future-event-editable
    Scenario: An Event still to come is editable
      When the coach selects an Event whose datetime has not passed
      Then nothing is disabled

  Rule: Planning an Event from the detail panel

    @spec:ui.set-focus-from-detail
    Scenario: Setting a Line's Focus for the selected Event
      When the coach types a Focus into a Line's text field and it loses focus
      Then the Event's whole attachment set is saved with that change and the timeline reloads

    @spec:ui.clear-focus-from-detail
    Scenario: Clearing a Line's Focus
      When the coach empties a Line's Focus text field
      Then that Line's attachment is dropped from the saved set

    @spec:ui.same-focus-again
    Scenario: Repeating a Line's most recent Focus
      Given a Line has a Focus set on an earlier Event and none on the selected one
      When the coach chooses "Gleicher Fokus wie zuletzt" for that Line
      Then that earlier Focus text is copied into the field and saved as this Event's own text, not a reference

    @spec:ui.attendance-from-detail
    Scenario: Answering for a Player
      When the coach sets a Player's answer, with a reason when declining
      Then the answer is saved and the Event reloads

    @spec:ui.attending-counter
    Scenario: Each Line row counts who is coming
      Then each Line row shows how many of its snapshotted Players have said yes, out of its total

    @spec:ui.add-training
    Scenario: Adding a Training
      When the coach adds a Training
      Then it is created in the current Iteration at the next free slot and the timeline reloads

    @spec:ui.create-iteration
    Scenario: Creating an Iteration
      When the coach creates an Iteration
      Then the timeline jumps to it

    @unverified
    @spec:ui.reschedule-conflict-banner
    Scenario: A datetime already taken is reported
      When the coach moves an Event onto a datetime another Event in the Iteration already uses
      Then the screen says "Diese Zeit ist in dieser Iteration schon vergeben."

    @unverified
    @spec:ui.next-free-slot
    Scenario: Where a new Event lands
      When the coach adds an Event to an Iteration
      Then it is scheduled an hour after the Iteration's latest Event, or at the next full hour if
      the Iteration is empty

    @unverified
    @spec:ui.rename-match
    Scenario: Renaming a Match from the timeline
      When the coach edits a Match's name
      Then the new name is saved and shown on its dial

  Rule: The player screens show each Player's own profile (ADR-0015)

    @spec:ui.player-list
    Scenario: The Player list
      When the coach opens the Player list
      Then every Player is shown with avatar, name and Line badges, each linking to their player screen

    @spec:ui.player-view-shows-profile
    Scenario: A Player's screen
      When the coach opens a Player
      Then their name, initials avatar, Line badges, Player skill Ratings and Player development goals are shown

    @spec:ui.player-not-found
    Scenario: An unknown Player
      When the coach opens a Player that does not exist
      Then a "Spieler nicht gefunden" page is shown

    @spec:ui.player-rating-optimistic
    Scenario: A Player rating click lands immediately and is undone on failure
      When the coach clicks a rating segment on a Player skill
      Then the bar updates before the server answers, and returns to its previous value if the save fails

    @spec:ui.player-goal-toggle-saves-full-set
    Scenario: Toggling a Player development goal saves the whole set
      When the coach toggles a Player development goal chip
      Then the Player's full Player development goal list is saved

    @spec:ui.create-player-skill-auto-associates
    Scenario: A Player skill created from the player screen is rated straight away
      When the coach creates a Player skill with a chosen color
      Then it is added to the Player skill list and rated on that Player

  Rule: Players and Lines link to each other

    @spec:ui.player-link-from-line
    Scenario: A roster row opens the Player
      Then each roster row's avatar and name link to that Player, while its remove button stays a separate control

    @spec:ui.player-link-from-event
    Scenario: An attendance row opens the Player
      Then each attendance row in the event detail links to that Player, with their avatar

    @spec:ui.line-badge-jumps-to-line
    Scenario: A Line badge opens that Line
      When the coach clicks a Line badge on a Player
      Then the line screen opens with that Line selected

  Rule: Shell

    @spec:ui.navigation
    Scenario: Moving between the screens
      Then Team-Übersicht, Blöcke and Spieler are reachable from the header

    @spec:ui.default-route-is-team
    Scenario: Landing on the team overview
      Then the root path lands on the team overview

    @spec:ui.theme-toggle
    Scenario: Light and dark
      Then the chosen theme is remembered, falling back to the system preference

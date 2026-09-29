Feature: What the coach sees
  Three areas: "Team-Übersicht" (`/team`, the landing screen), the Iteration timeline where Events
  — and each Line's Focus text per Event — are planned; "Blöcke" (`/lines`), where a Line's roster,
  ratings and Development goals are managed; and "Spieler" (`/players`, `/players/:id`), the Player
  list and each Player's own Ratings and Development goals. UI copy is German; domain terms in code
  and API stay English (docs/glossary.md). "The coach" below is any User who may make that change;
  what a Player sees differently is its own Rule (ADR-0017).

  Rule: The line screen edits one Line at a time

    @spec:ui.first-line-selected
    Scenario: The first Line is selected on arrival
      When the coach opens the line screen
      Then the first Line is selected and its roster, Skills and Development goals are shown

    @spec:ui.opens-own-line
    Scenario: A User on a Line lands on it
      Given the User is on one or more Lines
      When they open the line screen
      Then the first of their Lines is selected, not the first Line overall

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
      Then the shown Iteration's Events are drawn in datetime order

    @spec:ui.opens-on-next-event
    Scenario: The timeline opens where the next Event is
      When the coach opens the team overview
      Then the Iteration holding the next Event is shown, or the latest Iteration once every Event
      has passed

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
      Then its name, datetime and delete button are disabled, each Focus and answer is plain text, and
      a Line with no Focus shows no field at all

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

    @spec:ui.attendance-aligned
    Scenario: Answers line up
      Then every attendance row's answer sits in the same column, however many or long its Line
      badges are, with a decline reason on a line of its own below

    @spec:ui.set-focus-from-detail
    Scenario: Setting a Line's Focus for the selected Event
      When the coach types a Focus into a Line's text field and it loses focus
      Then that Line's Focus alone is saved and the timeline reloads

    @spec:ui.clear-focus-from-detail
    Scenario: Clearing a Line's Focus
      When the coach empties a Line's Focus text field
      Then that Line's Focus is cleared

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
      Then their name, Avatar, Line badges, Player skill Ratings and Player development goals are shown

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

  Rule: A Player paints their own Avatar (ADR-0016)

    @spec:ui.avatar-painted-or-initials
    Scenario: A painted Avatar replaces the initials wherever the Player is shown
      Given a Player with a painted Avatar and one without
      Then the first shows their painting and the second their initials, falling back to initials if the image fails to load

    @spec:ui.avatar-painter-opens
    Scenario: The painter opens from the player screen
      When the coach clicks the Avatar on a player screen
      Then the painter opens with brush, spray, fill and eraser tools, three brush sizes and a color palette

    @spec:ui.avatar-eraser
    Scenario: The eraser paints the blank background back
      When the coach paints a stroke and erases over it
      Then the stroke is gone without a leftover edge, and the erased area is the blank background color, not see-through

    @spec:ui.avatar-painter-shows-crop
    Scenario: The painter shows what the round Avatar will show
      When the painter is open
      Then a circle guide marks the visible area, the corners outside it are dimmed, and a round preview follows every stroke

    @spec:ui.avatar-painter-edit-or-new
    Scenario: Painting on the existing Avatar or starting fresh
      Given a Player with a painted Avatar
      When the coach opens the painter
      Then it starts on the existing Avatar ("Bearbeiten"), and "Neu" swaps to a blank canvas as an undoable step

    @spec:ui.avatar-painter-undo
    Scenario: Undo and redo
      When the coach paints and then undoes
      Then the canvas goes back one step, and redo brings it back

    @spec:ui.avatar-save
    Scenario: Saving the painting
      When the coach clicks "Speichern"
      Then the canvas is uploaded as a PNG, the painter closes and the new Avatar is shown; if the upload fails the painter stays open with an error

    @spec:ui.avatar-cancel
    Scenario: Cancelling the painter
      When the coach clicks "Abbrechen" or presses Escape after painting
      Then they are asked to confirm, and on confirming nothing is saved

    @spec:ui.avatar-remove
    Scenario: Removing the Avatar
      Given a Player with a painted Avatar
      When the coach clicks "Entfernen" in the painter
      Then the Avatar is removed and the initials are shown again

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

  Rule: Who is using the app decides what can be changed (ADR-0017)

    @spec:ui.user-switcher
    Scenario: Picking the User from the header
      Then the header has a dropdown of every User, grouped Admin, Trainer, Spieler, and choosing one
      reloads the app as that User

    @spec:ui.default-user
    Scenario: The first visit
      Given no User has been chosen in this browser
      When the app opens
      Then the SysAdmin is chosen and remembered

    @spec:ui.no-screen-without-user
    Scenario: No screen before a User is known
      Then no screen is shown until the User is loaded, and if the Users cannot be loaded the app says so

    @spec:ui.player-line-access
    Scenario: A Player on the line screen
      Given a Player is using the app
      Then a Line they are not on is shown read-only, a Line they are on can be edited, and creating,
      deleting and restoring Lines is not offered

    @spec:ui.player-profile-access
    Scenario: A Player on the player screen
      Given a Player is using the app
      Then another Player is shown read-only, and on their own screen they can rate, set goals and paint their Avatar

    @spec:ui.player-edits-own-on-event
    Scenario: A Player on an Event still to come
      Given a Player is using the app
      When they select an Event whose datetime has not passed
      Then only their own Lines' Focus fields and their own answer are fields; other Lines' Focus and
      other Players' answers are plain text, an empty Focus they cannot set is not shown, and the
      Event's name, datetime and delete button cannot be changed

    @spec:ui.player-done-event-locked
    Scenario: A Player on a done Event
      Given a Player is using the app
      When they select an Event whose datetime has passed
      Then nothing can be changed, and "Trotzdem bearbeiten" is not offered

    @spec:ui.player-cannot-plan-timeline
    Scenario: A Player cannot plan the timeline
      Given a Player is using the app
      Then renaming and deleting Iterations, creating Iterations and adding Events are not offered

  Rule: Drills are animated from photos of the tactic board (ADR-0018, ADR-0019)

    @spec:ui.drill-list
    Scenario: The Drill list
      Then "Übungen" lists every Drill with its status, tags and number of photos, the most recently changed first

    @spec:ui.drill-list-filter
    Scenario: Filtering Drills by tag
      When the coach picks one or more tags
      Then only the Drills carrying all of them are listed

    @spec:ui.drill-upload
    Scenario: Uploading a Drill
      When the coach adds photos of the tactic board, orders and turns them, adds notes, picks tags and how the photos relate, and uploads
      Then the photos are sent upright and downscaled, and the new Drill opens

    @spec:ui.drill-upload-legend
    Scenario: The upload screen shows how to draw
      Then next to the upload form a legend explains the usual symbols

    @spec:ui.drill-waits-while-interpreting
    Scenario: The Drill says when Claude is still reading it
      Given a Drill being interpreted
      Then its screen says Claude is reading the sketches, and checks again every few seconds until it is done

    @spec:ui.drill-questions
    Scenario: Answering Claude's questions
      Given a Drill waiting for answers
      Then each question offers its suggested answers and a text field
      And the coach sends the answers, or lets Claude decide with "Rate einfach"

    @spec:ui.drill-failed-retry
    Scenario: A failed interpretation can be tried again
      Given a failed Drill
      Then its screen shows why, and the coach can try again

    @spec:ui.drill-plays
    Scenario: A Drill plays on the rink
      Given a ready Drill
      Then its Stage plays on the small-court rink in a loop, with the Step paths drawn faintly
      And the coach can pause, scrub, change the speed and step from one Step to the next

    @spec:ui.drill-stages
    Scenario: A Drill with several Stages
      Given a ready Drill with several Stages
      Then each Stage is a tab, and choosing one plays it

    @spec:ui.drill-readings
    Scenario: What Claude read on a photo
      Then each photo can show the symbols Claude read on it, drawn on the rink
      And while the coach looks at a question, its photo and symbols are highlighted

    @spec:ui.drill-chat-correction
    Scenario: Correcting the animation by chat
      When the coach tells Claude what is wrong
      Then the message and Claude's reply appear in the conversation, the reply with its new version

    @spec:ui.drill-revert
    Scenario: Undoing a change
      When the coach chooses "Rückgängig", or restores an earlier version
      Then that version plays again

    @spec:ui.drill-editor
    Scenario: Editing the animation by hand
      When the coach edits by hand
      Then they can drag start points, waypoints and cones, change a Step's timing and speed, see the run as a timeline, undo, preview and save

    @spec:ui.drill-player-draws
    Scenario: A Player watches, draws and edits Drills, but cannot start Claude
      Given a Player is using the app
      Then "Übungen" is reachable and every Drill plays
      And drawing a Drill, editing it by hand, going back to an earlier version, renaming, deleting and its photos are offered
      And uploading photos for Claude, answering, correcting by chat and retrying are not

    @spec:ui.drill-photos
    Scenario: Photos can be added, removed and brought back
      Given a ready Drill
      When the coach adds photos, removes one, and restores it from "Gelöschte Fotos"
      Then the photos come and go without starting Claude, and a Coach can ask Claude to work new photos in

    @spec:ui.drill-restore
    Scenario: A deleted Drill can be brought back
      When someone chooses "Gelöschte Übungen" on the Drill list and restores one
      Then it is in the list again

  Rule: A Drill is drawn by hand in the animator, on a phone as well as on a desktop (ADR-0020)

    @spec:ui.drill-draw-choose
    Scenario: Choosing how to make a Drill
      When someone opens "Neue Übung"
      Then "Selbst zeichnen" is offered to everyone, and uploading photos for Claude only to a Coach

    @spec:ui.drill-draw-place
    Scenario: Placing figures and objects
      When the coach picks a figure or object from the tool bar and taps the rink
      Then it is placed there, can be picked, moved, renamed and deleted, and there is nothing to save until there is a figure

    @spec:ui.drill-draw-path
    Scenario: Drawing a path
      When the coach drags from a figure with a path tool, or taps the points one by one and presses "Fertig"
      Then a Step is added after that figure's last Step, a freehand stroke is simplified to a few waypoints, and a waypoint can be moved, inserted or deleted

    @spec:ui.drill-pass-during-run
    Scenario: A pass or shot while someone runs
      Given a run drawn on the rink
      When the coach draws a pass or shot starting on the middle of that run
      Then it starts part-way through the run, the runner keeps running, and it stays on that spot when the run gets faster

    @spec:ui.drill-draw-pass-target
    Scenario: Passing to a moving receiver
      When a pass ends on another figure or on its run
      Then the pass goes to that figure, aimed ahead of them so the ball arrives where they will be, and stays aimed when their run changes

    @spec:ui.drill-draw-ball-hint
    Scenario: A pass from someone without the ball
      When a pass or shot starts at a figure that does not have the ball by then
      Then it is drawn anyway, with a hint saying who has no ball

    @spec:ui.drill-timeline-retime
    Scenario: Re-timing on the timeline
      When the coach picks a bar of the timeline and drags it, or drags its grip
      Then the Step starts later or earlier, during, after or with its anchor as it lands, or gets a new speed or duration; an unpicked bar scrolls the timeline instead

    @spec:ui.drill-draw-stages
    Scenario: Several Stages
      When the coach adds, renames, moves, removes a Stage or changes its field
      Then the new Stage starts with the same figures and objects, the order changes with the buttons, and the last Stage cannot be removed

    @spec:ui.drill-draw-save
    Scenario: Saving a drawn Drill
      When the coach saves
      Then the name and tags are asked for, the Drill is created ready with no photos, and it opens; a refused script says why

    @spec:ui.drill-draw-draft
    Scenario: A drawing survives a reload
      When the page is left or reloaded before saving
      Then the drawing is offered again, and can be discarded

    @spec:ui.drill-draw-mobile
    Scenario: Drawing on a phone
      Given a phone in portrait
      Then the rink fills the width, the tools are a bar of labelled buttons at the bottom, the selected item's form is a sheet above it, every control is at least 44 px, and a whole drill can be drawn with touch alone

  Rule: Shell

    @spec:ui.navigation
    Scenario: Moving between the screens
      Then Team-Übersicht, Blöcke, Spieler and Übungen are reachable from the header

    @spec:ui.default-route-is-team
    Scenario: Landing on the team overview
      Then the root path lands on the team overview

    @spec:ui.theme-toggle
    Scenario: Light and dark
      Then the chosen theme is remembered, falling back to the system preference

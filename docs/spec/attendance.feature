Feature: Attendance
  Which Lines attend an Event is a snapshot taken once when the Event is created — every Line active
  at that moment (ADR-0012) — not a live list. One level deeper, each attending Line's roster is
  snapshotted too, so a done Event shows who was actually expected. Each Player holds exactly one
  answer per Event no matter how many attending Lines they are on.

  Rule: Attendance is frozen at Event creation (ADR-0012)

    @spec:attendance.snapshot-on-create
    Scenario: A new Event lists every active Line's Players, all pending
      Given the active Lines "Kiwi" and "Bäri" with their rosters
      When the coach creates an Event
      Then every Player of both Lines is on its attendance list with status PENDING

    @spec:attendance.line-added-later-does-not-join
    Scenario: A Line created after the Event never joins it retroactively
      Given an Event created while only "Kiwi" existed
      When the coach creates the Line "Bäri"
      Then "Bäri" does not appear on that Event

    @spec:attendance.deleted-line-still-shows
    Scenario: A deleted Line still shows on the Events it attended
      Given an Event attended by "Kiwi"
      When the coach deletes "Kiwi"
      Then "Kiwi" still appears on that Event's attending Lines

    @spec:attendance.one-answer-per-player
    Scenario: A Player on two attending Lines has one answer, listing both Lines
      Given a Player rostered on both "Kiwi" and "Bäri", which both attend an Event
      When the Event's attendance is read
      Then she appears once, with both line ids on her single entry

  Rule: A roster change reaches upcoming Events only

    @spec:attendance.roster-add-joins-upcoming
    Scenario: A newly rostered Player joins upcoming Events
      Given a Line attending an Event still to come
      When the coach adds a Player to that Line
      Then the Player is on that Event's attendance list, PENDING

    @spec:attendance.roster-drop-leaves-upcoming
    Scenario: A dropped Player leaves upcoming Events
      Given a Line attending an Event still to come, with Carmela on its roster
      When the coach removes Carmela from that Line
      Then she is no longer on that Event's attendance list

    @spec:attendance.roster-drop-keeps-other-line
    Scenario: Dropping a Player from one of two attending Lines keeps her on the Event
      Given Carmela on both "Kiwi" and "Bäri", which both attend an upcoming Event
      When the coach removes her from "Kiwi"
      Then she is still on that Event's attendance list through "Bäri"

    @spec:attendance.done-event-is-history
    Scenario: A roster change never touches a done Event
      Given a Line attending an Event whose datetime has passed
      When the coach changes that Line's roster
      Then the done Event's attendance snapshot is untouched

  Rule: A Player answers once per Event

    @spec:attendance.set-attending
    Scenario: Saying yes
      Given a pending Player on an Event
      When the coach sets her to ATTENDING
      Then her status is ATTENDING and no decline message is kept

    @spec:attendance.decline-with-message
    Scenario: Saying no with a reason
      When the coach sets a Player to DECLINED with a message
      Then the status and the message are both stored

    @spec:attendance.undecline-clears-message
    Scenario: Changing away from declined drops the reason
      Given a Player DECLINED with a message
      When the coach sets her to ATTENDING
      Then the decline message is cleared

    @spec:attendance.player-not-on-event
    Scenario: Answering for a Player who is not on the Event
      When the coach sets attendance for a Player who is on no attending Line
      Then the response is not found

    @spec:attendance.unknown-status
    Scenario: An unknown status
      When the coach sets a status that is not PENDING, ATTENDING or DECLINED
      Then the request is rejected as a bad request

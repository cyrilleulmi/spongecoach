Feature: Attendance
  Which Lines attend an Event is a snapshot taken once when the Event is created — every Line active
  at that moment (ADR-0012) — not a live list. One level deeper, each attending Line's roster is
  snapshotted too, so a done Event shows who was actually expected. Each Player holds exactly one
  answer per Event no matter how many attending Lines they are on.

  Rule: Attendance is frozen at Event creation (ADR-0012)

    @spec:attendance.snapshot-on-create
    Scenario: A new Event lists every active Line's Players, all pending
      Given a Line "Kiwi" rostering "Carmela" and "Debi"
      And a Line "Bäri" rostering "Rahel"
      When an Event "Einheit 1" is created
      Then "Carmela", "Debi" and "Rahel" are on its attendance list with status "PENDING"

    @spec:attendance.line-added-later-does-not-join
    Scenario: A Line created after the Event never joins it retroactively
      Given a Line "Kiwi" rostering "Carmela"
      And an Event "Einheit 1"
      When the coach creates a Line "Bäri" rostering "Rahel"
      Then "Bäri" does not appear among "Einheit 1"'s attending Lines

    @spec:attendance.deleted-line-still-shows
    Scenario: A deleted Line still shows on the Events it attended
      Given a Line "Kiwi" rostering "Carmela"
      And an Event "Einheit 1"
      When the coach deletes the Line "Kiwi"
      Then "Kiwi" still appears among "Einheit 1"'s attending Lines

    @spec:attendance.one-answer-per-player
    Scenario: A Player on two attending Lines has one answer, listing both Lines
      Given a Line "Kiwi" rostering "Sophie"
      And a Line "Bäri" rostering "Sophie"
      And an Event "Einheit 1"
      When "Einheit 1"'s attendance is read
      Then "Sophie" appears once, carrying both "Kiwi"'s and "Bäri"'s line ids

  Rule: A roster change reaches upcoming Events only

    @spec:attendance.roster-add-joins-upcoming
    Scenario: A newly rostered Player joins upcoming Events
      Given a Line "Kiwi"
      And an upcoming Event "Einheit 1"
      And the Team Player "Carmela", on no Line
      When the coach sets "Kiwi"'s roster to "Carmela"
      Then "Carmela" is on "Einheit 1"'s attendance list with status "PENDING"

    @spec:attendance.roster-drop-leaves-upcoming
    Scenario: A dropped Player leaves upcoming Events
      Given a Line "Kiwi" rostering "Carmela"
      And an upcoming Event "Einheit 1"
      When the coach clears "Kiwi"'s roster
      Then "Carmela" is not on "Einheit 1"'s attendance list

    @spec:attendance.roster-drop-keeps-other-line
    Scenario: Dropping a Player from one of two attending Lines keeps her on the Event
      Given a Line "Kiwi" rostering "Sophie"
      And a Line "Bäri" rostering "Sophie"
      And an upcoming Event "Einheit 1"
      When the coach clears "Kiwi"'s roster
      Then "Sophie" is still on "Einheit 1"'s attendance list through "Bäri" but not through "Kiwi"

    @spec:attendance.done-event-is-history
    Scenario: A roster change never touches a done Event
      Given a Line "Kiwi"
      And a done Event "Einheit 1"
      And the Team Player "Carmela", on no Line
      When the coach sets "Kiwi"'s roster to "Carmela"
      Then "Carmela" is not on "Einheit 1"'s attendance list

  Rule: A Player answers once per Event

    @spec:attendance.set-attending
    Scenario: Saying yes
      Given a Line "Kiwi" rostering "Carmela"
      And an Event "Einheit 1"
      When the coach sets "Carmela" to "ATTENDING" on "Einheit 1"
      Then her status is "ATTENDING" and no decline message is kept

    @spec:attendance.decline-with-message
    Scenario: Saying no with a reason
      Given a Line "Kiwi" rostering "Carmela"
      And an Event "Einheit 1"
      When the coach sets "Carmela" to "DECLINED" on "Einheit 1" with the message "Verletzt"
      Then her status is "DECLINED" and the message "Verletzt" is stored

    @spec:attendance.undecline-clears-message
    Scenario: Changing away from declined drops the reason
      Given a Line "Kiwi" rostering "Carmela"
      And an Event "Einheit 1"
      And the coach has set "Carmela" to "DECLINED" on "Einheit 1" with the message "Verletzt"
      When the coach sets "Carmela" to "ATTENDING" on "Einheit 1"
      Then her status is "ATTENDING" and no decline message is kept

    @spec:attendance.player-not-on-event
    Scenario: Answering for a Player who is not on the Event
      Given a Line "Kiwi" rostering "Carmela"
      And an Event "Einheit 1"
      And the Team Player "Stocki", on no Line
      When the coach sets "Stocki" to "ATTENDING" on "Einheit 1"
      Then the response is a not-found error envelope

    @spec:attendance.unknown-status
    Scenario: An unknown status
      Given a Line "Kiwi" rostering "Carmela"
      And an Event "Einheit 1"
      When the coach sets "Carmela" to "MAYBE" on "Einheit 1"
      Then the request is rejected as a bad request

Feature: Iterations and Events
  An Iteration is a named group of Events, ordered against other Iterations by a plain integer
  position (ADR-0007). An Event is a Training or a Match, belongs to exactly one Iteration, and is
  ordered inside it by its mandatory datetime, which must be unique within that Iteration
  (ADR-0011). The whole team-overview timeline is one aggregate read.

  Rule: The timeline is one read

    @spec:timeline.aggregate-read
    Scenario: Listing Iterations nests their Events in datetime order
      Given an Iteration "Vorbereitung"
      And a Training "Einheit 2" in "Vorbereitung" at "2026-08-12 18:00"
      And a Training "Einheit 1" in "Vorbereitung" at "2026-08-05 18:00"
      And a Match "Testspiel" in "Vorbereitung" at "2026-08-19 19:00"
      When the Iteration list is read
      Then "Vorbereitung" nests its Events in the order "Einheit 1", "Einheit 2", "Testspiel"

    @spec:timeline.iteration-unknown
    Scenario: An unknown Iteration
      When an Iteration that does not exist is read
      Then the response is a not-found error envelope

  Rule: Iterations are coach-managed

    @spec:timeline.create-iteration-with-events
    Scenario: Creating an Iteration with its first Events in one call
      When the coach creates an Iteration "Rückrunde" holding a Match "Derby" at "2026-09-15 19:00" and a Training at "2026-09-01 18:00"
      Then the Iteration and both Events are persisted, in datetime order

    @spec:timeline.iteration-needs-name
    Scenario: An Iteration needs a name
      When the coach creates an Iteration with a blank name
      Then the request is rejected as a bad request

    @spec:timeline.rename-iteration
    Scenario: Renaming an Iteration
      Given an Iteration "Vorbereitung"
      When the coach renames it to "Rückrunde" at position 7
      Then the new name and position come back on the next read

    @spec:timeline.delete-iteration
    Scenario: Deleting an Iteration takes its Events with it
      Given an Iteration "Vorbereitung"
      And a Training "Einheit 1" in "Vorbereitung" at "2026-08-05 18:00"
      When the coach deletes the Iteration
      Then neither it nor its Events appear on the timeline

  Rule: Every Event has a datetime, unique within its Iteration (ADR-0011)

    @spec:timeline.event-requires-datetime
    Scenario: An Event without a datetime is rejected
      Given an Iteration "Vorbereitung"
      When the coach adds a Training to "Vorbereitung" with no datetime
      Then the request is rejected as a bad request

    @spec:timeline.event-carries-datetime
    Scenario: The datetime comes back on the Event
      Given an Iteration "Vorbereitung"
      When the coach adds a Training to "Vorbereitung" at "2026-10-05 18:00"
      Then that datetime is returned on the created Event

    @spec:timeline.slot-collision-on-create
    Scenario: Two Events in one Iteration cannot share a datetime
      Given an Iteration "Vorbereitung"
      And a Training "Einheit 1" in "Vorbereitung" at "2026-08-05 18:00"
      When the coach adds a Training to "Vorbereitung" at "2026-08-05 18:00"
      Then the request is rejected as a scheduling conflict

    @spec:timeline.slot-scoped-to-iteration
    Scenario: The same datetime in two Iterations is fine
      Given an Iteration "Vorbereitung"
      And a Training "Einheit 1" in "Vorbereitung" at "2026-08-05 18:00"
      And an Iteration "Rückrunde"
      When the coach adds a Training to "Rückrunde" at "2026-08-05 18:00"
      Then the Event is created

    @spec:timeline.reschedule
    Scenario: Rescheduling an Event
      Given an Iteration "Vorbereitung"
      And a Training "Einheit 1" in "Vorbereitung" at "2026-08-05 18:00"
      And a Training "Einheit 2" in "Vorbereitung" at "2026-08-12 18:00"
      When the coach moves "Einheit 1" to "2026-08-19 18:00"
      Then its datetime is "2026-08-19 18:00", and it now comes after "Einheit 2" on the timeline

    @spec:timeline.reschedule-to-own-slot
    Scenario: Rescheduling an Event onto its own current datetime
      Given an Iteration "Vorbereitung"
      And a Training "Einheit 1" in "Vorbereitung" at "2026-08-05 18:00"
      When the coach moves "Einheit 1" to "2026-08-05 18:00"
      Then its datetime is "2026-08-05 18:00" — an Event never collides with itself

    @spec:timeline.reschedule-collision
    Scenario: Rescheduling onto a sibling's datetime
      Given an Iteration "Vorbereitung"
      And a Training "Einheit 1" in "Vorbereitung" at "2026-08-05 18:00"
      And a Training "Einheit 2" in "Vorbereitung" at "2026-08-12 18:00"
      When the coach moves "Einheit 1" to "2026-08-12 18:00"
      Then the request is rejected as a scheduling conflict

    @spec:timeline.reschedule-collision-via-event-endpoint
    Scenario: The same collision rule applies to the Iteration-free Event endpoint
      Given an Iteration "Vorbereitung"
      And a Training "Einheit 1" in "Vorbereitung" at "2026-08-05 18:00"
      And a Training "Einheit 2" in "Vorbereitung" at "2026-08-12 18:00"
      When the coach moves "Einheit 1" to "2026-08-12 18:00" by Event id alone
      Then the request is rejected as a scheduling conflict

  Rule: Event identity

    @spec:timeline.event-type-must-exist
    Scenario: An Event needs a known Event type
      Given an Iteration "Vorbereitung"
      When the coach adds an Event of an unknown Event type to "Vorbereitung"
      Then the response is not found

    @spec:timeline.rename-event
    Scenario: Naming a Match
      Given a Line "Kiwi"
      And an Iteration "Vorbereitung"
      And a Match "Testspiel" in "Vorbereitung" at "2026-08-19 19:00"
      And "Kiwi" has the Focus "Spielaufbau aus der tiefen Zone" for "Testspiel"
      When the coach renames "Testspiel" to "Testspiel gegen Bern"
      Then the name is stored, and the Event's Focus attachments are left untouched

    @spec:timeline.delete-event
    Scenario: Deleting a single Event
      Given an Iteration "Vorbereitung"
      And a Training "Einheit 1" in "Vorbereitung" at "2026-08-05 18:00"
      And a Training "Einheit 2" in "Vorbereitung" at "2026-08-12 18:00"
      When the coach deletes the Event "Einheit 2"
      Then only "Einheit 2" is gone and "Einheit 1" remains

    @spec:timeline.event-unknown
    Scenario: Updating an Event that does not exist
      When an Event that does not exist is updated
      Then the response is not found

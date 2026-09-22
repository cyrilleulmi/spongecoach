Feature: Iterations and Events
  An Iteration is a named group of Events, ordered against other Iterations by a plain integer
  position (ADR-0007). An Event is a Training or a Match, belongs to exactly one Iteration, and is
  ordered inside it by its mandatory datetime, which must be unique within that Iteration
  (ADR-0011). The whole team-overview timeline is one aggregate read.

  Rule: The timeline is one read

    @spec:timeline.aggregate-read
    Scenario: Listing Iterations nests their Events in datetime order
      Given an Iteration with three Events scheduled out of order
      When the Iteration list is read
      Then its Events come back nested, ordered by datetime ascending

    @spec:timeline.iteration-unknown
    Scenario: An unknown Iteration
      When an Iteration that does not exist is read
      Then the response is a not-found error envelope

  Rule: Iterations are coach-managed

    @spec:timeline.create-iteration-with-events
    Scenario: Creating an Iteration with its first Events in one call
      When the coach creates an Iteration with two nested Events
      Then the Iteration and both Events are persisted, in datetime order

    @spec:timeline.iteration-needs-name
    Scenario: An Iteration needs a name
      When the coach creates an Iteration with a blank name
      Then the request is rejected as a bad request

    @spec:timeline.rename-iteration
    Scenario: Renaming an Iteration
      Given an Iteration named "Vorbereitung"
      When the coach renames it
      Then the new name is returned on the next read

    @spec:timeline.delete-iteration
    Scenario: Deleting an Iteration takes its Events with it
      Given an Iteration holding Events
      When the coach deletes the Iteration
      Then neither it nor its Events appear on the timeline

  Rule: Every Event has a datetime, unique within its Iteration (ADR-0011)

    @spec:timeline.event-requires-datetime
    Scenario: An Event without a datetime is rejected
      When the coach adds an Event with no datetime
      Then the request is rejected as a bad request

    @spec:timeline.event-carries-datetime
    Scenario: The datetime comes back on the Event
      When the coach adds an Event at a given datetime
      Then that datetime is returned on the created Event

    @spec:timeline.slot-collision-on-create
    Scenario: Two Events in one Iteration cannot share a datetime
      Given an Iteration with an Event at 2026-08-05 18:00
      When the coach adds another Event at 2026-08-05 18:00
      Then the request is rejected as a scheduling conflict

    @spec:timeline.slot-scoped-to-iteration
    Scenario: The same datetime in two Iterations is fine
      Given two Iterations
      When the coach adds an Event at the same datetime in each
      Then both are accepted

    @spec:timeline.reschedule
    Scenario: Rescheduling an Event
      Given an Event in an Iteration
      When the coach moves it to a free datetime
      Then its datetime changes, and the timeline reorders around it

    @spec:timeline.reschedule-to-own-slot
    Scenario: Rescheduling an Event onto its own current datetime
      Given an Event at 2026-08-05 18:00
      When the coach reschedules it to 2026-08-05 18:00
      Then it is accepted — an Event never collides with itself

    @spec:timeline.reschedule-collision
    Scenario: Rescheduling onto a sibling's datetime
      Given two Events in one Iteration
      When the coach moves one onto the other's datetime
      Then the request is rejected as a scheduling conflict

    @spec:timeline.reschedule-collision-via-event-endpoint
    Scenario: The same collision rule applies to the Iteration-free Event endpoint
      Given two Events in one Iteration
      When the coach moves one onto the other's datetime by Event id alone
      Then the request is rejected as a scheduling conflict

  Rule: Event identity

    @spec:timeline.event-type-must-exist
    Scenario: An Event needs a known Event type
      When the coach adds an Event with an unknown Event type
      Then the response is not found

    @spec:timeline.rename-event
    Scenario: Naming a Match
      Given a Match on the timeline
      When the coach sets its name to the opponent
      Then the name is stored, and the Event's Focus attachments are left untouched

    @spec:timeline.delete-event
    Scenario: Deleting a single Event
      Given an Iteration with two Events
      When the coach deletes one
      Then only that Event is gone and its sibling remains

    @spec:timeline.event-unknown
    Scenario: Updating an Event that does not exist
      When an unknown Event is updated
      Then the response is not found

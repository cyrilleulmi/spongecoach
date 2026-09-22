Feature: Planning a Focus per Line per Event
  A Focus is no longer implicitly one Line's, so attaching one to an Event is a three-way
  Line-Focus-Event link: at most one Focus per Line per Event (ADR-0010). The team overview writes
  the whole set of an Event's attachments in one call, so clearing is just sending less.

  Background:
    Given an Event attended by the Lines "Kiwi" and "Bäri"
    And the Focuses "Spielaufbau aus der tiefen Zone" and "Abschlussübungen 2-auf-1"

  Rule: An Event's attachments are replaced as a set

    @spec:focus.attach-per-line
    Scenario: Setting a Focus for each attending Line
      When the coach sets a Focus for "Kiwi" and one for "Bäri" in one call
      Then each Line's Focus comes back inline on the Event

    @spec:focus.replace-shorter-set
    Scenario: Sending a shorter set clears the Lines left out
      Given both Lines have a Focus set
      When the coach sends a set containing only "Kiwi"'s
      Then "Bäri" has no Focus for this Event any more

    @spec:focus.clear-all
    Scenario: Sending an empty set clears every attachment
      Given both Lines have a Focus set
      When the coach sends an empty attachment set
      Then no Line has a Focus for this Event

    @spec:focus.change-one-line-only
    Scenario: Changing one Line's Focus leaves the others alone
      Given both Lines have a Focus set
      When the coach changes only "Kiwi"'s Focus
      Then "Bäri" keeps the Focus it had

    @spec:focus.rename-leaves-attachments
    Scenario: Editing an Event's own fields does not disturb its attachments
      Given an Event with Focus attachments
      When the coach renames the Event
      Then its attachments are unchanged

  Rule: Attachments are validated against the Event's attending Lines

    @spec:focus.unknown-line
    Scenario: Attaching for a Line that does not exist
      When the coach attaches a Focus for an unknown line id
      Then the response is not found

    @spec:focus.unknown-focus
    Scenario: Attaching a Focus that does not exist
      When the coach attaches an unknown focus id
      Then the response is not found

    @spec:focus.line-not-attending
    Scenario: Attaching for a Line that is not attending this Event
      Given a Line that was created after the Event
      When the coach attaches a Focus for it
      Then the request is rejected as a bad request

  Rule: The timeline reads attachments denormalised

    @spec:focus.inline-names-on-timeline
    Scenario: The timeline carries Line and Focus names, not just ids
      Given Events with Focus attachments
      When the Iteration list is read
      Then each attachment carries its Line name and Focus name inline

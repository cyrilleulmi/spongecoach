Feature: Planning a Focus per Line per Event
  A Focus is no longer implicitly one Line's, so attaching one to an Event is a three-way
  Line-Focus-Event link: at most one Focus per Line per Event (ADR-0010). The team overview writes
  the whole set of an Event's attachments in one call, so clearing is just sending less.

  Background:
    Given an Event "Einheit 1" attended by the Lines "Kiwi" and "Bäri"
    And the Focuses "Spielaufbau aus der tiefen Zone" and "Abschlussübungen 2-auf-1"

  Rule: An Event's attachments are replaced as a set

    @spec:focus.attach-per-line
    Scenario: Setting a Focus for each attending Line
      When the coach sets "Spielaufbau aus der tiefen Zone" for "Kiwi" and "Abschlussübungen 2-auf-1" for "Bäri" in one call
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
      When the coach changes only "Bäri"'s Focus to "Abschlussübungen 2-auf-1"
      Then "Kiwi" keeps the Focus it had

    @spec:focus.rename-leaves-attachments
    Scenario: Editing an Event's own fields does not disturb its attachments
      Given "Kiwi" has the Focus "Spielaufbau aus der tiefen Zone" for "Einheit 1"
      When the coach renames "Einheit 1" to "Testspiel gegen Bern"
      Then its attachments are unchanged

  Rule: Attachments are validated against the Event's attending Lines

    @spec:focus.unknown-line
    Scenario: Attaching for a Line that does not exist
      When the coach attaches a Focus for a line id that does not exist
      Then the response is a not-found error envelope

    @spec:focus.unknown-focus
    Scenario: Attaching a Focus that does not exist
      When the coach attaches a focus id that does not exist for "Kiwi"
      Then the response is not found

    @spec:focus.line-not-attending
    Scenario: Attaching for a Line that is not attending this Event
      Given a Line "Lama" created after the Event
      When the coach attaches "Spielaufbau aus der tiefen Zone" for "Lama"
      Then the request is rejected as a bad request

  Rule: The timeline reads attachments denormalised

    @spec:focus.inline-names-on-timeline
    Scenario: The timeline carries Line and Focus names, not just ids
      Given "Kiwi" has the Focus "Spielaufbau aus der tiefen Zone" for "Einheit 1"
      When the Iteration list is read
      Then "Kiwi"'s attachment on "Einheit 1" carries its Line name and Focus name inline

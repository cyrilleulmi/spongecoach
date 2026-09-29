Feature: Drills animated from tactic-board photos
  A coach photographs a drill drawn on the tactic board and uploads the photos. The interpreter
  (Claude; a stub in this suite) reads them in the background and answers with Clarifying questions
  or a script the app can play. The coach answers, corrects it by chat, or edits the script by hand;
  every change is a new version (ADR-0018, ADR-0019).

  Rule: A coach uploads photos of a drill, and the interpreter turns them into a script

    @spec:drills.upload-interpreted
    Scenario: An upload is interpreted in the background
      When the coach uploads the Drill "Bresil" with 2 sketches
      Then the upload is accepted while the Drill "Bresil" is being interpreted
      And the Drill "Bresil" becomes ready with script version 1

    @spec:drills.sketches-kept-in-order
    Scenario: The sketches are kept in order, with their notes
      When the coach uploads the Drill "Bresil" with sketches noted "Start" and "Steigerung"
      Then the Drill "Bresil" has sketch 1 noted "Start" and sketch 2 noted "Steigerung"
      And each of its sketches is served as a JPEG

    @spec:drills.readings-kept
    Scenario: What the interpreter read is kept with each sketch
      When the coach uploads the Drill "Bresil" with 2 sketches
      Then the Drill "Bresil" becomes ready with script version 1
      And each of its sketches carries what the interpreter read on it

    @spec:drills.tags-and-relation-kept
    Scenario: Tags and how the sketches relate are kept
      When the coach uploads the Drill "Bresil" tagged "Passen" and "Mit Goalie" as a progression
      Then the Drill "Bresil" is tagged "Passen" and "Mit Goalie", as a progression

    @spec:drills.sketches-must-be-jpegs
    Scenario Outline: A Drill needs 1 to 12 JPEG sketches
      When the coach uploads the Drill "Bresil" with <sketches>
      Then the request is rejected as a bad request

      Examples:
        | sketches     |
        | no sketches  |
        | a PNG sketch |
        | 13 sketches  |

    @spec:drills.name-required
    Scenario: A Drill needs a name
      When the coach uploads a Drill with a blank name
      Then the request is rejected as a bad request

    @spec:drills.unknown-tag-rejected
    Scenario: A Drill can only carry tags from the list
      When the coach uploads the Drill "Bresil" tagged with a tag that does not exist
      Then the request is rejected as a bad request

    @spec:drills.tag-list
    Scenario: The tag list is fixed
      When the Drill tags are listed
      Then they are the 10 seeded tags, from "Mit Gegenspielern" to "Wiederholung spiegelverkehrt"

  Rule: The interpreter asks instead of guessing

    @spec:drills.asks-when-unclear
    Scenario: An unclear drawing waits for answers
      Given the interpreter asks "Sind die Kreise Verteidigerinnen oder Hütchen?" about the Drill "3v2"
      When the coach uploads the Drill "3v2" with 1 sketch
      Then the Drill "3v2" waits for an answer to "Sind die Kreise Verteidigerinnen oder Hütchen?"

    @spec:drills.answers-lead-to-script
    Scenario: Answering the questions produces a script
      Given a Drill "3v2" waiting for answers
      When the coach answers "Verteidigerinnen" on the Drill "3v2"
      Then the Drill "3v2" becomes ready with script version 2
      And the conversation of "3v2" ends with the answer "Verteidigerinnen" and the interpreter's reply

    @spec:drills.guess-instead-of-answering
    Scenario: The coach can let the interpreter decide
      Given a Drill "3v2" waiting for answers
      When the coach tells the interpreter to guess on the Drill "3v2"
      Then the Drill "3v2" becomes ready with script version 2

    @spec:drills.answers-need-open-questions
    Scenario: There is nothing to answer on a ready Drill
      Given a ready Drill "Slalom"
      When the coach answers "Verteidigerinnen" on the Drill "Slalom"
      Then the request is rejected as a bad request

    @spec:drills.answer-names-open-question
    Scenario: An answer must be to an open question
      Given a Drill "3v2" waiting for answers
      When the coach answers a question the Drill "3v2" did not ask
      Then the request is rejected as a bad request

  Rule: A script is checked before it is kept

    @spec:drills.unplayable-script-sent-back-once
    Scenario: An unplayable script goes back to the interpreter once
      Given the interpreter's first script for the Drill "Acht" is unplayable
      When the coach uploads the Drill "Acht" with 1 sketch
      Then the Drill "Acht" becomes ready with script version 1

    @spec:drills.unplayable-twice-fails
    Scenario: A script that stays unplayable fails the Drill
      Given the interpreter's scripts for the Drill "Acht" are always unplayable
      When the coach uploads the Drill "Acht" with 1 sketch
      Then the Drill "Acht" fails, saying the script was invalid

    @spec:drills.interpreter-failure-fails
    Scenario: A refusal fails the Drill with the reason
      Given the interpreter refuses the Drill "Acht"
      When the coach uploads the Drill "Acht" with 1 sketch
      Then the Drill "Acht" fails, saying the request was refused

    @spec:drills.retry
    Scenario: A failed Drill can be interpreted again
      Given a failed Drill "Acht"
      When the coach retries the Drill "Acht"
      Then the Drill "Acht" becomes ready with script version 1

    @spec:drills.one-job-at-a-time
    Scenario: A Drill being interpreted takes no other change
      Given a Drill "Acht" being interpreted
      When the coach sends the correction "Mehr Tempo" for the Drill "Acht"
      Then the request is refused because the Drill is busy

  Rule: The coach corrects the animation, and every change is a new version

    @spec:drills.chat-makes-new-version
    Scenario: A correction by chat makes a new version
      Given a ready Drill "Slalom"
      When the coach sends the correction "Die Hütchen stehen weiter links" for the Drill "Slalom"
      Then the Drill "Slalom" becomes ready with script version 2
      And version 2 of "Slalom" is summarised as "Die Hütchen stehen weiter links"

    @spec:drills.hand-edit-new-version
    Scenario: A hand-edited script is saved as a new version
      Given a ready Drill "Slalom"
      When the coach saves a hand-edited script for the Drill "Slalom"
      Then version 2 of "Slalom" is current, as a hand edit

    @spec:drills.hand-edit-must-be-playable
    Scenario Outline: A hand-edited script must be playable
      Given a ready Drill "Slalom"
      When the coach saves a hand-edited script for the Drill "Slalom" with <problem>
      Then the request is rejected as a bad request

      Examples:
        | problem                                  |
        | a step for a part that does not exist    |
        | a position off the rink                  |
        | a field the format does not know         |
        | steps waiting for each other in a cycle  |
        | a seamless loop that drops a part        |

    @spec:drills.revert
    Scenario: Reverting makes an earlier version current again, as a new version
      Given a ready Drill "Slalom"
      And the coach saved a hand-edited script for the Drill "Slalom"
      When the coach reverts the Drill "Slalom" to version 1
      Then version 3 of "Slalom" is current, as a revert with the script of version 1

    @spec:drills.revert-unknown-version
    Scenario: Reverting to a version that does not exist
      Given a ready Drill "Slalom"
      When the coach reverts the Drill "Slalom" to version 7
      Then the response is not found

  Rule: The Drill list

    @spec:drills.list-recent-first
    Scenario: The most recently changed Drill comes first
      Given a ready Drill "Acht"
      And a ready Drill "Slalom"
      When the Drills are listed
      Then "Slalom" comes before "Acht", each with its sketch count

    @spec:drills.rename-and-retag
    Scenario: A Drill is renamed and retagged
      Given a ready Drill "Slalom"
      When the coach renames the Drill "Slalom" to "Slalom lang" and tags it "Stockführung"
      Then the Drill "Slalom" is named "Slalom lang" and tagged "Stockführung"

    @spec:drills.delete-hides
    Scenario: A deleted Drill is gone from the list
      Given a ready Drill "Slalom"
      When the coach deletes the Drill "Slalom"
      Then the Drill "Slalom" is not listed, and reading it is not found

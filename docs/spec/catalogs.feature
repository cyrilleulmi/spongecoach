Feature: Shared catalogs
  Skills and Development goals are Team-wide catalogs, not per-Line copies (ADR-0006): a Line
  associates with an existing catalog row, so editing that row is visible to every Line linked to
  it. Coaches create new entries straight from the line screen (ADR-0009). Catalog rows are hidden
  by soft delete, never removed.

  Rule: Catalogs list only what is still active

    @spec:catalogs.list-active-skills
    Scenario: Soft-deleted Skills are out of the listing
      Given the Skill "Passgenauigkeit"
      And the soft-deleted Skill "Stockführung"
      When the Skill catalog is listed
      Then "Passgenauigkeit" is in the Skill catalog but "Stockführung" is not

    @spec:catalogs.list-active-goals
    Scenario: Soft-deleted Development goals are out of the listing
      Given the Development goal "Überzahlspiel verbessern"
      And the soft-deleted Development goal "Kompakte Defensive aufbauen"
      When the Development goal catalog is listed
      Then "Überzahlspiel verbessern" is in the Development goal catalog but "Kompakte Defensive aufbauen" is not

  Rule: Every catalog entry carries a color from a fixed palette

    @spec:catalogs.create-skill
    Scenario: Creating a Skill from the line screen
      When the coach creates the Skill "Bully-Kontrolle" with the color "#4f7a3f"
      Then it is listed in the Skill catalog with that color

    @spec:catalogs.create-goal
    Scenario: Creating a Development goal from the line screen
      When the coach creates the Development goal "Überzahlspiel verbessern" with the color "#b1467a"
      Then it is listed in the Development goal catalog with that color

    @spec:catalogs.recolor-skill
    Scenario: Recoloring a Skill
      Given the Skill "Passgenauigkeit", rated 60 by the Line "Kiwi"
      When the coach changes that Skill's color to "#c8722e"
      Then the new color comes back, and "Kiwi" sees it on that Skill

    @spec:catalogs.recolor-goal
    Scenario: Recoloring a Development goal
      Given the Development goal "Überzahlspiel verbessern"
      When the coach changes that Development goal's color to "#3b6ea5"
      Then the new color comes back on the Development goal

    @spec:catalogs.create-requires-name
    Scenario: A catalog entry needs a name
      When the coach creates a Skill with a blank name
      Then the request is rejected as a bad request

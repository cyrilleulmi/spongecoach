Feature: Shared catalogs
  Skills, Development goals and Focuses are Team-wide catalogs, not per-Line copies (ADR-0006): a
  Line associates with an existing catalog row, so editing that row is visible to every Line linked
  to it. Coaches create new entries straight from the line screen (ADR-0009). Catalog rows are
  hidden by soft delete, never removed.

  Rule: Catalogs list only what is still active

    @spec:catalogs.list-active-skills
    Scenario: Soft-deleted Skills are out of the listing
      Given an active Skill and a soft-deleted Skill
      When the Skill catalog is listed
      Then only the active one is returned

    @spec:catalogs.list-active-goals
    Scenario: Soft-deleted Development goals are out of the listing
      Given an active Development goal and a soft-deleted one
      When the Development goal catalog is listed
      Then only the active one is returned

  Rule: Every catalog entry carries a color from a fixed palette

    @spec:catalogs.create-skill
    Scenario: Creating a Skill from the line screen
      When the coach creates the Skill "Bully-Kontrolle" with a chosen color
      Then it is listed in the catalog with that color

    @spec:catalogs.create-goal
    Scenario: Creating a Development goal from the line screen
      When the coach creates the Development goal "Überzahlspiel verbessern"
      Then it is listed in the catalog

    @spec:catalogs.recolor-skill
    Scenario: Recoloring a Skill
      Given a Skill in the catalog
      When the coach changes its color
      Then the new color is persisted and seen by every Line associated with it

    @spec:catalogs.recolor-goal
    Scenario: Recoloring a Development goal
      Given a Development goal in the catalog
      When the coach changes its color
      Then the new color is persisted

    @spec:catalogs.create-requires-name
    Scenario: A catalog entry needs a name
      When the coach creates a Skill with a blank name
      Then the request is rejected as a bad request

  Rule: A Focus is derived from at least one Development goal

    @spec:catalogs.create-focus
    Scenario: Creating a Focus from one Development goal
      When the coach creates the Focus "Cross-Pässe unter Druck" from one Development goal
      Then it is listed carrying that goal's id

    @spec:catalogs.focus-from-several-goals
    Scenario: Creating a Focus from several Development goals
      When the coach creates a Focus from two Development goals
      Then both goal ids are linked to it

    @spec:catalogs.focus-needs-a-goal
    Scenario: A Focus with no originating Development goal is rejected
      When the coach creates a Focus with an empty goal list
      Then the request is rejected as a bad request

    @spec:catalogs.focus-carries-goal-ids
    Scenario: The Focus listing carries each Focus's originating goals
      Given Focuses derived from Development goals
      When the Focus catalog is listed
      Then each Focus carries the ids of the goals it came from

  Rule: Focus deletion is not implemented in v1

    @unverified
    @spec:catalogs.focus-delete-blocked
    Scenario: Deleting a Focus that a Line still uses
      Given a Focus associated with a Line
      When the coach deletes that Focus
      Then the deletion is refused while any Line is still associated with it
      # CONTEXT.md describes this rule, but v1 exposes no Focus deletion endpoint at all.
      # Nothing can regress here until that endpoint exists.

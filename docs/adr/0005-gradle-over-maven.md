# Gradle (Kotlin DSL) over Maven for the Quarkus backend

Chose Gradle with the Kotlin DSL over Maven for `backend/`, despite Maven being Quarkus's more common default and the one most Quarkus docs/examples assume. The driver is personal/team familiarity with Gradle rather than a technical requirement specific to this project; Quarkus supports Gradle as a first-class build tool, so nothing about the domain forces either choice. Recorded because a future reader (or the next Quarkus tutorial they follow) will otherwise wonder why this repo departs from the ecosystem default.

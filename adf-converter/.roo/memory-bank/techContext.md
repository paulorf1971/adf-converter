# Tech Context

Language

Java 17

Framework

Spring Boot

Build

Maven

Libraries

- Jackson
- Lombok
- flexmark `0.64.8`
- jsoup `1.17.2`

Verification

- Full test suite: `./mvnw test`
- Compile only: `./mvnw test-compile`
- Focused test class: `./mvnw -Dtest=MarkdownConvertersTest test`
- Sample fixtures: `src/test/resources/samples/`

Code Style

- Prefer small, behavior-preserving changes with focused tests.
- Use structural assertions for ADF object graphs.
- Keep converter behavior in source/tests as the executable truth when memory-bank notes conflict.

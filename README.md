# Sherpa

## Project Vision

To seamlessly transition from a legacy messaging system to a robust, enterprise-grade platform without disrupting
existing client applications, thereby enhancing overall platform maintainability, performance and scalability.

## Product Backlog

- Epics:
  - [ ] MVP before 2024 mid-August.

- Stories:
  - [x] Transfers messages to enterprise platform.

      A new connection activates a new session.
      Correctly formed messages are interpreted into appropriate commands and executed on the target platform.
      Incorrectly formed messages are interpreted to corresponding error messages and sent back.
      Closing a connection results in the session being closed.

  - [x] Transfers messages from enterpise platform.

      Target platform exceptions cause the connection and session to be closed.
      Messages from the target platform are interpreted and sent to the client.

  - [ ] Proofs itself.
      
      Design and implement test strategy for main modules: flow, codec and platform.

  - [ ] Explains itself.
      
      Cover codebase with javadocs & write usage instructions.

## Resources

- Architecture & Design: <https://www.figma.com/board/G3RXv0wJOW2fBGq17y7Qso/sherpa?node-id=0-1&t=X5ItWFhX0CbPDgDP-1`>

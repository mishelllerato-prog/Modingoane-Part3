# Modingoane QuickChat Part 3

This Java with Ant project continues the supplied `UserRegistrationSystem` project for PROG5121. It opens QuickChat after registration and a successful login.

## Build and run

Use JDK 17 or later and Apache Ant. Run these commands from the project directory.

```sh
ant clean test jar
java -jar dist/UserRegistrationSystem.jar
```

Open this folder as an existing Java with Ant project in NetBeans. The first build downloads Gson, JUnit, and Hamcrest. When moving the runnable JAR, keep `dist/lib` beside it.

## Use QuickChat

Select **1. Send Messages** to create a message and choose whether to send, disregard, or store it. **2. Show recently sent messages** displays the sent report for the session. Sends are simulated in the console.

Select **4. Stored Messages** to view senders and recipients, show the longest stored message, search by ID or recipient, delete by hash, and print the stored report. The submenu includes a combined recipient search for sent and stored messages. It preserves the local South African recipient format used in Part 2.

QuickChat saves stored messages in `messages.json` in the working directory and reloads them on startup. A Part 2 archive can be used as-is. If a legacy record lacks a sender, the report displays **Unknown**. Sent and disregarded messages are tracked for the current session.

## Tests and walkthrough

Run `ant test` to exercise the original registration and login tests, the five Part 3 fixtures, and recovery and compatibility tests for the JSON archive. The same commands run in GitHub Actions on Linux and Windows.

1. Register a demo user, sign in, and send one message. Open the sent report.
2. Store two messages for the same recipient. Show the longest message, the stored report, and the recipient results.
3. Delete one message by copying its hash from the report. Restart the application and show that the deletion persisted.
4. Explain how the message arrays, ID and hash indexes, and JSON archive work. Run the tests and explain what the five fixtures exercise.

## Source and assistance

[1] Google, “Gson User Guide,” Array Examples. [Online]. Available: https://google.github.io/gson/UserGuide.html#array-examples

OpenAI Codex assisted with implementing and testing Part 3.

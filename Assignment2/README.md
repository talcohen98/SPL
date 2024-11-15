
# Set Card Game - Java Concurrency, Synchronization, and Testing

## Overview

This project is an implementation of a simplified version of the **Set Card Game**, designed to practice Java concurrency and synchronization techniques while incorporating unit testing. Players compete to identify legal sets of cards based on specified rules.

The project includes game logic and backend functionality, leveraging Java threads for player actions and game management.

## Features

- **Concurrent Gameplay**: Multiple player threads (human and non-human) compete simultaneously.
- **Synchronization Mechanisms**: Ensures correct handling of shared resources (e.g., table state, dealer actions).
- **Penalty and Scoring**: Automatic tracking of legal sets, points, and penalties for incorrect actions.
- **Dynamic Gameplay**: Dealer reshuffles the cards periodically and manages game flow.
- **Unit Testing**: Includes JUnit tests for critical components such as `Table`, `Dealer`, and `Player`.

## How to Run

1. **Prerequisites**: Ensure Java 8 and Maven are installed on your system.
2. **Build the Project**:
   ```bash
   mvn clean compile
   ```
3. **Run the Game**:
   ```bash
   java -cp target/classes bguspl.set.Main
   ```
4. **Run Tests**:
   ```bash
   mvn test
   ```

## Directory Structure

- **src/main/java/bguspl/set/ex**: Contains the game logic and implementation.
- **src/test**: Includes unit tests for core components.
- **pom.xml**: Maven configuration file for dependencies and build settings.

## Key Components

- **Dealer**: Main thread managing the game flow, including dealing cards, scoring, and ending the game.
- **Player**: Represents human or non-human participants, with separate threads managing actions.
- **Table**: Shared resource holding the current state of cards and player actions.
- **Config**: Configuration class for customizable game settings.
- **JUnit Tests**: Verifies the correctness and robustness of the game logic.

## Credits

Developed as part of the Java Concurrency assignment in SPL course @BGU.

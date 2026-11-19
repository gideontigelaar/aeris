# Caligo
A custom game engine built in Java using LWJGL.

## Project structure
- `engine/`: the engine itself
- `network/`: the shared client for the game server, can be used by games
- `games/tictactoe/`: Tic-tac-toe, see [its README](games/tictactoe/README.md)
- `games/reversi/`: Reversi, see [its README](games/reversi/README.md)

## Requirements
- Java 17 or newer
- Maven 3.6 or newer

## Building
Run all commands from the root of the repository.

Build everything:
```bash
mvn clean package
```

Build only tic-tac-toe:
```bash
mvn clean package -pl games/tictactoe -am
```

Build only reversi:
```bash
mvn clean package -pl games/reversi -am
```

Build one jar for Windows, Linux and macOS:
```bash
mvn clean package -DallNatives
```

The games are built to `games/tictactoe/target/tictactoe.jar` and `games/reversi/target/reversi.jar`.

## Running
Tic-tac-toe:
```bash
java -jar games/tictactoe/target/tictactoe.jar
```

Reversi:
```bash
java -jar games/reversi/target/reversi.jar
```

On macOS, add `-XstartOnFirstThread`:
```bash
java -XstartOnFirstThread -jar games/tictactoe/target/tictactoe.jar
java -XstartOnFirstThread -jar games/reversi/target/reversi.jar
```

## Notes
- Engine dependencies are in `engine/pom.xml`, shared versions like LWJGL in the root `pom.xml`.
- Each game is its own module in `games/`.
- Each module keeps its resources in a folder that matches its package.
- The server protocol lives in `network/`. Each game passes its own game name to `OnlineSession`.
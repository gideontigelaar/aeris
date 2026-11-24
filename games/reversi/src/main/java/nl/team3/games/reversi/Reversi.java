package nl.team3.games.reversi;

import nl.team3.engine.core.Engine;
import nl.team3.engine.core.EngineConfig;
import nl.team3.engine.input.Keys;
import nl.team3.games.reversi.scenes.MainMenuScene;
import org.joml.Vector4f;

public final class Reversi {
    public static final String GAME_TYPE = "Reversi";

    private static final Vector4f MENU_BLUE = new Vector4f(0.137f, 0.286f, 0.459f, 1.0f);

    private Reversi() {
    }

    public static void main(String[] args) {
        EngineConfig config = EngineConfig.builder(GAME_TYPE)
                .windowSize(720, 720)
                .minWindowSize(640, 640)
                .minGuiSize(640, 640)
                .clearColor(MENU_BLUE)
                .build();

        Engine.run(config, context -> {
            context.actions().bind("back", Keys.ESCAPE);

            return new MainMenuScene(context);
        });
    }
}
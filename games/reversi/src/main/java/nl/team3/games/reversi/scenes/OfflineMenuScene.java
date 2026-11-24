package nl.team3.games.reversi.scenes;

import nl.team3.engine.assets.AssetManager;
import nl.team3.engine.core.EngineContext;
import nl.team3.games.reversi.logic.AIOpponent;

public class OfflineMenuScene extends MenuScene {
    private final Runnable returnToSelector;

    public OfflineMenuScene(EngineContext context) {
        this(context, null);
    }

    public OfflineMenuScene(EngineContext context, Runnable returnToSelector) {
        super(context, "");
        this.returnToSelector = returnToSelector;
    }

    @Override
    protected void initMenu(AssetManager assets) {
        addButton("PLAYER VS PLAYER", () -> open(new MatchScene(context, returnToSelector)));
        addButton("50 / 50 AI", () -> playAI(AIOpponent.Difficulty.FIFTY_FIFTY));
        addButton("UNBEATABLE AI", () -> playAI(AIOpponent.Difficulty.UNBEATABLE));
        addSmallButton("BACK", this::onBack);
    }

    private void playAI(AIOpponent.Difficulty difficulty) {
        open(new MatchScene(context, new AIOpponent(difficulty), returnToSelector));
    }

    @Override
    protected void onBack() {
        open(new MainMenuScene(context, returnToSelector));
    }
}
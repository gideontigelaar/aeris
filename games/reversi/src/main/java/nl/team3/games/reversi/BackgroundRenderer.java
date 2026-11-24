package nl.team3.games.reversi;

import nl.team3.engine.assets.AssetManager;
import nl.team3.engine.graphics.Mesh;
import nl.team3.engine.graphics.ShaderProgram;

public class BackgroundRenderer {
    public static final int PALETTE_BLACK = 0;
    public static final int PALETTE_WHITE = 1;
    public static final int PALETTE_MENU = 2;

    private static final String SHADER_KEY = "background";
    private static final String VERTEX_PATH = "/games/reversi/shaders/background.vert";
    private static final String FRAGMENT_PATH = "/games/reversi/shaders/background.frag";

    private final ShaderProgram shader;
    private final Mesh quad;
    private float time = 0f;
    private int palette = PALETTE_MENU;
    private int previousPalette = PALETTE_MENU;
    private float startTime = 0f;

    public BackgroundRenderer(AssetManager assets) {
        this.shader = assets.loadShader(SHADER_KEY, VERTEX_PATH, FRAGMENT_PATH);
        this.quad = assets.getMesh(AssetManager.QUAD_MESH);
    }

    public void update(float dt) {
        time += dt;
    }

    public void setPalette(int num) {
        if (num == palette) {
            return;
        }
        previousPalette = palette;
        palette = num;
        startTime = time;
    }

    public int getPalette() {
        return palette;
    }

    public void render(int width, int height) {
        shader.bind();
        shader.setUniform1f("iTime", time);
        shader.setUniform2f("iResolution", width, height);
        shader.setUniform1i("palette", palette);
        shader.setUniform1i("previousPalette", previousPalette);
        shader.setUniform1f("startTime", startTime);
        quad.render();
    }
}
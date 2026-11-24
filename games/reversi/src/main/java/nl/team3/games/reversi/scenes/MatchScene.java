package nl.team3.games.reversi.scenes;

import nl.team3.engine.assets.AssetManager;
import nl.team3.engine.core.EngineContext;
import nl.team3.engine.graphics.Font;
import nl.team3.engine.graphics.Sprite;
import nl.team3.engine.graphics.Texture;
import nl.team3.engine.graphics.animation.Animation;
import nl.team3.engine.graphics.animation.EasingType;
import nl.team3.engine.graphics.animation.SpriteAnimation;
import nl.team3.engine.input.MouseButtons;
import nl.team3.engine.ui.Button;
import nl.team3.games.reversi.BackgroundRenderer;
import nl.team3.games.reversi.logic.AIOpponent;
import nl.team3.games.reversi.logic.Board;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MatchScene extends BaseScene {
    protected static final Vector4f WIN_COLOR = HIGHLIGHT_COLOR;
    protected static final Vector4f LOSE_COLOR = new Vector4f(1f, 0.25f, 0.25f, 1f);

    private static final Vector4f SKY_COLOR = new Vector4f(0.408f, 0.682f, 0.831f, 1f);
    private static final Vector4f BLUE_COLOR = new Vector4f(0.137f, 0.286f, 0.459f, 1f);
    private static final float BOARD_SCALE = 4f;
    private static final float START_OFFSET_X = 2f * BOARD_SCALE;
    private static final float START_OFFSET_Y = 2f * BOARD_SCALE;
    private static final float STRIDE_X = 13f * BOARD_SCALE;
    private static final float STRIDE_Y = 14f * BOARD_SCALE;
    private static final float CELL_HALF_W = 6f * BOARD_SCALE;
    private static final float CELL_HALF_H = 6.5f * BOARD_SCALE;

    private static final float GRAB_SCALE = BOARD_SCALE + 1f;
    private static final float MOVE_TIME = 0.3f;
    private static final float PLAY_MOVE_TIME = 0.5f;
    private static final float PLAY_MOVE_ARC = 3f * BOARD_SCALE;
    private static final float GHOST_ALPHA = 0.4f;
    private static final float SIDE_BOARD_GAP = 72f;
    private static final float BOB_HEIGHT = BOARD_SCALE;
    private static final float BOB_TIME = 1.2f;

    private static final float FADE_ALPHA = 0.35f;
    private static final float FADE_TIME = 0.4f;
    private static final float AI_MOVE_DELAY = 0.5f;

    private static final float RESULT_DROP = 2 * UI_SPACING;
    private static final float RESULT_DROP_TIME = 0.5f;
    private static final int CONFETTI_COUNT = 90;
    private static final float CONFETTI_GRAVITY = 900f;
    private static final float CONFETTI_TIME = 2.5f;
    private static final float CONFETTI_SIZE = 2 * UI_SCALE;

    protected final Board board;
    private final Board.Mark firstTurn;
    private final Board.Mark leftMark;
    private final AIOpponent ai;
    private final Board.Mark aiMark;
    private float aiMoveTimer;

    private Sprite boardSprite;
    private Texture boardTexture;
    private final Vector2f boardTopLeft = new Vector2f();
    private Sprite logo;
    private boolean logoVisible;
    private Sprite ghost;
    private Sprite pixel;
    private PieceSide blackSide;
    private PieceSide whiteSide;
    private final Sprite[][] boardPieces = new Sprite[Board.SIZE][Board.SIZE];
    private final List<PieceMove> settling = new ArrayList<>();
    private int pressedCell = -1;
    private int ghostCell = -1;

    private Animation fadeAnim;
    private Animation resultAnim;
    private final List<Confetti> confetti = new ArrayList<>();

    protected Button btnAction;
    private Button btnPlayAgain;
    private float statusY;
    private float statusHeight;
    private float nameY;
    private float countY;
    private boolean namesBelowBoard;

    private class PieceSide {
        private final Board.Mark mark;
        private final float direction;
        private final Texture pieceTexture;
        private Sprite active;
        private SpriteAnimation grabAnim;
        private SpriteAnimation returnAnim;
        private boolean pickedUp = false;
        private float bobTime;
        private float alpha = 1f;
        private float nameX;
        private float countX;

        private PieceSide(Board.Mark mark, float direction, Texture pieceTexture) {
            this.mark = mark;
            this.direction = direction;
            this.pieceTexture = pieceTexture;
            spawnNext();
        }

        private void spawnNext() {
            pickedUp = false;
            returnAnim = null;
            bobTime = 0f;

            if (isMatchOver() || board.isGameOver()) {
                active = null;
                return;
            }

            active = new Sprite(pieceTexture);
            active.setOrigin(0.5f, 0.5f);
            active.setScale(BOARD_SCALE);
            active.setAlpha(alpha);
            layout();
        }

        private void pickUp() {
            pickedUp = true;
            returnAnim = null;
            grabAnim = SpriteAnimation.builder().sprite(active).scale(new Vector2f(active.getScale()), new Vector2f(GRAB_SCALE, GRAB_SCALE)).duration(MOVE_TIME).easing(EasingType.EXPONENTIAL_OUT).build();
            grabAnim.startAnimation();
        }

        private void putBack() {
            pickedUp = false;
            bobTime = 0f;
            returnAnim = SpriteAnimation.builder().sprite(active)
                    .position(new Vector2f(active.getPosition()), new Vector2f(sideX(), sideY()))
                    .scale(new Vector2f(active.getScale()), new Vector2f(BOARD_SCALE, BOARD_SCALE))
                    .duration(MOVE_TIME).easing(EasingType.EXPONENTIAL_OUT).build();
            returnAnim.startAnimation();
        }

        private float sideX() {
            float halfBoard = boardTexture.getWidth() * BOARD_SCALE / 2f;
            float centerX = Math.round(currentWidth / 2f);
            float preferred = centerX + direction * (halfBoard + SIDE_BOARD_GAP);
            float marginCenter = direction < 0 ? boardTopLeft.x / 2f : (boardTopLeft.x + halfBoard * 2f + currentWidth) / 2f;
            return Math.round(direction < 0 ? Math.max(marginCenter, preferred) : Math.min(marginCenter, preferred));
        }

        private float sideY() {
            return Math.round(currentHeight / 2f);
        }

        private float bobOffset() {
            return Math.round(BOB_HEIGHT * (1f - (float) Math.cos(2 * Math.PI * bobTime / BOB_TIME)) / 2f);
        }

        private void snapActiveToRest() {
            active.setPosition(sideX(), sideY() - bobOffset());
        }

        private void layout() {
            if (active != null && !pickedUp && returnAnim == null) {
                snapActiveToRest();
            }
        }

        private void setAlpha(float alpha) {
            this.alpha = alpha;
            if (active != null) active.setAlpha(alpha);
        }

        private void drawResting() {
            if (active != null && !pickedUp && returnAnim == null) spriteRenderer.draw(active);
        }

        private void drawMoving() {
            if (active != null && (pickedUp || returnAnim != null)) spriteRenderer.draw(active);
        }

        private void collectSprites(List<Sprite> out) {
            if (active != null) out.add(active);
        }
    }

    private static final class PieceMove {
        private final Sprite piece;
        private final Vector2f from;
        private final Vector2f to;
        private final float fromScale;
        private final float lift;
        private final float arc;
        private final Animation progress;

        private PieceMove(Sprite piece, Vector2f to, float lift, float arc, float time, EasingType easing) {
            this.piece = piece;
            this.from = new Vector2f(piece.getPosition());
            this.to = to;
            this.fromScale = piece.getScale().x;
            this.lift = lift;
            this.arc = arc;
            this.progress = new Animation(0f, 1f, easing, time);
            progress.startAnimation();
        }

        private boolean update(float dt) {
            progress.update(dt);
            float amount = progress.getValue();
            float bump = (float) Math.sin(Math.PI * amount);
            piece.setPosition(Math.round(from.x + (to.x - from.x) * amount),
                    Math.round(from.y + (to.y - from.y) * amount - arc * bump));
            piece.setScale(fromScale + (BOARD_SCALE - fromScale) * amount + lift * bump);
            return progress.isFinished();
        }
    }

    private static final class Confetti {
        private final Vector2f position = new Vector2f();
        private final Vector2f velocity = new Vector2f();
        private final Vector4f color = new Vector4f();
        private float flipTime;
        private float age;
    }

    public MatchScene(EngineContext context) {
        this(context, ThreadLocalRandom.current().nextBoolean() ? Board.Mark.BLACK : Board.Mark.WHITE,
                Board.Mark.BLACK, null, null);
    }

    // Against the computer, which plays white on the right
    public MatchScene(EngineContext context, AIOpponent ai) {
        this(context, Board.Mark.BLACK, Board.Mark.BLACK, ai, Board.Mark.WHITE);
    }

    protected MatchScene(EngineContext context,
                         Board.Mark firstTurn, Board.Mark leftMark, AIOpponent ai, Board.Mark aiMark) {
        super(context);
        this.board = new Board(firstTurn);
        this.firstTurn = firstTurn;
        this.leftMark = leftMark;
        this.ai = ai;
        this.aiMark = aiMark;
    }

    @Override
    protected void onInit(AssetManager assets) {
        boardTexture = assets.loadTexture("board", "/games/reversi/textures/board.png");
        boardSprite = new Sprite(boardTexture);
        boardSprite.setScale(BOARD_SCALE);
        logo = createLogo();
        pixel = new Sprite(assets.getTexture(AssetManager.WHITE_TEXTURE));

        Texture blackPieceTexture = assets.loadTexture("blackPiece", "/games/reversi/textures/black.png");
        Texture whitePieceTexture = assets.loadTexture("whitePiece", "/games/reversi/textures/white.png");

        blackSide = new PieceSide(Board.Mark.BLACK, leftMark == Board.Mark.BLACK ? -1f : 1f, blackPieceTexture);
        whiteSide = new PieceSide(Board.Mark.WHITE, leftMark == Board.Mark.WHITE ? -1f : 1f, whitePieceTexture);

        ghost = new Sprite(blackPieceTexture);
        ghost.setScale(BOARD_SCALE);
        ghost.setAlpha(GHOST_ALPHA);

        btnAction = createButton("LEAVE GAME", BUTTON_WIDTH, this::leave);
        btnPlayAgain = createButton("PLAY AGAIN", BUTTON_WIDTH, this::playAgain);

        for (int c = 0; c < Board.SIZE; c++) {
            for (int r = 0; r < Board.SIZE; r++) {
                Board.Mark mark = board.getCell(c, r);
                if (mark != null) {
                    Sprite s = new Sprite(sideOf(mark).pieceTexture);
                    s.setScale(BOARD_SCALE);
                    s.setPosition(getGridCenter(c, r));
                    boardPieces[c][r] = s;
                }
            }
        }
    }

    protected boolean canMove(Board.Mark mark) {
        return true;
    }

    private boolean isHuman(Board.Mark mark) {
        return ai == null || mark != aiMark;
    }

    protected void onMove(int col, int row) {
    }

    protected boolean isMatchOver() {
        return board.isGameOver();
    }

    protected boolean canPlayAgain() {
        return true;
    }

    private boolean showsPlayAgain() {
        return isMatchOver() && canPlayAgain();
    }

    private static String markName(Board.Mark mark) {
        return mark == Board.Mark.BLACK ? "BLACK" : "WHITE";
    }

    protected String getScoreText() {
        return "BLACK: " + board.getBlackScore() + " | WHITE: " + board.getWhiteScore();
    }

    protected String getStatus() {
        if (ai == null) {
            return markName(board.getTurn()) + "'S TURN";
        }
        return board.getTurn() == aiMark ? "AI'S TURN" : "YOUR TURN";
    }

    protected String getResultTitle() {
        Board.Mark winner = board.getWinner();
        if (winner == null) {
            return "DRAW!";
        }
        if (ai == null) {
            return markName(winner) + " WINS!";
        }
        return winner == aiMark ? "YOU LOSE!" : "YOU WIN!";
    }

    protected String getResultDetail() {
        return getScoreText();
    }

    protected Vector4f getResultColor() {
        Board.Mark winner = board.getWinner();
        if (winner == null) {
            return WHITE;
        }
        return ai != null && winner == aiMark ? LOSE_COLOR : WIN_COLOR;
    }

    protected boolean isCelebrating() {
        return board.getWinner() != null && isHuman(board.getWinner());
    }

    protected String getPlayerName(Board.Mark mark) {
        if (ai == null) return markName(mark);
        return mark == aiMark ? "AI" : "YOU";
    }

    private int getScore(Board.Mark mark) {
        return mark == Board.Mark.BLACK ? board.getBlackScore() : board.getWhiteScore();
    }

    protected void leave() {
        leaveTo(new OfflineMenuScene(context));
    }

    // Take turns starting
    protected void playAgain() {
        leaveTo(new MatchScene(context, firstTurn.opponent(), leftMark, ai, aiMark));
    }

    protected void leaveTo(BaseScene scene) {
        transitionTo(scene, getTransitionSprites(), getTransitionButtons());
    }

    @Override
    protected int getPalette() {
        if (!board.isGameOver()) {
            return paletteOf(board.getTurn());
        }
        Board.Mark winner = board.getWinner();
        return winner == null ? BackgroundRenderer.PALETTE_MENU : paletteOf(winner);
    }

    protected static int paletteOf(Board.Mark mark) {
        return mark == Board.Mark.BLACK ? BackgroundRenderer.PALETTE_BLACK : BackgroundRenderer.PALETTE_WHITE;
    }

    private Vector2f getGridCenter(int col, int row) {
        float cellTopLeftX = boardTopLeft.x + START_OFFSET_X + (col * STRIDE_X);
        float cellTopLeftY = boardTopLeft.y + START_OFFSET_Y + (row * STRIDE_Y);

        return new Vector2f(cellTopLeftX + CELL_HALF_W, cellTopLeftY + CELL_HALF_H);
    }

    private PieceSide sideOf(Board.Mark mark) {
        return mark == Board.Mark.BLACK ? blackSide : whiteSide;
    }

    @Override
    protected void onUpdate(float dt) {
        if (!isLeaving()) {
            btnAction.update(input);
            if (showsPlayAgain()) {
                btnPlayAgain.update(input);
            }
        }

        updateAI(dt);
        updateSide(blackSide, whiteSide, dt);
        updateSide(whiteSide, blackSide, dt);
        updateTap();
        updateSettling(dt);
        updateGameOver(dt);
        updateConfetti(dt);
    }

    private void updateAI(float dt) {
        if (ai == null || isTransitioning() || isMatchOver() || board.getTurn() != aiMark || !canMove(aiMark)) {
            aiMoveTimer = 0f;
            return;
        }

        aiMoveTimer += dt;
        if (aiMoveTimer < AI_MOVE_DELAY) return;

        aiMoveTimer = 0f;
        int[] move = ai.chooseMove(board);
        if (move != null) {
            playMove(move[0], move[1]);
            onMove(move[0], move[1]);
        }
    }

    private boolean isPlayable(PieceSide side) {
        return !isTransitioning() && !isMatchOver() && !board.isGameOver() && board.getTurn() == side.mark
                && canMove(side.mark) && isHuman(side.mark);
    }

    private void updateSide(PieceSide side, PieceSide other, float dt) {
        if (side.active == null) return;

        if (side.pickedUp && !isPlayable(side)) {
            side.putBack();
        }
        if (isPlayable(side) && !other.pickedUp && input.isButtonPressed(MouseButtons.LEFT) && side.active.contains(mousePosition)) {
            side.pickUp();
        }

        if (side.pickedUp) {
            if (input.isButtonDown(MouseButtons.LEFT)) {
                side.grabAnim.update(dt);
                side.grabAnim.setScale();
                Vector2f position = side.active.getPosition();
                side.active.setPosition(position.x + (float) input.getMouseDeltaX(), position.y + (float) input.getMouseDeltaY());
            } else {
                dropPiece(side);
            }
        } else if (side.returnAnim != null) {
            side.returnAnim.update(dt);
            side.returnAnim.setTransformation();
            if (side.returnAnim.isFinished()) {
                side.returnAnim = null;
                side.layout();
            }
        } else {
            side.bobTime = isPlayable(side) ? side.bobTime + dt : settleBob(side.bobTime, dt);
            side.snapActiveToRest();
        }
    }

    private static float settleBob(float bobTime, float dt) {
        float end = (float) Math.ceil(bobTime / BOB_TIME) * BOB_TIME;
        float next = bobTime + dt;
        return next >= end ? 0f : next;
    }

    private void updateTap() {
        PieceSide side = sideOf(board.getTurn());
        ghostCell = -1;
        if (side.active == null || !isPlayable(side)) {
            pressedCell = -1;
            return;
        }
        if (side.pickedUp) {
            pressedCell = -1;
            ghostCell = cellAt(side.active.getPosition());
            return;
        }

        int cell = cellAt(mousePosition);
        if (input.isButtonPressed(MouseButtons.LEFT)) {
            pressedCell = cell;
        }
        if (input.isButtonReleased(MouseButtons.LEFT)) {
            int tapped = pressedCell;
            pressedCell = -1;
            if (cell >= 0 && cell == tapped) {
                placePiece(side, cell % Board.SIZE, cell / Board.SIZE, true);
                onMove(cell % Board.SIZE, cell / Board.SIZE);
                return;
            }
        }
        ghostCell = cell;
    }

    private int cellAt(Vector2f position) {
        float boardW = boardTexture.getWidth() * BOARD_SCALE;
        float boardH = boardTexture.getHeight() * BOARD_SCALE;
        if (position.x < boardTopLeft.x || position.x > boardTopLeft.x + boardW
                || position.y < boardTopLeft.y || position.y > boardTopLeft.y + boardH) {
            return -1;
        }

        int bestCol = 0;
        int bestRow = 0;
        float bestDist = Float.MAX_VALUE;
        for (int c = 0; c < Board.SIZE; c++) {
            for (int r = 0; r < Board.SIZE; r++) {
                float dist = position.distanceSquared(getGridCenter(c, r));
                if (dist < bestDist) {
                    bestDist = dist;
                    bestCol = c;
                    bestRow = r;
                }
            }
        }
        return board.canPlace(bestCol, bestRow) ? bestRow * Board.SIZE + bestCol : -1;
    }

    private void dropPiece(PieceSide side) {
        int cell = cellAt(side.active.getPosition());
        if (cell >= 0) {
            placePiece(side, cell % Board.SIZE, cell / Board.SIZE, false);
            onMove(cell % Board.SIZE, cell / Board.SIZE);
            return;
        }

        side.putBack();
    }

    protected void playMove(int col, int row) {
        PieceSide side = sideOf(board.getTurn());
        if (side.active != null) {
            placePiece(side, col, row, true);
        }
    }

    private void placePiece(PieceSide side, int col, int row, boolean carried) {
        Sprite piece = side.active;
        boardPieces[col][row] = piece;
        board.place(col, row);

        PieceMove move = carried
                ? new PieceMove(piece, getGridCenter(col, row), GRAB_SCALE - BOARD_SCALE, PLAY_MOVE_ARC, PLAY_MOVE_TIME, EasingType.SINE_IN_OUT)
                : new PieceMove(piece, getGridCenter(col, row), 0f, 0f, MOVE_TIME, EasingType.EXPONENTIAL_OUT);
        settling.add(move);

        syncPieceTextures();

        side.spawnNext();

        if (board.isGameOver()) {
            finishMatch();
        } else {
            updatePalette();
        }
    }

    protected void finishMatch() {
        if (fadeAnim == null) {
            fadeAnim = new Animation(1f, FADE_ALPHA, EasingType.EXPONENTIAL_OUT, FADE_TIME);
            fadeAnim.startAnimation();
            resultAnim = new Animation(0f, 1f, EasingType.BACK_OUT, RESULT_DROP_TIME);
        }
        blackSide.active = null;
        whiteSide.active = null;
        layoutButtons();
        updatePalette();
    }

    private void updateSettling(float dt) {
        Iterator<PieceMove> it = settling.iterator();
        while (it.hasNext()) {
            if (it.next().update(dt)) {
                it.remove();
            }
        }
    }

    private void updateGameOver(float dt) {
        if (fadeAnim == null || !settling.isEmpty()) return;

        if (!resultAnim.hasStarted()) {
            resultAnim.startAnimation();
            if (isCelebrating()) {
                spawnConfetti();
            }
        }
        resultAnim.update(dt);
        fadeAnim.update(dt);
        float alpha = fadeAnim.getValue();
        blackSide.setAlpha(alpha);
        whiteSide.setAlpha(alpha);
    }

    private void spawnConfetti() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector4f main = getResultColor();
        for (int i = 0; i < CONFETTI_COUNT; i++) {
            Confetti piece = new Confetti();
            boolean left = i % 2 == 0;
            piece.position.set(left ? -UI_SPACING : currentWidth + UI_SPACING, currentHeight * random.nextFloat(0.55f, 0.85f));
            float angle = (float) Math.toRadians(random.nextFloat(50f, 80f));
            float speed = random.nextFloat(500f, 1000f);
            piece.velocity.set((left ? 1f : -1f) * (float) Math.cos(angle) * speed, -(float) Math.sin(angle) * speed);
            Vector4f color = switch (i % 4) {
                case 0 -> main;
                case 1 -> WHITE;
                case 2 -> SKY_COLOR;
                default -> BLUE_COLOR;
            };
            piece.color.set(color);
            piece.flipTime = random.nextFloat(0.08f, 0.2f);
            confetti.add(piece);
        }
    }

    private void updateConfetti(float dt) {
        Iterator<Confetti> it = confetti.iterator();
        while (it.hasNext()) {
            Confetti piece = it.next();
            piece.age += dt;
            piece.velocity.y += CONFETTI_GRAVITY * dt;
            piece.velocity.x -= piece.velocity.x * Math.min(1f, 1.5f * dt);
            piece.position.add(piece.velocity.x * dt, piece.velocity.y * dt);
            if (piece.age > CONFETTI_TIME || piece.position.y > currentHeight + UI_SPACING) {
                it.remove();
            }
        }
    }

    private boolean isSettling(Sprite piece) {
        for (PieceMove move : settling) {
            if (move.piece == piece) return true;
        }
        return false;
    }

    @Override
    protected void onRender() {
        if (logoVisible) {
            spriteRenderer.draw(logo);
        }
        spriteRenderer.draw(boardSprite);
        blackSide.drawResting();
        whiteSide.drawResting();
        for (int c = 0; c < Board.SIZE; c++) {
            for (int r = 0; r < Board.SIZE; r++) {
                Sprite piece = boardPieces[c][r];
                if (piece != null && !isSettling(piece)) spriteRenderer.draw(piece);
            }
        }
        renderGhost();
        renderConfetti();

        renderStatus();
        drawSideInfo(blackSide);
        drawSideInfo(whiteSide);

        btnAction.render(spriteRenderer, textRenderer);
        if (showsPlayAgain()) {
            btnPlayAgain.render(spriteRenderer, textRenderer);
        }

        for (PieceMove move : settling) {
            spriteRenderer.draw(move.piece);
        }
        blackSide.drawMoving();
        whiteSide.drawMoving();
    }

    private void renderGhost() {
        if (ghostCell < 0) return;

        PieceSide side = sideOf(board.getTurn());
        ghost.setTexture(side.pieceTexture);
        ghost.setPosition(getGridCenter(ghostCell % Board.SIZE, ghostCell / Board.SIZE));
        spriteRenderer.draw(ghost);
    }

    private void renderConfetti() {
        pixel.setOrigin(0.5f, 0.5f);
        for (Confetti piece : confetti) {
            boolean flipped = (int) (piece.age / piece.flipTime) % 2 == 0;
            pixel.setPosition(Math.round(piece.position.x), Math.round(piece.position.y));
            pixel.setScale(flipped ? CONFETTI_SIZE : UI_SCALE, flipped ? UI_SCALE : CONFETTI_SIZE);
            pixel.setTint(piece.color.x, piece.color.y, piece.color.z, 1f);
            pixel.setAlpha(Math.min(1f, 3f * (CONFETTI_TIME - piece.age)));
            spriteRenderer.draw(pixel);
        }
        pixel.setAlpha(1f);
    }

    private void syncPieceTextures() {
        for (int c = 0; c < Board.SIZE; c++) {
            for (int r = 0; r < Board.SIZE; r++) {
                Board.Mark mark = board.getCell(c, r);
                if (boardPieces[c][r] != null && mark != null) {
                    boardPieces[c][r].setTexture(sideOf(mark).pieceTexture);
                }
            }
        }
    }

    private void renderStatus() {
        if (fadeAnim != null) {
            if (!resultAnim.hasStarted()) return;

            float drop = Math.round((1f - resultAnim.getValue()) * RESULT_DROP);
            drawCenteredText(gameFont, fitText(gameFont, getResultTitle(), getMaxTextWidth()), statusY - drop, getResultColor());

            String detail = fitText(smallFont, getResultDetail(), getMaxTextWidth());
            drawCenteredText(smallFont, detail, statusY + gameFont.getCapHeight() + LINE_GAP - drop, WHITE);
            return;
        }

        String status = getStatus();
        Font statusFont = gameFont.getTextWidth(status) <= getMaxTextWidth() ? gameFont : smallFont;
        float y = statusY + (statusHeight - statusFont.getCapHeight()) / 2f;
        drawCenteredText(statusFont, fitText(statusFont, status, getMaxTextWidth()), y, WHITE);
    }

    private void drawSideInfo(PieceSide side) {
        String name = getPlayerName(side.mark);
        if (name != null) {
            drawText(smallFont, fitText(smallFont, name, getNameSpace()), side.nameX, nameY, WHITE);
        }
        String count = String.valueOf(getScore(side.mark));
        drawText(smallFont, count, side.countX, countY, DIM_WHITE);
    }

    private float getNameSpace() {
        return currentWidth / 2f - 1.5f * UI_SPACING;
    }

    @Override
    protected void onResize(int width, int height) {
        float boardW = boardTexture.getWidth() * BOARD_SCALE;
        float boardH = boardTexture.getHeight() * BOARD_SCALE;
        boardSprite.setPosition(Math.round(width / 2f), Math.round(height / 2f));
        boardTopLeft.set(boardSprite.getPosition().x - boardW / 2f, boardSprite.getPosition().y - boardH / 2f);

        float logoHeight = logo.getTexture().getHeight() * UI_SCALE;
        statusHeight = gameFont.getCapHeight() + LINE_GAP + smallFont.getCapHeight();
        logoVisible = logoHeight + UI_SPACING + statusHeight + 2 * UI_SPACING <= boardTopLeft.y;

        float headerHeight = logoVisible ? logoHeight + UI_SPACING + statusHeight : statusHeight;
        float headerY = Math.round((boardTopLeft.y - headerHeight) / 2f);
        logo.setPosition(Math.round(width / 2f), headerY + logoHeight / 2f);
        statusY = logoVisible ? headerY + logoHeight + UI_SPACING : headerY;

        settling.clear();
        for (int c = 0; c < Board.SIZE; c++) {
            for (int r = 0; r < Board.SIZE; r++) {
                if (boardPieces[c][r] != null) {
                    boardPieces[c][r].setTexture(sideOf(board.getCell(c, r)).pieceTexture);
                    boardPieces[c][r].setPosition(getGridCenter(c, r));
                    boardPieces[c][r].setScale(BOARD_SCALE);
                }
            }
        }

        blackSide.returnAnim = null;
        whiteSide.returnAnim = null;
        blackSide.layout();
        whiteSide.layout();

        layoutNames();
        layoutButtons();
    }

    private void layoutNames() {
        float boardLeft = boardTopLeft.x;
        float boardRight = boardLeft + boardTexture.getWidth() * BOARD_SCALE;
        float boardBottom = boardTopLeft.y + boardTexture.getHeight() * BOARD_SCALE;
        namesBelowBoard = false;
        for (PieceSide side : List.of(blackSide, whiteSide)) {
            String name = getPlayerName(side.mark);
            if (name != null && smallFont.getTextWidth(name) > boardLeft - 2 * UI_SPACING) {
                namesBelowBoard = true;
            }
        }

        float pieceHalfH = blackSide.pieceTexture.getHeight() * BOARD_SCALE / 2f;
        nameY = namesBelowBoard
                ? boardBottom + UI_SPACING
                : Math.round(currentHeight / 2f) + pieceHalfH + UI_SPACING;
        countY = nameY + smallFont.getCapHeight() + LINE_GAP;
        float leftLimit = namesBelowBoard ? currentWidth / 2f : boardLeft;
        float rightLimit = namesBelowBoard ? currentWidth / 2f : boardRight;

        for (PieceSide side : List.of(blackSide, whiteSide)) {
            String name = getPlayerName(side.mark);
            float width = name == null ? 0f : smallFont.getTextWidth(fitText(smallFont, name, getNameSpace()));
            float min = side.direction < 0 ? UI_SPACING : rightLimit + UI_SPACING / 2f;
            float max = side.direction < 0 ? leftLimit - UI_SPACING / 2f - width : currentWidth - UI_SPACING - width;
            side.nameX = Math.round(Math.max(min, Math.min(max, side.sideX() - width / 2f)));

            float countWidth = smallFont.getTextWidth(String.valueOf(getScore(side.mark)));
            float countMax = side.direction < 0 ? leftLimit - UI_SPACING / 2f - countWidth : currentWidth - UI_SPACING - countWidth;
            side.countX = Math.round(Math.max(min, Math.min(countMax, side.sideX() - countWidth / 2f)));
        }
    }

    @Override
    protected Collection<Sprite> getTransitionSprites() {
        List<Sprite> sprites = new ArrayList<>();
        sprites.add(boardSprite);
        if (logoVisible) sprites.add(logo);
        blackSide.collectSprites(sprites);
        whiteSide.collectSprites(sprites);
        for (int c = 0; c < Board.SIZE; c++) {
            for (int r = 0; r < Board.SIZE; r++) {
                if (boardPieces[c][r] != null) sprites.add(boardPieces[c][r]);
            }
        }
        return sprites;
    }

    @Override
    protected Button[] getTransitionButtons() {
        return showsPlayAgain() ? new Button[]{btnPlayAgain, btnAction} : new Button[]{btnAction};
    }

    private void layoutButtons() {
        float boardBottom = boardTopLeft.y + boardTexture.getHeight() * BOARD_SCALE;
        float top = namesBelowBoard ? countY + smallFont.getCapHeight() : boardBottom;
        float y = Math.round(top + (currentHeight - top - BUTTON_HEIGHT) / 2f);
        float centerX = Math.round(currentWidth / 2f);

        if (showsPlayAgain()) {
            btnPlayAgain.setPosition(centerX - UI_SPACING / 2f - BUTTON_WIDTH, y);
            btnAction.setPosition(centerX + UI_SPACING / 2f, y);
        } else {
            btnAction.setPosition(centerX - BUTTON_WIDTH / 2f, y);
        }
    }
}
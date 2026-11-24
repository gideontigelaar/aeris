package nl.team3.games.reversi.logic;

public class Board {
    public static final int SIZE = 8;

    public enum Mark {
        BLACK, WHITE;

        public Mark opponent() {
            return this == BLACK ? WHITE : BLACK;
        }
    }

    private boolean gameOver = false;
    private Mark[][] cells = new Mark[SIZE][SIZE];
    private Mark turn;
    private Mark winner;
    private int blackScore = 0;
    private int whiteScore = 0;

    public Board(Mark firstTurn) {
        this.turn = firstTurn;
        int mid = SIZE / 2;
        cells[mid - 1][mid - 1] = Mark.WHITE;
        cells[mid][mid]         = Mark.WHITE;
        cells[mid - 1][mid]     = Mark.BLACK;
        cells[mid][mid - 1]     = Mark.BLACK;
        updateScore();
    }

    private boolean hasAnyMove(Mark mark) {
        for (int c = 0; c < SIZE; c++)
            for (int r = 0; r < SIZE; r++)
                if (getAllFlipped(cells, c, r, mark) != cells) return true;
        return false;
    }

    public boolean canPlace(int col, int row) {
        return !isGameOver() && cells[col][row] == null && cells != getAllFlipped(cells, col, row, turn);
    }

    private void updateScore() {
        blackScore = 0;
        whiteScore = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (cells[x][y] == Mark.BLACK) {
                    blackScore += 1;
                } else if (cells[x][y] == Mark.WHITE) {
                    whiteScore += 1;
                }
            }
        }
    }

    public void place(int col, int row) {
        if (!canPlace(col, row)) {
            throw new IllegalStateException("Cell " + col + "," + row + " is not available");
        }

        cells = getAllFlipped(cells, col, row, turn);

        updateScore();
        Mark next = turn.opponent();

        if (hasAnyMove(next)) {
            turn = next;
        } else if (!hasAnyMove(turn)) {
            gameOver = true;
            if (blackScore > whiteScore) {
                winner = Mark.BLACK;
            } else if (whiteScore > blackScore) {
                winner = Mark.WHITE;
            } else {
                winner = null;
            }
        }
    }

    Mark[][] copyCells() {
        Mark[][] copy = new Mark[SIZE][];
        for (int col = 0; col < SIZE; col++) {
            copy[col] = cells[col].clone();
        }
        return copy;
    }

    public Mark[][] flipInDirection(Mark[][] cells, int dx, int dy, int col, int row, Mark player) {
        Mark[][] flips = new Mark[SIZE][];
        for (int i = 0; i < SIZE; i++) {
            flips[i] = cells[i].clone();
        }
        int stepX = Integer.signum(dx);
        int stepY = Integer.signum(dy);
        int c = col + stepX;
        int r = row + stepY;

        boolean straightLine = (dx != 0 || dy != 0)
                && (dx == 0 || dy == 0 || Math.abs(dx) == Math.abs(dy));
        if (straightLine) {
            while (c >= 0 && c < SIZE && r >= 0 && r < SIZE) {
                if (cells[c][r] == null) {
                    return cells;
                }
                if (cells[c][r] == player) {
                    if (c == col + stepX && r == row + stepY) {
                        return cells;
                    }
                    return flips;
                } else {
                    flips[c][r] = player;
                }
                c += stepX;
                r += stepY;
            }
        }
        return cells;
    }

    public Mark[][] getAllFlipped(Mark[][] cells, int col, int row, Mark player) {
        if (col < 0 || col >= SIZE || row < 0 || row >= SIZE || cells[col][row] != null) {
            return cells;
        }

        Mark[][] result = cells;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                result = flipInDirection(result, dx, dy, col, row, player);
            }
        }

        if (result != cells) {
            result[col][row] = player;
        }

        return result;
    }

    public Mark getCell(int col, int row) {
        return cells[col][row];
    }

    public Mark getTurn() {
        return turn;
    }

    public Mark getWinner() {
        return winner;
    }

    public int getBlackScore() {
        return blackScore;
    }

    public int getWhiteScore() {
        return whiteScore;
    }

    public boolean isDraw() {
        return gameOver && winner == null;
    }

    public boolean isGameOver() {
        return gameOver;
    }
}
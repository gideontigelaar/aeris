package nl.team3.games.reversi.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AIOpponent {
    public enum Difficulty { FIFTY_FIFTY, UNBEATABLE }

    private static final int SIZE = Board.SIZE;
    private static final int CELLS = SIZE * SIZE;

    private static final long TIME_LIMIT_MS = 10;
    private static final int MAX_DEPTH = 12;
    private static final int EXACT_EMPTIES = 10;
    private static final int WIN_SCALE = 1000;
    private static final int INF = 1_000_000;

    private static final int[][] DIRS = {
            {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}
    };

    private static final int[] WEIGHT = new int[CELLS];

    private static final int[] ORDER = new int[CELLS];

    static {
        int[][] w = {
                {100,  -5,  10,   5,   5,  10,  -5, 100},
                { -5,   0,   1,   1,   1,   1,   0,  -5},
                { 10,   1,   3,   2,   2,   3,   1,  10},
                {  5,   1,   2,   1,   1,   2,   1,   5},
                {  5,   1,   2,   1,   1,   2,   1,   5},
                { 10,   1,   3,   2,   2,   3,   1,  10},
                { -5,   0,   1,   1,   1,   1,   0,  -5},
                {100,  -5,  10,   5,   5,  10,  -5, 100}
        };
        Integer[] idx = new Integer[CELLS];
        for (int c = 0; c < SIZE; c++) {
            for (int r = 0; r < SIZE; r++) {
                WEIGHT[c * SIZE + r] = w[r][c];
                idx[c * SIZE + r] = c * SIZE + r;
            }
        }
        java.util.Arrays.sort(idx, (a, b) -> WEIGHT[b] - WEIGHT[a]);
        for (int i = 0; i < CELLS; i++) ORDER[i] = idx[i];
    }

    private final Difficulty difficulty;

    private long deadline;
    private boolean timedOut;
    private int nodes;

    public AIOpponent(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public int[] chooseMove(Board board) {
        if (board.isGameOver()) {
            return null;
        }

        int me = board.getTurn() == Board.Mark.BLACK ? 1 : -1;
        int[] cells = toArray(board.copyCells());

        List<Integer> legal = new ArrayList<>();
        for (int sq : ORDER) {
            if (cells[sq] == 0 && play(cells, sq, me) != null) legal.add(sq);
        }
        if (legal.isEmpty()) {
            return null;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (difficulty == Difficulty.FIFTY_FIFTY && random.nextBoolean()) {
            return toMove(legal.get(random.nextInt(legal.size())));
        }
        if (legal.size() == 1) {
            return toMove(legal.get(0));
        }

        return toMove(search(cells, me, legal, random));
    }

    private int search(int[] cells, int me, List<Integer> legal, ThreadLocalRandom random) {
        deadline = System.currentTimeMillis() + TIME_LIMIT_MS;
        timedOut = false;
        nodes = 0;

        int empties = countEmpties(cells);
        int startDepth = empties <= EXACT_EMPTIES ? empties : 1;
        int maxDepth = Math.min(MAX_DEPTH, empties);

        int[] bestScores = null;
        List<Integer> moves = new ArrayList<>(legal);

        for (int depth = startDepth; depth <= maxDepth; depth++) {
            int[] scores = new int[moves.size()];
            boolean complete = true;

            for (int i = 0; i < moves.size(); i++) {
                int[] child = play(cells, moves.get(i), me);
                scores[i] = -negamax(child, -me, depth - 1, -INF, INF, false);
                if (timedOut) {
                    complete = false;
                    break;
                }
            }
            if (!complete) break;

            bestScores = scores;
            int bestIdx = 0;
            for (int i = 1; i < scores.length; i++) if (scores[i] > scores[bestIdx]) bestIdx = i;
            Integer best = moves.remove(bestIdx);
            moves.add(0, best);
            int bestScore = scores[bestIdx];
            int[] reordered = new int[scores.length];
            reordered[0] = bestScore;
            for (int i = 0, j = 1; i < scores.length; i++) {
                if (i != bestIdx) reordered[j++] = scores[i];
            }
            bestScores = reordered;
        }

        if (bestScores == null) {
            return legal.get(0);
        }

        int top = Integer.MIN_VALUE;
        for (int s : bestScores) top = Math.max(top, s);
        List<Integer> best = new ArrayList<>();
        for (int i = 0; i < bestScores.length; i++) {
            if (bestScores[i] == top) best.add(moves.get(i));
        }
        return best.get(random.nextInt(best.size()));
    }

    private int negamax(int[] cells, int player, int depth, int alpha, int beta, boolean passed) {
        if ((++nodes & 1023) == 0 && System.currentTimeMillis() > deadline) {
            timedOut = true;
        }
        if (timedOut) return 0;

        int empties = countEmpties(cells);
        if (empties == 0) {
            return finalScore(cells, player);
        }
        if (depth <= 0) {
            return evaluate(cells, player);
        }

        int best = -INF;
        boolean moved = false;
        for (int sq : ORDER) {
            if (cells[sq] != 0) continue;
            int[] child = play(cells, sq, player);
            if (child == null) continue;

            moved = true;
            int score = -negamax(child, -player, depth - 1, -beta, -alpha, false);
            if (timedOut) return 0;

            if (score > best) best = score;
            if (score > alpha) alpha = score;
            if (alpha >= beta) return best;
        }

        if (!moved) {
            if (passed) {
                return finalScore(cells, player);
            }
            return -negamax(cells, -player, depth, -beta, -alpha, true); // pass
        }
        return best;
    }

    private static int evaluate(int[] cells, int player) {
        int positional = 0;
        int discs = 0;
        int empties = 0;
        for (int i = 0; i < CELLS; i++) {
            int v = cells[i];
            if (v == 0) { empties++; continue; }
            positional += v * WEIGHT[i];
            discs += v;
        }

        positional += cornerNeighbours(cells, 0, 0, 1, 1);
        positional += cornerNeighbours(cells, SIZE - 1, 0, -1, 1);
        positional += cornerNeighbours(cells, 0, SIZE - 1, 1, -1);
        positional += cornerNeighbours(cells, SIZE - 1, SIZE - 1, -1, -1);

        int myMoves = countMoves(cells, player);
        int oppMoves = countMoves(cells, -player);
        int mobility = 0;
        if (myMoves + oppMoves > 0) {
            mobility = 100 * (myMoves - oppMoves) / (myMoves + oppMoves);
        }

        int score = player * positional + 4 * mobility;
        if (empties < 16) {
            score += 3 * player * discs;
        }
        return score;
    }

    private static int cornerNeighbours(int[] cells, int cc, int cr, int dc, int dr) {
        if (cells[cc * SIZE + cr] != 0) return 0;
        int x = cells[(cc + dc) * SIZE + (cr + dr)];
        int c1 = cells[(cc + dc) * SIZE + cr];
        int c2 = cells[cc * SIZE + (cr + dr)];
        return -(x * 40 + c1 * 15 + c2 * 15);
    }

    private static int finalScore(int[] cells, int player) {
        int diff = 0;
        for (int v : cells) diff += v;
        return diff * player * WIN_SCALE;
    }

    private static int countMoves(int[] cells, int player) {
        int n = 0;
        for (int sq = 0; sq < CELLS; sq++) {
            if (cells[sq] == 0 && canFlip(cells, sq, player)) n++;
        }
        return n;
    }

    private static boolean canFlip(int[] cells, int sq, int player) {
        int col = sq / SIZE, row = sq % SIZE;
        for (int[] d : DIRS) {
            int c = col + d[0], r = row + d[1];
            int count = 0;
            while (c >= 0 && c < SIZE && r >= 0 && r < SIZE && cells[c * SIZE + r] == -player) {
                c += d[0];
                r += d[1];
                count++;
            }
            if (count > 0 && c >= 0 && c < SIZE && r >= 0 && r < SIZE && cells[c * SIZE + r] == player) {
                return true;
            }
        }
        return false;
    }

    private static int[] play(int[] cells, int sq, int player) {
        int col = sq / SIZE, row = sq % SIZE;
        int[] result = null;
        for (int[] d : DIRS) {
            int c = col + d[0], r = row + d[1];
            int count = 0;
            while (c >= 0 && c < SIZE && r >= 0 && r < SIZE && cells[c * SIZE + r] == -player) {
                c += d[0];
                r += d[1];
                count++;
            }
            if (count > 0 && c >= 0 && c < SIZE && r >= 0 && r < SIZE && cells[c * SIZE + r] == player) {
                if (result == null) {
                    result = cells.clone();
                    result[sq] = player;
                }
                for (int i = 1; i <= count; i++) {
                    result[(col + d[0] * i) * SIZE + (row + d[1] * i)] = player;
                }
            }
        }
        return result;
    }

    private static int countEmpties(int[] cells) {
        int n = 0;
        for (int v : cells) if (v == 0) n++;
        return n;
    }

    private static int[] toArray(Board.Mark[][] marks) {
        int[] cells = new int[CELLS];
        for (int c = 0; c < SIZE; c++) {
            for (int r = 0; r < SIZE; r++) {
                Board.Mark m = marks[c][r];
                cells[c * SIZE + r] = m == null ? 0 : (m == Board.Mark.BLACK ? 1 : -1);
            }
        }
        return cells;
    }

    private static int[] toMove(int sq) {
        return new int[]{sq / SIZE, sq % SIZE};
    }
}
package nl.team3.games.reversi.logic;

import nl.team3.engine.graphics.animation.Interpolators;
import org.joml.Vector2f;

import java.util.Arrays;
import java.util.List;

public class Board {
    public static final int SIZE = 8;
    public int Xscore = 0;
    public int Oscore = 0;
    public enum Mark {
        X, O;

        public Mark opponent() {
            return this == X ? O : X;
        }
    }

    // All rows, columns and diagonals as {col, row}

    private boolean gameOver = false;
    private Mark[][] cells = new Mark[SIZE][SIZE];
    private Mark turn;
    private Mark winner;
    private int[][] winningLine;
    private int moves;

    public Board(Mark firstTurn) {
        this.turn = firstTurn;
        int mid = SIZE / 2;
        cells[mid - 1][mid - 1] = Mark.O;
        cells[mid][mid]         = Mark.O;
        cells[mid - 1][mid]     = Mark.X;
        cells[mid][mid - 1]     = Mark.X;
        updateScore();
    }

    private boolean hasAnyMove(Mark mark) {
        for (int c = 0; c < SIZE; c++)
            for (int r = 0; r < SIZE; r++)
                if (getAllFlipped(cells, c, r, mark) != cells) return true;
        return false;
    }

    public boolean canPlace(int col, int row) {
        return !isGameOver() && cells[col][row] == null && cells != getAllFlipped(cells, col,row, turn);
    }

    private void updateScore(){
        Xscore = 0; Oscore = 0;
        for(int x = 0; x < SIZE; x++){
            for(int y = 0; y < SIZE; y++){
                if(cells[x][y] == Mark.X){Xscore += 1;}
                else if(cells[x][y] == Mark.O){Oscore += 1;}
            }
        }
    }

    public void place(int col, int row) {

        if (!canPlace(col, row)) {
            throw new IllegalStateException("Cell " + col + "," + row + " is not available");
        }



        cells = getAllFlipped(cells, col,row, turn);
        moves++;

        updateScore();
        Mark next = turn.opponent();

        if(hasAnyMove(next)){
            turn = turn.opponent();
        }
        else if(!hasAnyMove(turn)){
            gameOver = true;
            if(Xscore > Oscore) {
                winner = Mark.X;
            }
            else if(Oscore > Xscore){winner = Mark.O;}
            else{winner=null;}
        }
    }



    Mark[][] copyCells() {
        Mark[][] copy = new Mark[SIZE][];
        for (int col = 0; col < SIZE; col++) {
            copy[col] = cells[col].clone();
        }
        return copy;
    }


    public Mark[][] flipInDirection(Mark[][] cells, int dx, int dy, int col, int row, Mark player){

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
        if(straightLine){
            while (c >= 0 && c < SIZE && r >= 0 && r < SIZE) {
                if(cells[c][r] == null){return cells;}
                if(cells[c][r] == player){
                    if (c == col + stepX && r == row + stepY) {return cells;}
                    return flips;}
                else{flips[c][r] = player;}
                c += stepX;
                r += stepY;
            }
        }
        return cells;
    }


    public Mark[][] getAllFlipped( Mark[][] cells, int col, int row, Mark player){

        if (col < 0 || col >= SIZE || row < 0 || row >= SIZE || cells[col][row] != null) {
            return cells;
        }

        Mark[][] result = cells;
        for(int dx = -1; dx <= 1; dx++){
            for(int dy = -1; dy <= 1; dy++){
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

    public int getXscore(){ return Xscore;}
    public int getOscore(){ return Oscore;}

    public boolean isDraw() {
        return gameOver && winner == null;
    }

    public boolean isGameOver() {
        return gameOver;
    }
}
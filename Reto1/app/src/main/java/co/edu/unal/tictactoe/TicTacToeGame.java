package co.edu.unal.tictactoe;

import java.util.Random;

public class TicTacToeGame {
    public static final int BOARD_SIZE = 9;
    public static final int STATUS_IN_PROGRESS = 0;
    public static final int STATUS_TIE = 1;
    public static final int STATUS_HUMAN_WON = 2;
    public static final int STATUS_COMPUTER_WON = 3;

    public static final char HUMAN_PLAYER = 'X';
    public static final char COMPUTER_PLAYER = 'O';
    public static final char OPEN_SPOT = ' ';

    private final char[] mBoard;
    private final Random mRandom;

    public TicTacToeGame() {
        mBoard = new char[BOARD_SIZE];
        mRandom = new Random();
        clearBoard();
    }

    public void clearBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            mBoard[i] = OPEN_SPOT;
        }
    }

    public boolean setMove(char player, int location) {
        if (location >= 0 && location < BOARD_SIZE && mBoard[location] == OPEN_SPOT) {
            mBoard[location] = player;
            return true;
        }

        return false;
    }

    public int getComputerMove() {
        int winningMove = findWinningMove(COMPUTER_PLAYER);
        if (winningMove != -1) {
            return winningMove;
        }

        int blockingMove = findWinningMove(HUMAN_PLAYER);
        if (blockingMove != -1) {
            return blockingMove;
        }

        if (mBoard[4] == OPEN_SPOT) {
            return 4;
        }

        int move = getRandomOpenMove();

        return move;
    }

    public int checkForWinner() {
        for (int[] line : getWinningLines()) {
            if (mBoard[line[0]] == HUMAN_PLAYER
                    && mBoard[line[1]] == HUMAN_PLAYER
                    && mBoard[line[2]] == HUMAN_PLAYER) {
                return STATUS_HUMAN_WON;
            }

            if (mBoard[line[0]] == COMPUTER_PLAYER
                    && mBoard[line[1]] == COMPUTER_PLAYER
                    && mBoard[line[2]] == COMPUTER_PLAYER) {
                return STATUS_COMPUTER_WON;
            }
        }

        if (hasOpenSpot()) {
            return STATUS_IN_PROGRESS;
        }

        return STATUS_TIE;
    }

    public char getBoardOccupant(int location) {
        if (location < 0 || location >= BOARD_SIZE) {
            return OPEN_SPOT;
        }

        return mBoard[location];
    }

    private int findWinningMove(char player) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = player;
                boolean canWin = checkForWinner() == (player == HUMAN_PLAYER
                        ? STATUS_HUMAN_WON
                        : STATUS_COMPUTER_WON);
                mBoard[i] = OPEN_SPOT;
                if (canWin) {
                    return i;
                }
            }
        }

        return -1;
    }

    private int getRandomOpenMove() {
        if (!hasOpenSpot()) {
            return -1;
        }

        int move;
        do {
            move = mRandom.nextInt(BOARD_SIZE);
        } while (mBoard[move] != OPEN_SPOT);

        return move;
    }

    private boolean hasOpenSpot() {
        for (char spot : mBoard) {
            if (spot == OPEN_SPOT) {
                return true;
            }
        }

        return false;
    }

    private int[][] getWinningLines() {
        return new int[][]{
                {0, 1, 2},
                {3, 4, 5},
                {6, 7, 8},
                {0, 3, 6},
                {1, 4, 7},
                {2, 5, 8},
                {0, 4, 8},
                {2, 4, 6}
        };
    }
}

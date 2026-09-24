package co.edu.unal.tictactoe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class AndroidTicTacToeActivity extends Activity {
    private TicTacToeGame mGame;
    private Button[] mBoardButtons;
    private TextView mInfoTextView;
    private TextView mHumanScoreTextView;
    private TextView mTieScoreTextView;
    private TextView mComputerScoreTextView;
    private Button mNewGameButton;
    private boolean mGameOver;
    private boolean mHumanStartsNextGame = true;
    private int mHumanWins;
    private int mComputerWins;
    private int mTies;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mBoardButtons = new Button[TicTacToeGame.BOARD_SIZE];
        mBoardButtons[0] = findViewById(R.id.one);
        mBoardButtons[1] = findViewById(R.id.two);
        mBoardButtons[2] = findViewById(R.id.three);
        mBoardButtons[3] = findViewById(R.id.four);
        mBoardButtons[4] = findViewById(R.id.five);
        mBoardButtons[5] = findViewById(R.id.six);
        mBoardButtons[6] = findViewById(R.id.seven);
        mBoardButtons[7] = findViewById(R.id.eight);
        mBoardButtons[8] = findViewById(R.id.nine);
        mInfoTextView = findViewById(R.id.information);
        mHumanScoreTextView = findViewById(R.id.human_score);
        mTieScoreTextView = findViewById(R.id.tie_score);
        mComputerScoreTextView = findViewById(R.id.computer_score);
        mNewGameButton = findViewById(R.id.new_game_button);
        mNewGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNewGame();
            }
        });

        mGame = new TicTacToeGame();
        startNewGame();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.new_game) {
            startNewGame();
            return true;
        } else if (itemId == R.id.ai_difficulty) {
            showDifficultyDialog();
            return true;
        } else if (itemId == R.id.about) {
            showAboutDialog();
            return true;
        } else if (itemId == R.id.quit) {
            showQuitDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startNewGame() {
        mGame.clearBoard();
        mGameOver = false;

        for (int i = 0; i < mBoardButtons.length; i++) {
            mBoardButtons[i].setText("");
            mBoardButtons[i].setTextColor(Color.rgb(15, 23, 42));
            mBoardButtons[i].setClickable(true);
            mBoardButtons[i].setOnClickListener(new ButtonClickListener(i));
        }

        updateScoreBoard();

        if (mHumanStartsNextGame) {
            mInfoTextView.setText(R.string.first_human);
        } else {
            mInfoTextView.setText(R.string.first_computer);
            makeComputerMove();
            updateGameState(mGame.checkForWinner());
        }

        mHumanStartsNextGame = !mHumanStartsNextGame;
    }

    private void setMove(char player, int location) {
        if (!mGame.setMove(player, location)) {
            return;
        }

        mBoardButtons[location].setClickable(false);
        mBoardButtons[location].setText(String.valueOf(player));

        if (player == TicTacToeGame.HUMAN_PLAYER) {
            mBoardButtons[location].setTextColor(Color.rgb(22, 163, 74));
        } else {
            mBoardButtons[location].setTextColor(Color.rgb(220, 38, 38));
        }
    }

    private void updateGameState(int winner) {
        if (winner == TicTacToeGame.STATUS_IN_PROGRESS) {
            mInfoTextView.setText(R.string.turn_human);
        } else if (winner == TicTacToeGame.STATUS_TIE) {
            mTies++;
            mInfoTextView.setText(R.string.result_tie);
            endGame();
        } else if (winner == TicTacToeGame.STATUS_HUMAN_WON) {
            mHumanWins++;
            mInfoTextView.setText(R.string.result_human_wins);
            endGame();
        } else {
            mComputerWins++;
            mInfoTextView.setText(R.string.result_computer_wins);
            endGame();
        }

        updateScoreBoard();
    }

    private void endGame() {
        mGameOver = true;
        for (Button button : mBoardButtons) {
            button.setClickable(false);
        }
    }

    private void makeComputerMove() {
        int move = mGame.getComputerMove();
        if (move != -1) {
            setMove(TicTacToeGame.COMPUTER_PLAYER, move);
        }
    }

    private void updateScoreBoard() {
        mHumanScoreTextView.setText(getString(R.string.score_human, mHumanWins));
        mTieScoreTextView.setText(getString(R.string.score_ties, mTies));
        mComputerScoreTextView.setText(getString(R.string.score_android, mComputerWins));
    }

    private void showDifficultyDialog() {
        final CharSequence[] levels = {
                getString(R.string.difficulty_easy),
                getString(R.string.difficulty_harder),
                getString(R.string.difficulty_expert)
        };

        int selected = getSelectedDifficultyIndex();

        new AlertDialog.Builder(this)
                .setTitle(R.string.difficulty_choose)
                .setSingleChoiceItems(levels, selected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (item == 0) {
                            mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.EASY);
                        } else if (item == 1) {
                            mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.HARDER);
                        } else {
                            mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.EXPERT);
                        }

                        Toast.makeText(getApplicationContext(), levels[item], Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private int getSelectedDifficultyIndex() {
        TicTacToeGame.DifficultyLevel difficultyLevel = mGame.getDifficultyLevel();
        if (difficultyLevel == TicTacToeGame.DifficultyLevel.EASY) {
            return 0;
        } else if (difficultyLevel == TicTacToeGame.DifficultyLevel.HARDER) {
            return 1;
        }

        return 2;
    }

    private void showQuitDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.quit_question)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        AndroidTicTacToeActivity.this.finish();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showAboutDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.about_dialog, null);

        new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private class ButtonClickListener implements View.OnClickListener {
        private final int mLocation;

        ButtonClickListener(int location) {
            mLocation = location;
        }

        @Override
        public void onClick(View view) {
            if (mGameOver || mGame.getBoardOccupant(mLocation) != TicTacToeGame.OPEN_SPOT) {
                return;
            }

            setMove(TicTacToeGame.HUMAN_PLAYER, mLocation);

            int winner = mGame.checkForWinner();
            if (winner == TicTacToeGame.STATUS_IN_PROGRESS) {
                mInfoTextView.setText(R.string.turn_computer);
                makeComputerMove();
                winner = mGame.checkForWinner();
            }

            updateGameState(winner);
        }
    }
}

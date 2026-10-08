package co.edu.unal.tictactoe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class AndroidTicTacToeActivity extends Activity {
    private static final String PREFS_NAME = "ttt_prefs";
    private static final String KEY_BOARD = "board";
    private static final String KEY_GAME_OVER = "game_over";
    private static final String KEY_COMPUTER_TURN = "computer_turn";
    private static final String KEY_HUMAN_STARTS_NEXT = "human_starts_next";
    private static final String KEY_INFO = "info";
    private static final String KEY_HUMAN_WINS = "human_wins";
    private static final String KEY_COMPUTER_WINS = "computer_wins";
    private static final String KEY_TIES = "ties";
    private static final String KEY_DIFFICULTY = "difficulty";

    private TicTacToeGame mGame;
    private BoardView mBoardView;
    private TextView mInfoTextView;
    private TextView mHumanScoreTextView;
    private TextView mTieScoreTextView;
    private TextView mComputerScoreTextView;
    private Button mNewGameButton;
    private MediaPlayer mHumanMediaPlayer;
    private MediaPlayer mComputerMediaPlayer;
    private SharedPreferences mPrefs;
    private final Handler mHandler = new Handler();
    private boolean mGameOver;
    private boolean mComputerTurn;
    private boolean mHumanStartsNextGame = true;
    private int mHumanWins;
    private int mComputerWins;
    private int mTies;

    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_DOWN || mGameOver || mComputerTurn) {
                return false;
            }

            int cellWidth = mBoardView.getBoardCellWidth();
            int cellHeight = mBoardView.getBoardCellHeight();
            if (cellWidth == 0 || cellHeight == 0) {
                return false;
            }

            int col = (int) event.getX() / cellWidth;
            int row = (int) event.getY() / cellHeight;
            int location = row * 3 + col;

            if (location >= 0
                    && location < TicTacToeGame.BOARD_SIZE
                    && setMove(TicTacToeGame.HUMAN_PLAYER, location)) {
                int winner = mGame.checkForWinner();
                if (winner == TicTacToeGame.STATUS_IN_PROGRESS) {
                    scheduleComputerMove();
                } else {
                    updateGameState(winner);
                }
            }

            return false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mGame = new TicTacToeGame();
        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mBoardView = findViewById(R.id.board);
        mBoardView.setGame(mGame);
        mBoardView.setOnTouchListener(mTouchListener);
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

        restorePersistentState();

        if (savedInstanceState == null) {
            startNewGame();
        } else {
            restoreInstanceState(savedInstanceState);
        }

        updateScoreBoard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHumanMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.human_move);
        mComputerMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.computer_move);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
        releaseMediaPlayer(mHumanMediaPlayer);
        releaseMediaPlayer(mComputerMediaPlayer);
        mHumanMediaPlayer = null;
        mComputerMediaPlayer = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        savePersistentState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharArray(KEY_BOARD, mGame.getBoardState());
        outState.putBoolean(KEY_GAME_OVER, mGameOver);
        outState.putBoolean(KEY_COMPUTER_TURN, mComputerTurn);
        outState.putBoolean(KEY_HUMAN_STARTS_NEXT, mHumanStartsNextGame);
        outState.putCharSequence(KEY_INFO, mInfoTextView.getText());
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
        } else if (itemId == R.id.reset_scores) {
            resetScores();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startNewGame() {
        mHandler.removeCallbacksAndMessages(null);
        mGame.clearBoard();
        mGameOver = false;
        mComputerTurn = false;
        mBoardView.invalidate();
        updateScoreBoard();

        if (mHumanStartsNextGame) {
            mInfoTextView.setText(R.string.first_human);
        } else {
            mInfoTextView.setText(R.string.first_computer);
            scheduleComputerMove();
        }

        mHumanStartsNextGame = !mHumanStartsNextGame;
    }

    private boolean setMove(char player, int location) {
        if (!mGame.setMove(player, location)) {
            return false;
        }

        playMoveSound(player);
        mBoardView.invalidate();
        return true;
    }

    private void scheduleComputerMove() {
        mHandler.removeCallbacksAndMessages(null);
        mComputerTurn = true;
        mInfoTextView.setText(R.string.turn_computer);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mGameOver) {
                    return;
                }

                makeComputerMove();
                int winner = mGame.checkForWinner();
                mComputerTurn = false;
                updateGameState(winner);
            }
        }, 1000);
    }

    private void makeComputerMove() {
        int move = mGame.getComputerMove();
        if (move != -1) {
            setMove(TicTacToeGame.COMPUTER_PLAYER, move);
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
        mComputerTurn = false;
    }

    private void updateScoreBoard() {
        mHumanScoreTextView.setText(getString(R.string.score_human, mHumanWins));
        mTieScoreTextView.setText(getString(R.string.score_ties, mTies));
        mComputerScoreTextView.setText(getString(R.string.score_android, mComputerWins));
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        mGame.setBoardState(savedInstanceState.getCharArray(KEY_BOARD));
        mGameOver = savedInstanceState.getBoolean(KEY_GAME_OVER);
        mComputerTurn = savedInstanceState.getBoolean(KEY_COMPUTER_TURN);
        mHumanStartsNextGame = savedInstanceState.getBoolean(KEY_HUMAN_STARTS_NEXT);
        mInfoTextView.setText(savedInstanceState.getCharSequence(KEY_INFO));
        mBoardView.invalidate();

        if (!mGameOver && mComputerTurn) {
            scheduleComputerMove();
        }
    }

    private void restorePersistentState() {
        mHumanWins = mPrefs.getInt(KEY_HUMAN_WINS, 0);
        mComputerWins = mPrefs.getInt(KEY_COMPUTER_WINS, 0);
        mTies = mPrefs.getInt(KEY_TIES, 0);

        int difficultyIndex = mPrefs.getInt(KEY_DIFFICULTY, TicTacToeGame.DifficultyLevel.EXPERT.ordinal());
        TicTacToeGame.DifficultyLevel[] levels = TicTacToeGame.DifficultyLevel.values();
        if (difficultyIndex < 0 || difficultyIndex >= levels.length) {
            difficultyIndex = TicTacToeGame.DifficultyLevel.EXPERT.ordinal();
        }

        mGame.setDifficultyLevel(levels[difficultyIndex]);
    }

    private void savePersistentState() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(KEY_HUMAN_WINS, mHumanWins);
        editor.putInt(KEY_COMPUTER_WINS, mComputerWins);
        editor.putInt(KEY_TIES, mTies);
        editor.putInt(KEY_DIFFICULTY, mGame.getDifficultyLevel().ordinal());
        editor.apply();
    }

    private void resetScores() {
        mHumanWins = 0;
        mComputerWins = 0;
        mTies = 0;
        updateScoreBoard();
        savePersistentState();
    }

    private void playMoveSound(char player) {
        MediaPlayer mediaPlayer = player == TicTacToeGame.HUMAN_PLAYER
                ? mHumanMediaPlayer
                : mComputerMediaPlayer;
        if (mediaPlayer == null) {
            return;
        }

        mediaPlayer.seekTo(0);
        mediaPlayer.start();
    }

    private void releaseMediaPlayer(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
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
                        savePersistentState();
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

    private void showAboutDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.about_dialog, null);

        new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}

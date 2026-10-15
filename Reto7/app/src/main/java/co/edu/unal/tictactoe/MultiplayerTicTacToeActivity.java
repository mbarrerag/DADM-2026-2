package co.edu.unal.tictactoe;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MultiplayerTicTacToeActivity extends Activity {
    private static final String WAITING = "WAITING";
    private static final String PLAYING = "PLAYING";
    private static final String OPEN = " ";

    private TextView statusText;
    private TextView playersText;
    private LinearLayout gamesContainer;
    private Button createButton;
    private Button leaveButton;
    private BoardView boardView;
    private TicTacToeGame game;
    private FirebaseAuth auth;
    private DatabaseReference gamesRef;
    private DatabaseReference currentGameRef;
    private ValueEventListener currentGameListener;
    private String currentGameId;
    private String playerName;
    private boolean myTurn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        bindViews();
        game = new TicTacToeGame();
        boardView.setGame(game);
        boardView.setOnTouchListener(boardTouchListener);
        createButton.setOnClickListener(view -> askForPlayerName(true, null));
        leaveButton.setOnClickListener(view -> leaveGame());

        try {
            auth = FirebaseAuth.getInstance();
            gamesRef = FirebaseDatabase.getInstance().getReference("games");
            signInAndLoadGames();
        } catch (IllegalStateException error) {
            statusText.setText("Falta conectar Firebase: agrega google-services.json");
            createButton.setEnabled(false);
        }
    }

    private void bindViews() {
        statusText = findViewById(R.id.status_text);
        playersText = findViewById(R.id.players_text);
        gamesContainer = findViewById(R.id.games_container);
        createButton = findViewById(R.id.create_game_button);
        leaveButton = findViewById(R.id.leave_game_button);
        boardView = findViewById(R.id.board);
    }

    private void signInAndLoadGames() {
        if (auth.getCurrentUser() != null) {
            loadAvailableGames();
            return;
        }
        auth.signInAnonymously().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                loadAvailableGames();
            } else {
                statusText.setText("No se pudo autenticar con Firebase");
                createButton.setEnabled(false);
            }
        });
    }

    private void loadAvailableGames() {
        statusText.setText("Elige una partida o crea una nueva");
        gamesRef.orderByChild("status").equalTo(WAITING)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        gamesContainer.removeAllViews();
                        if (!snapshot.exists()) {
                            TextView empty = new TextView(MultiplayerTicTacToeActivity.this);
                            empty.setText("No hay juegos disponibles todavía.");
                            empty.setPadding(0, 16, 0, 16);
                            gamesContainer.addView(empty);
                            return;
                        }
                        for (DataSnapshot gameSnapshot : snapshot.getChildren()) {
                            addGameRow(gameSnapshot);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        statusText.setText("No se pudo cargar la lista de juegos");
                    }
                });
    }

    private void addGameRow(DataSnapshot snapshot) {
        String id = snapshot.getKey();
        String hostName = snapshot.child("hostName").getValue(String.class);
        Button joinButton = new Button(this);
        joinButton.setText("Unirse a " + (hostName == null ? "jugador" : hostName));
        joinButton.setAllCaps(false);
        joinButton.setOnClickListener(view -> askForPlayerName(false, id));
        gamesContainer.addView(joinButton);
    }

    private void askForPlayerName(boolean create, String gameId) {
        EditText input = new EditText(this);
        input.setHint("Tu nombre");
        new AlertDialog.Builder(this)
                .setTitle(create ? "Crear juego" : "Unirse al juego")
                .setView(input)
                .setPositiveButton("Continuar", (dialog, which) -> {
                    playerName = input.getText().toString().trim();
                    if (playerName.isEmpty()) {
                        playerName = "Jugador";
                    }
                    if (create) {
                        createGame();
                    } else {
                        joinGame(gameId);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void createGame() {
        String id = gamesRef.push().getKey();
        if (id == null) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("hostUid", auth.getCurrentUser().getUid());
        values.put("hostName", playerName);
        values.put("guestUid", "");
        values.put("guestName", "");
        values.put("status", WAITING);
        values.put("board", emptyBoard());
        values.put("turnUid", auth.getCurrentUser().getUid());
        values.put("winner", "");
        gamesRef.child(id).setValue(values).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                openGame(id);
            } else {
                showMessage("No se pudo crear la partida");
            }
        });
    }

    private void joinGame(String id) {
        DatabaseReference ref = gamesRef.child(id);
        ref.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                String status = currentData.child("status").getValue(String.class);
                String guestUid = currentData.child("guestUid").getValue(String.class);
                if (!WAITING.equals(status) || (guestUid != null && !guestUid.isEmpty())) {
                    return Transaction.abort();
                }
                currentData.child("guestUid").setValue(auth.getCurrentUser().getUid());
                currentData.child("guestName").setValue(playerName);
                currentData.child("status").setValue(PLAYING);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (committed) {
                    openGame(id);
                } else {
                    showMessage("Otro jugador ya tomó esa partida");
                }
            }
        });
    }

    private void openGame(String id) {
        currentGameId = id;
        currentGameRef = gamesRef.child(id);
        createButton.setVisibility(View.GONE);
        gamesContainer.setVisibility(View.GONE);
        findViewById(R.id.games_title).setVisibility(View.GONE);
        playersText.setVisibility(View.VISIBLE);
        boardView.setVisibility(View.VISIBLE);
        leaveButton.setVisibility(View.VISIBLE);
        listenToCurrentGame();
    }

    private void listenToCurrentGame() {
        currentGameListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    leaveGame();
                    return;
                }
                String hostName = value(snapshot, "hostName");
                String guestName = value(snapshot, "guestName");
                String guestUid = value(snapshot, "guestUid");
                String turnUid = value(snapshot, "turnUid");
                String board = value(snapshot, "board");
                game.setBoardState(board.toCharArray());
                boardView.invalidate();
                myTurn = !guestUid.isEmpty() && auth.getCurrentUser().getUid().equals(turnUid);
                playersText.setText(hostName + "  vs  " + (guestName.isEmpty() ? "Esperando rival..." : guestName));
                if (guestUid.isEmpty()) {
                    statusText.setText("Comparte tu partida: esperando al profesor o al segundo jugador");
                } else if (value(snapshot, "winner").isEmpty()) {
                    statusText.setText(myTurn ? "Tu turno" : "Turno del otro jugador");
                } else {
                    statusText.setText("Partida terminada");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                statusText.setText("Se perdió la conexión con la partida");
            }
        };
        currentGameRef.addValueEventListener(currentGameListener);
    }

    private final View.OnTouchListener boardTouchListener = (view, event) -> {
        if (event.getAction() != MotionEvent.ACTION_DOWN || !myTurn) {
            return false;
        }
        int width = boardView.getBoardCellWidth();
        int height = boardView.getBoardCellHeight();
        int col = (int) event.getX() / width;
        int row = (int) event.getY() / height;
        int position = row * 3 + col;
        if (position >= 0 && position < TicTacToeGame.BOARD_SIZE
                && game.getBoardOccupant(position) == TicTacToeGame.OPEN_SPOT) {
            submitMove(position);
        }
        return true;
    };

    private void submitMove(int position) {
        currentGameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                String turnUid = currentData.child("turnUid").getValue(String.class);
                String board = currentData.child("board").getValue(String.class);
                if (!auth.getCurrentUser().getUid().equals(turnUid) || board == null || board.charAt(position) != ' ') {
                    return Transaction.abort();
                }
                char[] updated = board.toCharArray();
                updated[position] = isHost(currentData) ? 'X' : 'O';
                currentData.child("board").setValue(new String(updated));
                int result = resultFor(updated);
                if (result != TicTacToeGame.STATUS_IN_PROGRESS) {
                    currentData.child("winner").setValue(result == TicTacToeGame.STATUS_TIE ? "TIE" : String.valueOf(updated[position]));
                } else {
                    String next = value(currentData, "hostUid").equals(turnUid)
                            ? value(currentData, "guestUid") : value(currentData, "hostUid");
                    currentData.child("turnUid").setValue(next);
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (!committed && error == null) {
                    showMessage("Ese movimiento ya no está disponible");
                }
            }
        });
    }

    private boolean isHost(MutableData data) {
        return auth.getCurrentUser().getUid().equals(value(data, "hostUid"));
    }

    private int resultFor(char[] board) {
        int[][] lines = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
        for (int[] line : lines) {
            if (board[line[0]] != ' ' && board[line[0]] == board[line[1]] && board[line[1]] == board[line[2]]) {
                return board[line[0]] == 'X' ? TicTacToeGame.STATUS_HUMAN_WON : TicTacToeGame.STATUS_COMPUTER_WON;
            }
        }
        for (char cell : board) {
            if (cell == ' ') return TicTacToeGame.STATUS_IN_PROGRESS;
        }
        return TicTacToeGame.STATUS_TIE;
    }

    private String emptyBoard() {
        return "         ";
    }

    private String value(DataSnapshot snapshot, String key) {
        String value = snapshot.child(key).getValue(String.class);
        return value == null ? "" : value;
    }

    private String value(MutableData data, String key) {
        String value = data.child(key).getValue(String.class);
        return value == null ? "" : value;
    }

    private void leaveGame() {
        if (currentGameRef != null) {
            currentGameRef.removeEventListener(currentGameListener);
        }
        currentGameRef = null;
        currentGameId = null;
        myTurn = false;
        boardView.setVisibility(View.GONE);
        playersText.setVisibility(View.GONE);
        leaveButton.setVisibility(View.GONE);
        createButton.setVisibility(View.VISIBLE);
        gamesContainer.setVisibility(View.VISIBLE);
        findViewById(R.id.games_title).setVisibility(View.VISIBLE);
        statusText.setText("Elige una partida o crea una nueva");
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

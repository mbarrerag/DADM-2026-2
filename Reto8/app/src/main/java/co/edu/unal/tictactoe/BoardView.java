package co.edu.unal.tictactoe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class BoardView extends View {
    public static final int GRID_WIDTH = 8;

    private static final int PIECE_SIZE = 256;

    private Bitmap mHumanBitmap;
    private Bitmap mComputerBitmap;
    private Paint mPaint;
    private TicTacToeGame mGame;

    public BoardView(Context context) {
        super(context);
        initialize();
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public BoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public void setGame(TicTacToeGame game) {
        mGame = game;
        invalidate();
    }

    public int getBoardCellWidth() {
        return getWidth() / 3;
    }

    public int getBoardCellHeight() {
        return getHeight() / 3;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int boardWidth = getWidth();
        int boardHeight = getHeight();
        int cellWidth = boardWidth / 3;
        int cellHeight = boardHeight / 3;

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.rgb(148, 163, 184));
        mPaint.setStrokeWidth(GRID_WIDTH);
        canvas.drawLine(cellWidth, 0, cellWidth, boardHeight, mPaint);
        canvas.drawLine(cellWidth * 2, 0, cellWidth * 2, boardHeight, mPaint);
        canvas.drawLine(0, cellHeight, boardWidth, cellHeight, mPaint);
        canvas.drawLine(0, cellHeight * 2, boardWidth, cellHeight * 2, mPaint);

        if (mGame == null) {
            return;
        }

        int padding = GRID_WIDTH * 3;
        for (int i = 0; i < TicTacToeGame.BOARD_SIZE; i++) {
            int col = i % 3;
            int row = i / 3;
            int left = col * cellWidth + padding;
            int top = row * cellHeight + padding;
            int right = (col + 1) * cellWidth - padding;
            int bottom = (row + 1) * cellHeight - padding;
            Rect destination = new Rect(left, top, right, bottom);

            if (mGame.getBoardOccupant(i) == TicTacToeGame.HUMAN_PLAYER) {
                canvas.drawBitmap(mHumanBitmap, null, destination, null);
            } else if (mGame.getBoardOccupant(i) == TicTacToeGame.COMPUTER_PLAYER) {
                canvas.drawBitmap(mComputerBitmap, null, destination, null);
            }
        }
    }

    private void initialize() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHumanBitmap = createHumanBitmap();
        mComputerBitmap = createComputerBitmap();
        setWillNotDraw(false);
    }

    private Bitmap createHumanBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(PIECE_SIZE, PIECE_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(22, 163, 74));
        paint.setStrokeWidth(28f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        int inset = 48;
        canvas.drawLine(inset, inset, PIECE_SIZE - inset, PIECE_SIZE - inset, paint);
        canvas.drawLine(PIECE_SIZE - inset, inset, inset, PIECE_SIZE - inset, paint);
        return bitmap;
    }

    private Bitmap createComputerBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(PIECE_SIZE, PIECE_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(220, 38, 38));
        paint.setStrokeWidth(28f);
        paint.setStyle(Paint.Style.STROKE);
        int inset = 48;
        canvas.drawOval(inset, inset, PIECE_SIZE - inset, PIECE_SIZE - inset, paint);
        return bitmap;
    }
}

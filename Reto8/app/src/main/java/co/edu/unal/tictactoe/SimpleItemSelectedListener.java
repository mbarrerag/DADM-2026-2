package co.edu.unal.tictactoe;

import android.view.View;
import android.widget.AdapterView;

public class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {
    private final Runnable callback;

    public SimpleItemSelectedListener(Runnable callback) {
        this.callback = callback;
    }

    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { callback.run(); }
    @Override public void onNothingSelected(AdapterView<?> parent) { callback.run(); }
}

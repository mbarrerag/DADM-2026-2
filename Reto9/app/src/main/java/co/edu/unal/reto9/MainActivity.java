package co.edu.unal.reto9;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int LOCATION_REQUEST = 9;
    private static final String RADIUS_KEY = "search_radius_km";
    private static final int[] RADII = {1, 3, 5, 10};
    private static final String[] RADIUS_LABELS = {"1 km", "3 km", "5 km", "10 km"};

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Marker> markers = new ArrayList<>();
    private final List<Place> places = new ArrayList<>();
    private final PlacesRepository repository = new PlacesRepository();
    private MapView map;
    private TextView status;
    private TextView count;
    private TextView empty;
    private LinearLayout list;
    private ProgressBar loading;
    private Button radiusButton;
    private Button locateButton;
    private TextView[] filters;
    private String category = "Todos";
    private Location currentLocation;
    private int radiusKm;
    private int requestNumber;
    private boolean waitingForLocation;
    private boolean sampleMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().load(getApplicationContext(), preferences);
        Configuration.getInstance().setUserAgentValue(
                "CercaDeMiReto9/1.0 (+https://github.com/mbarrerag/DADM-2026-2)");
        setContentView(R.layout.activity_main);
        radiusKm = preferences.getInt(RADIUS_KEY, 5);
        bindViews();
        configureMap();
        configureActions();
        updateRadiusButton();
        selectCategory(0);
        if (hasLocationPermission()) locate();
    }

    private void bindViews() {
        map = findViewById(R.id.map);
        status = findViewById(R.id.location_status);
        count = findViewById(R.id.result_count);
        empty = findViewById(R.id.empty_state);
        list = findViewById(R.id.places_list);
        loading = findViewById(R.id.loading);
        radiusButton = findViewById(R.id.radius_button);
        locateButton = findViewById(R.id.locate_button);
        filters = new TextView[]{findViewById(R.id.filter_all), findViewById(R.id.filter_health),
                findViewById(R.id.filter_tourism), findViewById(R.id.filter_parks)};
    }

    private void configureMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(12.5);
        map.getController().setCenter(new GeoPoint(4.636, -74.083));
    }

    private void configureActions() {
        locateButton.setOnClickListener(view -> locate());
        radiusButton.setOnClickListener(view -> showRadiusDialog());
        for (int i = 0; i < filters.length; i++) {
            final int index = i;
            filters[i].setOnClickListener(view -> selectCategory(index));
        }
    }

    private void showRadiusDialog() {
        int selected = 2;
        for (int i = 0; i < RADII.length; i++) if (RADII[i] == radiusKm) selected = i;
        new AlertDialog.Builder(this)
                .setTitle("Radio de búsqueda")
                .setSingleChoiceItems(RADIUS_LABELS, selected, (dialog, which) -> {
                    radiusKm = RADII[which];
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(RADIUS_KEY, radiusKm).commit();
                    updateRadiusButton();
                    dialog.dismiss();
                    if (currentLocation != null) searchPlaces();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateRadiusButton() {
        radiusButton.setText(radiusKm + " km");
    }

    private void selectCategory(int index) {
        category = new String[]{"Todos", "Salud", "Turismo", "Parques"}[index];
        for (int i = 0; i < filters.length; i++) {
            filters[i].setSelected(i == index);
            filters[i].setTextColor(i == index ? 0xffffffff : 0xff175b60);
        }
        showPlaces();
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void locate() {
        if (!hasLocationPermission()) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            showMessage("Ubicación no disponible", "No se pudo acceder al GPS.");
            return;
        }
        String provider = null;
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        }
        if (provider == null) {
            showMessage("Ubicación desactivada", "Activa la ubicación del dispositivo.");
            return;
        }
        status.setText("Buscando posición…");
        empty.setText("Buscando posición…");
        loading.setVisibility(View.VISIBLE);
        waitingForLocation = true;
        int request = ++requestNumber;
        Location cached = manager.getLastKnownLocation(provider);
        if (cached != null && System.currentTimeMillis() - cached.getTime() < 5 * 60 * 1000) {
            acceptLocation(cached, request);
            return;
        }
        if (Build.VERSION.SDK_INT >= 30) {
            manager.getCurrentLocation(provider, null, getMainExecutor(), location -> acceptLocation(location, request));
        } else {
            manager.requestSingleUpdate(provider, new LocationListener() {
                @Override public void onLocationChanged(Location location) {
                    acceptLocation(location, request);
                }
            }, Looper.getMainLooper());
        }
        handler.postDelayed(() -> {
            if (waitingForLocation && request == requestNumber) {
                waitingForLocation = false;
                showMessage("Sin señal GPS", "No llegó una posición. Intenta de nuevo.");
            }
        }, 20000);
    }

    private void acceptLocation(Location location, int request) {
        if (!waitingForLocation || request != requestNumber) return;
        waitingForLocation = false;
        if (location == null) {
            showMessage("Sin señal GPS", "No llegó una posición. Intenta de nuevo.");
            return;
        }
        currentLocation = location;
        map.getController().setCenter(new GeoPoint(location.getLatitude(), location.getLongitude()));
        map.getController().setZoom(radiusKm <= 1 ? 14.5 : radiusKm <= 3 ? 13.5 : radiusKm <= 5 ? 12.5 : 11.5);
        searchPlaces();
    }

    private void searchPlaces() {
        if (currentLocation == null) return;
        int request = ++requestNumber;
        int searchRadiusKm = radiusKm;
        Location origin = new Location(currentLocation);
        status.setText(String.format(Locale.US, "%.4f, %.4f  ·  %d km",
                origin.getLatitude(), origin.getLongitude(), radiusKm));
        empty.setText("Buscando lugares cercanos…");
        loading.setVisibility(View.VISIBLE);
        locateButton.setEnabled(false);
        executor.execute(() -> {
            try {
                PlacesRepository.Result found = repository.nearby(origin, searchRadiusKm);
                runOnUiThread(() -> {
                    if (request != requestNumber || isFinishing()) return;
                    places.clear();
                    places.addAll(found.places);
                    sampleMode = found.sample;
                    if (sampleMode) status.setText("Vista de ejemplo · sin datos en vivo");
                    loading.setVisibility(View.GONE);
                    locateButton.setEnabled(true);
                    empty.setText("No se encontraron lugares en este radio.");
                    showPlaces();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (request != requestNumber || isFinishing()) return;
                    loading.setVisibility(View.GONE);
                    locateButton.setEnabled(true);
                    showMessage("Consulta no disponible", "No se pudieron cargar lugares. Intenta de nuevo.");
                });
            }
        });
    }

    private void showMessage(String heading, String detail) {
        status.setText(heading);
        empty.setText(detail);
        empty.setVisibility(View.VISIBLE);
        loading.setVisibility(View.GONE);
        locateButton.setEnabled(true);
    }

    private void showPlaces() {
        if (list == null || map == null) return;
        list.removeAllViews();
        map.getOverlays().removeAll(markers);
        markers.clear();
        if (currentLocation != null) {
            Marker me = marker(new GeoPoint(currentLocation), "Tu ubicación", "Centro de búsqueda", R.drawable.marker_user);
            markers.add(me);
            map.getOverlays().add(me);
        }
        int visible = 0;
        for (Place place : places) {
            if (!"Todos".equals(category) && !category.equals(place.category)) continue;
            visible++;
            int icon = "Salud".equals(place.category) ? R.drawable.marker_health
                    : "Turismo".equals(place.category) ? R.drawable.marker_tourism : R.drawable.marker_park;
            Marker marker = marker(new GeoPoint(place.latitude, place.longitude), place.name,
                    place.kind + " · " + distance(place.distanceMeters), icon);
            markers.add(marker);
            map.getOverlays().add(marker);
            if (visible <= 40) list.addView(placeRow(place, marker));
        }
        count.setText(visible + (visible == 1 ? " lugar" : " lugares"));
        if (sampleMode) count.setText(visible + (visible == 1 ? " ejemplo" : " ejemplos"));
        empty.setVisibility(visible == 0 ? View.VISIBLE : View.GONE);
        map.invalidate();
    }

    private Marker marker(GeoPoint point, String title, String subtitle, int icon) {
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setSnippet(subtitle);
        marker.setIcon(getDrawable(icon));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        return marker;
    }

    private View placeRow(Place place, Marker marker) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(2, dp(10), 2, dp(10));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView name = new TextView(this);
        name.setText(place.name);
        name.setTextColor(0xff102a2e);
        name.setTextSize(15);
        name.setTypeface(null, Typeface.BOLD);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(name);
        TextView details = new TextView(this);
        details.setText(place.kind + " · " + distance(place.distanceMeters));
        details.setTextColor(0xff597277);
        details.setTextSize(12);
        labels.addView(details);
        row.addView(labels);
        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(0xff087e83);
        arrow.setTextSize(26);
        arrow.setGravity(Gravity.CENTER);
        arrow.setLayoutParams(new LinearLayout.LayoutParams(dp(34), dp(42)));
        row.addView(arrow);
        row.setOnClickListener(view -> {
            map.getController().animateTo(marker.getPosition());
            marker.showInfoWindow();
        });
        return row;
    }

    private String distance(int meters) {
        return meters < 1000 ? meters + " m" : String.format(Locale.getDefault(), "%.1f km", meters / 1000.0);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == LOCATION_REQUEST) {
            if (hasLocationPermission()) locate();
            else showMessage("Permiso pendiente", "Se necesita ubicación para encontrar lugares cercanos.");
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override protected void onPause() {
        if (map != null) map.onPause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        requestNumber++;
        executor.shutdownNow();
        super.onDestroy();
    }
}

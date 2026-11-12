package co.edu.unal.reto9;

import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class PlacesRepository {
    private static final String[] ENDPOINTS = {
            "https://overpass.private.coffee/api/interpreter",
            "https://overpass-api.de/api/interpreter"
    };
    private static final String USER_AGENT = "CercaDeMiReto9/1.0 (+https://github.com/mbarrerag/DADM-2026-2)";

    static final class Result {
        final List<Place> places;
        final boolean sample;

        Result(List<Place> places, boolean sample) {
            this.places = places;
            this.sample = sample;
        }
    }

    Result nearby(Location origin, int radiusKm) throws Exception {
        int radiusMeters = radiusKm * 1000;
        String around = "(around:" + radiusMeters + "," + origin.getLatitude() + "," + origin.getLongitude() + ")";
        String query = "[out:json][timeout:20];("
                + "nwr[\"amenity\"~\"^(hospital|clinic)$\"]" + around + ";"
                + "nwr[\"tourism\"~\"^(attraction|museum|viewpoint)$\"]" + around + ";"
                + "nwr[\"leisure\"=\"park\"]" + around + ";"
                + ");out center 400;";

        Exception failure = null;
        for (String endpoint : ENDPOINTS) {
            try {
                return new Result(fetch(endpoint, query, origin, radiusMeters), false);
            } catch (Exception error) {
                Log.w("Reto9Places", "Overpass unavailable: " + endpoint, error);
                failure = error;
            }
        }
        List<Place> sample = bogotaSample(origin, radiusMeters);
        if (sample != null) return new Result(sample, true);
        throw failure == null ? new IllegalStateException("No Overpass endpoint") : failure;
    }

    private List<Place> fetch(String endpoint, String query, Location origin, int radiusMeters) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        connection.setDoOutput(true);
        byte[] body = ("data=" + URLEncoder.encode(query, "UTF-8")).getBytes(StandardCharsets.UTF_8);
        try {
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + connection.getResponseCode());
            }
            StringBuilder json = new StringBuilder();
            try (InputStream stream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
            }
            return parse(new JSONObject(json.toString()).getJSONArray("elements"), origin, radiusMeters);
        } finally {
            connection.disconnect();
        }
    }

    private List<Place> bogotaSample(Location origin, int radiusMeters) {
        float[] fromBogota = new float[1];
        Location.distanceBetween(origin.getLatitude(), origin.getLongitude(), 4.636, -74.083, fromBogota);
        if (fromBogota[0] > 10000) return null;
        List<Place> sample = new ArrayList<>();
        addSample(sample, origin, radiusMeters, "Hospital Universitario Nacional", "Hospital", "Salud", 4.6320, -74.0929);
        addSample(sample, origin, radiusMeters, "Museo Nacional de Colombia", "Museo", "Turismo", 4.6154, -74.0682);
        addSample(sample, origin, radiusMeters, "Parque Simón Bolívar", "Parque", "Parques", 4.6578, -74.0931);
        addSample(sample, origin, radiusMeters, "Jardín Botánico de Bogotá", "Atracción", "Turismo", 4.6675, -74.1013);
        sample.sort(Comparator.comparingInt(place -> place.distanceMeters));
        return sample;
    }

    private void addSample(List<Place> sample, Location origin, int radiusMeters, String name,
                           String kind, String category, double latitude, double longitude) {
        float[] distance = new float[1];
        Location.distanceBetween(origin.getLatitude(), origin.getLongitude(), latitude, longitude, distance);
        if (distance[0] <= radiusMeters) {
            sample.add(new Place(name, kind, category, latitude, longitude, Math.round(distance[0])));
        }
    }

    private List<Place> parse(JSONArray elements, Location origin, int radiusMeters) {
        List<Place> places = new ArrayList<>();
        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.optJSONObject(i);
            if (element == null) continue;
            JSONObject tags = element.optJSONObject("tags");
            if (tags == null) continue;
            String name = tags.optString("name", tags.optString("name:es", "")).trim();
            if (name.isEmpty()) continue;
            JSONObject center = element.optJSONObject("center");
            double latitude = center == null ? element.optDouble("lat", Double.NaN) : center.optDouble("lat", Double.NaN);
            double longitude = center == null ? element.optDouble("lon", Double.NaN) : center.optDouble("lon", Double.NaN);
            if (Double.isNaN(latitude) || Double.isNaN(longitude)) continue;
            float[] distance = new float[1];
            Location.distanceBetween(origin.getLatitude(), origin.getLongitude(), latitude, longitude, distance);
            if (distance[0] > radiusMeters) continue;

            String category;
            String kind;
            if (tags.has("amenity")) {
                category = "Salud";
                kind = "hospital".equals(tags.optString("amenity")) ? "Hospital" : "Clínica";
            } else if (tags.has("tourism")) {
                category = "Turismo";
                String tourism = tags.optString("tourism");
                kind = "museum".equals(tourism) ? "Museo" : "viewpoint".equals(tourism) ? "Mirador" : "Atracción";
            } else {
                category = "Parques";
                kind = "Parque";
            }
            places.add(new Place(name, kind, category, latitude, longitude, Math.round(distance[0])));
        }
        places.sort(Comparator.comparingInt(place -> place.distanceMeters));
        return places;
    }
}

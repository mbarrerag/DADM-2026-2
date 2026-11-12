package co.edu.unal.reto9;

final class Place {
    final String name;
    final String kind;
    final String category;
    final double latitude;
    final double longitude;
    final int distanceMeters;

    Place(String name, String kind, String category, double latitude, double longitude, int distanceMeters) {
        this.name = name;
        this.kind = kind;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
    }
}

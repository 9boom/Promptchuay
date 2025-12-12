package android.promptchuay.model;

import java.io.Serializable;

public class Location implements Serializable {
    public double lat = 0.0;
    public double lng = 0.0;

    public Location() {
    }

    public Location(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }
}
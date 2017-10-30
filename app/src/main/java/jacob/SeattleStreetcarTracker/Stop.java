package jacob.SeattleStreetcarTracker;

import com.google.android.gms.maps.model.Marker;

/**
 * Created by jacob on 10/24/17.
 */

public class Stop {
    public int stopId;
    public float lat;
    public float lon;
    public String title;
    public Marker marker;

    public Stop(int id, String stopTitle) {
        stopId = id;
        title = stopTitle;
    }
}

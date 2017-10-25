package jacob.SeattleStreetcarTracker;

import com.google.android.gms.maps.model.Marker;

/**
 * Created by jacob on 10/24/17.
 */

public class Streetcar {
    public int streetcar_id;
    public float x;
    public float y;
    public int route_id;
    public int heading;
    public int speedkmhr;
    public boolean predictable;
    public String created_at;
    public Marker marker;
}

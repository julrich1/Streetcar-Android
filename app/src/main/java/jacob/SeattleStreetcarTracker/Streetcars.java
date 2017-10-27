package jacob.SeattleStreetcarTracker;

import java.util.ArrayList;
import android.util.Log;

import com.google.android.gms.maps.model.Marker;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by jacob on 10/24/17.
 */

public class Streetcars {
    private int STREETCAR_MARKER_LIFE = 5;

    public ArrayList<Streetcar> streetcars = new ArrayList<>();

    public void update(Streetcar streetcar) {
        int currentIndex = findByStreetcarId(streetcar.streetcar_id);

        if (currentIndex == -1) {
            streetcars.add(streetcar);
            Log.v("Add", "Added to array");
        }
        else {
            Marker marker = streetcars.get(currentIndex).marker;
            streetcars.remove(currentIndex);

            streetcar.marker = marker;
            streetcars.add(currentIndex, streetcar);
        }
    }

    public int findByStreetcarId(int id) {
        for (int i = 0; i < streetcars.size(); i++) {
            if (streetcars.get(i).streetcar_id == id) {
                return i;
            }
        }

        return -1;
    }

    public void delete(int index) {
        streetcars.remove(index);
    }

    public Streetcar get(int i) {
        return streetcars.get(i);
    }

    public int length() {
        return streetcars.size();
    }

    public void checkForOldStreetcars() {
        DateTime currentTime = new DateTime(DateTimeZone.forID("America/Los_Angeles"));

        for (int i = 0; i < streetcars.size(); i++) {
            DateTime lastUpdated = new DateTime(streetcars.get(i).updated_at);

            if (lastUpdated.isBefore(currentTime.minusMinutes(STREETCAR_MARKER_LIFE))) {
                Log.v("Outdated streetcar!", "Deleting this thing - " + i);
                streetcars.get(i).marker.remove();
                delete(i);
            }
        }
    }

}

package jacob.SeattleStreetcarTracker;

import java.util.ArrayList;
import android.util.Log;

import com.google.android.gms.maps.model.Marker;

/**
 * Created by jacob on 10/24/17.
 */

public class Streetcars {
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
}

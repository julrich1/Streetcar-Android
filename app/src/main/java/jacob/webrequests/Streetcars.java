package jacob.webrequests;

import java.util.ArrayList;
import android.util.Log;

/**
 * Created by jacob on 10/24/17.
 */

public class Streetcars {
    public ArrayList<Streetcar> streetcars = new ArrayList<>();

    public void update(Streetcar streetcar) {
        streetcars.add(streetcar);
        Log.v("Add", "Added to array");
    }

    public Streetcar get(int i) {
        return streetcars.get(i);
    }

    public int length() {
        return streetcars.size();
    }
}

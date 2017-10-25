package jacob.SeattleStreetcarTracker;

import org.json.JSONArray;

/**
 * Created by jacob on 10/24/17.
 */

public interface FetchStreetcars {
    void onTaskCompleted(JSONArray response);
}

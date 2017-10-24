package jacob.webrequests;

import org.json.JSONArray;

/**
 * Created by jacob on 10/24/17.
 */

public interface FetchedStopsAndRoutes {
    void onTaskCompleted(JSONArray stops, JSONArray path);
}

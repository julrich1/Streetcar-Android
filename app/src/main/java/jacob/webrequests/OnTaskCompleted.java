package jacob.webrequests;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by jacob on 10/24/17.
 */

public interface OnTaskCompleted {
    void onTaskCompleted(JSONArray response);
}

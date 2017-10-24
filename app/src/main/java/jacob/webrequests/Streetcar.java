package jacob.webrequests;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;

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
}

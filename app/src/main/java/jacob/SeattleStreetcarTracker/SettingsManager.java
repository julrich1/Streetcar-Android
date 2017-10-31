package jacob.SeattleStreetcarTracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

/**
 * Created by jacob on 10/30/17.
 */

public class SettingsManager {

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("streetcar_settings", Context.MODE_PRIVATE);
    }

    public static void saveFavoriteStops(Context context, FavoriteStops fvStops) {
        Gson gson = new Gson();
        String favorites_JSON = gson.toJson(fvStops);

        SharedPreferences.Editor editor = getPrefs(context).edit();

        editor.putString("stops", favorites_JSON);
        Log.v("Settings:", "Saving them now with this string: " + favorites_JSON);
        editor.commit();
    }

    public static FavoriteStops getFavoriteStops(Context context) {
        Gson gson = new Gson();

        String favorites_JSON = getPrefs(context).getString("stops", "");

        return gson.fromJson(favorites_JSON, FavoriteStops.class);
    }

    public static void saveRoute (Context context, int route) {
        SharedPreferences.Editor editor = getPrefs(context).edit();

        editor.putInt("route", route);
        editor.commit();
    }

    public static int loadRoute (Context context) {
        int loadedRoute = getPrefs(context).getInt("route", 1);

        return loadedRoute;
    }
}

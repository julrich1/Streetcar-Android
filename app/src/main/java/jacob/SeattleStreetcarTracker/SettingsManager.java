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
        return context.getSharedPreferences("favorites", Context.MODE_PRIVATE);
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

//        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
//        boolean silent = settings.getBoolean("silentMode", false);
//        setSilent(silent);


//        editor.putString("stops", favorites_JSON);
//        Log.v("Settings:", "Saving them now with this string: " + favorites_JSON);
//        editor.commit();
    }



//    public void saveFavoritesSettings(FavoriteStops fvStops) {
//        String favorites_JSON = gson.toJson(fvStops);
//
//        SharedPreferences settings = getSharedPreferences("favorites", 0);
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putString("stops", favorites_JSON);
//
//        // Commit the edits!
//        editor.commit();
//
//
//        private static SharedPreferences.Editor editor = settings.edit;
//
//
//        editor.putString("favorites", user_json);
//        editor.commit();
//
//    }
}

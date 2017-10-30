package jacob.SeattleStreetcarTracker;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;

/**
 * Created by jacob on 10/30/17.
 */

public class FavoriteStops {
    public ArrayList<FavoriteStop> FHS = new ArrayList<>();
    public ArrayList<FavoriteStop> SLU = new ArrayList<>();

    public void addFavorite(int stopId, String stopTitle, int route) {
        if (route == 1) {
            FHS.add(new FavoriteStop(stopId, stopTitle));
        }
        else if (route == 2) {
            SLU.add(new FavoriteStop(stopId, stopTitle));
        }
    }

    public void removeFavorite(int stopId, int route) {
        if (route == 1) {
            for (int i = 0; i < FHS.size(); i++) {
                if (FHS.get(i).stopId == stopId) {
                    FHS.remove(i);
                    return;
                }
            }
        }
        else if (route == 2) {
            for (int i = 0; i < SLU.size(); i++) {
                if (SLU.get(i).stopId == stopId) {
                    SLU.remove(i);
                    return;
                }
            }
        }
    }

    public boolean isFavorited(int stopId, int route) {
        if (route == 1) {
            for (int i = 0; i < FHS.size(); i++) {
                if (FHS.get(i).stopId == stopId) { return true; }
            }
        }
        else if (route == 2) {
            for (int i = 0; i < SLU.size(); i++) {
                if (SLU.get(i).stopId == stopId) { return true; }
            }
        }

        return false;
    }

    public FavoriteStop searchByStopId(int stopId, int route) {
        if (route == 1) {
            for (int i = 0; i < FHS.size(); i++) {
                if (FHS.get(i).stopId == stopId) { return FHS.get(i); }
            }
        }
        else if (route == 2) {
            for (int i = 0; i < SLU.size(); i++) {
                if (SLU.get(i).stopId == stopId) { return SLU.get(i); }
            }
        }

        return null;
    }

    public String getQueryString(int route) {
        String textRoute;
        String queryString = "";

        if (route == 1) {
            textRoute = "FHS";
            for (int i = 0; i < FHS.size(); i++) {
                queryString += "&stops=" + textRoute + "|" + FHS.get(i).stopId;
            }

        }
        else if (route == 2) {
            textRoute = "SLU";
            for (int i = 0; i < SLU.size(); i++) {
                queryString += "&stops=" + textRoute + "|" + SLU.get(i).stopId;
            }
        }

        return queryString;
    }
}

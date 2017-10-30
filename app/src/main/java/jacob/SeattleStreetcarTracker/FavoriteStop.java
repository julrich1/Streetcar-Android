package jacob.SeattleStreetcarTracker;

/**
 * Created by jacob on 10/30/17.
 */

public class FavoriteStop {
    public int stopId;
    public String stopTitle;
    public String arrivalTimes = "";

    public FavoriteStop(int sId, String title) {
        stopId = sId;
        stopTitle = title;
    }
}

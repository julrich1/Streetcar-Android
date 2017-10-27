package jacob.SeattleStreetcarTracker;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ActionBarContainer;
import android.util.Log;

/**
 * Created by jacob on 10/27/17.
 */

public class ActionBarActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("ActionBarActivity", "Has been instantiated");
    }
}

package jacob.SeattleStreetcarTracker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

/**
 * Created by jacob on 10/31/17.
 */

public class ImageHandler {

    static public Bitmap resizeMapIcons(Resources resources, String packageName, String iconName, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(resources, resources.getIdentifier(iconName, "drawable", packageName));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }

    static public Bitmap scaleMapIcons(Resources resources, String packageName, String iconName, float scaleW, float scaleH) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(resources, resources.getIdentifier(iconName, "drawable", packageName));

        int width = Math.round(imageBitmap.getWidth() * scaleW);
        int height= Math.round(imageBitmap.getHeight() * scaleH);

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }

    static public Bitmap createBitmap(Resources resources, String packageName, String iconName) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(resources, resources.getIdentifier(iconName, "drawable", packageName));
        return imageBitmap;
    }

    static public BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth() * 2, vectorDrawable.getIntrinsicHeight() * 2);
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth() * 2, vectorDrawable.getIntrinsicHeight() * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

}

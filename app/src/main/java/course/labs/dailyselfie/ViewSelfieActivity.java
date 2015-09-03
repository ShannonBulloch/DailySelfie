package course.labs.dailyselfie;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.widget.ImageView;

import com.ortiz.touch.TouchImageView;

import java.io.IOException;


public class ViewSelfieActivity extends Activity {
    public static String TAG = "ViewSelfie";
    private String mFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the Intent used to start this Activity
        Intent intent = getIntent();

        // Get the ID of the image to display and set it as the image for this ImageView
        mFilePath = intent.getStringExtra(MainActivity.EXTRA_FILE_PATH);

        setContentView(R.layout.activity_view_selfie);
        TouchImageView imageView = (TouchImageView) findViewById(R.id.imageView);
        //imageView.setPadding(0,0,0,0);

        Bitmap bitmap = getPic(mFilePath);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            imageView.setBackgroundColor(Color.BLACK);
        } else {
            imageView.setImageResource(R.drawable.sadpanda);
            imageView.setBackgroundColor(Color.GRAY);
        }

    }

    private Bitmap getPic(String path) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap != null) {
            int rotate = getRotationToApply(path);
            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotate);

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, true);
            }
        }
        return bitmap;
    }

    private int getRotationToApply(String currentPath) {
        int degree = 0;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(currentPath);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                    case ExifInterface.ORIENTATION_UNDEFINED:
                        // on my emulator, the orientation is undefined, but is actually rotated 90
                        degree = 0;
                        break;
                }

            }
        }
        return degree;
    }


}

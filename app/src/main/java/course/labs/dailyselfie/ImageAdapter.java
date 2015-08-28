package course.labs.dailyselfie;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Code adapted from example found at http://developer.android.com/guide/topics/ui/layout/gridview.html
 */
public class ImageAdapter extends BaseAdapter {
    private static final int PADDING = 8;
    public static final int WIDTH = 275;
    public static final int HEIGHT = 275;
    private Context mContext;
    private List<String> mThumbPaths = new ArrayList<String>();

    private Map<String, Bitmap> mThumbBmps = new HashMap<String, Bitmap>();

    public ImageAdapter(Context c) {
        mContext = c;
    }

    public ImageAdapter(Context c, List<String> paths) {
        mContext = c;
        mThumbPaths = paths;
    }


    public List<String> getThumbPaths() {
        return mThumbPaths;
    }

    public void setThumbPaths(List<String> mThumbPaths) {
        this.mThumbPaths = mThumbPaths;
    }

    public int getCount() {
        return mThumbPaths.size();
    }

    // Return the data item at position
    public Object getItem(int position) {
        return mThumbPaths.get(position);
    }

    // Will get called to provide the ID that
    // is passed to OnItemClickListener.onItemClick()
    public long getItemId(int position) {
        return position;
    }

    /**
     * Add a path for an image file to the list for this adapter
     *
     * @param path
     */
    public void addItem(String path) {
        mThumbPaths.add(path);
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(WIDTH, HEIGHT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            //imageView.setPadding(PADDING, PADDING, PADDING, PADDING);
        } else {
            imageView = (ImageView) convertView;
        }

        if (!mThumbBmps.containsKey(mThumbPaths.get(position))) {
            Bitmap bitmap = getPic(mThumbPaths.get(position));
            if (bitmap != null) {
                mThumbBmps.put(mThumbPaths.get(position), bitmap);
                imageView.setImageBitmap(bitmap);
            } else {
                // image not found or could not be decoded.
                // Assume the image was deleted or file was renamed outside of the DailySelfie app
                imageView.setImageResource(R.drawable.sadpanda);
            }
        } else {
            imageView.setImageBitmap(mThumbBmps.get(mThumbPaths.get(position)));
        }
        return imageView;
    }

    // The follow method is adapted from the tutorial at http://developer.android.com/training/camera/photobasics.html
    private Bitmap getPic(String currentPath) {
        if (currentPath == null) {
            return null;
        }
        // Get the dimensions of the View
        int targetW = WIDTH / 2;
        int targetH = HEIGHT / 2;

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPath, bmOptions);
        if (bitmap != null) {
            // determine if bitmap should be rotated
            int rotate = getRotationToApply(currentPath);
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
                        degree = 90;
                        break;
                }

            }
        }
        return degree;
    }
}
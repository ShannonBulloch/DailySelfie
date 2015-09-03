package course.labs.dailyselfie;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "DailySelfie";
    private static final String FILE_NAME = "Selfies_Settings.txt";
    public static final String EXTRA_FILE_PATH = "PATH";
    public static final String EXTRA_NOTIFICATION_SETTINGS = "NOTIFICATION_SETTINGS";
    public static final String PHOTO_DIR = "Selfies";
    public static final String STATE_PHOTO_PATH = "lastPhotoPath";

    public static final int REQUEST_SETTINGS = 2;

    private AlarmManager mAlarmManager;
    private Intent mNotificationReceiverIntent;
    private PendingIntent mNotificationReceiverPendingIntent;

    // 2 minute delay and interval
    private static final long INITIAL_ALARM_DELAY = 5 * 1000L;
    private static final long ALARM_INTERVAL = 5 * 1000L;

    GridView mGridView;
    private ImageAdapter mAdapter;

    private Map<String, Object> mSavedSettings;

    private String mPathToDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mCurrentPhotoPath = savedInstanceState.getString(STATE_PHOTO_PATH);
        }

        setTitle(getResources().getString(R.string.title_activity_main));

        mGridView = (GridView) findViewById(R.id.gridView);
        mAdapter = new ImageAdapter(this);
        mGridView.setAdapter(mAdapter);

        // Set an setOnItemClickListener on the GridView
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                //Create an Intent to start the ImageViewActivity
                Intent intent = new Intent(MainActivity.this,
                        ViewSelfieActivity.class);

                // Add the ID of the thumbnail to display as an Intent Extra
                intent.putExtra(EXTRA_FILE_PATH, (String) mAdapter.getItem(position));

                // Start the ImageViewActivity
                startActivity(intent);
            }
        });

        populateImageAdapter();

        try {
            readSavedSettings();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        // Get the AlarmManager Service
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        setUpNotifications();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the current photo path
        savedInstanceState.putString(STATE_PHOTO_PATH, mCurrentPhotoPath);

        // Call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_camera) {
            // call method to launch intent
            dispatchTakePictureIntent();

        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this,
                    SettingsActivity.class);

            // Add saved user settings as an Intent Extra
            intent.putExtra(EXTRA_NOTIFICATION_SETTINGS, bundleSettingsForIntent());

            // Start the SettingsActivity
            startActivityForResult(intent, REQUEST_SETTINGS);
        } else if (id == R.id.action_cancel_alarms) {
            mSavedSettings.put(SettingsActivity.NOTIFICATIONS_CHECKED, false);
            cancelAlarms();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        writeSavedSettings();
    }

    private Bundle bundleSettingsForIntent() {
        Bundle settings = new Bundle();
        if (mSavedSettings == null) {

        } else {

            for (String key : mSavedSettings.keySet()) {
                Object value = mSavedSettings.get(key);
                switch (key) {
                    case SettingsActivity.NOTIFICATIONS_CHECKED:
                        Boolean b = value != null ? (Boolean) value : true;
                        settings.putBoolean(SettingsActivity.NOTIFICATIONS_CHECKED, b);
                        break;
                    case SettingsActivity.REPEAT_TYPE:
                        settings.putInt(SettingsActivity.REPEAT_TYPE, (Integer) mSavedSettings.get(key));
                        break;
                    case SettingsActivity.INTERVAL_TYPE:
                        settings.putInt(SettingsActivity.INTERVAL_TYPE, (Integer) mSavedSettings.get(key));
                        break;
                    default:
                        settings.putInt(key, (Integer) mSavedSettings.get(key));
                }
            }
        }
        return settings;
    }

    private void updateSettingsMap(Bundle newSettings) {
        mSavedSettings = new HashMap<String, Object>();

        for (String key : newSettings.keySet()) {
            switch (key) {
                case SettingsActivity.NOTIFICATIONS_CHECKED:
                    Boolean b = newSettings.getBoolean(key);
                    mSavedSettings.put(key, b);
                    break;
                case SettingsActivity.REPEAT_TYPE:
                    mSavedSettings.put(key, newSettings.getInt(key));
                    break;
                case SettingsActivity.INTERVAL_TYPE:
                    mSavedSettings.put(key, newSettings.getInt(key));
                    break;
                default:
                    mSavedSettings.put(key, newSettings.getInt(key));

            }
        }
    }

    private void readSavedSettings() throws IOException, JSONException {
        FileInputStream fis = null;
        boolean createDefaultSettings = false;
        try {
            fis = openFileInput(FILE_NAME);
        } catch (FileNotFoundException e) {
            // file not found. no settings have been saved yet
            createDefaultSettings = true;
        }

        mSavedSettings = new HashMap<String, Object>();
        if (createDefaultSettings) {
            mSavedSettings.put(SettingsActivity.NOTIFICATIONS_CHECKED, true);
            mSavedSettings.put(SettingsActivity.START_HRS, 12);
            mSavedSettings.put(SettingsActivity.START_MINS, 0);
            mSavedSettings.put(SettingsActivity.REPEAT_TYPE, SettingsActivity.RepeatType.DAILY.getValue());
        } else {
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String line = br.readLine();
            br.close();

            JSONObject json = new JSONObject(line);
            Iterator<String> itr = json.keys();
            while (itr.hasNext()) {
                String key = itr.next();
                switch (key) {
                    case SettingsActivity.NOTIFICATIONS_CHECKED:
                        Boolean b = json.getBoolean(key);
                        mSavedSettings.put(key, b);
                        break;
                    case SettingsActivity.REPEAT_TYPE:
                        mSavedSettings.put(key, json.getInt(key));
                        break;
                    case SettingsActivity.INTERVAL_TYPE:
                        mSavedSettings.put(key, json.getInt(key));
                        break;
                    default:
                        mSavedSettings.put(key, json.getInt(key));

                }
            }
        }
    }

    private void writeSavedSettings() {
        JSONObject json = new JSONObject(mSavedSettings);
        System.out.println(json.toString());

        try {
            FileOutputStream fos = openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            fos.write(json.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void setUpNotifications() {
        // Create an Intent to broadcast to the AlarmNotificationReceiver
        mNotificationReceiverIntent = new Intent(MainActivity.this,
                AlarmNotificationReceiver.class);

        Boolean alarmsOn = (Boolean) mSavedSettings.get(SettingsActivity.NOTIFICATIONS_CHECKED);
        if (alarmsOn != null && alarmsOn) {
            // find start time for alarm
            Integer startHr = (Integer) mSavedSettings.get(SettingsActivity.START_HRS);
            Integer startMin = (Integer) mSavedSettings.get(SettingsActivity.START_MINS);
            GregorianCalendar c = new GregorianCalendar();
            if (startHr != null && startMin != null) {
                c.set(Calendar.HOUR_OF_DAY, startHr);
                c.set(Calendar.MINUTE, startMin);
                c.set(Calendar.SECOND, 0);
            } else {
                // set the first alarm for a minute from now, just to annoy Nick
                // this code shouldn't be hit because we default the start alarm to 12:00 in
                // readSavedSettings()
                c.add(Calendar.MINUTE, 1);
            }

            long initialAlarmTime = c.getTimeInMillis();

            Integer repeatType = (Integer) mSavedSettings.get(SettingsActivity.REPEAT_TYPE);
            long alarmInterval = 0L;
            if (repeatType != null && repeatType != SettingsActivity.RepeatType.NONE.getValue()) {
                if (repeatType == SettingsActivity.RepeatType.DAILY.getValue()) {
                    alarmInterval = 24 * 60 * 60 * 1000L; // 24 hrs/day * 60 min/hr * 60 s/min * 1000 ms/s
                } else if (repeatType == SettingsActivity.RepeatType.CUSTOM.getValue()) {
                    Integer intervalType = (Integer) mSavedSettings.get(SettingsActivity.INTERVAL_TYPE);
                    Integer interval = (Integer) mSavedSettings.get(SettingsActivity.INTERVAL);
                    if (intervalType != null && interval != null) {
                        if (intervalType == SettingsActivity.IntervalType.MINS.getValue()) {
                            alarmInterval = interval * 60 * 1000L; // # min * 60 s/min * 1000 ms/s
                        } else if (intervalType == SettingsActivity.IntervalType.HRS.getValue()) {
                            alarmInterval = interval * 60 * 60 * 1000L; // # hr * 60 min/hr * 60 s/min * 1000 ms/s
                        }
                    }

                    Integer endHr = (Integer) mSavedSettings.get(SettingsActivity.END_HRS);
                    Integer endMin = (Integer) mSavedSettings.get(SettingsActivity.END_MINS);
                    if (endHr != null && endMin != null) {
                        // create Calendar for the end time.
                        GregorianCalendar endCal = new GregorianCalendar();
                        endCal.set(Calendar.HOUR_OF_DAY, endHr);
                        endCal.set(Calendar.MINUTE, endMin);
                        endCal.set(Calendar.SECOND, 0);
                        // if the end time is earlier than  or equal to the start time,
                        // assume it means to end the following day
                        if (endHr < startHr || (endHr == startHr && endMin <= startMin)) {
                            endCal.add(Calendar.HOUR_OF_DAY, 24);
                        }
                        mNotificationReceiverIntent.putExtra(AlarmNotificationReceiver.END_TIME, endCal.getTimeInMillis());
                        mNotificationReceiverIntent.putExtra(AlarmNotificationReceiver.INTERVAL_MILLIS, alarmInterval);
                    }
                }
            }
            // Create an PendingIntent that holds the NotificationReceiverIntent
            mNotificationReceiverPendingIntent = PendingIntent.getBroadcast(
                    MainActivity.this, 0, mNotificationReceiverIntent, 0);

            // cancel any previously set up alarms
            cancelAlarms();

            if (alarmInterval == 0) {
                // no repeat.  set one time alarm
                mAlarmManager.set(AlarmManager.RTC_WAKEUP, initialAlarmTime, mNotificationReceiverPendingIntent);
            } else {
                // set repeating alarm
                mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                        initialAlarmTime, alarmInterval,
                        mNotificationReceiverPendingIntent);
            }
        } else {
            // cancel any previously set up alarms
            cancelAlarms();
        }
    }

    private void cancelAlarms() {
        if (mNotificationReceiverPendingIntent == null) {
            if (mNotificationReceiverIntent == null) {
                mNotificationReceiverIntent = new Intent(MainActivity.this,
                        AlarmNotificationReceiver.class);
            }
            mNotificationReceiverPendingIntent = PendingIntent.getBroadcast(
                    MainActivity.this, 0, mNotificationReceiverIntent, 0);
        }
        mAlarmManager.cancel(mNotificationReceiverPendingIntent);
    }

    private void populateImageAdapter() {
        // clear any old images
        mAdapter.setThumbPaths(new ArrayList<String>());

        // Get list of all files in target directory and add to GridView

        File targetDir = getSelfiesDir();
        if (targetDir != null) {

            // Note: `getSelfiesDir` will create a new empty folder if not present
            // or return `null` if SD card not present or not writable
            // which is safer than this old code:

            File[] files = targetDir.listFiles();
            // Sort in alpha order - this will show as oldest first
            //Arrays.sort(files);
            if (files != null) {
                for (File file : files) {
                    mAdapter.addItem(file.getAbsolutePath());
                }
            }
        } else {
            // display error to user as a dialog after onCreate finishes
            Toast.makeText(getApplicationContext(), "SD storage not available", Toast.LENGTH_LONG).show();
        }
    }

    private File getSelfiesDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), PHOTO_DIR);

            if (storageDir != null) {
                storageDir.mkdirs();
                if (!(storageDir.exists() && storageDir.isDirectory())) {
                    // directory doesn't exist or isn't a directory.  Return null and display error to user when onCreate finishes
                    Log.e(TAG, "Storage Directory " + Environment.DIRECTORY_PICTURES + "/" + PHOTO_DIR + " not found or failed to be created.");
                    return null;
                }
            }
        } else {
            Log.e(TAG, "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    // The follow methods are adapted from the tutorial at http://developer.android.com/training/camera/photobasics.html
    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getSelfiesDir();
        if (storageDir == null) {
            // return early, display errors to user
            return null;
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();

        return image;
    }

    static final int REQUEST_TAKE_PHOTO = 1;
    private File mTempFile;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                mTempFile = null;
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, ex.getMessage(), ex);
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                mTempFile = photoFile;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {
                galleryAddPic();
                mAdapter.addItem(mCurrentPhotoPath);
                mAdapter.notifyDataSetChanged();
            } else if (resultCode == RESULT_CANCELED) {
                if (mTempFile != null) {
                    // delete the temp file
                    mTempFile.delete();
                }
            }
        } else if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                updateSettingsMap(extras);
                setUpNotifications();
            }
        }
    }

    private void galleryAddPic() {
        if (mCurrentPhotoPath != null) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(mCurrentPhotoPath);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
        }
    }

}

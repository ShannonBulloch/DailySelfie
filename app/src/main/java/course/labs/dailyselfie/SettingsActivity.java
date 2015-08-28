package course.labs.dailyselfie;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;


public class SettingsActivity extends AppCompatActivity {

    public static final String NOTIFICATIONS_CHECKED = "NotificationsChecked";
    public static final String START_HRS = "StartHrs";
    public static final String START_MINS = "StartMins";
    public static final String REPEAT_TYPE = "RepeatType";
    public static final String INTERVAL = "Interval";
    public static final String INTERVAL_TYPE = "IntervalType";
    public static final String END_HRS = "EndHrs";
    public static final String END_MINS = "EndMins";

    private Bundle mSettings;
    private boolean mSettingsEnabled = true;
    private boolean mCustomEnabled = false;

    private boolean mNotifications = true;
    private int mStartHrs;
    private int mStartMins;
    private RepeatType mRepeat = RepeatType.DAILY;
    private Integer mInterval;
    private IntervalType mMinsHrs = IntervalType.MINS;
    private Integer mEndHrs;
    private Integer mEndMins;

    private CheckBox mCheckbox;
    private TextView mStartTime;
    private Button mSetStart;
    private RadioGroup mRepeatGroup;
    private RadioButton mDaily;
    private RadioButton mCustom;
    private EditText mIntervalNum;
    private RadioGroup mIntervalGroup;
    private RadioButton mMin;
    private RadioButton mHr;
    private TextView mEndTime;
    private Button mSetEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Intent intent = getIntent();
        mSettings = intent.getBundleExtra(MainActivity.EXTRA_NOTIFICATION_SETTINGS);
        getSavedSettings();

        mCheckbox = (CheckBox) findViewById(R.id.checkBoxNotifications);
        mStartTime = (TextView) findViewById(R.id.timeStartTime);
        mSetStart = (Button) findViewById(R.id.buttonSetStart);
        mRepeatGroup = (RadioGroup) findViewById(R.id.radioGroupRepeat);
        mDaily = (RadioButton) findViewById(R.id.radioOnceDaily);
        mCustom = (RadioButton) findViewById(R.id.radioCustom);
        mIntervalNum = (EditText) findViewById(R.id.editTextInterval);
        mIntervalGroup = (RadioGroup) findViewById(R.id.radioGroupMinHr);
        mMin = (RadioButton) findViewById(R.id.radioMins);
        mHr = (RadioButton) findViewById(R.id.radioHrs);
        mEndTime = (TextView) findViewById(R.id.timeEndTime);
        mSetEnd = (Button) findViewById(R.id.buttonSetEnd);

        mCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettingsEnabled = isChecked;
                enableSettingsUI();
            }
        });

        mRepeatGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mCustom.getId() == checkedId) {
                    mRepeat = RepeatType.CUSTOM;
                   mCustomEnabled = true;
                } else {
                    mCustomEnabled = false;
                    if (mDaily.getId() == checkedId) {
                        mRepeat = RepeatType.DAILY;
                    } else {
                        mRepeat = RepeatType.NONE;
                    }
                }
                enableSettingsUI();
            }
        });

        mIntervalGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == mMin.getId()) {
                    mMinsHrs = IntervalType.MINS;
                } else {
                    mMinsHrs = IntervalType.HRS;
                }
            }
        });

        // Set an OnClickListener for the Set start time button
        mSetStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Create a new DatePickerFragment
                DialogFragment newFragment = new StartTimePickerFragment();
                Bundle args = new Bundle();
                args.putInt(START_HRS, mStartHrs);
                args.putInt(START_MINS, mStartMins);
                newFragment.setArguments(args);


                // Display DatePickerFragment
                newFragment.show(getFragmentManager(), "TimePicker");
            }
        });

        // Set an OnClickListener for the Set end time button
        mSetEnd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Create a new DatePickerFragment
                DialogFragment newFragment = new EndTimePickerFragment();
                if (mEndHrs != null && mEndMins != null) {
                    Bundle args = new Bundle();
                    args.putInt(END_HRS, mEndHrs);
                    args.putInt(END_MINS, mEndMins);
                    newFragment.setArguments(args);
                }

                // Display DatePickerFragment
                newFragment.show(getFragmentManager(), "TimePicker");
            }
        });

        mSettingsEnabled = mNotifications;
        mCustomEnabled = mRepeat == RepeatType.CUSTOM;
        initUiValues();
        enableSettingsUI();
    }

    private void getSavedSettings() {
        // if saved settings aren't found, apply default settings:
        // notifications are on, and user will get a daily alarm at noon.
        mNotifications = mSettings.getBoolean(NOTIFICATIONS_CHECKED,true);
        mStartHrs = mSettings.getInt(START_HRS, 12);
        mStartMins = mSettings.getInt(START_MINS, 0);
        int repeatIndex = mSettings.getInt(REPEAT_TYPE, RepeatType.DAILY.getValue());
        if (repeatIndex == RepeatType.DAILY.getValue()) {
            mRepeat = RepeatType.DAILY;
        } else if (repeatIndex == RepeatType.CUSTOM.getValue()) {
            mRepeat = RepeatType.CUSTOM;
        } else {
            mRepeat = RepeatType.NONE;
        }

        if (mSettings.getInt(INTERVAL, -1) != -1) {
            mInterval = mSettings.getInt(INTERVAL);
        }

        int intervalIndex = mSettings.getInt(INTERVAL_TYPE, IntervalType.NONE.getValue());
        if (intervalIndex == IntervalType.MINS.getValue()) {
            mMinsHrs = IntervalType.MINS;
        } else if (intervalIndex == IntervalType.HRS.getValue()) {
            mMinsHrs = IntervalType.HRS;
        } else {
            mMinsHrs = IntervalType.NONE;
        }

        int endHrs = mSettings.getInt(END_HRS, -1);
        int endMins = mSettings.getInt(END_MINS, -1);
        if (endHrs != -1 && endMins != -1) {
            mEndHrs = endHrs;
            mEndMins = endMins;
        }
    }

    private Bundle bundleSettingsForIntent() {
        Bundle settings = new Bundle();
        settings.putBoolean(SettingsActivity.NOTIFICATIONS_CHECKED, mCheckbox.isChecked());
        settings.putInt(SettingsActivity.START_HRS, mStartHrs);
        settings.putInt(SettingsActivity.START_MINS, mStartMins);
        if (mRepeat != null) {
            settings.putInt(SettingsActivity.REPEAT_TYPE, mRepeat.getValue());
        }
        String intervalStr = mIntervalNum.getText().toString();
        if (intervalStr != null) {
            intervalStr.trim();
            if (!intervalStr.isEmpty()) {
                mInterval = Integer.valueOf(intervalStr);
            }
        }
        if (mInterval != null) {
            settings.putInt(SettingsActivity.INTERVAL, mInterval);
        }
        if (mMinsHrs != null) {
            settings.putInt(SettingsActivity.INTERVAL_TYPE, mMinsHrs.getValue());
        }
        if (mEndHrs != null && mEndMins != null) {
            settings.putInt(SettingsActivity.END_HRS, mEndHrs);
            settings.putInt(SettingsActivity.END_MINS, mEndMins);
        }
        return settings;
    }

    private void initUiValues() {
        mCheckbox.setChecked(mNotifications);
        mStartTime.setText(formatTime(mStartHrs,mStartMins));

        switch (mRepeat) {
            case DAILY:
                mDaily.setChecked(true);
                break;
            case CUSTOM:
                mCustom.setChecked(true);
                break;
            default:
                // TODO set None radio button
                // make sure neither are checked
                mDaily.setChecked(false);
                mCustom.setChecked(false);
                break;
        }

        mIntervalNum.setText("" + (mInterval != null ? mInterval : ""));

        switch (mMinsHrs) {
            case MINS:
                mMin.setChecked(true);
                break;
            case HRS:
                mHr.setChecked(true);
                break;
            default:
                // make sure neither are checked
                mMin.setChecked(false);
                mHr.setChecked(false);
                break;
        }

        if (mEndHrs != null && mEndMins != null) {
            mEndTime.setText(formatTime(mEndHrs, mEndMins));
        }
    }

    private void enableSettingsUI() {
        mStartTime.setEnabled(mSettingsEnabled);
        mSetStart.setEnabled(mSettingsEnabled);
        mDaily.setEnabled(mSettingsEnabled);
        mCustom.setEnabled(mSettingsEnabled);
        mIntervalNum.setEnabled(mSettingsEnabled && mCustomEnabled);
        mIntervalGroup.setEnabled(mSettingsEnabled && mCustomEnabled);
        mMin.setEnabled(mSettingsEnabled && mCustomEnabled);
        mHr.setEnabled(mSettingsEnabled && mCustomEnabled);
        mEndTime.setEnabled(mSettingsEnabled && mCustomEnabled);
        mSetEnd.setEnabled(mSettingsEnabled && mCustomEnabled);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_settings, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public void onBackPressed() {

        Intent result = new Intent();
        //result.putExtra(MainActivity.EXTRA_NOTIFICATION_SETTINGS, bundleSettingsForIntent());
        result.putExtras(bundleSettingsForIntent());

        setResult(RESULT_OK, result);
        super.onBackPressed();

    }

    public enum RepeatType {
        NONE(0), DAILY(1), CUSTOM(2);

        private final int value;
        RepeatType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public enum IntervalType {
        MINS(0), HRS(1), NONE(-1);

        private final int value;
        IntervalType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static class StartTimePickerFragment extends DialogFragment implements
            TimePickerDialog.OnTimeSetListener {


        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            Bundle b = getArguments();
            int hourOfDay = b.getInt(START_HRS);
            int minute = b.getInt(START_MINS);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), TimePickerDialog.THEME_HOLO_LIGHT, this,
                    hourOfDay, minute, false);


        }

        // Callback to TimePickerFragmentActivity.onTimeSet() to update the UI
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            ((SettingsActivity) getActivity()).onStartTimeSet(view, hourOfDay,
                    minute);

        }
    }

    public static class EndTimePickerFragment extends DialogFragment implements
            TimePickerDialog.OnTimeSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            Bundle b = getArguments();
            int hourOfDay;
            int minute;
            if (b != null) {
                hourOfDay = b.getInt(END_HRS);
                minute = b.getInt(END_MINS);
            } else {
                final Calendar c = Calendar.getInstance();
                hourOfDay = c.get(Calendar.HOUR_OF_DAY);
                minute = c.get(Calendar.MINUTE);
            }

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), TimePickerDialog.THEME_HOLO_LIGHT, this,
                    hourOfDay, minute, false);

        }

        // Callback to TimePickerFragmentActivity.onTimeSet() to update the UI
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            ((SettingsActivity) getActivity()).onEndTimeSet(view, hourOfDay,
                    minute);

        }
    }

    public void onStartTimeSet(TimePicker view, int hourOfDay, int minute) {
        mStartHrs = hourOfDay;
        mStartMins = minute;

        mStartTime.setText(formatTime(hourOfDay,minute));
    }

    public void onEndTimeSet(TimePicker view, int hourOfDay, int minute) {
        mEndHrs = hourOfDay;
        mEndMins = minute;
        mEndTime.setText(formatTime(hourOfDay, minute));
    }

    private String formatTime(int hour, int minute) {
        String time = "";

        int hourMod = hour > 12 ? hour - 12 : hour;
        String minStr = minute < 10 ? "0"+minute : "" + minute;

        return (hourMod < 10 ? " " : "") + hourMod + ":" + minStr + " " + (hour < 12 ? "AM" : "PM");
    }
}

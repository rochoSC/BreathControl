package psi.tamu.controlyourbreath.orbotix.controlui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import psi.tamu.controlyourbreath.MainMenuFragment;
import psi.tamu.controlyourbreath.R;
import psi.tamu.controlyourbreath.orbotix.robot.app.ColorPickerActivity;
import psi.tamu.controlyourbreath.orbotix.robot.widgets.CalibrationImageButtonView;
import psi.tamu.controlyourbreath.orbotix.robot.widgets.NoSpheroConnectedView;
import psi.tamu.controlyourbreath.orbotix.robot.widgets.NoSpheroConnectedView.OnConnectButtonClickListener;
import psi.tamu.controlyourbreath.orbotix.robot.widgets.SlideToSleepView;
import psi.tamu.controlyourbreath.orbotix.robot.widgets.joystick.JoystickView;
import orbotix.sphero.Sphero;
import orbotix.view.calibration.CalibrationView;
import orbotix.view.calibration.ControllerActivity;

public class MainControlUI extends ControllerActivity {


    public final String DIR_NAME = "BreathControl_Records";
    public final String FILE_NAME = "Records.csv";

    /**
     * ID to start the StartupActivity for result to connect the Robot
     */
    private final static int STARTUP_ACTIVITY = 0;
    private static final int BLUETOOTH_ENABLE_REQUEST = 11;
    private static final int BLUETOOTH_SETTINGS_REQUEST = 12;

    /**
     * ID to start the ColorPickerActivity for result to select a color
     */
    private final static int COLOR_PICKER_ACTIVITY = 1;
    private boolean mColorPickerShowing = false;

    /**
     * The Robot to control.
     * ROBOT is STATIC for any object generated. This object must be initialized before this activity start.
     */
    static public Sphero mRobot;

    /**
     * One-Touch Calibration Button
     */
    private CalibrationImageButtonView mCalibrationImageButtonView;

    /**
     * Calibration View widget
     */
    private CalibrationView mCalibrationView;

    /**
     * Slide to sleep view
     */
    private SlideToSleepView mSlideToSleepView;

    /**
     * No Sphero Connected Pop-Up View
     */
    private NoSpheroConnectedView mNoSpheroConnectedView;

    /**
     * General views
     */
    private TextView countDownView;
    private JoystickView joystick;
    public Chronometer chronometer;
    public TextView txtRate;
    public LinearLayout scoreLayout;
    public TextView txtScore;
    public LinearLayout countdownLayout;

    public Button btnStop;

    //Colors
    private int mRed = 0xff;
    private int mGreen = 0xff;
    private int mBlue = 0xff;

    //Files
    public String root = "";
    public String dirPath = "";
    public String filePath = "";
    public File dir;
    public File file;


    /**
     * Control variables.
     */
    public final int EASY = 1, MEDIUM = 2, HARD = 3;
    public int ACTUAL_DIFICULTY = EASY;

    //To determine whether  the robot will receive noise or not.
    public boolean isRecordEnabled;
    public boolean isNoiseEnabled;

    boolean firstTickControl = true;
    public ArrayList<Double> gameMeasuresPerSecond = new ArrayList<>();
    public int timesUnderIdealBR = 0;
    public double minBR = 9999.9;
    public double maxBR = 0.0;
    public double measuresSum = 0;
    public int measuresNum = 0;

    //To receive the colors provided by the user through the color picker. NOT ENABLED FOR THIS PROJECT
    BroadcastReceiver mColorChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // update colors
            int red = intent.getIntExtra(ColorPickerActivity.EXTRA_COLOR_RED, 0);
            int green = intent.getIntExtra(ColorPickerActivity.EXTRA_COLOR_GREEN, 0);
            int blue = intent.getIntExtra(ColorPickerActivity.EXTRA_COLOR_BLUE, 0);

            // change the color on the ball
            mRobot.setColor(red, green, blue);
        }
    };

    /**
     * Receiver for the measures of the BioHarness
     */
    BroadcastReceiver bcBHReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MainMenuFragment.isBioHarnessConected) {
                String rate = intent.getStringExtra(MainMenuFragment.EXTRA_NEW_MEASURE);
                if (Double.valueOf(rate) < JoystickView.MAX_IDEAL_BREATH_RATE) {
                    txtRate.setTextColor(Color.WHITE);
                    if (mRobot != null)
                        mRobot.setColor(0, 255, 0);
                } else {
                    txtRate.setTextColor(Color.RED);
                    if (mRobot != null)
                        mRobot.setColor(255, 0, 0);
                }
                txtRate.setText(rate);
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ACTUAL_DIFICULTY = getIntent().getIntExtra("dificulty", EASY);
        countDownView = (TextView) findViewById(R.id.txtCountDown);
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        txtRate = (TextView) findViewById(R.id.txtBreathRateGUI);
        btnStop = (Button) findViewById(R.id.btnStop);

        //Add the JoystickView as a Controller
        addController((JoystickView) findViewById(R.id.joystick));

        // Add the calibration view
        mCalibrationView = (CalibrationView) findViewById(R.id.calibration_view);

        // Set up sleep view
        mSlideToSleepView = (SlideToSleepView) findViewById(R.id.slide_to_sleep_view);
        mSlideToSleepView.hide();
        // Send ball to sleep after completed widget movement. NOT ENABLED FOR THIS PROJECT
        mSlideToSleepView.setOnSleepListener(new SlideToSleepView.OnSleepListener() {
            @Override
            public void onSleep() {
                mRobot.sleep(0);
            }
        });

        // Initialize calibrate button view where the calibration circle shows above button
        // This is the default behavior
        mCalibrationImageButtonView = (CalibrationImageButtonView) findViewById(R.id.calibration_image_button);
        mCalibrationImageButtonView.setCalibrationView(mCalibrationView);
        // You can also change the size and location of the calibration views (or you can set it in XML)
        mCalibrationImageButtonView.setRadius(100);
        mCalibrationImageButtonView.setOrientation(CalibrationView.CalibrationCircleLocation.ABOVE);

        // Grab the No Sphero Connected View
        mNoSpheroConnectedView = (NoSpheroConnectedView) findViewById(R.id.no_sphero_connected_view);
        mNoSpheroConnectedView.setOnConnectButtonClickListener(new OnConnectButtonClickListener() {

            @Override
            public void onConnectClick() {
                finish();
                //mSpheroConnectionView.setVisibility(View.VISIBLE);
                //mSpheroConnectionView.startDiscovery();
            }

            @Override
            public void onSettingsClick() {
                // Open the Bluetooth Settings Intent
                Intent settingsIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                MainControlUI.this.startActivityForResult(settingsIntent, BLUETOOTH_SETTINGS_REQUEST);
            }
        });
        joystick = (JoystickView) findViewById(R.id.joystick);
        joystick.setDificulty(this.ACTUAL_DIFICULTY);
        this.isRecordEnabled = getIntent().getBooleanExtra("isRecordActivated", false);
        joystick.isNoiseEnabled = getIntent().getBooleanExtra("isNoiseActivated", false);

        this.isNoiseEnabled = getIntent().getBooleanExtra("isNoiseActivated", false);

        //Remember, at this point the robot should be already connected.
        //ROBOT is STATIC for any object generated. This object must be initialized before this activity start.
        setRobot(mRobot);
        mCalibrationView.setRobot(mRobot);

        setDifficulty();

        mRobot.setColor(0, 255, 0);
        mNoSpheroConnectedView.setVisibility(View.GONE);
        mNoSpheroConnectedView.switchToConnectButton();

        prepareJoystick();
        prepareFile();
    }

    public void setDifficulty() {
        TextView txtDificulty = (TextView) findViewById(R.id.txtActualDificulty);
        if (ACTUAL_DIFICULTY == EASY)
            txtDificulty.setText("EASY");

        if (ACTUAL_DIFICULTY == MEDIUM)
            txtDificulty.setText("MEDIUM");

        if (ACTUAL_DIFICULTY == HARD)
            txtDificulty.setText("HARD");
    }

    public void prepareJoystick() {
        if (MainMenuFragment.isBioHarnessConected) {
            joystick.setVisibility(View.GONE);
            btnStop.setEnabled(true);
            startCountdownAnimation();
        } else {
            txtRate.setText("Not connected");
            joystick.setVisibility(View.VISIBLE);
            btnStop.setEnabled(false);
        }
    }

    public void prepareFile() {
        root = Environment.getExternalStorageDirectory().toString();
        dirPath = root + File.separator + this.DIR_NAME;
        filePath = dirPath + File.separator + this.FILE_NAME;
        dir = new File(dirPath);
        if (!dir.exists())
            dir.mkdirs();
        file = new File(filePath);
        try {
            if (!file.exists()) {

                file.createNewFile();

                //Writing the headers
                FileOutputStream writer = new FileOutputStream(file, true);
                String data = "Date, Difficulty, Minutes played, Seconds played, Min. BR, Max. BR, Average BR, Noise" + "\r\n";
                writer.write(data.getBytes());
                writer.flush();
                writer.close();

            }
        } catch (IOException e) {
            Toast.makeText(MainControlUI.this, "An error has happened when creating file", Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mColorPickerShowing) {
            mColorPickerShowing = false;
            return;
        }
        Log.d("", "registering Color Change Listener");
        IntentFilter filter = new IntentFilter(ColorPickerActivity.ACTION_COLOR_CHANGE);
        registerReceiver(mColorChangeReceiver, filter);

        IntentFilter filter2 = new IntentFilter(MainMenuFragment.ACTION_NEW_MEASURE);
        registerReceiver(bcBHReceiver, filter2);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRobot != null)
            mRobot.stop();
        try {
            unregisterReceiver(bcBHReceiver);
            unregisterReceiver(mColorChangeReceiver);
        } catch (Exception ex) {

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRobot != null)
            mRobot.stop();
        try {
            unregisterReceiver(bcBHReceiver);
            unregisterReceiver(mColorChangeReceiver);
        } catch (Exception ex) {

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == COLOR_PICKER_ACTIVITY) {
                //Get the colors
                mRed = data.getIntExtra(ColorPickerActivity.EXTRA_COLOR_RED, 0xff);
                mGreen = data.getIntExtra(ColorPickerActivity.EXTRA_COLOR_GREEN, 0xff);
                mBlue = data.getIntExtra(ColorPickerActivity.EXTRA_COLOR_BLUE, 0xff);

                //Set the color
                mRobot.setColor(mRed, mGreen, mBlue);
            } else if (requestCode == BLUETOOTH_ENABLE_REQUEST) {
                // User enabled bluetooth, so refresh Sphero list
                //mSpheroConnectionView.setVisibility(View.VISIBLE);
                //mSpheroConnectionView.startDiscovery();
            }
        } else {
            if (requestCode == STARTUP_ACTIVITY) {
                // Failed to return any robot, so we bring up the no robot connected view
                mNoSpheroConnectedView.setVisibility(View.VISIBLE);
            } else if (requestCode == BLUETOOTH_ENABLE_REQUEST) {

                // User clicked "NO" on bluetooth enable settings screen
                Toast.makeText(MainControlUI.this,
                        "Enable Bluetooth to Connect to Sphero", Toast.LENGTH_LONG).show();
            } else if (requestCode == BLUETOOTH_SETTINGS_REQUEST) {
                // User enabled bluetooth, so refresh Sphero list
                // mSpheroConnectionView.setVisibility(View.VISIBLE);
                // mSpheroConnectionView.startDiscovery();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mRobot != null)
            mRobot.setColor(0, 0, 255);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mCalibrationView.interpretMotionEvent(event);
        mSlideToSleepView.interpretMotionEvent(event);
        return super.dispatchTouchEvent(event);
    }

    /**
     * When the user clicks the "Color" button, show the ColorPickerActivity. NOT ENABLED
     */
    public void onColorClick(View v) {

        mColorPickerShowing = true;
        Intent i = new Intent(this, ColorPickerActivity.class);

        //Tell the ColorPickerActivity which color to have the cursor on.
        i.putExtra(ColorPickerActivity.EXTRA_COLOR_RED, mRed);
        i.putExtra(ColorPickerActivity.EXTRA_COLOR_GREEN, mGreen);
        i.putExtra(ColorPickerActivity.EXTRA_COLOR_BLUE, mBlue);

        startActivityForResult(i, COLOR_PICKER_ACTIVITY);
    }

    /**
     * When the user clicks the "Sleep" button, show the SlideToSleepView shows
     */
    public void onSleepClick(View v) {
        mSlideToSleepView.show();
    }

    /**
     * When the user clicks the score screen to return.
     */
    public void onClickFinalScore(View v) {
        onBackPressed();
    }

    public void startScoreAnimation() {
        scoreLayout = (LinearLayout) findViewById(R.id.scoreLayout);
        txtScore = (TextView) findViewById(R.id.txtScore);

        scoreLayout.setVisibility(View.VISIBLE);

        Animation anim1 = AnimationUtils.loadAnimation(this, R.anim.appear);
        long scoredSeconds = getGameScore(); //Total number of seconds.
        Time scoreTime = getScoreTime(scoredSeconds);

        txtScore.setText(scoreTime.minute + " min " + scoreTime.second + " sec");

        scoreLayout.startAnimation(anim1);

    }

    public Time getScoreTime(long seconds) {
        int minutes = 0;
        double sec = 0;

        //Getting minutes and seconds.
        if (seconds >= 60) {
            minutes = (int) seconds / 60;
            sec = seconds % 60;
        } else {
            sec = seconds;
        }
        Time t = new Time();
        t.minute = minutes;
        t.second = (int) sec;
        return t;
    }

    public void startCountdownAnimation() {
        final Animation anim1 = AnimationUtils.loadAnimation(this, R.anim.appear);
        final Animation anim2 = AnimationUtils.loadAnimation(this, R.anim.disappear);
        countdownLayout = (LinearLayout) findViewById(R.id.countDownLayout);
        countdownLayout.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {

                //  3
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDownView.setVisibility(View.VISIBLE);
                        countDownView.setText("3");
                        countDownView.startAnimation(anim1);
                    }
                });

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDownView.startAnimation(anim2);
                    }
                });

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDownView.setVisibility(View.GONE);
                    }
                });

                //  2
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDownView.setVisibility(View.VISIBLE);
                        countDownView.setText("2");
                        countDownView.startAnimation(anim1);
                    }
                });

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDownView.startAnimation(anim2);
                    }
                });

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDownView.setVisibility(View.GONE);
                    }
                });

                //  1
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDownView.setVisibility(View.VISIBLE);
                        countDownView.setText("1");
                        countDownView.startAnimation(anim1);
                    }
                });

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDownView.startAnimation(anim2);
                    }
                });

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDownView.setVisibility(View.GONE);
                    }
                });

                //  Start
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDownView.setVisibility(View.VISIBLE);
                        countDownView.setTextSize(45);
                        countDownView.setText("START!");
                        countDownView.startAnimation(anim1);
                    }
                });

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDownView.startAnimation(anim2);
                    }
                });

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDownView.setVisibility(View.GONE);
                    }
                });

                //End of the complete animation.

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startGame();
                    }
                });

            }
        }).start();
    }

    public void startGame() {
        joystick.setVisibility(View.VISIBLE);
        countdownLayout.setVisibility(View.GONE);
        chronometer.setBase(SystemClock.elapsedRealtime());
        storageMeasuresPerSecond();
        chronometer.start();
    }

    public void storageMeasuresPerSecond() {
        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                if (firstTickControl) {//Ignore the first tick
                    firstTickControl = false;
                } else {
                    double currentBR = Double.valueOf(txtRate.getText().toString());
                    measuresNum++;
                    measuresSum += currentBR;
                    if (currentBR < minBR)
                        minBR = currentBR;

                    if (currentBR > maxBR)
                        maxBR = currentBR;
                    /* Old technique
                    if(Double.valueOf(txtRate.getText().toString())<=JoystickView.MAX_IDEAL_BREATH_RATE){
                        timesUnderIdealBR++;
                    }
                    gameMeasuresPerSecond.add(Double.valueOf(txtRate.getText().toString()));
                    */


                }

            }
        });

    }

    public void stopGame() {
        chronometer.stop();
        mRobot.stop();
        btnStop.setEnabled(false);
        joystick.setEnabled(false);

        startScoreAnimation();

        if (isRecordEnabled)
            storeDataCSV();

    }

    //To store the info into the CSV file
    public void storeDataCSV() {
        file = new File(filePath); //Writing the data in csv form
        try {
            FileOutputStream writer = new FileOutputStream(file, true);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date now = new Date();
            String strDate = sdf.format(now);

            String date = "\"" + strDate + "\"";
            String difficulty;
            if (ACTUAL_DIFICULTY == EASY)
                difficulty = "Easy";
            else if (ACTUAL_DIFICULTY == MEDIUM)
                difficulty = "Medium";
            else
                difficulty = "Hard";

            double averageBR = measuresSum / (double) measuresNum;
            long scoredSeconds = getGameScore(); //Total number of seconds.
            Time scoreTime = getScoreTime(scoredSeconds);

            String data = date + "," + difficulty + "," + scoreTime.minute + "," + scoreTime.second + "," +
                    minBR + "," + maxBR + "," + averageBR + "," + isNoiseEnabled + "\r\n";
            writer.write(data.getBytes());
            writer.flush();
            writer.close();
            new SingleMediaScanner(getBaseContext(), file.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getGameScore() {
        long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
        long elapsedSeconds = elapsedMillis / 1000;
        return elapsedSeconds;
    }

    public double getGameScoreOldTechniche() {
        long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
        long elapsedSeconds = elapsedMillis / 1000;

        //Measuring the percent of times that the user was under the ideal BR
        double percentTimesUnderIDeal = timesUnderIdealBR / (double) elapsedSeconds;
        //Toast.makeText(UiSampleActivity.this, "timesUnderIdealBR: " + timesUnderIdealBR + "elapsedSeconds: " + elapsedSeconds, Toast.LENGTH_SHORT).show();

        //Let's get the percent of closeness of each measure above the IDEAL

        double referenceDistance = JoystickView.MAX_BREATH_RATE - JoystickView.MAX_IDEAL_BREATH_RATE;
        double sumOfPercents = 0;
        double averageOfPersents = 0;
        double measure_i;
        double particularBRDistance;
        double particularPercent;//Measuring the percent of closeness to the ideal BR. As far as he is, as low as this percent is.
        for (int c = 0; c < gameMeasuresPerSecond.size(); c++) {
            measure_i = gameMeasuresPerSecond.get(c);

            if (measure_i > JoystickView.MAX_IDEAL_BREATH_RATE) {
                //Get the percent of the closeness
                if (measure_i > JoystickView.MAX_BREATH_RATE) {
                    measure_i = JoystickView.MAX_BREATH_RATE;
                }
                particularBRDistance = JoystickView.MAX_BREATH_RATE - measure_i;
                particularPercent = particularBRDistance / referenceDistance;

                sumOfPercents += particularPercent;
            } else {
                //By this case, the percent of closeness will be 100% even if is under 8
                sumOfPercents += 1;
            }
        }
        if (gameMeasuresPerSecond.size() > 0)
            averageOfPersents = sumOfPercents / gameMeasuresPerSecond.size();
        //At this point, we have two quantities. Let's return the average between them

        //This is returned considering 1 as 100%
        return (percentTimesUnderIDeal + averageOfPersents) / 2;
    }

    public void onClickStop(View v) {
        stopGame();
    }

    //This class reboot the memory card to be able to see changes over MTP protocol.
    private class SingleMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {
        private MediaScannerConnection mediaScan;
        private String path;

        SingleMediaScanner(Context context, String f) {
            path = f;
            mediaScan = new MediaScannerConnection(context, this);
            mediaScan.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            mediaScan.scanFile(path, null);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            mediaScan.disconnect();
        }
    }


}

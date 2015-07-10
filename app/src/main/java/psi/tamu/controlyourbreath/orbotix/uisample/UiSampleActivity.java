package psi.tamu.controlyourbreath.orbotix.uisample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import orbotix.macro.Sleep;
import psi.tamu.controlyourbreath.BHConnectedEventListener;
import psi.tamu.controlyourbreath.MainMenuFragment;
import psi.tamu.controlyourbreath.R;
import psi.tamu.controlyourbreath.WelcomeActivity;
import psi.tamu.controlyourbreath.orbotix.robot.app.ColorPickerActivity;
import orbotix.robot.base.Robot;
import psi.tamu.controlyourbreath.orbotix.robot.widgets.CalibrationImageButtonView;
import psi.tamu.controlyourbreath.orbotix.robot.widgets.NoSpheroConnectedView;
import psi.tamu.controlyourbreath.orbotix.robot.widgets.NoSpheroConnectedView.OnConnectButtonClickListener;
import psi.tamu.controlyourbreath.orbotix.robot.widgets.SlideToSleepView;
import psi.tamu.controlyourbreath.orbotix.robot.widgets.joystick.JoystickView;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.Sphero;
import orbotix.view.calibration.CalibrationView;
import orbotix.view.calibration.ControllerActivity;
import orbotix.view.connection.SpheroConnectionView;
import zephyr.android.BioHarnessBT.BTClient;

public class UiSampleActivity extends ControllerActivity {


    public final int EASY = 1, MEDIUM = 2, HARD = 3;
    public int ACTUAL_DIFICULTY = EASY;
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
     * The Robot to control
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

    private TextView countDownView;

    private JoystickView joystick;

    public Chronometer chronometer;

    public TextView txtRate;

    public Button btnStop;

    //Colors
    private int mRed = 0xff;
    private int mGreen = 0xff;
    private int mBlue = 0xff;


    //To receive the colors provided by the user through the color picker.
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

    BroadcastReceiver bcBHReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(MainMenuFragment.isBioHarnessConected) {
                String rate = intent.getStringExtra(MainMenuFragment.EXTRA_NEW_MEASURE);
                if (Double.valueOf(rate) < JoystickView.MAX_IDEAL_BREATH_RATE) {
                    txtRate.setTextColor(Color.WHITE);
                    if (mRobot !=null)
                        mRobot.setColor(0, 255, 0);
                }else {
                    txtRate.setTextColor(Color.RED);
                    if (mRobot !=null)
                        mRobot.setColor(255,0, 0);
                }
                txtRate.setText(rate);
            }
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        ACTUAL_DIFICULTY = getIntent().getIntExtra("dificulty", EASY);


        countDownView = (TextView) findViewById(R.id.txtCountDown);
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        txtRate = (TextView) findViewById(R.id.txtBreathRateGUI);
        btnStop = (Button) findViewById(R.id.btnStop);

        // Set up the Sphero Connection View
        //mSpheroConnectionView = (SpheroConnectionView) findViewById(R.id.sphero_connection_view);
        //When we get the view, instantly launches "StartDiscovery" and shows the view <---NOTE
        //When a connection is accepted, automatically hides the view.
        //mSpheroConnectionView.addConnectionListener(new ConnectionListener() {

        //Robot r  = getIntent().getParcelableExtra("robot");
        //mRobot = (Sphero)r;
        //setRobot(mRobot);
        // Make sure you let the calibration view knows the robot it should control

        //mRobot.setColor(0, 255, 0);
         /*   public void onConnected(Robot robot) {
                // Set Robot
                mRobot = (Sphero) robot; // safe to cast for now
                //Set connected Robot to the Controllers
                setRobot(mRobot);
                // Make sure you let the calibration view knows the robot it should control
                mCalibrationView.setRobot(mRobot);
                mRobot.setColor(0, 255, 0);


                // Make connect sphero pop-up invisible if it was previously up
                mNoSpheroConnectedView.setVisibility(View.GONE);
                mNoSpheroConnectedView.switchToConnectButton();
            }

            @Override
            public void onConnectionFailed(Robot sphero) {
                // let the SpheroConnectionView handle or hide it and do something here...
            }

            @Override
            public void onDisconnected(Robot sphero) {
                mSpheroConnectionView.startDiscovery();
            }
        });*/

        //Add the JoystickView as a Controller
        addController((JoystickView) findViewById(R.id.joystick));

        // Add the calibration view
        mCalibrationView = (CalibrationView) findViewById(R.id.calibration_view);

        // Set up sleep view
        mSlideToSleepView = (SlideToSleepView) findViewById(R.id.slide_to_sleep_view);
        mSlideToSleepView.hide();
        // Send ball to sleep after completed widget movement
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
                UiSampleActivity.this.startActivityForResult(settingsIntent, BLUETOOTH_SETTINGS_REQUEST);
            }
        });
        joystick = (JoystickView) findViewById(R.id.joystick);
        joystick.setDificulty(this.ACTUAL_DIFICULTY);




        setRobot(mRobot);
        // Make sure you let the calibration view knows the robot it should control

        //Remember, at this point the robot should be already connected
        TextView txtDificulty = (TextView) findViewById(R.id.txtActualDificulty);

        mCalibrationView.setRobot(mRobot);

        if (ACTUAL_DIFICULTY == EASY) {
            txtDificulty.setText("EASY");

        }

        if (ACTUAL_DIFICULTY == MEDIUM) {
            txtDificulty.setText("MEDIUM");
            //mRobot.setColor(255, 228, 0);
        }

        if (ACTUAL_DIFICULTY == HARD) {
            txtDificulty.setText("HARD");
            //mRobot.setColor(255, 0, 0);
        }
        mRobot.setColor(0, 255, 0);
        mNoSpheroConnectedView.setVisibility(View.GONE);
        mNoSpheroConnectedView.switchToConnectButton();

        if(MainMenuFragment.isBioHarnessConected){
            joystick.setVisibility(View.GONE);
            btnStop.setEnabled(true);
            startAnimation();
        }else{
            txtRate.setText("Not connected");
            joystick.setVisibility(View.VISIBLE);
            btnStop.setEnabled(false);
        }
    }


    /**
     * Called when the user comes back to this activity
     */
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
    /**
     * Called when the user presses the back or home button
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mRobot != null)
            mRobot.stop();
        /*if (mColorPickerShowing) return;

        // Disconnect Robot properly
        if (mRobot != null) {
            mRobot.disconnect();
        }
        try {
            unregisterReceiver(mColorChangeReceiver); // many times throws exception on leak
        } catch (Exception e) {
        }*/
        try {
            unregisterReceiver(bcBHReceiver);
            unregisterReceiver(mColorChangeReceiver);
        }catch (Exception ex){

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRobot != null)
            mRobot.stop();
        /*try {
            unregisterReceiver(mColorChangeReceiver); // many times throws exception on leak
        } catch (Exception e) {
        }*/
        try {
            unregisterReceiver(bcBHReceiver);
            unregisterReceiver(mColorChangeReceiver);
        }catch (Exception ex){

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
                Toast.makeText(UiSampleActivity.this,
                        "Enable Bluetooth to Connect to Sphero", Toast.LENGTH_LONG).show();
            } else if (requestCode == BLUETOOTH_SETTINGS_REQUEST) {
                // User enabled bluetooth, so refresh Sphero list
                // mSpheroConnectionView.setVisibility(View.VISIBLE);
                // mSpheroConnectionView.startDiscovery();
            }
        }
    }

    /**
     * When the user clicks the "Color" button, show the ColorPickerActivity
     *
     * @param v The Button clicked
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
     *
     * @param v The Button clicked
     */
    public void onSleepClick(View v) {
        mSlideToSleepView.show();

    }

    public LinearLayout scoreLayout;
    public TextView txtWellDone;
    public TextView txtWellDone2;
    public TextView txtScore;

    public void onClickFinalScore(View v){
        onBackPressed();
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        if(mRobot != null)
            mRobot.setColor(0, 0, 255);
    }
    public void scoreAnimation(){
          scoreLayout = (LinearLayout) findViewById(R.id.scoreLayout);
          txtWellDone = (TextView) findViewById(R.id.txtWellDone);
          txtWellDone2 = (TextView) findViewById(R.id.txtWellDone2);
          txtScore = (TextView) findViewById(R.id.txtScore);

        final Animation anim1 = AnimationUtils.loadAnimation(this, R.anim.appear);
        new Thread(new Runnable() {
            @Override
            public void run() {


                //  Layout
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scoreLayout.setVisibility(View.VISIBLE);
                        txtWellDone.setVisibility(View.VISIBLE);
                        txtWellDone2.setVisibility(View.VISIBLE);
                        txtScore.setVisibility(View.VISIBLE);
                        double score = getGameScore() * 100;
                        txtScore.setText(String.format("%.2f",score)+"%");
                        scoreLayout.startAnimation(anim1);
                    }
                });

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }

            }
        }).start();

    }
    LinearLayout countDownLayout;
    public void startAnimation() {
        final Animation anim1 = AnimationUtils.loadAnimation(this, R.anim.appear);
        final Animation anim2 = AnimationUtils.loadAnimation(this, R.anim.disappear);
        countDownLayout = (LinearLayout) findViewById(R.id.countDownLayout);
        countDownLayout.setVisibility(View.VISIBLE);
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
                } catch (InterruptedException e){
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
                } catch (InterruptedException e){
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
                } catch (InterruptedException e){
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
                } catch (InterruptedException e){
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
                } catch (InterruptedException e){
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
                } catch (InterruptedException e){
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

    public int global = 0;
    public void startGame() {
        joystick.setVisibility(View.VISIBLE);
        countDownLayout.setVisibility(View.GONE);
        chronometer.setBase(SystemClock.elapsedRealtime());
        storageMeasuresPerSecond();
        chronometer.start();
    }

    boolean control = true;
    public ArrayList<Double> gameMeasuresPerSecond = new ArrayList<>();
    public int timesUnderIdealBR = 0;

    public void storageMeasuresPerSecond(){
        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                if(control) {//Ignore the first tick
                    control = false;
                }else{
                    if(Double.valueOf(txtRate.getText().toString())<=JoystickView.MAX_IDEAL_BREATH_RATE){
                        timesUnderIdealBR++;
                    }
                    gameMeasuresPerSecond.add(Double.valueOf(txtRate.getText().toString()));
                }

            }
        });

    }

    public void stopGame() {
        chronometer.stop();
        mRobot.stop();
        //double score = getGameScore() * 100;
        //Toast.makeText(UiSampleActivity.this, "Your score: " + score, Toast.LENGTH_SHORT).show();

        btnStop.setEnabled(false);
        joystick.setEnabled(false);

        scoreAnimation();
        //Here we will calculate the performance percent of this game.

    }

    public double getGameScore(){
        long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
        long elapsedSeconds = elapsedMillis / 1000;

        //Measuring the percent of times that the user was under the ideal BR
        double percentTimesUnderIDeal = timesUnderIdealBR/ (double)elapsedSeconds;
        //Toast.makeText(UiSampleActivity.this, "timesUnderIdealBR: " + timesUnderIdealBR + "elapsedSeconds: " + elapsedSeconds, Toast.LENGTH_SHORT).show();





        //Let's get the percent of closeness of each measure above the IDEAL

        double referenceDistance = JoystickView.MAX_BREATH_RATE - JoystickView.MAX_IDEAL_BREATH_RATE;
        double sumOfPercents = 0;
        double averageOfPersents = 0;
        double measure_i;
        double particularBRDistance;
        double particularPercent;//Measuring the percent of closeness to the ideal BR. As far as he is, as low as this percent is.
        for(int c=0; c<gameMeasuresPerSecond.size();c++) {
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
        if(gameMeasuresPerSecond.size() > 0)
            averageOfPersents = sumOfPercents / gameMeasuresPerSecond.size();
        //At this point, we have two quantities. Let's return the average between them

        //This is returned considering 1 as 100%
        return (percentTimesUnderIDeal + averageOfPersents) / 2;
    }

    public void onClickStop(View v) {
        stopGame();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mCalibrationView.interpretMotionEvent(event);
        mSlideToSleepView.interpretMotionEvent(event);
        return super.dispatchTouchEvent(event);
    }


}

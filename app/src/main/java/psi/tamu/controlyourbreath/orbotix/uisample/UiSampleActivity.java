package psi.tamu.controlyourbreath.orbotix.uisample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.Set;

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
    public  int ACTUAL_DIFICULTY = EASY;
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

    /**
     * Sphero Connection View
     */
   // private SpheroConnectionView mSpheroConnectionView;

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

    BroadcastReceiver bcBHReceiver  = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String rate = intent.getStringExtra(MainMenuFragment.EXTRA_NEW_MEASURE);
            TextView txtRate = (TextView) findViewById(R.id.txtBreathRateGUI);
            txtRate.setText(rate);
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
        JoystickView js = (JoystickView)findViewById(R.id.joystick);
        js.setDificulty(this.ACTUAL_DIFICULTY);

        setRobot(mRobot);
        // Make sure you let the calibration view knows the robot it should control

        //Remember, at this point the robot should be already connected
        TextView txtDificulty = (TextView) findViewById(R.id.txtActualDificulty);

        mCalibrationView.setRobot(mRobot);

        if(ACTUAL_DIFICULTY == EASY) {
            txtDificulty.setText("EASY");
            mRobot.setColor(0, 255, 0);
        }

        if(ACTUAL_DIFICULTY == MEDIUM) {
            txtDificulty.setText("MEDIUM");
            mRobot.setColor(255, 228, 0);
        }

        if(ACTUAL_DIFICULTY == HARD) {
            txtDificulty.setText("HARD");
            mRobot.setColor(255, 0, 0);
        }
        mNoSpheroConnectedView.setVisibility(View.GONE);
        mNoSpheroConnectedView.switchToConnectButton();

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
        registerReceiver(bcBHReceiver,filter2);

    }

    /**
     * Called when the user presses the back or home button
     */
    @Override
    protected void onPause() {
        super.onPause();
        if(mRobot!=null)
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mRobot!=null)
            mRobot.stop();
        /*try {
            unregisterReceiver(mColorChangeReceiver); // many times throws exception on leak
        } catch (Exception e) {
        }*/
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


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mCalibrationView.interpretMotionEvent(event);
        mSlideToSleepView.interpretMotionEvent(event);
        return super.dispatchTouchEvent(event);
    }


}

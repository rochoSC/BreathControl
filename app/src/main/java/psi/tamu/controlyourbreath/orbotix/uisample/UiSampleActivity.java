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
import psi.tamu.controlyourbreath.MainActivity;
import psi.tamu.controlyourbreath.R;
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
    private Sphero mRobot;

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
    private SpheroConnectionView mSpheroConnectionView;

    //Colors
    private int mRed = 0xff;
    private int mGreen = 0xff;
    private int mBlue = 0xff;

    /*BioHarness*/
    BluetoothAdapter btAdapter = null; //To work with Bluetooth
    BTClient btClient;
    BHConnectedEventListener bhConnectedListener;

    final int RESPIRATION_RATE = 0x101;

    static boolean isBioHarnessConected = false;

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


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //mColorChangeReceiver
        // Set up the Sphero Connection View
        mSpheroConnectionView = (SpheroConnectionView) findViewById(R.id.sphero_connection_view);
        //When we get the view, instantly launches "StartDiscovery" and shows the view <---NOTE
        //When a connection is accepted, automatically hides the view.
        mSpheroConnectionView.addConnectionListener(new ConnectionListener() {

            @Override
            public void onConnected(Robot robot) {
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
        });

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
                mSpheroConnectionView.setVisibility(View.VISIBLE);
                mSpheroConnectionView.startDiscovery();
            }

            @Override
            public void onSettingsClick() {
                // Open the Bluetooth Settings Intent
                Intent settingsIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                UiSampleActivity.this.startActivityForResult(settingsIntent, BLUETOOTH_SETTINGS_REQUEST);
            }
        });


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
        connectToBH();
    }

    /**
     * Called when the user presses the back or home button
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mColorPickerShowing) return;

        // Disconnect Robot properly
        if (mRobot != null) {
            mRobot.disconnect();
        }
        try {
            unregisterReceiver(mColorChangeReceiver); // many times throws exception on leak
        } catch (Exception e) {
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(mColorChangeReceiver); // many times throws exception on leak
        } catch (Exception e) {
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
                mSpheroConnectionView.setVisibility(View.VISIBLE);
                mSpheroConnectionView.startDiscovery();
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
                mSpheroConnectionView.setVisibility(View.VISIBLE);
                mSpheroConnectionView.startDiscovery();
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

    /*
    * BioHarness Section
    */


    //BH messages handler
    final Handler Newhandler = new Handler() {
        public void handleMessage(Message msg) {
            TextView txtRate;
            if (msg.what == RESPIRATION_RATE) {
                String respirationRate = msg.getData().getString("RespirationRate");
                txtRate = (TextView) findViewById(R.id.txtRate);
                if (txtRate != null) {
                    txtRate.setText(respirationRate);
                    JoystickView.USER_CURRENT_BREATH_RATE = Double.parseDouble(respirationRate);
                }
            }
        }
    };

    public void connectToBH(){
        //Sending a message to android that we are going to initiate a pairing request
        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");

        //Registering a new BTBroadcast receiver from the Main Activity context with pairing request event
        this.getApplicationContext().registerReceiver(new BTBroadcastReceiver(), filter);

        // Registering the BTBondReceiver in the application that the status of the receiver has changed to Paired
        IntentFilter filter2 = new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED");
        this.getApplicationContext().registerReceiver(new BTBondReceiver(), filter2);


        TextView txtStatusMessage = (TextView) findViewById(R.id.txtRate);
        String bhStatusMessage = "BioHarness Not Connected";
        txtStatusMessage.setText(bhStatusMessage);

        boolean hasPairedBHDevice = false;

        String BhMacID = "00:07:80:9D:8A:E8"; //Random MAC address
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices(); //Getting the paired devices. The device must be paired before

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().startsWith("BH")) { //By this, will connect with the first found.
                    BhMacID = device.getAddress();
                    hasPairedBHDevice = true;
                    break;
                }
            }
        }

        if (hasPairedBHDevice) {
            BluetoothDevice Device = btAdapter.getRemoteDevice(BhMacID);
            String DeviceName = Device.getName();
            btClient = new BTClient(btAdapter, BhMacID); //Getting the BH bluetooth client
            bhConnectedListener = new BHConnectedEventListener(Newhandler, Newhandler);

            //Setup the BH bluetooth client with the listener
            btClient.addConnectedEventListener(bhConnectedListener);

            TextView txtBreathRate = (TextView) findViewById(R.id.txtRate);


            if (btClient.IsConnected()) {
                Log.i("Checkpoint","BH Connected: " + DeviceName);
                btClient.start();
                txtBreathRate.setText("Connected"); //This will be deleted by the next measure that it's ok
                //txtStatusMessage = (TextView) findViewById(R.id.txtStatusMsg);
                //bhStatusMessage = "Connected";
                //txtStatusMessage.setText(bhStatusMessage);
                isBioHarnessConected = true;
                //Reset all the values to 0s

            } else {// --------------------------------------------------------------       MANAGE THIS CASE AND THEL TO THE USER WHAT TO DO

                txtStatusMessage = (TextView) findViewById(R.id.txtRate);
                bhStatusMessage = "Unable to Connect!";
                txtStatusMessage.setText(bhStatusMessage);
                isBioHarnessConected = false;

                //Do something like do you try again?
            }

        }else{
            //-----------------------------------------------------------------------------NO PAIRED BIOHARNESS DEVICE AVAILABLE
            // TRY TO DO THIS AT FIRST INSTANCE; BEFORE THE APPLICATION BEGINS. THE SAME With sphero
            Toast.makeText(UiSampleActivity.this, "No paired BioHarness device available", Toast.LENGTH_LONG).show();
        }
    }

    /*Auxiliar class for the pairing request*/
    private class BTBondReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            BluetoothDevice device = btAdapter.getRemoteDevice(b.get("android.bluetooth.device.extra.DEVICE").toString());
            Log.d("Bond state", "BOND_STATED = " + device.getBondState());
        }
    }

    /*Auxiliar class for the pairing request*/
    private class BTBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BTIntent", intent.getAction());
            Bundle b = intent.getExtras();
            Log.d("BTIntent", b.get("android.bluetooth.device.extra.DEVICE").toString());
            Log.d("BTIntent", b.get("android.bluetooth.device.extra.PAIRING_VARIANT").toString());
            try {
                BluetoothDevice device = btAdapter.getRemoteDevice(b.get("android.bluetooth.device.extra.DEVICE").toString());
                Method m = BluetoothDevice.class.getMethod("convertPinToBytes", new Class[] {String.class} );
                byte[] pin = (byte[])m.invoke(device, "1234");
                m = device.getClass().getMethod("setPin", new Class [] {pin.getClass()});
                Object result = m.invoke(device, pin);
                Log.d("BTTest", result.toString());
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public void disconectFromBH(){
        TextView tv = (TextView) findViewById(R.id.txtRate);
        String ErrorText  = "BioHarness not connected";
        tv.setText(ErrorText);
        //This disconnects listener from acting on received messages
        btClient.removeConnectedEventListener(bhConnectedListener);
        //Close the communication with the device & throw an exception if failure
        btClient.Close();
        isBioHarnessConected = false;
    }
}

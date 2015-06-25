/*
* Author:       Roger Fernando Solis Castilla
* Date:         06/16/2015
* Description:  Activity that connects the bioharness and do the first measure
* */

package psi.tamu.controlyourbreath;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import psi.tamu.controlyourbreath.orbotix.robot.widgets.joystick.JoystickView;
import psi.tamu.controlyourbreath.orbotix.uisample.UiSampleActivity;
import zephyr.android.BioHarnessBT.*;

public class MainActivity extends ActionBarActivity {

    long START_TIME_FOR_CHECKPOINT_TEST = 10000; //The average Breathing Rate per minute is 12-20
    long STEP_SIZE = 1000;
    int MIN_BREATH_RATE = 12;
    int MAX_BREATH_RATE = 20;
    public static double TO_DELETE_RATE = 1;
    float USER_AVERAGE_BREATH_RATE = 0;

    boolean isConectedBH = false;
    boolean isChronometerRunning = false;
    float COMMON_MIDDLE_BREATH_RATE = MIN_BREATH_RATE + ((MAX_BREATH_RATE - MIN_BREATH_RATE) / 2);

    //For breathing data
    float breathingSum = 0;
    int numberOfMeasures = 0;
    float averageBreathing = 0;

    CounterDownChronometer chronometer = null;
    TextView txtCrmt = null;
    Button btnStartCrmtr = null;

    /*BioHarness*/
    BluetoothAdapter btAdapter = null; //To work with Bluetooth
    BTClient btClient;
    BHConnectedEventListener bhConnectedListener;

    final int RESPIRATION_RATE = 0x101;

    //BH messages handler
    final Handler Newhandler = new Handler(){
        public void handleMessage(Message msg){
            TextView txtBreathRate;
            if(msg.what == RESPIRATION_RATE){
                String respirationRate = msg.getData().getString("RespirationRate");
                txtBreathRate = (TextView)findViewById(R.id.txtBreathRate);
                if (txtBreathRate != null){
                    txtBreathRate.setText(respirationRate);

                    //This is where we will updating the rate of the user for the noise decisions
                    JoystickView.USER_CURRENT_BREATH_RATE = Double.parseDouble(respirationRate);
                    TO_DELETE_RATE = Double.parseDouble(respirationRate);



                    if(Double.parseDouble(respirationRate) < MIN_BREATH_RATE || Double.parseDouble(respirationRate) > MAX_BREATH_RATE)
                        txtBreathRate.setTextColor(Color.parseColor("#AA3939"));
                    else
                        txtBreathRate.setTextColor(Color.parseColor("#9FEF00"));

                    if(isChronometerRunning){
                        numberOfMeasures++;
                        breathingSum += Double.parseDouble(respirationRate);
                    }else{//Show average
                        if(numberOfMeasures!=0) { //Avoid indeterminate division and infinite operations
                            averageBreathing = breathingSum / numberOfMeasures;
                            breathingSum = 0;
                            numberOfMeasures = 0;
                            if(averageBreathing < MIN_BREATH_RATE || averageBreathing > MAX_BREATH_RATE ){
                                Toast.makeText(MainActivity.this, "Breathing rate out of range. Try it again", Toast.LENGTH_SHORT).show();
                            }else{
                                USER_AVERAGE_BREATH_RATE = averageBreathing;
                                //New activity sphero controll
                                //Consider the average rate of this user and the common middle rate
                                /*
                                *Depending on the level of game, the average rate will be nearest to the middle common. By this way, the new averga will serve as a
                                *checkpoint. And while most far the breathing rate is from this checkpoint (with error of +- 20%, this will be the "center") the robot
                                * will be more erratic.
                                *
                                *
                                * Common middle rate for adjust the checkpoint. The new checkpoint (acording to the level), will need to be considered as checkpoint +- 20%
                                *
                                *
                                * */

                                Toast.makeText(MainActivity.this, "Average: "+ averageBreathing, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtCrmt = (TextView) findViewById(R.id.txtChronometer);
        chronometer = new CounterDownChronometer(START_TIME_FOR_CHECKPOINT_TEST,STEP_SIZE);
        btnStartCrmtr = (Button) findViewById(R.id.button);

        //connectToBH();
    }

    public void connectToBH(){
        //Sending a message to android that we are going to initiate a pairing request
        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");

        //Registering a new BTBroadcast receiver from the Main Activity context with pairing request event
        this.getApplicationContext().registerReceiver(new BTBroadcastReceiver(), filter);

        // Registering the BTBondReceiver in the application that the status of the receiver has changed to Paired
        IntentFilter filter2 = new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED");
        this.getApplicationContext().registerReceiver(new BTBondReceiver(), filter2);


        TextView txtStatusMessage = (TextView) findViewById(R.id.txtStatusMsg);
        String bhStatusMessage = "Not Connected";
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

            TextView txtBreathRate = (TextView) findViewById(R.id.txtBreathRate);
            txtBreathRate.setText("0.0");

            if (btClient.IsConnected()) {
                Log.i("Checkpoint","BH Connected: " + DeviceName);
                btClient.start();
                txtStatusMessage = (TextView) findViewById(R.id.txtStatusMsg);
                bhStatusMessage = "Connected";
                isConectedBH = true;
                txtStatusMessage.setText(bhStatusMessage);

                //Reset all the values to 0s

            } else {// --------------------------------------------------------------       MANAGE THIS CASE AND THEL TO THE USER WHAT TO DO

                txtStatusMessage = (TextView) findViewById(R.id.txtStatusMsg);
                bhStatusMessage = "Unable to Connect!";
                txtStatusMessage.setText(bhStatusMessage);
            }

        }else{
            //-----------------------------------------------------------------------------NO PAIRED BIOHARNESS DEVICE AVAILABLE
            // TRY TO DO THIS AT FIRST INSTANCE; BEFORE THE APPLICATION BEGINS. THE SAME With sphero
            Toast.makeText(MainActivity.this, "No paired BioHarness device available", Toast.LENGTH_LONG).show();
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

    @Override
    public void onResume(){
        super.onResume();
        connectToBH();
    }

    @Override
    public void onPause(){
        super.onPause();
        //if (isConectedBH)
            //disconectFromBH();
        //isConectedBH = false;
    }

    public void disconectFromBH(){
        TextView tv = (TextView) findViewById(R.id.txtStatusMsg);
        String ErrorText  = "Disconnected";
        tv.setText(ErrorText);
        //This disconnects listener from acting on received messages
        btClient.removeConnectedEventListener(bhConnectedListener);
        //Close the communication with the device & throw an exception if failure
        btClient.Close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClickStartBreathingCheckpoint(View v){
        //chronometer.start();
        //isChronometerRunning = true;
        //btnStartCrmtr.setEnabled(false);
        //Check for Zephyr BioHarness
        Intent activityLauncher = new Intent(this,UiSampleActivity.class);
        this.startActivity(activityLauncher);
    }


    public class CounterDownChronometer extends CountDownTimer {
        public CounterDownChronometer(long startCount, long countStep){
            super(startCount,countStep);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            long millis = millisUntilFinished;
            String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
            txtCrmt.setText(hms);
        }

        @Override
        public void onFinish() {
            txtCrmt.setText("DONE!");
            isChronometerRunning = false;

            btnStartCrmtr.setEnabled(true);
        }
    }

}

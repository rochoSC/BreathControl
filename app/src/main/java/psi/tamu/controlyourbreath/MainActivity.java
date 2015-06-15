package psi.tamu.controlyourbreath;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import zephyr.android.BioHarnessBT.*;

public class MainActivity extends ActionBarActivity {

    CounterDownChronometer crmt = null;
    TextView txtCrmt = null;

    /*BioHarness*/
    BluetoothAdapter adapter = null;
    BTClient _bt;
    NewConnectedListener _NConnListener;

    private final int RESPIRATION_RATE = 0x101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtCrmt = (TextView) findViewById(R.id.txtChronometer);
        crmt = new CounterDownChronometer(4000,1000);


        /*Sending a message to android that we are going to initiate a pairing request*/
        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");
        /*Registering a new BTBroadcast receiver from the Main Activity context with pairing request event*/
        this.getApplicationContext().registerReceiver(new BTBroadcastReceiver(), filter);
        // Registering the BTBondReceiver in the application that the status of the receiver has changed to Paired
        IntentFilter filter2 = new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED");
        this.getApplicationContext().registerReceiver(new BTBondReceiver(), filter2);

        //Obtaining the handle to act on the CONNECT button
        TextView tv = (TextView) findViewById(R.id.lblStatusMsg);
        String ErrorText  = "Not Connected to BioHarness!";
        tv.setText(ErrorText);

                    String BhMacID = "00:07:80:9D:8A:E8";
                    adapter = BluetoothAdapter.getDefaultAdapter();

                    Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();

                    if (pairedDevices.size() > 0)
                    {
                        for (BluetoothDevice device : pairedDevices)
                        {
                            if (device.getName().startsWith("BH"))
                            {
                                BluetoothDevice btDevice = device;
                                BhMacID = btDevice.getAddress();
                                break;

                            }
                        }


                    }

                    //BhMacID = btDevice.getAddress();
                    BluetoothDevice Device = adapter.getRemoteDevice(BhMacID);
                    String DeviceName = Device.getName();
                    _bt = new BTClient(adapter, BhMacID); //Conectandome al BIO a través de bluetooth gracias a su MAC y su adapter
                    _NConnListener = new NewConnectedListener(Newhandler,Newhandler);
                    _bt.addConnectedEventListener(_NConnListener);

                    TextView tv1 = (TextView) findViewById(R.id.lblBreathRate);
                    tv1.setText("0.0");

                    if(_bt.IsConnected())
                    {
                        _bt.start();
                        tv = (TextView) findViewById(R.id.lblStatusMsg);
                        ErrorText  = "Connected to BioHarness "+DeviceName;
                        tv.setText(ErrorText);

                        //Reset all the values to 0s

                    }
                    else
                    {
                        tv = (TextView) findViewById(R.id.lblStatusMsg);
                        ErrorText  = "Unable to Connect !";
                        tv.setText(ErrorText);
                    }



        /*Obtaining the handle to act on the DISCONNECT button*/
       /*Button btnDisconnect = (Button) findViewById(R.id.ButtonDisconnect);
        if (btnDisconnect != null)
        {
            btnDisconnect.setOnClickListener(new OnClickListener() {
                @Override
				//Functionality to act if the button DISCONNECT is touched
                public void onClick(View v) {
                    // TODO Auto-generated method stub
					//Reset the global variables
                    TextView tv = (TextView) findViewById(R.id.labelStatusMsg);
                    String ErrorText  = "Disconnected from BioHarness!";
                    tv.setText(ErrorText);

					//This disconnects listener from acting on received messages
                    _bt.removeConnectedEventListener(_NConnListener);
					//Close the communication with the device & throw an exception if failure
                    _bt.Close();

                }
            });
        }*/
    }

    private class BTBondReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            BluetoothDevice device = adapter.getRemoteDevice(b.get("android.bluetooth.device.extra.DEVICE").toString());
            Log.d("Bond state", "BOND_STATED = " + device.getBondState());
        }
    }
    private class BTBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BTIntent", intent.getAction());
            Bundle b = intent.getExtras();
            Log.d("BTIntent", b.get("android.bluetooth.device.extra.DEVICE").toString());
            Log.d("BTIntent", b.get("android.bluetooth.device.extra.PAIRING_VARIANT").toString());
            try {
                BluetoothDevice device = adapter.getRemoteDevice(b.get("android.bluetooth.device.extra.DEVICE").toString());
                Method m = BluetoothDevice.class.getMethod("convertPinToBytes", new Class[] {String.class} );
                byte[] pin = (byte[])m.invoke(device, "1234");
                m = device.getClass().getMethod("setPin", new Class [] {pin.getClass()});
                Object result = m.invoke(device, pin);
                Log.d("BTTest", result.toString());
            } catch (SecurityException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (NoSuchMethodException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    boolean a = true;

    final Handler Newhandler = new Handler(){
        public void handleMessage(Message msg)
        {
            TextView tv;
            switch (msg.what){
                case RESPIRATION_RATE:
                    String RespirationRatetext = msg.getData().getString("RespirationRate");
                    tv = (TextView)findViewById(R.id.lblBreathRate);
                    if (a){
                        a=false;
                    }
                    if (tv != null)tv.setText(RespirationRatetext);
                    break;
            }
        }
    };


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
        crmt.start();

        //Check for Zephyr BioHarness
    }

    public class CounterDownChronometer extends CountDownTimer {
        public CounterDownChronometer(long millisInFuture, long countDownInterval){
            super(millisInFuture,countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            long millis = millisUntilFinished;
            String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
            Log.i("Chronometer", "Changed to: " + hms);
            txtCrmt.setText(hms);
        }

        @Override
        public void onFinish() {
            txtCrmt.setText("DONE!");
        }
    }

}

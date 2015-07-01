package psi.tamu.controlyourbreath;

import android.app.Activity;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.lang.reflect.Method;
import java.util.Set;

import orbotix.robot.base.Robot;
import orbotix.sphero.Sphero;
import psi.tamu.controlyourbreath.orbotix.robot.widgets.joystick.JoystickPuck;
import psi.tamu.controlyourbreath.orbotix.robot.widgets.joystick.JoystickView;
import psi.tamu.controlyourbreath.orbotix.uisample.UiSampleActivity;
import zephyr.android.BioHarnessBT.BTClient;


public class MainMenuFragment extends Fragment {

    public static final String ACTION_NEW_MEASURE = "psi.tamu.controlyourbreath.BREATH_CHANGE";
    public static final String EXTRA_NEW_MEASURE = "EXTRA_MEASURE";
    private OnFragmentInteractionListener mListener;
    public View myRootView;
    public final int EASY = 1, MEDIUM = 2, HARD = 3;
    ToggleButton tBtnTurnOnBH;
    /*BioHarness*/
    BluetoothAdapter btAdapter = null; //To work with Bluetooth
    BTClient btClient;
    BHConnectedEventListener bhConnectedListener;

    final int RESPIRATION_RATE = 0x101;

    static boolean isBioHarnessConected = false;

    public MainMenuFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        myRootView = inflater.inflate(R.layout.fragment_main_menu, container, false);
        setListeners();
        return myRootView;
    }

    public void setListeners(){
        Button btn = (Button) myRootView.findViewById(R.id.btnEasy);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                launchControlActivity(EASY);
            }
        });

        btn = (Button) myRootView.findViewById(R.id.btnMedium);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                launchControlActivity(MEDIUM);
            }
        });

        btn = (Button) myRootView.findViewById(R.id.btnHard);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                launchControlActivity(HARD);
            }
        });

        /**
         * layoutBreathRate, hide if I can not connect
         *
         * */
        tBtnTurnOnBH = (ToggleButton) myRootView.findViewById(R.id.tBtnTurnOnBH);

        tBtnTurnOnBH.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Toast.makeText(getActivity(), "Connecting BH", Toast.LENGTH_SHORT).show();
                    connectToBH();
                }else{
                    if(isBioHarnessConected) {
                        Toast.makeText(getActivity(), "Disconecting BH", Toast.LENGTH_SHORT).show();
                        disconectFromBH();
                        JoystickView.USER_CURRENT_BREATH_RATE = 6; //To restore default and be able to control the robot
                    }
                }
            }
        });
    }

    public void launchControlActivity(int dificulty){

        FragmentManager manager = getFragmentManager();
        Intent activityLanuncher = new Intent(getActivity(), UiSampleActivity.class);



        switch (dificulty){
            case EASY:
                activityLanuncher.putExtra("dificulty",EASY);
                break;
            case MEDIUM:
                activityLanuncher.putExtra("dificulty",MEDIUM);
                break;
            case HARD:
                activityLanuncher.putExtra("dificulty",HARD);
                break;
        }

        startActivity(activityLanuncher);

    }
    // TODO: Rename method, update argument and hook method into UI event
    //This is for interact with the ativity
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
    }

    //BH messages handler
    final Handler Newhandler = new Handler() {
        public void handleMessage(Message msg) {
            TextView txtRate;
            if (msg.what == RESPIRATION_RATE) {
                String respirationRate = msg.getData().getString("RespirationRate");
                txtRate = (TextView) myRootView.findViewById(R.id.txtBreathRateMenu);
                if (txtRate != null) {
                    txtRate.setText(respirationRate);

                    JoystickView.USER_CURRENT_BREATH_RATE = Double.parseDouble(respirationRate);

                        Intent intent = new Intent(ACTION_NEW_MEASURE);
                        intent.putExtra(EXTRA_NEW_MEASURE, respirationRate);
                        getActivity().sendBroadcast(intent);
                }
            }
        }
    };

    boolean hasPairedBHDevice;
    String BhMacID = "00:07:80:9D:8A:E8"; //Random MAC address
    TextView txtStatusMessage;
    String bhStatusMessage;
    public void connectToBH(){
        //Sending a message to android that we are going to initiate a pairing request
        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");

        //Registering a new BTBroadcast receiver from the Main Activity context with pairing request event
        this.getActivity().getApplicationContext().registerReceiver(new BTBroadcastReceiver(), filter);

        // Registering the BTBondReceiver in the application that the status of the receiver has changed to Paired
        IntentFilter filter2 = new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED");
        this.getActivity().getApplicationContext().registerReceiver(new BTBondReceiver(), filter2);


        txtStatusMessage = (TextView) myRootView.findViewById(R.id.txtBreathRateMenu);
         bhStatusMessage = "Not Connected";
        txtStatusMessage.setText(bhStatusMessage);

        hasPairedBHDevice = false;


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

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (hasPairedBHDevice) {
                    BluetoothDevice Device = btAdapter.getRemoteDevice(BhMacID);
                    String DeviceName = Device.getName();
                    btClient = new BTClient(btAdapter, BhMacID); //Getting the BH bluetooth client
                    bhConnectedListener = new BHConnectedEventListener(Newhandler, Newhandler);

                    //Setup the BH bluetooth client with the listener
                    btClient.addConnectedEventListener(bhConnectedListener);

                    //TextView txtBreathRate = (TextView) myRootView.findViewById(R.id.txtBreathRateMenu);


                    if (btClient.IsConnected()) {
                        Log.i("Checkpoint","BH Connected: " + DeviceName);
                        btClient.start();
                        //txtBreathRate.setText("Connected"); //This will be deleted by the next measure that it's ok

                        isBioHarnessConected = true;
                        //Reset all the values to 0s

                    } else {// --------------------------------------------------------------       MANAGE THIS CASE AND THEL TO THE USER WHAT TO DO

                        //txtStatusMessage = (TextView) myRootView.findViewById(R.id.txtBreathRateMenu);
                        //bhStatusMessage = "Unable to Connect!";
                        //txtStatusMessage.setText(bhStatusMessage);
                        isBioHarnessConected = false;

                        //Do something like do you try again?
                    }

                }else{
                    //-----------------------------------------------------------------------------NO PAIRED BIOHARNESS DEVICE AVAILABLE
                    // TRY TO DO THIS AT FIRST INSTANCE; BEFORE THE APPLICATION BEGINS. THE SAME With sphero
                    //Toast.makeText(UiSampleActivity.this, "No BioHarness device connected", Toast.LENGTH_LONG).show();
                    isBioHarnessConected = false;

                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onThreadFinish();
                    }
                });
            }
        }).start();

    }
    public void onThreadFinish(){
        txtStatusMessage = (TextView) myRootView.findViewById(R.id.txtBreathRateMenu);

        if(isBioHarnessConected){
            bhStatusMessage = "Connected";
            tBtnTurnOnBH.setChecked(true);
        }else{
            bhStatusMessage = "Not Connected";
            tBtnTurnOnBH.setChecked(false);
        }

        txtStatusMessage.setText(bhStatusMessage);
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
        TextView tv = (TextView) myRootView.findViewById(R.id.txtBreathRateMenu);
        String ErrorText  = "Not connected";
        tv.setText(ErrorText);
        //This disconnects listener from acting on received messages
        btClient.removeConnectedEventListener(bhConnectedListener);
        //Close the communication with the device & throw an exception if failure
        btClient.Close();
        isBioHarnessConected = false;
    }


}

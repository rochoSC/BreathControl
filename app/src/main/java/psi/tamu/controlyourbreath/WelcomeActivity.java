package psi.tamu.controlyourbreath;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import orbotix.robot.base.Robot;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.Sphero;
import orbotix.view.connection.SpheroConnectionView;
import psi.tamu.controlyourbreath.orbotix.uisample.UiSampleActivity;

public class WelcomeActivity extends ActionBarActivity implements TutorialFragment.OnFragmentInteractionListener, MainMenuFragment.OnFragmentInteractionListener {

    private SpheroConnectionView mSpheroConnectionView;
    public static Sphero mRobot;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);


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
               //////////////////////////////////////////////////////////////////////////////// setRobot(mRobot);
                // Make sure you let the calibration view knows the robot it should control
                ///////////////////////////////////////////////////////////////////mCalibrationView.setRobot(mRobot);
                mRobot.setColor(0, 255, 0);


                // Make connect sphero pop-up invisible if it was previously up
                //mNoSpheroConnectedView.setVisibility(View.GONE);
                //mNoSpheroConnectedView.switchToConnectButton();
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
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_welcome, menu);
        return true;
    }

    public void onClickAnyWhere(View v){
       // FragmentManager manager = getFragmentManager();
       // Intent activityLanuncher = new Intent(this, UiSampleActivity.class);
        //startActivity(activityLanuncher);

        if(mRobot!=null) {
            MainMenuFragment fragment = new MainMenuFragment();
            //fragment.setSpheroRobot(mRobot);
            UiSampleActivity.mRobot = this.mRobot;
            fragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(android.R.id.content, fragment).addToBackStack("mainmenu_fragment").commit();
        }
    }

    public void onClickTutorial(View v){
        TutorialFragment fragment = new TutorialFragment();
        fragment.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().add(android.R.id.content, fragment).addToBackStack("fragment_tutorial").commit();

    }

    public void onClickRefresh(View v){
        if(mSpheroConnectionView != null)
            mSpheroConnectionView.startDiscovery();
    }
    @Override
    public void onFragmentInteraction(Uri uri) {

    }



    /*@Override
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
    }*/

    @Override
    public void onBackPressed() {

        int count = getFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();
            //additional code
        } else {
            getFragmentManager().popBackStack();
        }

    }
}

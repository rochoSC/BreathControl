package psi.tamu.controlyourbreath;

import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import orbotix.robot.base.Robot;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.Sphero;
import orbotix.view.connection.SpheroConnectionView;
import psi.tamu.controlyourbreath.orbotix.controlui.MainControlUI;

/**
 * @author Roger Solis
 * First activity.<p>
 * This activity provides a list of the Sphero robots paired to the device and available to connect.
 * This uses tools provided by robotix examples as basis.
 */
public class WelcomeActivity extends ActionBarActivity implements MainMenuFragment.OnFragmentInteractionListener {

    private SpheroConnectionView mSpheroConnectionView;
    public static Sphero mRobot;
    public Button btnRefresh;

    /**
     * First called.
     * Here, we prepare the Sphero connection listener and we display the available robots.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Set up the Sphero Connection View
        mSpheroConnectionView = (SpheroConnectionView) findViewById(R.id.sphero_connection_view);
        //When we get the view, instantly launches "StartDiscovery" and shows the view
        //When a connection is accepted, automatically hides the view.
        mSpheroConnectionView.addConnectionListener(new ConnectionListener() {

            @Override
            public void onConnected(Robot robot) {
                // Set Robot
                mRobot = (Sphero) robot; // safe to cast for now
                mRobot.setColor(0, 0, 255);
                btnRefresh = (Button) findViewById(R.id.btnRefresh);

                if (mRobot != null && mRobot.isConnected())
                    btnRefresh.setVisibility(View.GONE);

            }

            @Override
            public void onConnectionFailed(Robot sphero) {
                //Do nothing
            }

            @Override
            public void onDisconnected(Robot sphero) {
                mSpheroConnectionView.startDiscovery();
            }
        });
    }

    public void onClickAnyWhere(View v) {
        if (mRobot != null) { //If a robot is connected we can continue to the main menu
            MainMenuFragment fragment = new MainMenuFragment();
            MainControlUI.mRobot = this.mRobot; //Setting the static variable to be able to use the robot
            fragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(android.R.id.content, fragment).addToBackStack("mainmenu_fragment").commit();
        }
    }

    public void onClickRefresh(View v) {
        //If we have instanced the connection view we can restart a discovery
        if (mSpheroConnectionView != null)
            mSpheroConnectionView.startDiscovery();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        //Necessary for the fragment
    }

    /**
     * When the user returns to this activity the connection view is showed again if the robot
     * is not connected.
     */
    @Override
    public void onResume() {
        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        super.onResume();
        if (mRobot != null && mRobot.isConnected()) {
            btnRefresh.setVisibility(View.GONE);
        } else {
            btnRefresh.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Configuring the stack of activities to make possible tu return when back button is pressed on fragments
     */
    @Override
    public void onBackPressed() {

        int count = getFragmentManager().getBackStackEntryCount();

        // Disconnect Robot properly just because we are the main activity. This means that we have closed the app
        if (mRobot != null)
            mRobot.disconnect();

        //Fragment managment
        if (count == 0) {
            super.onBackPressed();
        } else {
            getFragmentManager().popBackStack();
        }

    }


}

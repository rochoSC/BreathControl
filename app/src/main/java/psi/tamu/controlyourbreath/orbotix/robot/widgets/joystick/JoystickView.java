package psi.tamu.controlyourbreath.orbotix.robot.widgets.joystick;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import orbotix.robot.base.DriveControl;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RotationRateCommand;
import psi.tamu.controlyourbreath.R;
import orbotix.view.calibration.Controller;

/**
 * View that displays the Joystick, and handles the user's interactions with it.
 * In addition, allows the noise application acording to the BioHarness measures
 * <p/>
 * Common view author: Adam Williams. View in the UIExample provided by orbotix.
 * Integration with BioHarness project author: Roger Fernando Solis Castilla
 */
public class JoystickView extends View implements Controller {


    private final JoystickPuck puck;
    private final JoystickWheel wheel;

    //Parameters added by Roger
    public final float MAX_SCALED_SPEED = 1;

    public final int EASY = 1;
    public final double EASY_BASE_SPEED = MAX_SCALED_SPEED * .25;

    public final int MEDIUM = 2;
    public final double MEDIUM_BASE_SPEED = MAX_SCALED_SPEED * .45;

    public final int HARD = 3;
    public final double HARD_BASE_SPEED = MAX_SCALED_SPEED * .80;

    public int ACTUAL_DIFICULTY = this.EASY; //The actual level of the game

    public static double MAX_BREATH_RATE = 20; //This acts like the limit of the worst breathing rate for the user.
    public static double MAX_IDEAL_BREATH_RATE = 8; //This acts like up limit of the best breathing rate for the user.
    public static double USER_CURRENT_BREATH_RATE = 6; //This will be updated by BIOHarness

    //End of game parameters

    private int puck_radius = 25;
    private int wheel_radius = 150;


    private int puck_edge_overlap = 30;

    private final Point center_point = new Point();

    private boolean mEnabled = true;
    private volatile boolean draggingPuck = false;
    private int draggingPuckPointerId;

    private Robot robot = null;
    private DriveControl drive_control = DriveControl.INSTANCE; //This helps to control the robot with a joystick in a simple way

    private float speed = 0.8f;
    private float rotation = .7f;

    private Runnable mOnStartRunnable;
    private Runnable mOnDragRunnable;
    private Runnable mOnEndRunnable;

    public boolean isNoiseEnabled;

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.puck = new JoystickPuck();
        this.wheel = new JoystickWheel();


        if (attrs != null) {
            TypedArray a = this.getContext().obtainStyledAttributes(attrs, R.styleable.JoystickView);

            //Get puck size
            this.puck_radius = (int) a.getDimension(R.styleable.JoystickView_puck_radius, 25);

            //Get alpha

            this.setAlpha(a.getFloat(R.styleable.JoystickView_alpha, 255));

            //Get edge overlap
            this.puck_edge_overlap = (int) a.getDimension(R.styleable.JoystickView_edge_overlap, 10);


        }


    }


    public void setDificulty(int dificulty) {
        this.ACTUAL_DIFICULTY = dificulty;
    }

    public void setAlpha(float alpha) {
        alpha = (alpha > 1) ? 1 : alpha;
        alpha = (alpha < 0) ? 0 : alpha;

        alpha = (255 * alpha);

        this.puck.setAlpha((int) alpha);
        this.wheel.setAlpha((int) alpha);
    }

    /*Added by roger*/
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /*Added by roger*/
    public void setDriveCoords(int x, int y) {
        this.drive_control.driveJoyStick(x, y);
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;

        if (this.robot != null) {
            RotationRateCommand.sendCommand(this.robot, rotation);
        }
    }

    /**
     * Sets the radius of the puck to the provided radius, in pixels
     *
     * @param radius
     */
    public void setPuckRadius(int radius) {
        this.puck_radius = radius;

        this.puck.setRadius(radius);
    }

    /**
     * Resets the puck's position to the middle of the wheel
     */
    public void resetPuck() {
        this.puck.setPosition(new Point(this.center_point));
    }

    public void setRobot(Robot robot) {
        this.robot = robot;
    }

    public void setOnStartRunnable(Runnable runnable) {
        mOnStartRunnable = runnable;
    }

    public void setOnDragRunnable(Runnable runnable) {
        mOnDragRunnable = runnable;
    }

    public void setOnEndRunnable(Runnable runnable) {
        mOnEndRunnable = runnable;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {

        this.center_point.set(this.getMeasuredWidth() / 2, this.getMeasuredHeight() / 2);

        if (this.getMeasuredWidth() > this.getMeasuredHeight()) {
            this.wheel_radius = (this.getMeasuredWidth() / 2) - this.puck_edge_overlap - 2;
        } else {
            this.wheel_radius = (this.getMeasuredHeight() / 2) - this.puck_edge_overlap - 2;
        }

        //Check that the puck and wheel are within reasonable limits
        this.wheel_radius = (this.wheel_radius < 3) ? 3 : this.wheel_radius;
        this.puck_radius = (this.puck_radius < this.wheel_radius) ? this.puck_radius : (this.wheel_radius / 3);

        this.wheel.setRadius(this.wheel_radius);
        this.setPuckRadius(this.puck_radius);

        this.wheel.setPosition(this.center_point);
        this.puck.setPosition(this.center_point);
        DriveControl.INSTANCE.setJoyStickPadSize(this.wheel.getBounds().width(), this.wheel.getBounds().height());
    }

    @Override
    public void onDraw(Canvas canvas) {

        this.wheel.draw(canvas);
        this.puck.draw(canvas);

    }

    /**
     * Indicates whether the user is currently dragging the puck
     *
     * @return True, if so
     */
    public boolean getIsDraggingPuck() {
        return draggingPuck;
    }

    /**
     * From a provided current_position Point, returns an Point that contains
     * coordinates that are within the area of the puck wheel.
     *
     * @param current_position
     * @return an Point containing the puck's position
     */
    private Point getValidPuckPosition(Point current_position) {

        Point pointer = new Point(current_position);
        Point wheel_center = this.wheel.getPosition();
        Point adj_pointer = new Point(pointer);

        //Set the puck position to within the bounds of the wheel
        if (pointer.x != wheel_center.x || pointer.y != wheel_center.y) {

            //reset the drive coords to be the zeroed pointer coords
            adj_pointer.set(pointer.x, pointer.y);

            //Use the wheel center to zero the pointer coords
            adj_pointer.x = adj_pointer.x - wheel_center.x;
            adj_pointer.y = adj_pointer.y - wheel_center.y;

            double a = Math.abs(adj_pointer.y);
            double b = Math.abs(adj_pointer.x);

            double hyp = Math.hypot(a, b);

            final double radius = (wheel_radius - (puck_radius - puck_edge_overlap));

            if (hyp > radius) {
                final double factor = radius / hyp;

                pointer.x = (int) (adj_pointer.x * factor) + wheel_center.x;
                pointer.y = (int) (adj_pointer.y * factor) + wheel_center.y;
            }
        }

        return pointer;
    }

    /**
     * From a provided Point containing the puck's current position, returns an Point containing
     * a valid coordinate for use with the DriveControl's joystick area.
     *
     * @param current_position
     * @return an Point containing the clipped coordinates
     */
    public Point getDrivePuckPosition(Point current_position) {

        Point drive_coord = new Point(current_position);
        Rect bounds = this.wheel.getBounds();

        drive_coord.x = drive_coord.x - bounds.left;
        drive_coord.y = drive_coord.y - bounds.top;

        if (drive_coord.x < 0) {
            drive_coord.x = 0;
        } else if (drive_coord.x > bounds.width()) {
            drive_coord.x = bounds.width();
        }

        if (drive_coord.y < 0) {
            drive_coord.y = 0;
        } else if (drive_coord.y > bounds.height()) {
            drive_coord.y = bounds.height();
        }

        return drive_coord;
    }

    /**
     * Gets a point corrected for this Views position
     *
     * @param p a Point to correct
     * @return a Point, corrected for the View's position
     */
    private Point getCorrectedPoint(Point p) {

        final Point ret = new Point(p.x, p.y);

        ret.x -= getLeft();
        ret.y -= getTop();

        return ret;
    }

    public boolean isUserControlAct = true;
    public boolean flag = false;
    public boolean isInvertedControls = false;
    public boolean isUncontrolledActivated = false;

    @Override
    public void interpretMotionEvent(MotionEvent event) {

        boolean handled = false;

        int pointer_Point = event.getActionIndex();
        int pointer_id = event.getPointerId(pointer_Point);
        //GLOBAL_POINT_USAGE = new Point((int)event.getX(), (int)event.getY()); //ADDED
        final Point global_point = new Point((int) event.getX(), (int) event.getY());
        final Point local_point = getCorrectedPoint(global_point);
        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                if (mEnabled) {
                    if (this.robot != null && this.robot.isConnected()) {

                        if (puck.getBounds().contains(local_point.x, local_point.y)) {
                            draggingPuck = true;
                            draggingPuckPointerId = pointer_id;
                            handled = true;

                            switch (ACTUAL_DIFICULTY) {
                                case EASY:
                                    this.drive_control.setSpeedScale(this.EASY_BASE_SPEED);
                                    break;
                                case MEDIUM:
                                    this.drive_control.setSpeedScale(this.MEDIUM_BASE_SPEED);
                                    break;
                                case HARD:
                                    this.drive_control.setSpeedScale(this.HARD_BASE_SPEED);
                                    break;
                                default:
                                    this.drive_control.setSpeedScale(this.speed);
                                    break;
                            }


                            this.drive_control.startDriving(this.getContext(), DriveControl.JOY_STICK);

                            if (isNoiseEnabled)
                                if (!isUncontrolledActivated) {
                                    activateUncontrolledMoves();
                                    isUncontrolledActivated = true;
                                }
                            /*new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(3000);
                                        isUserControlAct = false;

                                    } catch (InterruptedException e) {
                                    }
                                }
                            }).start();*/
                        }

                        if (mOnStartRunnable != null) {
                            mOnStartRunnable.run();
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mEnabled && draggingPuck && draggingPuckPointerId == pointer_id) {


                    //Adjust drive coordinates for driving
                    final Point drive_coord = this.getDrivePuckPosition(local_point);
                    actualPoint = drive_coord;
                    //Returns an alteratedCoord if breath is out of range
                    final int maxX = this.wheel.getBounds().width(); //This represent the max X possible coord on the wheel
                    final int maxY = this.wheel.getBounds().height();
                    if (isUserControlAct) {//To leave total control to the uncontroled moves
                        Point alteratedCoord;
                        if (!isNoiseEnabled) {//If the user decided to not be affected by noise.
                            this.drive_control.driveJoyStick(drive_coord.x, drive_coord.y);
                        } else {
                            if (isInvertedControls) {
                                alteratedCoord = uncInvertedDriving(drive_coord.x, drive_coord.y, maxX, maxY);
                            } else {
                                alteratedCoord = this.getAlteredCoord(drive_coord);
                            }
                            this.drive_control.driveJoyStick(alteratedCoord.x, alteratedCoord.y);
                        }
                    }
                /*else{//Another type of feedback is already happening.
                    if(!flag) {
                        //uncBoost(drive_coord.x, drive_coord.y, maxX, maxY);
                        //uncStop();
                        //uncBackwardBoost(drive_coord.x, drive_coord.y, maxX, maxY);v
                        uncRoll(maxX,maxY);
                        flag = true;
                    }
                }*/

                    //this.drive_control.driveJoyStick(drive_coord.x , drive_coord.y);
                    //Set the puck position to within the bounds of the wheel
                    final Point i = getValidPuckPosition(local_point);
                    local_point.set(i.x, i.y);
                    this.puck.setPosition(new Point(local_point.x, local_point.y));
                    this.invalidate();

                    if (mOnDragRunnable != null) {
                        mOnDragRunnable.run();
                    }

                    handled = true;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (draggingPuck && draggingPuckPointerId == pointer_id) {
                    this.resetPuck();
                    invalidate();

                    draggingPuck = false;
                    handled = true;

                    if (isNoiseEnabled) {
                        //Will only stop if the current rate don't exceed the ideal rate
                        if (USER_CURRENT_BREATH_RATE <= MAX_IDEAL_BREATH_RATE) {
                            this.drive_control.stopDriving();
                        } else {
                            if (ACTUAL_DIFICULTY == EASY) {//In other case, only if is in the easy mode
                                this.drive_control.stopDriving();
                            } else if (ACTUAL_DIFICULTY == MEDIUM && USER_CURRENT_BREATH_RATE <= (MAX_IDEAL_BREATH_RATE + 4)) {
                                //In this case, the robot will only stop if the user is under MAX_IDEAL_BREATH_RATE + 4
                                this.drive_control.stopDriving();
                            }
                        }
                    } else {
                        this.drive_control.stopDriving();
                    }
                    isUncontrolledActivated = false;

                    //In any case, if you return or close the app, the robot will stop instantly

                    if (mOnEndRunnable != null) {
                        mOnEndRunnable.run();
                    }
                }
                break;

            default:
                break;
        }
    }

    public Point actualPoint;

    public void activateUncontrolledMoves() {
        /*
        This will be a thread where each random amount of seconds will result into a random
        movement with intensity according to the game level and the level of breathing rate.
        */

        new Thread(new Runnable() {
            @Override
            public void run() {
                actualPoint = new Point();
                actualPoint.x = 0;
                actualPoint.y = 0;
                final int maxX = wheel.getBounds().width(); //This represent the max X possible coord on the wheel
                final int maxY = wheel.getBounds().height();
                int timeBtwnMvmnts = 5000;
                while (true) {

                    try {
                        Thread.sleep(timeBtwnMvmnts);//
                    } catch (InterruptedException e) {

                    }
                    if (isUncontrolledActivated) {


                        double userCurrentSnap = USER_CURRENT_BREATH_RATE;

                        //Consider 6-8, MAX_IDEAL_BREATH_RATE (8) will be the max
                        double referenceDistance = MAX_BREATH_RATE - MAX_IDEAL_BREATH_RATE;
                        double myRateExcess = userCurrentSnap - MAX_IDEAL_BREATH_RATE;
                        double relationPreferedActual = myRateExcess / referenceDistance;
                        double percentOfExcess = relationPreferedActual * 100;

                        Random r = new Random();
                        if (percentOfExcess < 40 && percentOfExcess > 10) { //A little error margin to make it easier
                            //Boost, stop, roll , inverted. probabilities (70,16,10,4)
                            int diceRolled = r.nextInt(100); //This works as the "probability"
                            if (diceRolled < 4) {// 4% inverted
                                /*Enable inverted controlling. Determine how much time is this going to last.
                                Not more than the time between each movement.*/
                                final int duration = new Random().nextInt(timeBtwnMvmnts - 1000) + 1000;
                                invertControls(duration);
                            } else if (diceRolled >= 4 && diceRolled < 14) { // 10% roll
                                uncRoll(maxX, maxY);
                            } else if (diceRolled >= 14 && diceRolled < 30) { // 16% stop
                                uncStop();
                            } else if (diceRolled >= 30 && diceRolled < 100) { // 70% boost
                                int whichBoost = new Random().nextInt(1);
                                if (whichBoost == 0)
                                    uncBackwardBoost(actualPoint.x, actualPoint.y, maxX, maxY);
                                else
                                    uncForwardBoost(actualPoint.x, actualPoint.y, maxX, maxY);
                            }
                            timeBtwnMvmnts = 8000;
                        } else if (percentOfExcess < 60 && percentOfExcess > 10) {
                            //Boost, stop, roll , inverted. probabilities (45,30,15,10)
                            int diceRolled = r.nextInt(100);
                            if (diceRolled < 10) {// 10% inverted
                                final int duration = new Random().nextInt(timeBtwnMvmnts - 1000) + 1000;
                                invertControls(duration);
                            } else if (diceRolled >= 10 && diceRolled < 25) { // 15% roll
                                uncRoll(maxX, maxY);
                            } else if (diceRolled >= 25 && diceRolled < 55) { // 30% stop
                                uncStop();
                            } else if (diceRolled >= 55 && diceRolled < 100) { // 45% boost
                                int whichBoost = new Random().nextInt(1);
                                if (whichBoost == 0)
                                    uncBackwardBoost(actualPoint.x, actualPoint.y, maxX, maxY);
                                else
                                    uncForwardBoost(actualPoint.x, actualPoint.y, maxX, maxY);
                            }
                            timeBtwnMvmnts = 6700;
                        } else if (percentOfExcess > 10) {//Percent > 60
                            //Boost, stop, roll , inverted. probabilities (5,10,25,60)
                            int diceRolled = r.nextInt(100);
                            if (diceRolled < 60) {// 60% inverted
                                final int duration = new Random().nextInt(timeBtwnMvmnts - 1000) + 1000;
                                invertControls(duration);
                            } else if (diceRolled >= 60 && diceRolled < 85) { // 25% roll
                                uncRoll(maxX, maxY);
                            } else if (diceRolled >= 85 && diceRolled < 95) { // 10% stop
                                uncStop();
                            } else if (diceRolled >= 95 && diceRolled < 100) { // 5% boost
                                int whichBoost = new Random().nextInt(1);
                                if (whichBoost == 0)
                                    uncBackwardBoost(actualPoint.x, actualPoint.y, maxX, maxY);
                                else
                                    uncForwardBoost(actualPoint.x, actualPoint.y, maxX, maxY);
                            }
                            timeBtwnMvmnts = 5000;
                        }

                    } else {
                        break;
                    }
                }
            }
        }).start();
    }

    //Uncontrolled feedback

    //Weak ones (Low BR)
    public void uncForwardBoost(final int x, final int y, final int maxX, final int maxY) {
        //First, determine in which quadrant is the puck. Remember, here with the driver there is no negative values to the drive coords


        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                int centerX = maxX / 2;
                int centerY = maxY / 2;
                int finalX = 0, finalY = 0;
                int time = 1;
                if (ACTUAL_DIFICULTY == EASY)
                    time = 1;
                if (ACTUAL_DIFICULTY == MEDIUM)
                    time = 2;
                if (ACTUAL_DIFICULTY == HARD)
                    time = 3;

                if (x >= centerX && y < centerY) {//Quadrant 1. x btw 130 and 300. y btw 0 and 130
                    finalX = maxX;
                    finalY = 0;
                } else if (x < centerX && y < centerY) { //Quadrant 2.
                    finalX = 0;
                    finalY = 0;
                } else if (x < centerX && y >= centerY) { //Quadrant 3.
                    finalX = 0;
                    finalY = maxY;
                } else if (x >= centerX && y >= centerY) { //Quadrant 4.
                    finalX = maxX;
                    finalY = maxY;
                }
                while (count < time) {//Time of boost

                    drive_control.stopDriving();
                    drive_control.setSpeedScale(MAX_SCALED_SPEED);
                    drive_control.startDriving(getContext(), DriveControl.JOY_STICK);
                    drive_control.driveJoyStick(finalX, finalY);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    count++;
                }
                restoreValues();
            }
        }).start();
    }

    public void uncBackwardBoost(final int x, final int y, final int maxX, final int maxY) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                int centerX = maxX / 2;
                int centerY = maxY / 2;
                int finalX = 0, finalY = 0;
                int time = 3;
                if (ACTUAL_DIFICULTY == EASY)
                    time = 3;
                if (ACTUAL_DIFICULTY == MEDIUM)
                    time = 4;
                if (ACTUAL_DIFICULTY == HARD)
                    time = 6;

                if (x >= centerX && y < centerY) {//Quadrant 1. x btw 155 and 310. y btw 0 and 155
                    //Go to 3
                    finalX = 0;
                    finalY = maxY;
                } else if (x < centerX && y < centerY) { //Quadrant 2.
                    //Go to 4
                    finalX = maxX;
                    finalY = maxY;
                } else if (x < centerX && y >= centerY) { //Quadrant 3.
                    //Go to 1
                    finalX = maxX;
                    finalY = 0;
                } else if (x >= centerX && y >= centerY) { //Quadrant 4.
                    //Go to 2
                    finalX = 0;
                    finalY = 0;
                }
                while (count < time) {

                    drive_control.stopDriving();
                    drive_control.setSpeedScale(MAX_SCALED_SPEED);
                    drive_control.startDriving(getContext(), DriveControl.JOY_STICK);
                    drive_control.driveJoyStick(finalX, finalY);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    count++;
                }
                restoreValues();
            }
        }).start();
    }

    //Medium (Medium BR)
    public void uncStop() {
        int time = 7;
        if (ACTUAL_DIFICULTY == EASY)
            time = 7;
        if (ACTUAL_DIFICULTY == MEDIUM)
            time = 12;
        if (ACTUAL_DIFICULTY == HARD)
            time = 16;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                while (count < 15) {
                    drive_control.stopDriving();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {

                    }
                    count++;
                }
                restoreValues();
            }
        }).start();
    }

    //Strong ones (High BR)
    public Point uncInvertedDriving(final int x, final int y, final int maxX, final int maxY) {
        Point p = new Point();
        int centerX = maxX / 2;
        int centerY = maxY / 2;
        int finalX = 0, finalY = 0;

        if (x >= centerX && y < centerY) {//Quadrant 1. x btw 155 and 310. y btw 0 and 155
            //Go to 3
            finalX = maxX - x;
            finalY = maxY - y;
        } else if (x < centerX && y < centerY) { //Quadrant 2.
            //Go to 4
            finalX = maxX - x;
            finalY = maxY - y;
        } else if (x < centerX && y >= centerY) { //Quadrant 3.
            //Go to 1
            finalX = maxX - x;
            finalY = maxY - y;
        } else if (x >= centerX && y >= centerY) { //Quadrant 4.
            //Go to 2
            finalX = maxX - x;
            finalY = maxY - y;
        }
        p.x = finalX;
        p.y = finalY;
        return p;
    }

    public void uncRoll(final int maxX, final int maxY) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int time = 700;

                drive_control.stopDriving();
                drive_control.setSpeedScale(EASY_BASE_SPEED + .05);
                drive_control.startDriving(getContext(), DriveControl.JOY_STICK);

                drive_control.driveJoyStick(maxX, 0);
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                }

                drive_control.driveJoyStick(0, 0);
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                }

                drive_control.driveJoyStick(0, maxY);
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                }

                drive_control.driveJoyStick(maxX, maxY);
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                }
                restoreValues();
            }
        }).start();
    }

    public void restoreValues() {
        drive_control.stopDriving();
        if (ACTUAL_DIFICULTY == EASY)
            drive_control.setSpeedScale(EASY_BASE_SPEED);
        if (ACTUAL_DIFICULTY == MEDIUM)
            drive_control.setSpeedScale(MEDIUM);
        if (ACTUAL_DIFICULTY == HARD)
            drive_control.setSpeedScale(HARD);
        drive_control.startDriving(getContext(), DriveControl.JOY_STICK);
        isUserControlAct = true;
    }

    public void invertControls(final int duration) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                isInvertedControls = true;
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {

                }
                isInvertedControls = false;
            }
        }).start();
    }
    //End of uncontrolled feedback

    //If breath rate is out of range, returns an altered coordinate according to the level of out of range and
    //the current game level (Easy, Medium, Hard). If breath is in range, return the same coord.
    public Point getAlteredCoord(Point actualPoint) {
        Point alteratedPoint = actualPoint;

        Random randomNoise = new Random();

        //As USER_CURRENT_BREATH_RATE will be changing, we must do a "snapshot" at this time to work with proper measures.
        double userCurrentSnap = USER_CURRENT_BREATH_RATE;

        //Consider 6-8, MAX_IDEAL_BREATH_RATE (8) will be the max
        double referenceDistance = MAX_BREATH_RATE - MAX_IDEAL_BREATH_RATE;
        double myRateExcess = userCurrentSnap - MAX_IDEAL_BREATH_RATE;
        double relationPreferedActual = myRateExcess / referenceDistance;

        //How much far of the ideal is?, according to our limit of 20
        double percentOfExcess = relationPreferedActual * 100;

        /*
        EASY:
            Speed:                      25%
            Range of speed change:      (up to 15%)
            Maximum amount of noise:    +/- 30% of the maximum number on coord x or y
        MEDIUM:
            Speed:                      45%
            Range of speed change:      (up to 15%)
            Maximum amount of noise:    +/- 50% of the maximum number on coord x or y
        HARD:
            Speed:                      80%
            Range of speed change:      (up to 15%)
            Maximum amount of noise:    +/- 80% of the maximum number on coord x or y
         */

        //Maximum increment of speed in 15% of MAX_SCALED_SPEED
        double mxSpdInc = MAX_SCALED_SPEED * .15;

        //NOTE: The formulas obtained were based on x^2
        /*
        *Solving y=ax^2 + b
        *Finding the variables a, and b. X represents the amount of excess and Y represents the speed for that amount
        */
        double a, b;
        double maxSpeed; //Max space in case of 100% of
        double easyNoise = .38, mediumNoise = .58, hardNoise = .75;

        //drive_control works acording to the size of the wheel. And as closer to the circumference as faster that will be.
        int maxX = this.wheel.getBounds().width(); //This represent the max X possible coord on the wheel
        int maxY = this.wheel.getBounds().height();

        if (percentOfExcess > 0) {
            switch (this.ACTUAL_DIFICULTY) {
                case EASY:

                    //Alter speed. The speed will be changed
                    maxSpeed = EASY_BASE_SPEED + mxSpdInc;
                    b = EASY_BASE_SPEED; //Minimum
                    a = (maxSpeed - b) / (100 * 100); //Maximum
                    this.drive_control.setSpeedScale((float) getSpeedAccordingExcessPercent(a, b, percentOfExcess));

                    alteratedPoint = getFinalPoint(maxX, maxY, easyNoise, percentOfExcess, actualPoint.x, actualPoint.y);

                    break;
                case MEDIUM:
                    maxSpeed = MEDIUM_BASE_SPEED + mxSpdInc;
                    b = MEDIUM_BASE_SPEED; //Minimum
                    a = (maxSpeed - b) / (100 * 100); //Maximum
                    this.drive_control.setSpeedScale((float) getSpeedAccordingExcessPercent(a, b, percentOfExcess));
                    alteratedPoint = getFinalPoint(maxX, maxY, mediumNoise, percentOfExcess, actualPoint.x, actualPoint.y);


                    break;
                case HARD:
                    maxSpeed = HARD_BASE_SPEED + mxSpdInc;
                    b = HARD_BASE_SPEED; //Minimum
                    a = (maxSpeed - b) / (100 * 100); //Maximum
                    this.drive_control.setSpeedScale((float) getSpeedAccordingExcessPercent(a, b, percentOfExcess));
                    alteratedPoint = getFinalPoint(maxX, maxY, hardNoise, percentOfExcess, actualPoint.x, actualPoint.y);


                    break;
            }
        } else {//Else, the user's breath rate must be under 8 so it's ok.
            //In this case, we will ensure that the speed it's acording to the level.
            switch (this.ACTUAL_DIFICULTY) {
                case EASY:
                    this.drive_control.setSpeedScale(this.EASY_BASE_SPEED);
                    break;
                case MEDIUM:
                    this.drive_control.setSpeedScale(this.MEDIUM_BASE_SPEED);
                    break;
                case HARD:
                    this.drive_control.setSpeedScale(this.HARD_BASE_SPEED);
                    break;
            }
        }
        return alteratedPoint;
    }

    public Point getFinalPoint(int maxX, int maxY, double noise, double percentOfExcess, int x, int y) {
        Point alteredPoint = new Point();
        //Alter coords
        double maxXRange = maxX * noise; //Now we have the max number that could be added to the actual x coord. The range will be composed of 0 - maxRange
        double maxYRange = maxY * noise;
        //double maxXRange = maxX /2 * easyNoise; //Now we have the max number that could be added to the actual x coord. The range will be composed of 0 - maxRange
        //double maxYRange = maxY /2 * easyNoise;
        //Lets get the formula for determine which value between 0 and RANGE would be added acording to the level
        //Cero for 0% of excess, maxRange for 100% of excess
        //Solve y=ax^2

        double a_x = maxXRange / (100 * 100);
        double a_y = maxYRange / (100 * 100);

        //Then, lets look for a random number with the help of the number that the formula throw in this kind of range: -6 form < form < + 6  form
        //By this way, we could ensure that the directions will probably not be the same in a short time


        double noiseX = getNoiseAccordingExcessPercent(a_x, percentOfExcess); //This noise is direclty proportional to the percentOfExcess on breathing rate
        double noiseY = getNoiseAccordingExcessPercent(a_y, percentOfExcess);

        //If you want to be more accurate, you can use different values for different levels
        int minimum = 10;//< If you want to change in what excess the user will note a bigger change in the robot control, change this.

        noiseX += minimum;
        noiseY += minimum; //By this way, we ensure that also in low levels, we will get a minimum noise of 25. for example at 1% of excess

        int adjust = 6;//For make the range

        alteredPoint.x = getAlteredPoint(x, (int) noiseX, adjust);
        alteredPoint.y = getAlteredPoint(y, (int) noiseY, adjust);

        //Just to stay in bounds
        if (alteredPoint.x >= maxX) { //Because if we reach the max, it's like start again
            alteredPoint.x = maxX - 1;
        } else if (alteredPoint.y >= maxY) {
            alteredPoint.y = maxY - 1;
        }
        return alteredPoint;
    }

    public double getSpeedAccordingExcessPercent(double a, double b, double excess) {
        return a * excess * excess + b;
    }

    public double getNoiseAccordingExcessPercent(double a, double excess) {
        return a * excess * excess;
    }

    public int getAlteredPoint(int x, int noise, int adjust) {
        //Before we apply the noise, we must considere ir as an arange. This just to make it a little bit unpredictable
        Random rand = new Random();
        int max = noise + adjust;
        int min = noise - adjust;

        int noiseAdjusted = rand.nextInt((max - min) + 1) + min;

        //Lets make it positive, or negative.
        if (rand.nextInt(2) == 0)
            noiseAdjusted = noiseAdjusted * (-1);


        int generated;
        generated = x - noiseAdjusted;

        if (generated < 0)
            generated = 0;

        return generated;
    }

    @Override
    public void setEnabled(boolean val) {
        super.setEnabled(val);
        mEnabled = val;
    }

    @Override
    public void enable() {
        setEnabled(true);
    }

    @Override
    public void disable() {
        setEnabled(false);
    }
}

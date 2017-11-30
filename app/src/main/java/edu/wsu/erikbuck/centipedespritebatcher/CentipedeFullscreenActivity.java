package edu.wsu.erikbuck.centipedespritebatcher;

import android.annotation.SuppressLint;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.twicecircled.spritebatcher.Drawer;
import com.twicecircled.spritebatcher.SpriteBatcher;

import java.io.InputStream;
import java.util.Date;

import javax.microedition.khronos.opengles.GL10;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CentipedeFullscreenActivity extends AppCompatActivity  implements Drawer {

    private static final double MILLIS_PER_SEC = 1000.0;
    private static final double MIN_TIME_BETWEEN_UPDATES_SEC = 0.006;
    private static final double MAX_TIME_BETWEEN_UPDATES_SEC = 0.032;

    private GLSurfaceView mGLSurfaceView;
    private CentipedeBoard mBoard;
    private Date mCurrentInstant;
    private Date mLastUpdateInstant;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;

    private final View.OnTouchListener mPlayerTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int index = motionEvent.getActionIndex();
            int action = motionEvent.getActionMasked();
            int pointerId = motionEvent.getPointerId(index);

            switch (action) {
                case MotionEvent.ACTION_DOWN:

                    mBoard.movePlayerToPosition(motionEvent.getX(), motionEvent.getY());
                    break;
                case MotionEvent.ACTION_MOVE:
                    mBoard.movePlayerToPosition(motionEvent.getX(), motionEvent.getY());
                    mBoard.shoot();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    break;
            }
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_centipede_fullscreen);

        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        mGLSurfaceView = (GLSurfaceView) mContentView;
        int[] resourceIDs = new int[]{R.drawable.centipede};

        // Set the Renderer for drawing on the GLSurfaceView
        mGLSurfaceView.setRenderer(new SpriteBatcher(this, resourceIDs, this));

        InputStream is = getResources().openRawResource(R.raw.centipede);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mBoard = new CentipedeBoard(is, metrics.widthPixels, metrics.heightPixels);
        mBoard.createCentipede();
        mCurrentInstant = new Date();
        mLastUpdateInstant = new Date();

        mGLSurfaceView.setOnTouchListener(mPlayerTouchListener);
        mHidePart2Runnable.run();
    }

    @Override
    public void onDrawFrame(GL10 gl, SpriteBatcher sb) {
        final Date newCurrentInstant = new Date();
        final double timeDeltaMillis = newCurrentInstant.getTime() - mLastUpdateInstant.getTime();
        double timeDeltaSec = timeDeltaMillis / MILLIS_PER_SEC;
        if (timeDeltaSec < MIN_TIME_BETWEEN_UPDATES_SEC) {
            // Not enough time has passed to be worth drawing
            return;
        } else if (timeDeltaSec > MAX_TIME_BETWEEN_UPDATES_SEC) {
            // Java and Android are so bad that the garbage collector stops the world
            // for a time period large enough to break animations and make animated
            // components overshoot their target positions. Testing has shown 69+ ms wasted
            timeDeltaSec = MAX_TIME_BETWEEN_UPDATES_SEC;
        }
        mBoard.updateWithDeltaTime(timeDeltaSec);
        mBoard.onDrawFrame(gl, sb);
        mLastUpdateInstant = mCurrentInstant;
        mCurrentInstant = newCurrentInstant;
    }
}

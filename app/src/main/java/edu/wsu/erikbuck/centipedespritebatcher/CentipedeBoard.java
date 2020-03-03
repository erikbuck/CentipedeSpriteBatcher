package edu.wsu.erikbuck.centipedespritebatcher;

import android.graphics.Rect;
import android.util.Log;

import com.twicecircled.spritebatcher.SpriteBatcher;

import java.io.InputStream;
import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by erik on 3/10/15.
 * This class represents a 2D array of mushroom positions. Not every position
 * has a mushroom, and each mushroom can be in any of 4 states based on how many
 * times it has been hit by bullets. This class also encapsulates an array of
 * centipede segments, the shooter, and the bullet if any.
 */
public class CentipedeBoard {
    public static final int WIDTH = 24;
    public static final int HEIGHT = 30;
    public static final int MAX_NUM_SEGMENTS = 13;
    public static final int SHOOTER_MIN_HEIGHT_CELLS = HEIGHT - 6;
    public static final char WHOLE_MUSHROOM = 1;
    public static final char THREE_QUARTERS_MUSHROOM = 2;
    public static final char HALF_MUSHROOM = 3;
    public static final char ONE_QUARTER_MUSHROOM = 4;
    public static final int DEFAULT_MUSHROOM_SPACING = 16;
    public static final int standardNumMushrooms = (WIDTH * HEIGHT) / DEFAULT_MUSHROOM_SPACING;
    public static float xScaleFactor = 1.0f; // Shrink sprite to available space on screen
    public static float yScaleFactor = 1.0f; // Shrink sprite to available space on screen
    public static int cellWidth = 1; // Shrink sprite to available space on screen
    public static int cellHeight = 1; // Shrink sprite to available space on screen

    private AnimatedSprite mAnimatedSprite;
    private char[][] mStorage = new char[WIDTH][HEIGHT];
    private CentipedeSegment[] mSegments = new CentipedeSegment[MAX_NUM_SEGMENTS];
    private int mShooterPositionX;
    private int mShooterPositionY;
    private int mMinimumShooterPositionY;
    private int mBulletPositionX;
    private int mBulletPositionY;
    private Random mRandom;

    public CentipedeBoard(InputStream inputStream, int widthPixels, int heightPixels) {
        final int bitmapHeight = 512; // was 426;
        final int bitmapWidth =  512; // was 501;

        mAnimatedSprite = new AnimatedSprite(
                inputStream,
                R.drawable.centipede,
                bitmapWidth,
                bitmapHeight);

        Rect[] mushroomRects = this.frameRectsForAnimationName("Mushrooms");
        Rect mushroomRect = mushroomRects[0];
        CentipedeBoard.cellWidth = widthPixels / WIDTH;
        CentipedeBoard.cellHeight = heightPixels / HEIGHT;
        CentipedeBoard.xScaleFactor = (float) widthPixels / (mushroomRect.width() * WIDTH) ;
        CentipedeBoard.yScaleFactor = (float) heightPixels / (mushroomRect.height() * HEIGHT) ;

        Log.d("CentipedeBoard ", "Scale: " + xScaleFactor + " " + yScaleFactor);
        mShooterPositionX = WIDTH / 2;
        mShooterPositionY = HEIGHT - 1;
        Rect[] rects = this.frameRectsForAnimationName("Shooter");
        Rect rect = rects[0];
        mShooterPositionX = (int)(mShooterPositionX * rect.width() * xScaleFactor);
        mShooterPositionY = (int)(mShooterPositionY * rect.height() * yScaleFactor);
        mMinimumShooterPositionY = CentipedeBoard.cellHeight * SHOOTER_MIN_HEIGHT_CELLS;
        mBulletPositionX = 0;
        mBulletPositionY = -1; // off screen initially
        mRandom = new Random();

        resetMushrooms();
    }

    public char elementAtPosition(int x, int y) {
        char result = 255; // Must be non-zero value to prevent centipede leaving board

        if(isValidAtPosition(x, y)) {
            result = mStorage[x][y];
        }
        return result;
    }

    public void setElementAtPosition(char element, int x, int y) {
        if(isValidAtPosition(x, y)) {
            mStorage[x][y] = element;
        }
    }

    public boolean isValidAtPosition(int x, int y) {
        return (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT);
    }

    public void createCentipede() {
        for(int i = 0; i < MAX_NUM_SEGMENTS; i++) {
            mSegments[i] = new CentipedeSegment(i);
        }
    }

    public void addMushroomInCellAt(int x, int y) {
        if(0 == elementAtPosition(x, y)) {
             setElementAtPosition(WHOLE_MUSHROOM, x, y);
        }
    }

    public void resetMushrooms() {
        int numMushrooms = 0;

        for(int i = 0; i < WIDTH; i++) {
            for(int j = 0; j < HEIGHT; j++) {
                if (0 != mStorage[i][j]) {
                    mStorage[i][j] = 1;
                    numMushrooms++;
                }
            }
        }
        for(int j = 1; j < (HEIGHT - 4) && numMushrooms < standardNumMushrooms; j++) {
            for(int i = 0; i < WIDTH && numMushrooms < standardNumMushrooms; i++) {

                if (0 == mStorage[i][j]) {

                    if(0 == (mRandom.nextInt() % DEFAULT_MUSHROOM_SPACING)) {
                        mStorage[i][j] = 1;
                        numMushrooms++;
                    }
                }
            }
        }
    }

    public CentipedeSegment segmentAtIndex(int anIndex) {
        CentipedeSegment result = null;

        if(0 <= anIndex && anIndex < MAX_NUM_SEGMENTS) {
            result = mSegments[anIndex];
        }

        return result;
    }

    public void setSegmentAtIndex(CentipedeSegment aValue, int anIndex) {
        if(0 <= anIndex && anIndex < MAX_NUM_SEGMENTS) {
            mSegments[anIndex] = aValue;
        }
    }

    private void handlePossibleCentipedeDeath() {
        int numLivingSegments = 0;
        for(int i = 0; i < MAX_NUM_SEGMENTS; i++) {
            CentipedeSegment currentSegment = mSegments[i];

            if(null != currentSegment) {
                numLivingSegments++;
            }
        }

        if(0 == numLivingSegments) {
            // Centipede is dead. Reset the surviving mushrooms (adding some too) and
            // create a new centipede.
            resetMushrooms();
            createCentipede();
        }
    }


    public void updateWithDeltaTime(double timeSinceLastUpdate) {
        // Update all of the centipede segments
        for(int i = 0; i < MAX_NUM_SEGMENTS; i++) {
            CentipedeSegment currentSegment = mSegments[i];

            if(null != currentSegment) {
                currentSegment.updateWithBoardAtTime(this, timeSinceLastUpdate);
            }
        }

        // Handle bullet collisions and update the bullet position if it hasn't collided
        if(0 <= mBulletPositionY) {
            Rect[] rects = this.frameRectsForAnimationName("Bullet");
            Rect rect = rects[0];

            // Find out of bullet hit something. If so, change or remove element hit and
            // set mBulletPositionY < 0 so bullet can be fired again
            int hitPositionX = (int)(mBulletPositionX / (rect.width() * xScaleFactor));
            int hitPositionY = (int)(mBulletPositionY / (rect.height() * yScaleFactor));

            if( mBulletPositionY >= 0 &&
                    mBulletPositionX >= 0 &&
                    mBulletPositionX < (WIDTH * rect.width() * xScaleFactor) &&
                    mBulletPositionY < (HEIGHT * rect.height() * yScaleFactor)) {

                if(hitPositionY > 0 && 0 != mStorage[hitPositionX][hitPositionY - 1]) {
                    // Bullet actually hit mushroom below hitPositionY
                    hitPositionY -= 1;
                }

                if(0 != mStorage[hitPositionX][hitPositionY]) {
                    if (4 > mStorage[hitPositionX][hitPositionY]) {
                        mStorage[hitPositionX][hitPositionY] = (char) (1 +
                                mStorage[hitPositionX][hitPositionY]);
                    } else {
                        // Mushroom has been destroyed
                        mStorage[hitPositionX][hitPositionY] = 0;
                    }

                    mBulletPositionY = -1; // bullet hit something so take it off board
                }
            }

            // Handle any hits to centipede segments
            for(int i = 0; 0 <= mBulletPositionY && i < mSegments.length; i++) {
                if(null != mSegments[i]) {
                    if(mSegments[i].handleHitByBulletAt(this,
                            mBulletPositionX, mBulletPositionY)) {

                        addMushroomInCellAt(hitPositionX, hitPositionY);
                        handlePossibleCentipedeDeath();

                        mBulletPositionY = -1; // bullet hit something so take it off board
                    }
                }
            }

            mBulletPositionY -= HEIGHT * timeSinceLastUpdate * rect.height();
        }

    }

    public void onDrawFrame(GL10 gl, SpriteBatcher sb) {
        // Clear to medium gray color
        gl.glClearColor(0.4f, 0.4f, 0.4f, 1.0f);

        // Draw mushrooms
        Rect[] rects = this.frameRectsForAnimationName("Mushrooms");
        for(int i = 0; i < WIDTH; i++) {
            for(int j = 0; j < HEIGHT; j++) {
                if (0 != mStorage[i][j]) {
                    Rect rect = rects[mStorage[i][j] - 1];
                    int destX = (int)(i * CentipedeBoard.cellWidth);
                    int destY = (int)(j * rect.height() * yScaleFactor);
                    sb.draw(R.drawable.centipede,
                            rect,
                            new Rect(
                                    destX,
                                    destY,
                                    destX + rect.width(),
                                    destY + rect.height()));
                }
            }
        }

        // Draw centipede
        for(int i = 0; i < MAX_NUM_SEGMENTS; i++) {
            CentipedeSegment currentSegment = mSegments[i];

            if(null != currentSegment) {
                currentSegment.onDrawFrame(this, gl, sb);
            }
        }

        // Draw shooter
        rects = this.frameRectsForAnimationName("Shooter");
        Rect rect = rects[0];
        if(0 >= mBulletPositionY) {
            mBulletPositionX = mShooterPositionX + rect.width() / 2;
        }

        int destX = mShooterPositionX;
        int destY = mShooterPositionY;

        sb.draw(R.drawable.centipede,
                rect,
                new Rect(
                        destX,
                        destY,
                        destX + rect.width(),
                        destY + rect.height()));

        // Draw bullet
        if(0 <= mBulletPositionY) {
            rects = this.frameRectsForAnimationName("Bullet");
            rect = rects[0];
            destX = mBulletPositionX - rect.width() / 2;
            destY = mBulletPositionY - rect.height() / 2;

            sb.draw(R.drawable.centipede,
                    rect,
                    new Rect(
                            destX,
                            destY,
                            destX + rect.width(),
                            destY + rect.height()));
        }
    }

    public Rect[] frameRectsForAnimationName(String key) {
        return mAnimatedSprite.frameRectsForAnimationName(key);
    }

    public void movePlayerToPosition(float x, float y) {
        Rect[] rects = this.frameRectsForAnimationName("Shooter");
        Rect rect = rects[0];
        mShooterPositionX = (int)x - rect.width() / 2; // center shooter at x,y
        mShooterPositionY = (int)y - rect.height() / 2;// center shooter at x,y
        mShooterPositionY = Math.max(mMinimumShooterPositionY, mShooterPositionY);
    }

    public boolean shoot() {
        boolean result = 0 >= mBulletPositionY;
        if(result) {
            mBulletPositionY = mShooterPositionY;
        }
        return result;
    }
}

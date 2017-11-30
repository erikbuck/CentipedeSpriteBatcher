package edu.wsu.erikbuck.centipedespritebatcher;

import android.graphics.Rect;

import com.twicecircled.spritebatcher.SpriteBatcher;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by erik on 3/10/15.
 */
public class CentipedeSegment {

    // Used to make segment images similar size to mushroom images even though segment
    // images were drawn smaller than mushrooms by the artist
    private static final float SEGMENT_ENLARGEMENT_FACTOR = 1.75f;

    private int mIndex;
    private float mUnitsPerSecond;
    private float mPositionX;      // board position including fraction for intermediate position
    private float mPositionY;      // board position including fraction for intermediate position
    private int mTargetPositionX;  // target board position (whole number)
    private int mTargetPositionY;  // target board position (whole number)
    private int mLastTargetPositionX;  // previous target board position (whole number)
    private int mLastTargetPositionY;  // previous target board position (whole number)
    private int mDeltaX;
    private int mDeltaY;
    private int mDrawCounter;
    private boolean mHasReachedBottom;

    public CentipedeSegment(int index) {
        mIndex = index;
        mUnitsPerSecond = 3; // start moving a small number of  board positions per second
        mPositionX = 0;
        mPositionY = 0;
        mTargetPositionX = 0;
        mTargetPositionY = 0;
        mDeltaX = 1;
        mDeltaY = 1;
        mHasReachedBottom= false;
    }

    public int getFollowingPositionX() {
        return mLastTargetPositionX;
    }

    public int getFollowingPositionY() {
        return mLastTargetPositionY;
    }

    public float getPositionX() {
        return mPositionX;
    }

    public float getPositionY() {
        return mPositionY;
    }

    public void updateWithBoardAtTime(CentipedeBoard aBoard, double timeSinceLastUpdate) {
        // Get the preceding segment
        CentipedeSegment leader = aBoard.segmentAtIndex(mIndex - 1);

        if (null != leader) {   // Follow the leader
            if (mTargetPositionX != leader.getFollowingPositionX() ||
                    mTargetPositionY != leader.getFollowingPositionY()) {

                mLastTargetPositionX = mTargetPositionX;
                mLastTargetPositionY = mTargetPositionY;
                mPositionX = mTargetPositionX;
                mPositionY = mTargetPositionY;
                mTargetPositionX = leader.getFollowingPositionX();
                mTargetPositionY = leader.getFollowingPositionY();
                //Log.d("Segment Follow ", "Index: " + mIndex + " x: " + mTargetPositionX + " y: " + mTargetPositionY);
            }
        } else {

            float directionX = 1;
            float directionY = 1;
            if (mTargetPositionX < mLastTargetPositionX) {
                directionX = -1;
            }
            if (mTargetPositionY < mLastTargetPositionY) {
                directionY = -1;
            }

            if (0.1 > directionX * ((float) mTargetPositionX - mPositionX) &&
                    0.1 > directionY * ((float) mTargetPositionY - mPositionY)) {
                // We have arrived at target position. Calculate next target position
                mLastTargetPositionX = mTargetPositionX;
                mLastTargetPositionY = mTargetPositionY;

                int candidateTargetPositionX = mTargetPositionX + mDeltaX;
                int candidateTargetPositionY = mTargetPositionY;

                if (0 != aBoard.elementAtPosition(candidateTargetPositionX, candidateTargetPositionY)) {
                    mDeltaX *= -1;
                    candidateTargetPositionX = mTargetPositionX;
                    candidateTargetPositionY = mTargetPositionY + mDeltaY;
                }

                if (CentipedeBoard.HEIGHT <= candidateTargetPositionY) {
                    mDeltaY = -1;
                    candidateTargetPositionX = mTargetPositionX;
                    candidateTargetPositionY = CentipedeBoard.HEIGHT-1;
                    mHasReachedBottom= true;
                }

                if(mHasReachedBottom & (CentipedeBoard.HEIGHT - 7) > candidateTargetPositionY) {
                    mDeltaY = 1;
                }
                mTargetPositionX = candidateTargetPositionX;
                mTargetPositionY = candidateTargetPositionY;

                // Log.d("Segment ", "Index: " + mIndex + " x: " + mTargetPositionX + " y: " + mTargetPositionY);
            }
        }

        // Linearly interpolates toward target position
        float fractionToMove = mUnitsPerSecond * (float) timeSinceLastUpdate;
        mPositionX += (mTargetPositionX - mLastTargetPositionX) * fractionToMove;
        mPositionY += (mTargetPositionY - mLastTargetPositionY) * fractionToMove;
    }

    public Rect drawingDestRect(CentipedeBoard aBoard) {
        Rect[] rects = aBoard.frameRectsForAnimationName("CentipedeBodyWalk");
        Rect rect = rects[((int) (mDrawCounter / (18 / mUnitsPerSecond))) % rects.length];

        final int enlargedWidth = (int)(CentipedeBoard.cellWidth * SEGMENT_ENLARGEMENT_FACTOR);
        final int enlargedHeight = (int)(CentipedeBoard.cellHeight * SEGMENT_ENLARGEMENT_FACTOR);
        final int destX = (int) (getPositionX() * rect.width() * CentipedeBoard.xScaleFactor);
        final int destY = (int) (getPositionY() * rect.height() * CentipedeBoard.yScaleFactor);

        return new Rect(destX, destY, destX + enlargedWidth, destY + enlargedHeight);
    }


    public void onDrawFrame(CentipedeBoard aBoard, GL10 gl, SpriteBatcher sb) {
        mDrawCounter++;

        CentipedeSegment leader = aBoard.segmentAtIndex(mIndex - 1);
        Rect[] rects = aBoard.frameRectsForAnimationName("CentipedeBodyWalk");

        if (null == leader) {   // Follow the leader
            rects = aBoard.frameRectsForAnimationName("CentipedeWalk");
        } else {
            CentipedeSegment follower = aBoard.segmentAtIndex(mIndex + 1);
            if (null == follower) {
                rects = aBoard.frameRectsForAnimationName("CentipedeButtWalk");
            }
        }

        Rect rect = rects[((int) (mDrawCounter / (18 / mUnitsPerSecond))) % rects.length];
        Rect dest = drawingDestRect(aBoard);

        if (0 > mDeltaX || mTargetPositionX < mLastTargetPositionX) {
            // draw flipped when walking right to left
            sb.draw(R.drawable.centipede,
                    new Rect(
                            rect.right,
                            rect.top,
                            rect.left,
                            rect.bottom),
                    dest);
        } else {
            // draw regular when walking left to right
            sb.draw(R.drawable.centipede,
                    rect,
                    dest);

        }

    }

    public boolean handleHitByBulletAt(CentipedeBoard aBoard, int x, int y) {
        boolean result = false;

        Rect dest = drawingDestRect(aBoard);
        if (dest.contains(x, y)) {
            // Segment was hit
            aBoard.setSegmentAtIndex(null, mIndex);
            result = true;
        }

        return result;
    }
}

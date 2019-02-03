/*
 * Copyright (c) 2013 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

package com.midisheetmusic;

import android.graphics.*;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import android.view.animation.AnimationUtils;

/** @class ScrollAnimation
 */

public class ScrollAnimation {
    private final float timerInterval = 50.0f;    /** Call timer every 50 msec */
    private final float totalFlingTime = 3000.0f; /** Fling lasts 3 sec */

    ScrollAnimationListener listener;/** Callback to invoke when scroll position changes */
    private boolean  scrollVert;     /** True if we're scrolling vertically */
    private boolean  inMotion;       /** True if we're in a motion event */
    private float    downX;          /** x-pixel when down touch occurs */
    private float    downY;          /** y-pixel when down touch occurs */
    private float    moveX;          /** x-pixel when move touch occurs */
    private float    moveY;          /** y-pixel when move touch occurs */
    private float    prevMoveX;      /** x-pixel when previous move touch occurs */
    private float    prevMoveY;      /** y-pixel when previous move touch occurs */
    private float    upX;            /** x-pixel when up touch occurs */
    private float    upY;            /** y-pixel when up touch occurs */
    private float    deltaX;         /** change in x-pixel from move touch */
    private float    deltaY;         /** change in y-pixel from move touch */
    private long     downTime;       /** Time (millisec) when down touch occurs */
    private long     moveTime;       /** Time (millisec) when move touch occurs */
    private long     prevMoveTime;   /** Time (millisec) when previous move touch occurs */
    private long     upTime;         /** Time (millisec) when up touch occurs */
    private long     flingStartTime; /** Time (millisec) when up fling started */
    private float    flingVelocity;  /** Initial fling velocity (pixels/sec) */
    private float    velocityX;      /** Velocity of move (pixels/sec) */
    private float    velocityY;      /** Velocity of move (pixels/sec) */
    private Handler  scrollTimer;    /** Timer for doing 'fling' scrolling */


    public ScrollAnimation(ScrollAnimationListener listener, boolean scrollVert) {
        this.listener = listener;
        this.scrollVert = scrollVert;
        scrollTimer = new Handler();
    }

    /* Motion has stopped */
    public void stopMotion() {
        inMotion = false;
        downX = downY = moveX = moveY = prevMoveX = prevMoveY = upX = upY = deltaX = deltaY = velocityX = velocityY = 0;
        downTime = prevMoveTime = moveTime = upTime = flingStartTime = 0;
        flingVelocity = 0;
        scrollTimer.removeCallbacks(flingScroll);
    } 

    /** Handle touch/motion events to implement scrolling the sheet music.
     *  - On down touch, store the (x,y) of the touch
     *  - On a motion event, calculate the delta (change) in x, y.
     *    Update the scrolX, scrollY and redraw the sheet music.
     *  - On a up touch, implement a 'fling'.  Call flingScroll
     *    every 50 msec for the next 2 seconds.
     */
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        long currentTime;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                stopMotion();
                inMotion = true;
                prevMoveX = moveX = downX = event.getX();
                prevMoveY = moveY = downY = event.getY();
                prevMoveTime = moveTime = downTime = AnimationUtils.currentAnimationTimeMillis();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!inMotion)
                    return false;

                currentTime = AnimationUtils.currentAnimationTimeMillis();
                velocityX = (prevMoveX - event.getX()) * 1000.0f / (currentTime - prevMoveTime);
                velocityY = (prevMoveY - event.getY()) * 1000.0f / (currentTime - prevMoveTime);

                deltaX = moveX - event.getX();
                deltaY = moveY - event.getY();
                
                prevMoveX = moveX;
                prevMoveY = moveY;
                prevMoveTime = moveTime;
                
                moveX = event.getX();
                moveY = event.getY();
                moveTime = currentTime;

                // If this is a tap, do nothing.
                if (currentTime - downTime < 500 &&
                        Math.abs(moveX - downX) <= 20 &&
                        Math.abs(moveY - downY) <= 20) {
                    return true;
                }

                if (scrollVert) {
                    listener.scrollUpdate(0, (int)deltaY);
                }
                else {
                    if ((Math.abs(deltaY) > Math.abs(deltaX)) || (Math.abs(deltaY) > 4)) {
                        listener.scrollUpdate((int)deltaX, (int)deltaY);
                    }
                    else { 
                        listener.scrollUpdate((int)deltaX, 0);
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!inMotion)
                    return false;

                inMotion = false;
                upTime = AnimationUtils.currentAnimationTimeMillis();
                upX = event.getX();
                upY = event.getY();
                float overallDeltaX = upX - downX;
                float overallDeltaY = upY - downY;

                // If this is a tap, inform the listener.
                if (upTime - downTime < 500 &&
                    Math.abs(overallDeltaX) <= 20 &&
                    Math.abs(overallDeltaY) <= 20) {

                    listener.scrollTapped((int)downX, (int)downY);
                    return true;
                }

                if (scrollVert) {
                    if (Math.abs(overallDeltaY) <= 5) {
                        return true;
                    }
                    else if (Math.abs(velocityY) < 20) {
                        return true;
                    }
                }
                else {
                    if (Math.abs(overallDeltaX) <= 5) {
                        return true;
                    }
                    else if (Math.abs(velocityX) < 20) {
                        return true;
                    }
                }

                /* Keep scrolling for several seconds (fling). */
                flingStartTime = upTime;
                float scale = 0.95f;
                if (scrollVert) {
                    flingVelocity = scale * velocityY;
                }
                else {
                    flingVelocity = scale * velocityX;
                }
                scrollTimer.postDelayed(flingScroll, (int)timerInterval);
                return true;

            default:
                return false;
        }
    }

    /** The timer callback for doing 'fling' scrolling.
     *  Adjust the scrollX/scrollY using the last delta.
     *  Redraw the sheet music.
     *  Then, schedule this timer again.
     */
    Runnable flingScroll = new Runnable() {
    public void run() {
        long currentTime = AnimationUtils.currentAnimationTimeMillis();
        float percentDone = (currentTime - flingStartTime) / totalFlingTime;

        if (percentDone >= 1.0f) {
            return;
        }
        float exp = -2.0f + percentDone * 2.0f;  // -2 to 0
        float scale = (float)(1.0f - Math.pow(Math.E, exp)); // 0.99 to 0

        float velocity = flingVelocity * scale;
        float delta = velocity * 1.0f/timerInterval;
        
        if (scrollVert) {
            listener.scrollUpdate(0, (int)delta);
        }
        else {
            listener.scrollUpdate((int)delta, 0);
        }
        if (percentDone < 1.0) {
            scrollTimer.postDelayed(flingScroll, (int)timerInterval);
        }
    }
    };

}
 

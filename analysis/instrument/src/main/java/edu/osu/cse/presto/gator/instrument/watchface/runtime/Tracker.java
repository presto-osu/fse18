/*
 * Tracker.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.instrument.watchface.runtime;

import android.support.wearable.watchface.WatchFaceService;
import android.util.Log;

public class Tracker {
  public static final String TAG = "gator.instrument";

  public static final int MTD_START_TAG = 0;
  public static final int MTD_END_TAG = 1;

  public static long stateStartTime = -1;
  public static long interactiveTime = 0;
  public static long ambientTime = 0;
  public static long invisibleTime = 0;

  public static int interactive2ambient = 0;
  public static int ambient2interactive = 0;
  public static int interactive2invisible = 0;
  public static int invisible2interactive = 0;
  public static int null2invisible = 0;
  public static int invisible2invisible = 0;
  public static int invisible2ambient = 0;
  public static int ambient2invisible = 0;

  public static long numOnDraw = 0;
  public static long onDrawStartTime = -1;
  public static long timeOnDraw = 0;

  public static STATE currentState = null;

  public static long trackTime = 0;
  public static long numTrack = 0;

  public static void logOnCreate(int tag, String clz) {
    if (tag == MTD_START_TAG) {
      // reset
      currentState = null;
      trackTime = numTrack = 0;

      stateStartTime = -1;
      interactiveTime = 0;
      ambientTime = 0;
      invisibleTime = 0;

      interactive2ambient = 0;
      ambient2interactive = 0;
      interactive2invisible = 0;
      invisible2interactive = 0;
      null2invisible = 0;
      invisible2invisible = 0;
      invisible2ambient = 0;
      ambient2invisible = 0;

      numOnDraw = 0;
      onDrawStartTime = -1;
      timeOnDraw = 0;
    } else if (tag == MTD_END_TAG) {
      Log.i(TAG, clz + " onCreate time: " + System.currentTimeMillis());
    }
  }

  /**
   * Logging with the engine as parameter. This can only be used in non-obfuscated code
   * since the class names for the engine might be changed to others in obfuscated code.
   */
  public static void logTransition(int tag, String clz, String mtdSubSig, WatchFaceService.Engine engine, boolean is) {
    long currentTimeMillis = System.currentTimeMillis();

    if (tag == MTD_START_TAG) {
      STATE nextState;
      if (!engine.isVisible()) {
        nextState = STATE.INVISIBLE;
      } else if (engine.isInAmbientMode()) {
        nextState = STATE.AMBIENT;
      } else {
        nextState = STATE.INTERACTIVE;
      }

      Log.i(TAG, clz + " " + mtdSubSig + " " + is);
      Log.i(TAG, "current state: " + currentState);
      Log.i(TAG, "next state: " + nextState);

      if (currentState == null) {
        if (nextState == STATE.INVISIBLE) {
          null2invisible += 1;
        } else {
          throw new RuntimeException("Not possible from NULL to " + nextState);
        }
      } else if (currentState == STATE.INVISIBLE) {
        invisibleTime += currentTimeMillis - stateStartTime;
        if (nextState == STATE.INVISIBLE) {
          invisible2invisible += 1;
        } else if (nextState == STATE.INTERACTIVE) {
          invisible2interactive += 1;
        } else if (nextState == STATE.AMBIENT) {
          invisible2ambient += 1;
        } else {
          throw new RuntimeException("Not possible from INVISIBLE to " + nextState);
        }
      } else if (currentState == STATE.AMBIENT) {
        ambientTime += currentTimeMillis - stateStartTime;
        if (nextState == STATE.INTERACTIVE) {
          ambient2interactive += 1;
        } else if (nextState == STATE.INVISIBLE) {
          ambient2invisible += 1;
        } else {
          throw new RuntimeException("Not possible from AMBIENT to " + nextState);
        }
      } else if (currentState == STATE.INTERACTIVE) {
        interactiveTime += currentTimeMillis - stateStartTime;
        if (nextState == STATE.INVISIBLE) {
          interactive2invisible += 1;
        } else if (nextState == STATE.AMBIENT) {
          interactive2ambient += 1;
        } else {
          throw new RuntimeException("Not possible from INTERACTIVE to " + nextState);
        }
      }
      currentState = nextState;
    } else if (tag == MTD_END_TAG) {
      stateStartTime = currentTimeMillis;

      Log.i(TAG, "ambient: " + ambientTime + ", interactive: " + interactiveTime + ", invisible: " + invisibleTime);
      Log.i(TAG, "int -> amb: " + interactive2ambient);
      Log.i(TAG, "int -> inv: " + interactive2invisible);
      Log.i(TAG, "amb -> int: " + ambient2interactive);
      Log.i(TAG, "amb -> inv: " + ambient2invisible);
      Log.i(TAG, "inv -> int: " + invisible2interactive);
      Log.i(TAG, "inv -> amb: " + invisible2ambient);
      Log.i(TAG, "inv -> inv: " + invisible2invisible);
      Log.i(TAG, "nul -> inv: " + null2invisible);
    }

    numTrack += 1;
    trackTime += System.currentTimeMillis() - currentTimeMillis;
  }

  public static void logOnDraw(int tag, String clz) {
    long currentTimeMillis = System.currentTimeMillis();
    if (stateStartTime == -1) {
      // initialize state start time
      stateStartTime = currentTimeMillis;
    }
    if (tag == MTD_START_TAG) {
      onDrawStartTime = currentTimeMillis;
    } else if (tag == MTD_END_TAG) {
      timeOnDraw += currentTimeMillis - onDrawStartTime;
      numOnDraw += 1;

      Log.i(TAG, clz + " onDraw time: " + timeOnDraw + ", num: " + numOnDraw);
    }

    numTrack += 1;
    trackTime += System.currentTimeMillis() - currentTimeMillis;
  }

  public enum STATE {
    INVISIBLE(0),
    AMBIENT(1),
    INTERACTIVE(2);

    private int _state;

    STATE(int s) {
      this._state = s;
    }
  }


  public static class NonObf {
    public static final String CANVAS_WATCH_FACE_SERVICE_CLZ_NAME = "android.support.wearable.watchface.CanvasWatchFaceService";
    public static final String CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ_NAME = "android.support.wearable.watchface.CanvasWatchFaceService$Engine";
    public static final String GLES2_WATCH_FACE_SERVICE_CLZ_NAME = "android.support.wearable.watchface.Gles2WatchFaceService";
    public static final String GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ_NAME = "android.support.wearable.watchface.Gles2WatchFaceService$Engine";
    public static final String ON_CREATE_MTD_SUBSIG = "void onCreate(android.view.SurfaceHolder)";
    public static final String ON_DRAW_CANVAS_MTD_SUBSIG = "void onDraw(android.graphics.Canvas,android.graphics.Rect)";
    public static final String ON_DRAW_GLES2_MTD_SUBSIG = "void onDraw()";
    public static final String ON_AMBIENT_MODE_CHANGED_MTD_SUBSIG = "void onAmbientModeChanged(boolean)";
    public static final String ON_VISIBILITY_CHANGED_MTD_SUBSIG = "void onVisibilityChanged(boolean)";
  }

}

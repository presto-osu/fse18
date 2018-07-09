/*
 * ClassUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.util;

import com.google.common.collect.Maps;
import edu.osu.cse.presto.gator.Log;
import org.apache.commons.lang3.tuple.Triple;
import soot.Scene;
import soot.SootClass;

import java.util.Map;

public class ClassUtil {
  private static Log LOG = Log.Factory.getLog(ClassUtil.class.getSimpleName());

  // core classes
  public static SootClass CANVAS_WATCH_FACE_SERVICE_CLZ;
  public static SootClass GLES2_WATCH_FACE_SERVICE_CLZ;
  public static SootClass CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ;
  public static SootClass GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ;

  static {
    CANVAS_WATCH_FACE_SERVICE_CLZ =
            Scene.v().getSootClassUnsafe(WatchFaceNonObfAPI.CANVAS_WATCH_FACE_SERVICE_CLZ_NAME);
    GLES2_WATCH_FACE_SERVICE_CLZ =
            Scene.v().getSootClassUnsafe(WatchFaceNonObfAPI.GLES2_WATCH_FACE_SERVICE_CLZ_NAME);
    if (CANVAS_WATCH_FACE_SERVICE_CLZ == null || ClassUtil.GLES2_WATCH_FACE_SERVICE_CLZ == null) {
      LOG.error("APK is obfuscated!!! Ignore!!!");
    }

    CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ =
            Scene.v().getSootClassUnsafe(WatchFaceNonObfAPI.CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ_NAME);
    GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ =
            Scene.v().getSootClassUnsafe(WatchFaceNonObfAPI.GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ_NAME);
    if (CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ == null
            || GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ == null
            || CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ.isPhantom()
            || GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ.isPhantom()) {
//                || CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ.getMethodUnsafe(WatchFaceNonObfAPI.ENGINE_ONAMBIENTMODECHANGED_MTD_SUBSIG) == null
//                || GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ.getMethodUnsafe(WatchFaceNonObfAPI.ENGINE_ONAMBIENTMODECHANGED_MTD_SUBSIG) == null) {
      LOG.error("APK is obfuscated!!! Ignore!!!");
    }
  }

  public static class CanvasAPI {
    public static final String DRAW_COLOR_SIG1 = "<android.graphics.Canvas: void drawColor(int)>";
    public static final String DRAW_COLOR_SIG2 = "<android.graphics.Canvas: void drawColor(int,android.graphics.PorterDuff$Mode)>";

    // sig -> color_pos
    public static final Map<String, Integer> DRAW_COLOR_SIGS = Maps.newHashMap();

    static {
      DRAW_COLOR_SIGS.put(DRAW_COLOR_SIG1, 0);
      DRAW_COLOR_SIGS.put(DRAW_COLOR_SIG2, 0);
    }
  }

  public static class PaintAPI {
    public static final String SET_COLOR_SIG = "<android.graphics.Paint: void setColor(int)>";
    public static final String SET_ANTI_ALIAS_SIG = "<android.graphics.Paint: void setAntiAlias(boolean)>";
    public static final String SET_COLOR_FILTER_SIG = "<android.graphics.Paint: android.graphics.ColorFilter setColorFilter(android.graphics.ColorFilter)>";
  }

  public static class SensorManagerAPI {
    public static final String GET_DEFAULT_SENSOR_SIG_1 =
            "<android.hardware.SensorManager: android.hardware.Sensor getDefaultSensor(int)>";
    public static final String GET_DEFAULT_SENSOR_SIG_2 =
            "<android.hardware.SensorManager: android.hardware.Sensor getDefaultSensor(int,boolean)>";
    public static final String GET_SENSOR_LIST =
            "<android.hardware.SensorManager: java.util.List getSensorList(int)>";
    // sig -> obj_pos
    public static final Map<String, Integer> GET_SENSOR_SIGS = Maps.newHashMap();

    static {
      GET_SENSOR_SIGS.put(GET_DEFAULT_SENSOR_SIG_1, 0);
      GET_SENSOR_SIGS.put(GET_DEFAULT_SENSOR_SIG_2, 0);
      GET_SENSOR_SIGS.put(GET_SENSOR_LIST, 0);
    }
  }

  public static class SensorAPI {
    static final String REGISTER_LISTENER_SIG_1 =
            "<android.hardware.SensorManager: boolean registerListener(android.hardware.SensorEventListener,android.hardware.Sensor,int)>";
    static final String REGISTER_LISTENER_SIG_2 =
            "<android.hardware.SensorManager: boolean registerListener(android.hardware.SensorEventListener,android.hardware.Sensor,int,int)>";
    static final String REGISTER_LISTENER_SIG_3 =
            "<android.hardware.SensorManager: boolean registerListener(android.hardware.SensorEventListener,android.hardware.Sensor,int,android.os.Handler)>";
    static final String REGISTER_LISTENER_SIG_4 =
            "<android.hardware.SensorManager: boolean registerListener(android.hardware.SensorEventListener,android.hardware.Sensor,int,int,android.os.Handler)>";
    static final String UNREGISTER_LISTENER_SIG_1 =
            "<android.hardware.SensorManager: void unregisterListener(android.hardware.SensorEventListener)>";
    static final String UNREGISTER_LISTENER_SIG_2 =
            "<android.hardware.SensorManager: void unregisterListener(android.hardware.SensorEventListener,android.hardware.Sensor)>";
    // subsig -> <subsig, listener_pos, obj_pos>
    public static final Map<String, Triple<String, Integer, Integer>> REGISTER_LISTENER_SIGS = Maps.newHashMap();
    // subsig -> <subsig, listener_pos, obj_pos>
    public static final Map<String, Triple<String, Integer, Integer>> UNREGISTER_LISTENER_SIGS = Maps.newHashMap();

    static {
      REGISTER_LISTENER_SIGS.put(REGISTER_LISTENER_SIG_1, Triple.of(REGISTER_LISTENER_SIG_1, 0, 1));
      REGISTER_LISTENER_SIGS.put(REGISTER_LISTENER_SIG_2, Triple.of(REGISTER_LISTENER_SIG_2, 0, 1));
      REGISTER_LISTENER_SIGS.put(REGISTER_LISTENER_SIG_3, Triple.of(REGISTER_LISTENER_SIG_3, 0, 1));
      REGISTER_LISTENER_SIGS.put(REGISTER_LISTENER_SIG_4, Triple.of(REGISTER_LISTENER_SIG_4, 0, 1));
      UNREGISTER_LISTENER_SIGS.put(UNREGISTER_LISTENER_SIG_1, Triple.of(UNREGISTER_LISTENER_SIG_1, 0, -1));
      UNREGISTER_LISTENER_SIGS.put(UNREGISTER_LISTENER_SIG_2, Triple.of(UNREGISTER_LISTENER_SIG_2, 0, 1));
    }
  }

  public static class SensorEventListenerAPI {
    public static String ON_SENSOR_CHANGED_SUBSIG = "void onSensorChanged(android.hardware.SensorEvent)";
    public static String ON_ACCURACY_CHANGED_SUBSIG = "void onAccuracyChanged(android.hardware.Sensor,int)";
  }

  public static class LocationAPI {
    static final String REQUEST_LOCATION_UPDATES_SIG_1 =
            "<android.location.LocationManager: void requestLocationUpdates(long,float,android.location.Criteria,android.app.PendingIntent)>";
    static final String REQUEST_LOCATION_UPDATES_SIG_2 =
            "<android.location.LocationManager: void requestLocationUpdates(long,float,android.location.Criteria,android.location.LocationListener,android.os.Looper)>";
    static final String REQUEST_LOCATION_UPDATES_SIG_3 =
            "<android.location.LocationManager: void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener)>";
    static final String REQUEST_LOCATION_UPDATES_SIG_4 =
            "<android.location.LocationManager: void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener,android.os.Looper)>";
    static final String REQUEST_LOCATION_UPDATES_SIG_5 =
            "<android.location.LocationManager: void requestLocationUpdates(java.lang.String,long,float,android.app.PendingIntent)>";
    static final String REQUEST_SINGLE_UPDATE_SIG_1 =
            "<android.location.LocationManager: void requestSingleUpdate(java.lang.String,android.app.PendingIntent)>";
    static final String REQUEST_SINGLE_UPDATE_SIG_2 =
            "<android.location.LocationManager: void requestSingleUpdate(java.lang.String,android.location.LocationListener,android.os.Looper)>";
    static final String REQUEST_SINGLE_UPDATE_SIG_3 =
            "<android.location.LocationManager: void requestSingleUpdate(android.location.Criteria,android.location.LocationListener,android.os.Looper)>";
    static final String REQUEST_SINGLE_UPDATE_SIG_4 =
            "<android.location.LocationManager: void requestSingleUpdate(android.location.Criteria,android.app.PendingIntent)>";

    static final String REMOVE_UPDATES_SIG_1 = "<android.location.LocationManager: void removeUpdates(android.location.LocationListener)>";
    static final String REMOVE_UPDATES_SIG_2 = "<android.location.LocationManager: void removeUpdates(android.app.PendingIntent)>";

    // <subsig, listener_pos, obj_pos>
    public static final Map<String, Triple<String, Integer, Integer>> REQUEST_UPDATES_SIGS = Maps.newHashMap();
    // <subsig, listener_pos, obj_pos>
    public static final Map<String, Triple<String, Integer, Integer>> REMOVE_UPDATES_SIGS = Maps.newHashMap();

    static {
      REQUEST_UPDATES_SIGS.put(REQUEST_LOCATION_UPDATES_SIG_1, Triple.of(REQUEST_LOCATION_UPDATES_SIG_1, -1, -1));
      REQUEST_UPDATES_SIGS.put(REQUEST_LOCATION_UPDATES_SIG_2, Triple.of(REQUEST_LOCATION_UPDATES_SIG_2, 3, -1));
      REQUEST_UPDATES_SIGS.put(REQUEST_LOCATION_UPDATES_SIG_3, Triple.of(REQUEST_LOCATION_UPDATES_SIG_3, 3, -1));
      REQUEST_UPDATES_SIGS.put(REQUEST_LOCATION_UPDATES_SIG_4, Triple.of(REQUEST_LOCATION_UPDATES_SIG_4, 3, -1));
      REQUEST_UPDATES_SIGS.put(REQUEST_LOCATION_UPDATES_SIG_5, Triple.of(REQUEST_LOCATION_UPDATES_SIG_5, -1, -1));
      REQUEST_UPDATES_SIGS.put(REQUEST_SINGLE_UPDATE_SIG_1, Triple.of(REQUEST_SINGLE_UPDATE_SIG_1, -1, -1));
      REQUEST_UPDATES_SIGS.put(REQUEST_SINGLE_UPDATE_SIG_2, Triple.of(REQUEST_SINGLE_UPDATE_SIG_2, 1, -1));
      REQUEST_UPDATES_SIGS.put(REQUEST_SINGLE_UPDATE_SIG_3, Triple.of(REQUEST_SINGLE_UPDATE_SIG_3, 1, -1));
      REQUEST_UPDATES_SIGS.put(REQUEST_SINGLE_UPDATE_SIG_4, Triple.of(REQUEST_SINGLE_UPDATE_SIG_4, -1, -1));
      REMOVE_UPDATES_SIGS.put(REMOVE_UPDATES_SIG_1, Triple.of(REMOVE_UPDATES_SIG_1, 0, -1));
      REMOVE_UPDATES_SIGS.put(REMOVE_UPDATES_SIG_2, Triple.of(REMOVE_UPDATES_SIG_2, -1, -1));
    }
  }

  public static class WatchFaceNonObfAPI {
    public static final String CANVAS_WATCH_FACE_SERVICE_CLZ_NAME =
            "android.support.wearable.watchface.CanvasWatchFaceService";
    public static final String CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ_NAME =
            "android.support.wearable.watchface.CanvasWatchFaceService$Engine";
    public static final String GLES2_WATCH_FACE_SERVICE_CLZ_NAME =
            "android.support.wearable.watchface.Gles2WatchFaceService";
    public static final String GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ_NAME =
            "android.support.wearable.watchface.Gles2WatchFaceService$Engine";

    public static final String SERVICE_ONCREATE_MTD_SUBSIG =
            "void onCreate()";
    public static final String SERVICE_ONDESTROY_MTD_SUBSIG =
            "void onDestroy()";
    public static final String SERVICE_ONCREATEENGINE_MTD_SUBSIG_FMT =
            "%s onCreateEngine()";

    public static final String ENGINE_ONDRAW_MTD_SUBSIG =
            "void onDraw(android.graphics.Canvas,android.graphics.Rect)";

    public static final String ENGINE_ONCREATE_MTD_SUBSIG =
            "void onCreate(android.view.SurfaceHolder)";
    public static final String ENGINE_ONDESTROY_MTD_SUBSIG =
            "void onDestroy()";
    public static final String ENGINE_ONVISIBILITYCHANGED_MTD_SUBSIG =
            "void onVisibilityChanged(boolean)";
    public static final String ENGINE_ONAMBIENTMODECHANGED_MTD_SUBSIG =
            "void onAmbientModeChanged(boolean)";

    public static final String ENGINE_ISINAMBIENTMODE_MTD_SUBSIG =
            "boolean isInAmbientMode()";
    public static final String ENGINE_ISVISIBLE_MTD_SUBSIG =
            "boolean isVisible()";

    public static final String CANVAS_WATCH_FACE_ENGINE_INVALIDATE_SIG =
            "<android.support.wearable.watchface.CanvasWatchFaceService$Engine: void invalidate()>";
    public static final String GLES2_WATCH_FACE_ENGINE_INVALIDATE_SIG =
            "<android.support.wearable.watchface.Gles2WatchFaceService$Engine: void invalidate()>";
    public static final String ENGINE_INVALIDATE_SIG_FMT = "<%s: void invalidate()>";
  }

  public static boolean isLibraryClass(SootClass type) {
    return type.getName().startsWith("android")
            || type.getName().startsWith("com.google")
            || type.getName().startsWith("rx.")
            || type.getName().startsWith("org.apache")
            || type.getName().startsWith("okio.")
            || type.getName().startsWith("okhttp3.")
            || type.getName().startsWith("com.squareup.okhttp.")
            || type.getName().startsWith("com.fasterxml.")
            || type.getName().startsWith("org.joda.")
            || type.getName().startsWith("org.mozilla.");
  }

  public static boolean isWatchFaceEngine(SootClass type) {
    if (Hierarchy.v().isSubtype(type, ClassUtil.CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ)
            && type != ClassUtil.CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ
            || Hierarchy.v().isSubtype(type, ClassUtil.GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ)
            && type != ClassUtil.GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ) {
      return !type.getName().startsWith("android.support");
    }
    return false;
  }

  public static boolean isWatchFaceService(SootClass type) {
    if (Hierarchy.v().isSubtype(type, ClassUtil.CANVAS_WATCH_FACE_SERVICE_CLZ)
            && type != ClassUtil.CANVAS_WATCH_FACE_SERVICE_CLZ
            || Hierarchy.v().isSubtype(type, ClassUtil.GLES2_WATCH_FACE_SERVICE_CLZ)
            && type != ClassUtil.GLES2_WATCH_FACE_SERVICE_CLZ) {
      return !type.getName().startsWith("android.support");
    }
    return false;
  }
}

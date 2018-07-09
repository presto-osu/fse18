/*
 * NIntegerConstantNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds;

import com.google.common.collect.Maps;
import soot.Scene;
import soot.SootClass;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class NIntegerConstantNode extends NObjectNode {
  public int value;

  public NIntegerConstantNode(int v) {
    this.value = v;
  }

  @Override
  public SootClass getClassType() {
    return Scene.v().getSootClass("java.lang.Integer");
  }

  @Override
  public String toString() {
    return "IntConst[" + value + "]" + id;
  }

  // sensor related
  public static class SensorId {
    public static String getSensorNameByType(int type) {
      return SENSOR_NAME_MAP.get(type);
    }

    public static final int TYPE_ACCELEROMETER = 1;

    public static final String STRING_TYPE_ACCELEROMETER = "android.sensor.accelerometer";

    public static final int TYPE_MAGNETIC_FIELD = 2;

    public static final String STRING_TYPE_MAGNETIC_FIELD = "android.sensor.magnetic_field";

    public static final int TYPE_ORIENTATION = 3;

    public static final String STRING_TYPE_ORIENTATION = "android.sensor.orientation";

    public static final int TYPE_GYROSCOPE = 4;

    public static final String STRING_TYPE_GYROSCOPE = "android.sensor.gyroscope";

    public static final int TYPE_LIGHT = 5;

    public static final String STRING_TYPE_LIGHT = "android.sensor.light";

    public static final int TYPE_PRESSURE = 6;

    public static final String STRING_TYPE_PRESSURE = "android.sensor.pressure";

    public static final int TYPE_TEMPERATURE = 7;

    public static final String STRING_TYPE_TEMPERATURE = "android.sensor.temperature";

    public static final int TYPE_PROXIMITY = 8;

    public static final String STRING_TYPE_PROXIMITY = "android.sensor.proximity";

    public static final int TYPE_GRAVITY = 9;

    public static final String STRING_TYPE_GRAVITY = "android.sensor.gravity";

    public static final int TYPE_LINEAR_ACCELERATION = 10;

    public static final String STRING_TYPE_LINEAR_ACCELERATION = "android.sensor.linear_acceleration";

    public static final int TYPE_ROTATION_VECTOR = 11;

    public static final String STRING_TYPE_ROTATION_VECTOR = "android.sensor.rotation_vector";

    public static final int TYPE_RELATIVE_HUMIDITY = 12;

    public static final String STRING_TYPE_RELATIVE_HUMIDITY = "android.sensor.relative_humidity";

    public static final int TYPE_AMBIENT_TEMPERATURE = 13;

    public static final String STRING_TYPE_AMBIENT_TEMPERATURE = "android.sensor.ambient_temperature";

    public static final int TYPE_MAGNETIC_FIELD_UNCALIBRATED = 14;

    public static final String STRING_TYPE_MAGNETIC_FIELD_UNCALIBRATED = "android.sensor.magnetic_field_uncalibrated";

    public static final int TYPE_GAME_ROTATION_VECTOR = 15;

    public static final String STRING_TYPE_GAME_ROTATION_VECTOR = "android.sensor.game_rotation_vector";

    public static final int TYPE_GYROSCOPE_UNCALIBRATED = 16;

    public static final String STRING_TYPE_GYROSCOPE_UNCALIBRATED = "android.sensor.gyroscope_uncalibrated";

    public static final int TYPE_SIGNIFICANT_MOTION = 17;

    public static final String STRING_TYPE_SIGNIFICANT_MOTION = "android.sensor.significant_motion";

    public static final int TYPE_STEP_DETECTOR = 18;

    public static final String STRING_TYPE_STEP_DETECTOR = "android.sensor.step_detector";

    public static final int TYPE_STEP_COUNTER = 19;

    public static final String STRING_TYPE_STEP_COUNTER = "android.sensor.step_counter";

    public static final int TYPE_GEOMAGNETIC_ROTATION_VECTOR = 20;

    public static final String STRING_TYPE_GEOMAGNETIC_ROTATION_VECTOR = "android.sensor.geomagnetic_rotation_vector";

    public static final int TYPE_HEART_RATE = 21;

    public static final String STRING_TYPE_HEART_RATE = "android.sensor.heart_rate";

    public static final int TYPE_TILT_DETECTOR = 22;

    public static final String SENSOR_STRING_TYPE_TILT_DETECTOR = "android.sensor.tilt_detector";

    public static final int TYPE_WAKE_GESTURE = 23;

    public static final String STRING_TYPE_WAKE_GESTURE = "android.sensor.wake_gesture";

    public static final int TYPE_GLANCE_GESTURE = 24;

    public static final String STRING_TYPE_GLANCE_GESTURE = "android.sensor.glance_gesture";

    public static final int TYPE_PICK_UP_GESTURE = 25;

    public static final String STRING_TYPE_PICK_UP_GESTURE = "android.sensor.pick_up_gesture";

    static final Map<Integer, String> SENSOR_NAME_MAP = Maps.newHashMap();

    {
      try {
        Field[] fields = NIntegerConstantNode.class.getFields();

        for (Field f : fields) {
          if (f.getType().equals(int.class)) {
            if (f.getName().startsWith("TYPE_")) {
              String strName = "STRING_" + f.getName();
              Field strField = null;
              try {
                strField = NIntegerConstantNode.class.getField(strName);
              } catch (NoSuchFieldException ignored) {

              }
              if (strField == null) {
                strField = NIntegerConstantNode.class.getField("SENSOR_" + strName);
              }

              if (!strField.getType().equals(String.class)) {
                throw new RuntimeException("STRING_TYPE field is not a String object");
              }

              int modSensorInt = f.getModifiers();
              int modSensorStr = f.getModifiers();

              if (!(modSensorInt == modSensorStr &&
                      (modSensorInt & Modifier.PUBLIC) != 0 &&
                      (modSensorInt & Modifier.STATIC) != 0 &&
                      (modSensorInt & Modifier.FINAL) != 0)) {
                throw new RuntimeException("Modifiers do not match when processing Sensor IDs");
              }
              int sensorIdValue = f.getInt(null);
              String sensorStringValue = (String) strField.get(null);
              SENSOR_NAME_MAP.put(sensorIdValue, sensorStringValue);
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Exception in Processing Sensor IDs");
      }
    }
  }
}

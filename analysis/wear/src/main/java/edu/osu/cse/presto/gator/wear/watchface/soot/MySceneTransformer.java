/*
 * MySceneTransformer.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot;

import android.graphics.Color;
import com.google.common.collect.*;
import edu.osu.cse.presto.gator.Log;
import edu.osu.cse.presto.gator.Timer;
import edu.osu.cse.presto.gator.instrument.watchface.util.ApkUtil;
import edu.osu.cse.presto.gator.wear.watchface.graph.STG;
import edu.osu.cse.presto.gator.wear.watchface.soot.analysis.Alias;
import edu.osu.cse.presto.gator.wear.watchface.util.ClassUtil;
import edu.osu.cse.presto.gator.wear.watchface.util.Configuration;
import soot.*;
import soot.toolkits.scalar.Pair;

import java.util.*;

import edu.osu.cse.presto.gator.Timer;

public class MySceneTransformer extends SceneTransformer {
  private final Log LOG = Log.Factory.getLog(MySceneTransformer.class.getSimpleName());
  private String apkPath;
  private List<String> wfServices;
  private List<SootClass> wfServiceCls;

  MySceneTransformer(String apkPath) {
    this.apkPath = apkPath;
    this.wfServiceCls = Lists.newArrayList();
    wfServices = ApkUtil.v().getWatchFaceServices(apkPath);
    if (wfServices.isEmpty()) {
      LOG.error(apkPath + " contains no watch face!!!");
    }
  }

  @Override
  protected void internalTransform(String phaseName, Map<String, String> options) {
    for (String wfs : wfServices) {
      LOG.info("WatchFaceService:::: %s", wfs);
      SootClass clz = Scene.v().getSootClass(wfs);
      wfServiceCls.add(clz);
    }

    LOG.info("!!!!!!!!!! Going to analyze: %s", apkPath);

//    FlowGraph.v().dump("flowgraph.dot");

    Timer timer = new Timer();

    boolean analyzed = false;
    HashMultimap<Integer, Pair<List<STG.Transition>, Collection<Integer>>> leaks = HashMultimap.create();
    for (SootClass clz : Scene.v().getApplicationClasses()) {
      if (!clz.isConcrete() || clz.getName().startsWith("android.support"))
        continue;
//      if (!clz.hasOuterClass() || !wfServiceCls.contains(clz.getOuterClass()))
//        continue;
      if (ClassUtil.isWatchFaceEngine(clz)) {
        LOG.info("############## %s ##############", clz);
        analyzed = true;
        STG stg = STG.construct(clz, false);
        //stg.dump("stg.dot");

        Map<SootClass, SootMethod> listenerCallbacks = Analyzer.v().getListenerCallbacks();
        SetMultimap<SootMethod, STG.Label> listenerCallbackReleaseMap = HashMultimap.create();
        for (SootMethod mtd : listenerCallbacks.values()) {
          SetMultimap<STG.Callback, STG.Label> labelMap = HashMultimap.create();
          SetMultimap<STG.Callback, STG.Label> releaseLabelMap = HashMultimap.create();
          SetMultimap<STG.Callback, STG.Label> acquireLabelMap = HashMultimap.create();
          SetMultimap<STG.Callback, SetMultimap<Value, Integer>> antiAliasMap = HashMultimap.create();
          SetMultimap<STG.Callback, SetMultimap<Value, Integer>> colorMap = HashMultimap.create();
          Analyzer.v().lookup(stg, new STG.Callback("onSensorChanged"), mtd, labelMap, acquireLabelMap, releaseLabelMap, antiAliasMap, colorMap, false);
          listenerCallbackReleaseMap.putAll(mtd, releaseLabelMap.values());
        }

        if (!listenerCallbackReleaseMap.isEmpty())
          LOG.info("onSensorChanged: %s", listenerCallbackReleaseMap);

        stg.forwardTraversal(STG.NULL_STATE, Configuration.STG_PATH_LENGTH_K, new STG.PathVisitor() {
          @Override
          public void visit(List<STG.Transition> path) {
            if (!path.get(path.size() - 1).target.equals(STG.NULL_STATE)
                    && !path.get(path.size() - 1).target.equals(STG.AMBIENT_STATE)) {
              return;
            }
            Alias.clearContext();
            stg.reset();
            Analyzer.v().lookup(stg, STG.ON_AMBIENT_MODE_CHANGED_FALSE, stg.getConcreteCallback(STG.ON_AMBIENT_MODE_CHANGED_FALSE),
                    HashMultimap.create(), HashMultimap.create(), HashMultimap.create(), HashMultimap.create(), HashMultimap.create(), true);
            List<Pair<Collection<STG.Label>, Collection<STG.Label>>> labelsOnPath = Lists.newArrayList();
            for (STG.Transition t : path) {
              for (STG.Callback cb : t.callbacks) {
                SetMultimap<STG.Callback, STG.Label> labelMap = HashMultimap.create();
                SetMultimap<STG.Callback, STG.Label> releaseLabelMap = HashMultimap.create();
                SetMultimap<STG.Callback, STG.Label> acquireLabelMap = HashMultimap.create();
                stg.lookupAPI(cb, labelMap, releaseLabelMap, acquireLabelMap, null);
                labelsOnPath.add(new Pair<>(acquireLabelMap.values(), releaseLabelMap.values()));
              }
            }

            Multimap<SootClass, Integer> registerMap = HashMultimap.create();
            for (Pair<Collection<STG.Label>, Collection<STG.Label>> labels : labelsOnPath) {
              for (STG.Label label : labels.getO2()) {
                if (label == null || !(label.name instanceof STG.LabelName.SensorLabelName)) continue;
                SootClass listener = label.listener.getClassType();
                if (label.obj == -1) {
                  // ALL
                  registerMap.removeAll(listener);
                } else {
                  registerMap.remove(listener, label.obj);
                }
              }
              for (STG.Label label : labels.getO1()) {
                if (label == null || !(label.name instanceof STG.LabelName.SensorLabelName)) continue;
                SootClass listener = label.listener.getClassType();
                if (label.obj == -1) {
                  // should not happen as we can always find the sensor id
                  registerMap.put(listener, -1);
                } else {
                  registerMap.put(listener, label.obj);
                }
              }
            }
//            for (STG.Transition t : path) {
//              List<STG.Callback> callbacks = t.callbacks;
//              for (STG.Callback cb : callbacks) {
//                for (STG.Label label : stg.getRelLabel(cb)) {
//                  if (label == null || !(label.name instanceof STG.LabelName.SensorLabelName)) continue;
//                  SootClass listener = label.listener.getClassType();
//                  if (label.obj == -1) {
//                    // ALL
//                    registerMap.removeAll(listener);
//                  } else {
//                    registerMap.remove(listener, label.obj);
//                  }
//                }
//                for (STG.Label label : stg.getAcqLabel(cb)) {
//                  if (label == null || !(label.name instanceof STG.LabelName.SensorLabelName)) continue;
//                  SootClass listener = label.listener.getClassType();
//                  if (label.obj == -1) {
//                    // should not happen as we can always find the sensor id
//                    registerMap.put(listener, -1);
//                  } else {
//                    registerMap.put(listener, label.obj);
//                  }
//                }
//              }
//            }
            if (!registerMap.isEmpty()) {
              if (path.get(path.size() - 1).target.equals(STG.AMBIENT_STATE)) {
                boolean allStepCounter = true;
                for (int sensor : registerMap.values()) {
                  if (sensor != 19) {
                    allStepCounter = false;
                  }
                }
                if (allStepCounter) return;
              }
              Set<SootClass> toRemove = Sets.newHashSet();
              for (SootClass lis : registerMap.keySet()) {
                Set<STG.Label> releaseLabels = listenerCallbackReleaseMap.get(listenerCallbacks.get(lis));
                Set<Integer> releaseSet = Sets.newHashSet();
                for (STG.Label l : releaseLabels) {
                  releaseSet.add(l.obj);
                }
                for (int sen : registerMap.get(lis)) {
                  if (releaseSet.contains(sen)) {
                    toRemove.add(lis);
                  }
                }
              }
              for (SootClass c : toRemove) {
                registerMap.removeAll(c);
              }
              if (!registerMap.isEmpty()) {
                leaks.put(path.size(), new Pair<>(path, registerMap.values()));
//                LOG.info("$$$$$ SENSOR LEAK $$$$$ [%s] [%s] [%s]", path.size(), path, registerMap);
              }
            } else {
//              LOG.info("$$$$$ OK $$$$$");
            }
          }
        });

        ////////////////////////////////////////////////////////////////

        Alias.clearContext();
        stg.reset();
        SetMultimap<STG.Callback, SetMultimap<Value, Integer>> colorMapTrue = HashMultimap.create();
        stg.lookupAPI(STG.ON_AMBIENT_MODE_CHANGED_TRUE,
                HashMultimap.create(),
                HashMultimap.create(),
                HashMultimap.create(),
                colorMapTrue,
                false);

        Alias.clearContext();
        stg.reset();
        SetMultimap<STG.Callback, SetMultimap<Value, Integer>> colorMapFalse = HashMultimap.create();
        stg.lookupAPI(STG.ON_AMBIENT_MODE_CHANGED_FALSE,
                HashMultimap.create(),
                HashMultimap.create(),
                HashMultimap.create(),
                colorMapFalse,
                false);

        Set<SetMultimap<Value, Integer>> setColorTrue = colorMapTrue.get(STG.ON_AMBIENT_MODE_CHANGED_TRUE);
//        setColorTrue.addAll(stg.getSetColor(STG.ON_CREATE));
//        setColorTrue.addAll(stg.getSetColor(STG.ON_VISIBILITY_CHANGED_TRUE));
        Set<SetMultimap<Value, Integer>> setColorFalse = colorMapFalse.get(STG.ON_AMBIENT_MODE_CHANGED_FALSE);
        Set<String> colorsTrue = Sets.newHashSet();
        Set<String> colorsFalse = Sets.newHashSet();
        for (SetMultimap<Value, Integer> m : setColorTrue) {
          for (Integer i : m.values()) {
            if (i != null) {
              if (i >= 0xff000000 && i <= 0xffffffff) {
                String name = Color.getName(i);
                if (!Color.allowedColorsInAmbient.contains(name))
                  colorsTrue.add(Color.getName(i));
              }
            } else {
//              colorsTrue.add("null");
            }
          }
        }
        for (SetMultimap<Value, Integer> m : setColorFalse) {
          for (Integer i : m.values()) {
            if (i != null) {
              if (i >= 0xff000000 && i <= 0xffffffff) {
                String name = Color.getName(i);
                if (!Color.allowedColorsInAmbient.contains(name))
                  colorsFalse.add(Color.getName(i));
              }
            } else {
//              colorsTrue.add("null");
            }
          }
        }
        LOG.info("setColor @ onAmbientModeChanged(true): %s", setColorTrue);
        LOG.info("setColor @ onAmbientModeChanged(false): %s", setColorFalse);
        LOG.info("setColor equal=%s", setColorTrue.equals(setColorFalse));
        LOG.info("colors @ onAmbientModeChanged(true): %s", colorsTrue);
        LOG.info("colors @ onAmbientModeChanged(false): %s", colorsFalse);
        LOG.info("colors equal=%s", colorsTrue.equals(colorsFalse) && !colorsTrue.isEmpty());
        if (!colorsTrue.equals(colorsFalse)) {
          for (String color : colorsTrue) {
            if (!Color.allowedColorsInAmbient.contains(color)
                    && colorsTrue.size() > 1) {
              LOG.info("colors not equal, flows to ambient: %s", colorsTrue);
            }
          }
        }
      }
    }

    int i = 0;
    for (int len : leaks.keySet()) {
      for (Pair<List<STG.Transition>, Collection<Integer>> pair : leaks.get(len)) {
        i += 1;
        List<STG.Transition> path = pair.getO1();
        Collection<Integer> sensors = pair.getO2();
        LOG.info("@@@@@@ SENSOR LEAK @@@@@@ %s [%s]", sensors, path);
        for (STG.Transition t : path) {
          switch (t.event) {
            case STANDBY:
              LOG.info("LEAK TESTCASE %s: wearable.standby()", i);
              break;
            case SELECT:
              LOG.info("LEAK TESTCASE %s: wearable.select_on_handheld(HANDHELD_SERIALNO, labels[pkg])", i);
              break;
            case DESELECT:
              LOG.info("LEAK TESTCASE %s: wearable.deselect_on_handheld(HANDHELD_SERIALNO)", i);
              break;
            case SWIPE_LEFT:
              LOG.info("LEAK TESTCASE %s: wearable.swipe_left()", i);
              break;
            case SWIPE_RIGHT:
              LOG.info("LEAK TESTCASE %s: wearable.swipe_right()", i);
              break;
            case SWIPE_UP:
              LOG.info("LEAK TESTCASE %s: wearable.swipe_up()", i);
              break;
            case PRESS_SIDE_BUTTON:
              LOG.info("LEAK TESTCASE %s: wearable.press_side_button()", i);
              break;
            case TAP_SCREEN_CENTER:
              break;
          }
        }
      }
    }

    if (analyzed)
      LOG.info("TOTAL TIME: %f sec", timer.getInterval() / 1000.0);
    LOG.info("Soot stopped on " + new Date());
    System.exit(0);
  }
}

/*
 * STG.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.graph;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import edu.osu.cse.presto.gator.Log;
import edu.osu.cse.presto.gator.wear.watchface.soot.Analyzer;
import edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds.NAllocNode;
import javafx.util.Pair;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


// State Transition Graph
public class STG {
  private static final Log LOG = Log.Factory.getLog(STG.class.getName());

  public enum Event {
    PRESS_SIDE_BUTTON,
    SWIPE_RIGHT,
    SWIPE_LEFT,
    SWIPE_UP,
    SWIPE_DOWN,
    TAP_SCREEN_CENTER,
    STANDBY,
    SELECT,
    DESELECT
  }

  public static class Callback {
    String name;

    public Callback(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name + "()";
    }
  }

  public static class OnDrawCallback extends Callback {

    OnDrawCallback() {
      super("onDraw");
    }
  }

  private static class OnVisibilityChangedCallback extends Callback {
    boolean isVisible;

    private OnVisibilityChangedCallback(boolean visible) {
      super("onVisibilityChanged");
      this.isVisible = visible;
    }

    @Override
    public String toString() {
      return name + "(" + isVisible + ")";
    }
  }

  private static class OnAmbientModeChangedCallback extends Callback {
    boolean isAmbient;

    private OnAmbientModeChangedCallback(boolean isAmbient) {
      super("onAmbientModeChanged");
      this.isAmbient = isAmbient;
    }

    @Override
    public String toString() {
      return name + "(" + isAmbient + ")";
    }
  }

  private static class OnCreateCallback extends Callback {
    private OnCreateCallback() {
      super("onCreate");
    }
  }

  private static class OnDestroyCallback extends Callback {
    private OnDestroyCallback() {
      super("onDestroy");
    }
  }

  private static class WFOnCreateCallback extends Callback {
    private WFOnCreateCallback() {
      super("wfOnCreate");
    }
  }

  private static class WFOnDestroyCallback extends Callback {
    private WFOnDestroyCallback() {
      super("wfOnDestroy");
    }
  }

  private static class WFOnCreateEngineCallback extends Callback {
    private WFOnCreateEngineCallback() {
      super("wfOnCreateEngine");
    }
  }

  public static class State {
    String name;

    State(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name.toLowerCase().replace(' ', '_');
    }
  }

  public static class Transition {
    public State source, target;
    public Event event;
    public List<Callback> callbacks;

    Transition(State src, State tgt, Event e, List<Callback> callbacks) {
      this.source = src;
      this.target = tgt;
      this.event = e;
      this.callbacks = callbacks;
    }

    @Override
    public String toString() {
      return "<" + source + "::" + event + "::" + target + ">";
    }
  }

  public static class Label2 {
    public LabelName name;
    public Set<NAllocNode> listener;
    public Set<Integer> obj; // -1: ANY
    public Stmt site;

    public Label2(LabelName name, Set<NAllocNode> listener, Set<Integer> obj, Stmt site) {
      this.name = name;
      this.listener = listener;
      this.obj = obj;
      this.site = site;
    }

    @Override
    public String toString() {
      return "(" + name + "," + listener + "," + obj + ")";
    }
  }


  public static class Label {
    public LabelName name;
    public NAllocNode listener;
    public int obj; // -1: ANY
    public Stmt site;

    private static Map<Integer, Label> all = Maps.newHashMap();

    public static Label get(LabelName name, NAllocNode listener, int obj, Stmt site) {
      int hash = name.hashCode() * 101 + listener.hashCode() * 17 + site.hashCode() + obj;
      if (all.containsKey(hash)) {
        return all.get(hash);
      }
      Label newLabel = new Label(name, listener, obj, site);
      all.put(hash, newLabel);
      return newLabel;
    }

    private Label(LabelName name, NAllocNode listener, int obj, Stmt site) {
      this.name = name;
      this.listener = listener;
      this.obj = obj;
      this.site = site;
    }

    @Override
    public String toString() {
      return "(" + name + "," + listener + "," + obj + ")";
    }

    @Override
    public int hashCode() {
      return name.hashCode() * 101 + listener.hashCode() * 17 + site.hashCode() + obj;
    }
  }

  public interface PathVisitor {
    void visit(List<Transition> path);
  }

  public void forwardTraversal(State start, int k, PathVisitor visitor) {
    Preconditions.checkArgument(k > 0 && start != null && visitor != null);
    List<List<Transition>> paths = Lists.newArrayList();
    for (Transition t : transitions.values()) {
      if (t.source.equals(start)) {
        forwardTraversal(paths, Lists.newArrayList(t), k);
      }
    }
    for (List<Transition> path : paths) {
      visitor.visit(path);
    }
  }

  private void forwardTraversal(List<List<Transition>> paths,
                                List<Transition> curPath, int k) {
    if (curPath.size() > k) return;
    paths.add(Lists.newArrayList(curPath));
    for (Transition t : transitions.get(curPath.get(curPath.size() - 1).target)) {
      curPath.add(t);
      forwardTraversal(paths, curPath, k);
      curPath.remove(curPath.size() - 1);
    }
  }

  public void dump(String dotPath) {
    StringBuilder builder = new StringBuilder("digraph g {\n\trankdir=LR;\n\n");
    builder.append("\tnode_").append(INTERACTIVE_STATE).append(" [shape=oval,label=\"")
            .append(INTERACTIVE_STATE.name.replace(' ', '\n')).append("\"];\n");
    builder.append("\tnode_").append(NULL_STATE).append(" [shape=oval,label=\"")
            .append(NULL_STATE.name.replace(' ', '\n')).append("\"];\n");
    builder.append("\tnode_").append(AMBIENT_STATE).append(" [shape=oval,label=\"")
            .append(AMBIENT_STATE.name.replace(' ', '\n')).append("\"];\n");
    builder.append("\tnode_").append(INVISIBLE_WF_PICKER_STATE).append(" [shape=oval,label=\"")
            .append(INVISIBLE_WF_PICKER_STATE.name.replace(' ', '\n')).append("\"];\n");
    builder.append("\tnode_").append(INVISIBLE_APPLIST_STATE).append(" [shape=oval,label=\"")
            .append(INVISIBLE_APPLIST_STATE.name.replace(' ', '\n')).append("\"];\n");
    builder.append("\tnode_").append(INVISIBLE_NOTIF_STATE).append(" [shape=oval,label=\"")
            .append(INVISIBLE_NOTIF_STATE.name.replace(' ', '\n')).append("\"];\n");
    builder.append("\n");

    Map<Pair<State, State>, String> map = new HashMap<>();
    for (Transition t : transitions.values()) {
      Pair<State, State> edge = new Pair<>(t.source, t.target);
      if (map.containsKey(edge)) continue;
      StringBuilder label = new StringBuilder(map.getOrDefault(edge, ""));
      for (Callback cb : t.callbacks) {
        for (Label l : getLabel(cb)) {
          String tmp = l.name.toString() + l.obj + "[" + l.listener + "] @ " + cb;
          label = new StringBuilder(label.length() == 0 ? tmp
                  : label + "<BR/>" + tmp);
        }
      }
      map.putIfAbsent(edge, label.toString());
    }
    for (Map.Entry<Pair<State, State>, String> entry : map.entrySet()) {
      String from = "node_" + entry.getKey().getKey().toString();
      String to = "node_" + entry.getKey().getValue().toString();
      builder.append("\t").append(from).append(" -> ")
              .append(to).append(" [fontcolor=darkslategrey,label=<")
              .append(entry.getValue()).append(">];\n");
    }
    builder.append("}\n");
    try {
      BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(dotPath));
      bufferedWriter.append(builder.toString());
      bufferedWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void toDot(String dotPath) {
    StringBuilder builder = new StringBuilder("digraph g {\n\trankdir=LR;\n\n");
    builder.append("\tnode_").append(INTERACTIVE_STATE).append(" [shape=oval,label=\"")
            .append(INTERACTIVE_STATE.name.replace(' ', '\n')).append("\"];\n");
    builder.append("\tnode_").append(NULL_STATE).append(" [shape=oval,label=\"")
            .append(NULL_STATE.name.replace(' ', '\n')).append("\"];\n");
    builder.append("\tnode_").append(AMBIENT_STATE).append(" [shape=oval,label=\"")
            .append(AMBIENT_STATE.name.replace(' ', '\n')).append("\"];\n");
    builder.append("\tnode_").append(INVISIBLE_WF_PICKER_STATE).append(" [shape=oval,label=\"")
            .append(INVISIBLE_WF_PICKER_STATE.name.replace(' ', '\n')).append("\"];\n");
    builder.append("\tnode_").append(INVISIBLE_APPLIST_STATE).append(" [shape=oval,label=\"")
            .append(INVISIBLE_APPLIST_STATE.name.replace(' ', '\n')).append("\"];\n");
    builder.append("\tnode_").append(INVISIBLE_NOTIF_STATE).append(" [shape=oval,label=\"")
            .append(INVISIBLE_NOTIF_STATE.name.replace(' ', '\n')).append("\"];\n");
    builder.append("\n");

    Map<Pair<State, State>, String> map = new HashMap<>();
    for (Transition t : transitions.values()) {
      Pair<State, State> edge = new Pair<>(t.source, t.target);
      StringBuilder label = new StringBuilder(map.getOrDefault(edge, ""));
      label = new StringBuilder(label.length() == 0 ? t.event.toString().toLowerCase()
              : label + "<BR/>" + t.event.toString().toLowerCase());
      label.append("<BR/>").append(t.callbacks.isEmpty() ? "" : "<FONT FACE=\"Courier\">[");
      boolean first = true;
      for (Callback cb : t.callbacks) {
        if (!first) label.append(",<BR/>");
        first = false;
        label.append(cb);
      }
      label.append(t.callbacks.isEmpty() ? "" : "]</FONT>");
      map.put(edge, label.toString());
    }
    for (Map.Entry<Pair<State, State>, String> entry : map.entrySet()) {
      String from = "node_" + entry.getKey().getKey().toString();
      String to = "node_" + entry.getKey().getValue().toString();
      builder.append("\t").append(from).append(" -> ")
              .append(to).append(" [fontcolor=darkslategrey,label=<")
              .append(entry.getValue()).append(">];\n");
    }
    builder.append("}\n");
    try {
      BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(dotPath));
      bufferedWriter.append(builder.toString());
      bufferedWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Label names
  public static abstract class LabelName {
    String type;
    String name;

    public static boolean isAcq(LabelName name) {
      return name.type.equals("acq");
    }

    public static boolean isRel(LabelName name) {
      return name.type.equals("rel");
    }

    LabelName(String label, String name) {
      this.type = label;
      this.name = name;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof LabelName)) return false;
      return ((LabelName) o).type.equals(type) && ((LabelName) o).name.equals(name);
    }

    @Override
    public String toString() {
      return name;
    }

    public static class SensorLabelName extends LabelName {
      public static SensorLabelName register_sensor_listener = new SensorLabelName("acq", "register_sensor_listener");
      public static SensorLabelName unregister_sensor_listener = new SensorLabelName("rel", "unregister_sensor_listener");

      public SensorLabelName(String label, String name) {
        super(label, name);
      }
    }

    public static class LocationLabelName extends LabelName {
      public static LocationLabelName request_location_listener = new LocationLabelName("acq", "request_location_listener");
      public static LocationLabelName remove_location_listener = new LocationLabelName("rel", "remove_location_listener");

      public LocationLabelName(String label, String name) {
        super(label, name);
      }
    }
  }


  // Interesting callbacks
  public static final OnDrawCallback ON_DRAW = new OnDrawCallback();
  public static final WFOnCreateCallback WF_ON_CREATE = new WFOnCreateCallback();
  public static final WFOnDestroyCallback WF_ON_DESTROY = new WFOnDestroyCallback();
  public static final WFOnCreateEngineCallback WF_ON_CREATE_ENGINE = new WFOnCreateEngineCallback();
  public static final OnCreateCallback ON_CREATE = new OnCreateCallback();
  public static final OnDestroyCallback ON_DESTROY = new OnDestroyCallback();
  public static final OnVisibilityChangedCallback ON_VISIBILITY_CHANGED_TRUE =
          new OnVisibilityChangedCallback(true);
  public static final OnVisibilityChangedCallback ON_VISIBILITY_CHANGED_FALSE =
          new OnVisibilityChangedCallback(false);
  public static final OnAmbientModeChangedCallback ON_AMBIENT_MODE_CHANGED_TRUE =
          new OnAmbientModeChangedCallback(true);
  public static final OnAmbientModeChangedCallback ON_AMBIENT_MODE_CHANGED_FALSE =
          new OnAmbientModeChangedCallback(false);

  // States
  public static final State INTERACTIVE_STATE = new State("Interactive");
  public static final State INVISIBLE_NOTIF_STATE = new State("Invisible Notification");
  public static final State INVISIBLE_WF_PICKER_STATE = new State("Invisible WFPicker");
  public static final State INVISIBLE_APPLIST_STATE = new State("Invisible AppList");
  public static final State NULL_STATE = new State("Null");
  public static final State AMBIENT_STATE = new State("Ambient");

  // Transitions
  public static final Multimap<State, Transition> transitions;

  static {
    transitions = new ImmutableSetMultimap.Builder<State, Transition>()
            // from Interactive
            .put(INTERACTIVE_STATE, new Transition(INTERACTIVE_STATE, AMBIENT_STATE,
                    Event.STANDBY, Lists.newArrayList(ON_AMBIENT_MODE_CHANGED_TRUE)))
            .put(INTERACTIVE_STATE, new Transition(INTERACTIVE_STATE, INVISIBLE_APPLIST_STATE,
                    Event.PRESS_SIDE_BUTTON, Lists.newArrayList(ON_VISIBILITY_CHANGED_FALSE)))
            .put(INTERACTIVE_STATE, new Transition(INTERACTIVE_STATE, INVISIBLE_NOTIF_STATE,
                    Event.SWIPE_UP, Lists.newArrayList(ON_VISIBILITY_CHANGED_FALSE)))
            .put(INTERACTIVE_STATE, new Transition(INTERACTIVE_STATE, INVISIBLE_WF_PICKER_STATE,
                    Event.SWIPE_LEFT, Lists.newArrayList(ON_VISIBILITY_CHANGED_FALSE)))
            .put(INTERACTIVE_STATE, new Transition(INTERACTIVE_STATE, INVISIBLE_WF_PICKER_STATE,
                    Event.SWIPE_RIGHT, Lists.newArrayList(ON_VISIBILITY_CHANGED_FALSE)))
            // from Ambient
            .put(AMBIENT_STATE, new Transition(AMBIENT_STATE, INTERACTIVE_STATE,
                    Event.PRESS_SIDE_BUTTON, Lists.newArrayList(ON_AMBIENT_MODE_CHANGED_FALSE)))
            .put(AMBIENT_STATE, new Transition(AMBIENT_STATE, INTERACTIVE_STATE,
                    Event.TAP_SCREEN_CENTER, Lists.newArrayList(ON_AMBIENT_MODE_CHANGED_FALSE)))
            // from Invisible Applist
            .put(INVISIBLE_APPLIST_STATE, new Transition(INVISIBLE_APPLIST_STATE, INTERACTIVE_STATE,
                    Event.SWIPE_RIGHT, Lists.newArrayList(ON_VISIBILITY_CHANGED_TRUE)))
            .put(INVISIBLE_APPLIST_STATE, new Transition(INVISIBLE_APPLIST_STATE, INTERACTIVE_STATE,
                    Event.PRESS_SIDE_BUTTON, Lists.newArrayList(ON_VISIBILITY_CHANGED_TRUE)))
            .put(INVISIBLE_APPLIST_STATE, new Transition(INVISIBLE_APPLIST_STATE, AMBIENT_STATE,
                    Event.STANDBY, Lists.newArrayList(ON_AMBIENT_MODE_CHANGED_TRUE, ON_VISIBILITY_CHANGED_TRUE)))
            // from Invisible Notification
            .put(INVISIBLE_NOTIF_STATE, new Transition(INVISIBLE_NOTIF_STATE, INTERACTIVE_STATE,
                    Event.SWIPE_DOWN, Lists.newArrayList(ON_VISIBILITY_CHANGED_TRUE)))
            .put(INVISIBLE_NOTIF_STATE, new Transition(INVISIBLE_NOTIF_STATE, INTERACTIVE_STATE,
                    Event.PRESS_SIDE_BUTTON, Lists.newArrayList(ON_VISIBILITY_CHANGED_TRUE)))
            .put(INVISIBLE_NOTIF_STATE, new Transition(INVISIBLE_NOTIF_STATE, AMBIENT_STATE,
                    Event.STANDBY, Lists.newArrayList(ON_AMBIENT_MODE_CHANGED_TRUE, ON_VISIBILITY_CHANGED_TRUE)))
            // from Invisible WFPicker
            .put(INVISIBLE_WF_PICKER_STATE, new Transition(INVISIBLE_WF_PICKER_STATE, INTERACTIVE_STATE,
                    Event.PRESS_SIDE_BUTTON, Lists.newArrayList(ON_VISIBILITY_CHANGED_TRUE)))
            .put(INVISIBLE_WF_PICKER_STATE, new Transition(INVISIBLE_WF_PICKER_STATE, INTERACTIVE_STATE,
                    Event.STANDBY, Lists.newArrayList(ON_VISIBILITY_CHANGED_TRUE)))
            .put(INVISIBLE_WF_PICKER_STATE, new Transition(INVISIBLE_WF_PICKER_STATE, NULL_STATE,
                    Event.DESELECT, Lists.newArrayList(ON_DESTROY, WF_ON_DESTROY)))
            // from Null
            .put(NULL_STATE, new Transition(NULL_STATE, INTERACTIVE_STATE,
                    Event.SELECT, Lists.newArrayList(WF_ON_CREATE, WF_ON_CREATE_ENGINE, ON_CREATE, ON_VISIBILITY_CHANGED_TRUE))).build();
  }

  private Map<Callback, SootMethod> callbackMap = Maps.newHashMap();

  // Each callback may have multiple labels, e.g., first unregister then register
  private SetMultimap<Callback, Label> labelMap = HashMultimap.create();
  private SetMultimap<Callback, Label> releaseLabelMap = HashMultimap.create();
  private SetMultimap<Callback, Label> acquireLabelMap = HashMultimap.create();

  private SetMultimap<Callback, SetMultimap<Value, Integer>> antiAliasMap = HashMultimap.create();
  private SetMultimap<Callback, SetMultimap<Value, Integer>> colorMap = HashMultimap.create();

  public Set<SetMultimap<Value, Integer>> getAntiAlias(Callback cb) {
    return antiAliasMap.get(cb);
  }

  public Set<SetMultimap<Value, Integer>> getSetColor(Callback cb) {
    return colorMap.get(cb);
  }

  public Set<Label> getLabel(Callback cb) {
    return labelMap.get(cb);
  }

  public Set<Label> getAcqLabel(Callback cb) {
    return acquireLabelMap.get(cb);
  }

  public Set<Label> getRelLabel(Callback cb) {
    return releaseLabelMap.get(cb);
  }

  public Set<Callback> getLabelledCallbacks() {
    return labelMap.keySet();
  }

  public Set<Callback> getCallbacks() {
    return callbackMap.keySet();
  }

  public SootMethod getConcreteCallback(Callback cb) {
    return callbackMap.get(cb);
  }

  private SootClass wfClz;

  public SootClass getWatchFaceClass() {
    return wfClz;
  }

  public static STG construct(SootClass wfClass, boolean lookupAPI) {
    STG stg = new STG();
    stg.wfClz = wfClass;

    Map<Callback, SootMethod> map = Analyzer.v().getCorrespondingCallbackMethods(wfClass);
    stg.callbackMap.put(ON_DRAW, map.getOrDefault(ON_DRAW, null));
    stg.callbackMap.put(WF_ON_CREATE, map.getOrDefault(WF_ON_CREATE, null));
    stg.callbackMap.put(WF_ON_DESTROY, map.getOrDefault(WF_ON_DESTROY, null));
    stg.callbackMap.put(WF_ON_CREATE_ENGINE, map.getOrDefault(WF_ON_CREATE_ENGINE, null));
    stg.callbackMap.put(ON_CREATE, map.getOrDefault(ON_CREATE, null));
    stg.callbackMap.put(ON_DESTROY, map.getOrDefault(ON_DESTROY, null));
    stg.callbackMap.put(ON_AMBIENT_MODE_CHANGED_TRUE, map.getOrDefault(ON_AMBIENT_MODE_CHANGED_TRUE, null));
    stg.callbackMap.put(ON_AMBIENT_MODE_CHANGED_FALSE, map.getOrDefault(ON_AMBIENT_MODE_CHANGED_FALSE, null));
    stg.callbackMap.put(ON_VISIBILITY_CHANGED_TRUE, map.getOrDefault(ON_VISIBILITY_CHANGED_TRUE, null));
    stg.callbackMap.put(ON_VISIBILITY_CHANGED_FALSE, map.getOrDefault(ON_VISIBILITY_CHANGED_FALSE, null));

    if (lookupAPI) {
      Analyzer.v().lookup(stg, WF_ON_CREATE, stg.callbackMap.get(WF_ON_CREATE), stg.labelMap, stg.acquireLabelMap, stg.releaseLabelMap, stg.antiAliasMap, stg.colorMap, false);
      Analyzer.v().lookup(stg, WF_ON_DESTROY, stg.callbackMap.get(WF_ON_DESTROY), stg.labelMap, stg.acquireLabelMap, stg.releaseLabelMap, stg.antiAliasMap, stg.colorMap, false);
      Analyzer.v().lookup(stg, WF_ON_CREATE_ENGINE, stg.callbackMap.get(WF_ON_CREATE_ENGINE), stg.labelMap, stg.acquireLabelMap, stg.releaseLabelMap, stg.antiAliasMap, stg.colorMap, false);
      Analyzer.v().lookup(stg, ON_CREATE, stg.callbackMap.get(ON_CREATE), stg.labelMap, stg.acquireLabelMap, stg.releaseLabelMap, stg.antiAliasMap, stg.colorMap, false);
      Analyzer.v().lookup(stg, ON_DESTROY, stg.callbackMap.get(ON_DESTROY), stg.labelMap, stg.acquireLabelMap, stg.releaseLabelMap, stg.antiAliasMap, stg.colorMap, false);
      Analyzer.v().lookupWithParam(stg, ON_AMBIENT_MODE_CHANGED_TRUE,
              stg.callbackMap.get(ON_AMBIENT_MODE_CHANGED_TRUE), true, true, stg.labelMap, stg.acquireLabelMap, stg.releaseLabelMap, stg.antiAliasMap, stg.colorMap, false);
      Analyzer.v().lookupWithParam(stg, ON_AMBIENT_MODE_CHANGED_FALSE,
              stg.callbackMap.get(ON_AMBIENT_MODE_CHANGED_FALSE), false, true, stg.labelMap, stg.acquireLabelMap, stg.releaseLabelMap, stg.antiAliasMap, stg.colorMap, false);
      Analyzer.v().lookupWithParam(stg, ON_VISIBILITY_CHANGED_TRUE,
              stg.callbackMap.get(ON_VISIBILITY_CHANGED_TRUE), true, true, stg.labelMap, stg.acquireLabelMap, stg.releaseLabelMap, stg.antiAliasMap, stg.colorMap, false);
      Analyzer.v().lookupWithParam(stg, ON_VISIBILITY_CHANGED_FALSE,
              stg.callbackMap.get(ON_VISIBILITY_CHANGED_FALSE), false, false, stg.labelMap, stg.acquireLabelMap, stg.releaseLabelMap, stg.antiAliasMap, stg.colorMap, false);
    }
    return stg;
  }

  public void lookupAPI(Callback cb, SetMultimap<Callback, Label> labelMap, SetMultimap<Callback, Label> releaseLabelMap, SetMultimap<Callback, Label> acquireLabelMap, SetMultimap<Callback, SetMultimap<Value, Integer>> colorMap) {
    lookupAPI(cb, labelMap, releaseLabelMap, acquireLabelMap, colorMap, true);
  }

  public void lookupAPI(Callback cb, SetMultimap<Callback, Label> labelMap, SetMultimap<Callback, Label> releaseLabelMap, SetMultimap<Callback, Label> acquireLabelMap, SetMultimap<Callback, SetMultimap<Value, Integer>> colorMap, boolean retainAlias) {
    if (cb.equals(WF_ON_CREATE))
      Analyzer.v().lookup(this, WF_ON_CREATE, callbackMap.get(WF_ON_CREATE), labelMap, acquireLabelMap, releaseLabelMap, this.antiAliasMap, colorMap == null ? this.colorMap : colorMap, retainAlias);
    else if (cb.equals(WF_ON_DESTROY))
      Analyzer.v().lookup(this, WF_ON_DESTROY, callbackMap.get(WF_ON_DESTROY), labelMap, acquireLabelMap, releaseLabelMap, this.antiAliasMap, colorMap == null ? this.colorMap : colorMap, retainAlias);
    else if (cb.equals(WF_ON_CREATE_ENGINE))
      Analyzer.v().lookup(this, WF_ON_CREATE_ENGINE, callbackMap.get(WF_ON_CREATE_ENGINE), labelMap, acquireLabelMap, releaseLabelMap, this.antiAliasMap, colorMap == null ? this.colorMap : colorMap, retainAlias);
    else if (cb.equals(ON_CREATE))
      Analyzer.v().lookup(this, ON_CREATE, callbackMap.get(ON_CREATE), labelMap, acquireLabelMap, releaseLabelMap, this.antiAliasMap, colorMap == null ? this.colorMap : colorMap, retainAlias);
    else if (cb.equals(ON_DESTROY))
      Analyzer.v().lookup(this, ON_DESTROY, callbackMap.get(ON_DESTROY), labelMap, acquireLabelMap, releaseLabelMap, this.antiAliasMap, colorMap == null ? this.colorMap : colorMap, retainAlias);
    else if (cb.equals(ON_AMBIENT_MODE_CHANGED_TRUE))
      Analyzer.v().lookupWithParam(this, ON_AMBIENT_MODE_CHANGED_TRUE, this.callbackMap.get(ON_AMBIENT_MODE_CHANGED_TRUE), true, true, labelMap, acquireLabelMap, releaseLabelMap, this.antiAliasMap, colorMap == null ? this.colorMap : colorMap, retainAlias);
    else if (cb.equals(ON_AMBIENT_MODE_CHANGED_FALSE))
      Analyzer.v().lookupWithParam(this, ON_AMBIENT_MODE_CHANGED_FALSE, this.callbackMap.get(ON_AMBIENT_MODE_CHANGED_FALSE), false, true, labelMap, acquireLabelMap, releaseLabelMap, this.antiAliasMap, colorMap == null ? this.colorMap : colorMap, retainAlias);
    else if (cb.equals(ON_VISIBILITY_CHANGED_TRUE))
      Analyzer.v().lookupWithParam(this, ON_VISIBILITY_CHANGED_TRUE, this.callbackMap.get(ON_VISIBILITY_CHANGED_TRUE), true, true, labelMap, acquireLabelMap, releaseLabelMap, this.antiAliasMap, colorMap == null ? this.colorMap : colorMap, retainAlias);
    else if (cb.equals(ON_VISIBILITY_CHANGED_FALSE))
      Analyzer.v().lookupWithParam(this, ON_VISIBILITY_CHANGED_FALSE, this.callbackMap.get(ON_VISIBILITY_CHANGED_FALSE), false, false, labelMap, acquireLabelMap, releaseLabelMap, this.antiAliasMap, colorMap == null ? this.colorMap : colorMap, retainAlias);
  }

  public void reset() {
    clearMaps();
  }

  private void clearMaps() {
    this.labelMap.clear();
    this.acquireLabelMap.clear();
    this.releaseLabelMap.clear();
    this.colorMap.clear();
    this.antiAliasMap.clear();
  }
}

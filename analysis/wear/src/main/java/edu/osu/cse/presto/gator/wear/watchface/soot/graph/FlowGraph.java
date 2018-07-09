/*
 * FlowGraph.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot.graph;

import android.graphics.Color;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.gator.Log;
import edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds.*;
import edu.osu.cse.presto.gator.wear.watchface.util.ClassUtil;
import edu.osu.cse.presto.gator.wear.watchface.util.GraphUtil;
import edu.osu.cse.presto.gator.wear.watchface.util.Hierarchy;
import edu.osu.cse.presto.gator.wear.watchface.util.JimpleUtil;
import soot.*;
import soot.jimple.*;
import soot.shimple.PhiExpr;
import soot.shimple.Shimple;
import soot.toolkits.scalar.Pair;
import soot.toolkits.scalar.ValueUnitPair;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class FlowGraph {
  private static final Log LOG = Log.Factory.getLog(FlowGraph.class.getSimpleName());


  private Hierarchy hier = Hierarchy.v();

  private Set<NNode> allNNodes = Sets.newTreeSet(Comparator.comparingInt(o -> o.id));
  private Map<Local, NVarNode> allNVarNodes = Maps.newHashMap();
  private Map<SootField, NFieldNode> allNFieldNodes = Maps.newHashMap();
  private Map<Integer, NIntegerConstantNode> allIntegerConstNodes = Maps.newHashMap();
  private Map<String, NStringConstantNode> allNStringConstantNodes = Maps.newHashMap();
  private Map<Expr, NAllocNode> allNAllocNodes = Maps.newHashMap();
  private Map<SootClass, NAllocNode> cls2node = Maps.newHashMap();

  private static FlowGraph instance;

  public static synchronized FlowGraph v() {
    if (instance == null) {
      instance = new FlowGraph();
      instance.processApplicationClasses();
    } else {
//      LOG.debug("Flow graph has been constructed earlier, just return.");
    }
    return instance;
  }

  // -------------------------------------------------------
  // build flow graph
  private void processApplicationClasses() {
    // Now process each "ordinary" statements
    int totalClz = 0;
    int totalMtd = 0;
    int totalStm = 0;
    for (SootClass c : Scene.v().getApplicationClasses()) {
      if (ClassUtil.isLibraryClass(c))
        continue;
      ++totalClz;
      for (SootMethod currentMethod : Iterables.unmodifiableIterable(c.getMethods())) {
        if (!currentMethod.isConcrete()) {
          continue;
        }
        ++totalMtd;
        Body b = currentMethod.retrieveActiveBody();
        for (Unit unit : b.getUnits()) {
          ++totalStm;
          Stmt currentStmt = (Stmt) unit;
          if (currentStmt instanceof ReturnVoidStmt) {
            continue;
          }
          if (currentStmt instanceof ThrowStmt) {
            continue;
          }
          if (currentStmt instanceof GotoStmt) {
            continue;
          }
          if (currentStmt instanceof BreakpointStmt) {
            continue;
          }
          if (currentStmt instanceof NopStmt) {
            continue;
          }
          if (currentStmt instanceof RetStmt) {
            continue;
          }
          if (currentStmt instanceof IfStmt) {
            continue;
          }
          if (currentStmt instanceof TableSwitchStmt) {
            continue;
          }
          if (currentStmt instanceof LookupSwitchStmt) {
            continue;
          }
          if (currentStmt instanceof MonitorStmt) {
            continue;
          }
          JimpleUtil.v().record(currentStmt, currentMethod); // remember the method

          // Some "special" handling of calls
          if (currentStmt.containsInvokeExpr()) {
            InvokeExpr ie = currentStmt.getInvokeExpr();
            SootMethod stm;
            try {
              stm = ie.getMethod(); // static target
            } catch (Exception e) {
              continue;
            }

            // Model Android framework calls
            NOpNode opNode = null;
            try {
              opNode = getOrCreateOpNode(currentStmt);
            } catch (Exception e) {
              e.printStackTrace();
              LOG.error(e, currentStmt);
            }
            if (opNode != null && opNode != NOpNode.NullNode) {
              allNNodes.add(opNode);
              continue;
            }
            // It is an operation node, but with missing parameters. So, there
            // is no point continue matching other cases.
            if (opNode == NOpNode.NullNode) {
              continue;
            }

            // flow graph edges at non-virtual calls
            if (ie instanceof StaticInvokeExpr
                    || ie instanceof SpecialInvokeExpr) {
              if (stm.getDeclaringClass().isApplicationClass()) {
                processFlowAtCall(currentStmt, stm);
              }
              continue;
            }

            // flow graph edges at virtual calls
            Local rcv_var = JimpleUtil.v().receiver(ie);
            Type rcv_t = rcv_var.getType();
            // could be ArrayType, for clone() calls
            if (!(rcv_t instanceof RefType)) {
              continue;
            }
            SootClass stc = ((RefType) rcv_t).getSootClass();
            hier.CHA(stm, stc, target -> processFlowAtCall(currentStmt, target));
            continue;
          } // the statement was a call

          // assignment (but not with a call; calls are already handled)
          if (!(currentStmt instanceof DefinitionStmt)) {
            continue;
          }
          DefinitionStmt ds = (DefinitionStmt) currentStmt;
          Value lhs = ds.getLeftOp();
          // filter based on types
//                    if (!JimpleUtil.v().interesting(lhs.getType())) {
//                        continue;
//                    }
          Value rhs = ds.getRightOp();
          if (rhs instanceof CaughtExceptionRef) {
            continue;
          }
          // parameter passing taken care of by processFlowAtCall
          if (rhs instanceof ParameterRef) {
            continue;
          }
          if (rhs instanceof ThisRef) {
            if (ClassUtil.isWatchFaceEngine(c)) {
              NNode nn_lhs = simpleNode(lhs), nn_rhs = fakeThisNode((ThisRef) rhs);
              if (nn_lhs != null && nn_rhs != null) {
                nn_rhs.addEdgeTo(nn_lhs, currentStmt);
              }
            }
            continue;
          }

          NNode nn_lhs = simpleNode(lhs), nn_rhs = simpleNode(rhs);
          // create the flow edge
          if (nn_lhs != null && nn_rhs != null) {
            nn_rhs.addEdgeTo(nn_lhs, currentStmt);
          }
        } // all statements in the method body
      } // all methods in an application class
    } // all application classes

    LOG.info("@@@@ Total Class:   %d", totalClz);
    LOG.info("@@@@ Total Methods: %d", totalMtd);
    LOG.info("@@@@ Total Stmts:   %d", totalStm);
  }

  private void processFlowAtCall(Stmt callSite, SootMethod callee) {
    // Check & filter
    InvokeExpr ie = callSite.getInvokeExpr();
    if (!callee.getDeclaringClass().isApplicationClass()) {
      throw new RuntimeException();
    }
    if (!callee.isConcrete()) {
      return; // could happen for native methods
    }
    // Parameter binding
    Body b = callee.retrieveActiveBody();
    Iterator<Unit> stmts = b.getUnits().iterator();
    int num_param = callee.getParameterCount();
    if (!callee.isStatic()) {
      num_param++;
    }
    Local receiverLocal;
    for (int i = 0; i < num_param; i++) {
      //we have seen strange cases, in which method have empty bodies.
      if (!stmts.hasNext()) {
        return;
      }
      Stmt s = (Stmt) stmts.next();
      Value actual;
      if (ie instanceof InstanceInvokeExpr) {
        if (i == 0) {
          receiverLocal = JimpleUtil.v().receiver(ie);
          actual = receiverLocal;
        } else {
          actual = ie.getArg(i - 1);
        }
      } else {
        actual = ie.getArg(i);
      }

      //Here is an example where the method body does not read the formal Param.
      //From <com.amazon.inapp.purchasing.PurchasingObserver: void onContentDownloadResponse(com.amazon.inapp.purchasing.ContentDownloadResponse)> in WSJ
      // $r0 = new java.lang.Error;
      // specialinvoke $r0.<java.lang.Error: void <init>(java.lang.String)>("Unresolved compilation error: Method <com.amazon.inapp.purchasing.PurchasingObserver: void onContentDownloadResponse(com.amazon.inapp.purchasing.ContentDownloadResponse)> does not exist!");
      // throw $r0;
      if (!(s instanceof DefinitionStmt)) {
        return;
      }

      Local formal = JimpleUtil.v().lhsLocal(s);
      NVarNode lhsNode = varNode(formal);
      NNode rhsNode = simpleNode(actual);
      if (rhsNode != null) {
        rhsNode.addEdgeTo(lhsNode, callSite);
      }
    }

    // Now, do something for the return
    if (callSite instanceof InvokeStmt) {
      return; // ret ignored
    }

    Local lhs_at_call = JimpleUtil.v().lhsLocal(callSite);
    NNode lhsNode = varNode(lhs_at_call);
    while (stmts.hasNext()) {
      Stmt d = (Stmt) stmts.next();
      if (!(d instanceof ReturnStmt)) {
        continue;
      }
      Value retval = ((ReturnStmt) d).getOp();
      NNode returnValueNode = simpleNode(retval);
      if (returnValueNode != null) {
        returnValueNode.addEdgeTo(lhsNode, callSite);

        // TODO: since library classes are not processed, we just connect the parameters to the return
        if (callee.getDeclaringClass().getName().startsWith("android.support")
                || callee.getDeclaringClass().getName().startsWith("com.google")) {

        }
      }
    }
  }

  private NNode simpleNode(Value jimpleValue) {
    if (Shimple.isPhiExpr(jimpleValue)) {
      return phiNode(jimpleValue);
    }
    if (jimpleValue instanceof FieldRef) {
      return fieldNode(((FieldRef) jimpleValue).getField());
    }
    if (jimpleValue instanceof Local) {
      return varNode((Local) jimpleValue);
    }
    if (jimpleValue instanceof IntConstant) {
      Integer integerConstant = ((IntConstant) jimpleValue).value;
      return integerConstantNode(integerConstant);
    }
    if (jimpleValue instanceof StringConstant) {
      String stringValue = ((StringConstant) jimpleValue).value;
      return stringConstantNode(stringValue);
    }
    if (jimpleValue instanceof NewExpr || jimpleValue instanceof NewArrayExpr
            || jimpleValue instanceof NewMultiArrayExpr) {
      return allocNodeOrSpecialObjectNode((Expr) jimpleValue);
    }
    if (jimpleValue instanceof CastExpr) {
      return simpleNode(((CastExpr) jimpleValue).getOp());
    }
    if (jimpleValue instanceof OrExpr) {
      Value op1 = ((OrExpr) jimpleValue).getOp1();
      Value op2 = ((OrExpr) jimpleValue).getOp2();
      if (op1 instanceof IntConstant && op2 instanceof IntConstant) {
        int v1 = ((IntConstant) op1).value, v2 = ((IntConstant) op2).value;
        int res = v1 | v2;
        return new NIntegerConstantNode(res);
      }
      NNode n1 = simpleNode(op1);
      NNode n2 = simpleNode(op2);
      if (n1 != null && n2 != null)
        return new NOrExprNode((OrExpr) jimpleValue, n1, n2);
    }
    return null;
  }

  private NAllocNode fakeThisNode(ThisRef ref) {
    SootClass clz = ((RefType) ref.getType()).getSootClass();
    NAllocNode n = cls2node.get(clz);
    if (n != null) {
      return n;
    }
    n = new NFakeThisNode(clz);
    cls2node.put(clz, n);
    allNNodes.add(n);
    return n;
  }

  public NPhiNode phiNode(Value jimpleValue) {
    PhiExpr phiExpr = (PhiExpr) jimpleValue;
    NPhiNode ret = new NPhiNode(phiExpr);
    allNNodes.add(ret);
    for (ValueUnitPair p : phiExpr.getArgs()) {
      Value arg = p.getValue();
      simpleNode(arg).addEdgeTo(ret);
    }
    return ret;
  }

  private NStringConstantNode stringConstantNode(String value) {
    Preconditions.checkNotNull(value);
    NStringConstantNode stringConstantNode = allNStringConstantNodes.get(value);
    if (stringConstantNode == null) {
      stringConstantNode = new NStringConstantNode();
      stringConstantNode.value = value;
      allNStringConstantNodes.put(value, stringConstantNode);
      allNNodes.add(stringConstantNode);
    }
    return stringConstantNode;
  }

  private NIntegerConstantNode integerConstantNode(Integer value) {
    Preconditions.checkNotNull(value);
    NIntegerConstantNode integerConstantNode = allIntegerConstNodes.get(value);
    if (integerConstantNode == null) {
      integerConstantNode = new NIntegerConstantNode(value);
      allIntegerConstNodes.put(value, integerConstantNode);
      allNNodes.add(integerConstantNode);
    }
    return integerConstantNode;
  }

  private NObjectNode allocNodeOrSpecialObjectNode(Expr e) {
    return allocNode(e);
  }

  private NAllocNode allocNode(Expr e) {
    NAllocNode x = allNAllocNodes.get(e);
    if (x != null) {
      return x;
    }
    x = new NAllocNode(e);
    allNAllocNodes.put(e, x);
    cls2node.put(x.getClassType(), x);
    allNNodes.add(x);
    return x;
  }

  private NFieldNode fieldNode(SootField f) {
    NFieldNode x = allNFieldNodes.get(f);
    if (x != null) {
      return x;
    }
    x = new NFieldNode();
    x.f = f;
    allNFieldNodes.put(f, x);
    allNNodes.add(x);
    return x;
  }


  private NVarNode varNode(Local l) {
    if (l == null)
      return null;
    NVarNode x = allNVarNodes.get(l);
    if (x != null) {
      return x;
    }
    x = new NVarNode(l);
    allNVarNodes.put(l, x);
    allNNodes.add(x);
    return x;
  }

  // Op Nodes
  public NOpNode getOrCreateOpNode(Stmt s) {
    NOpNode node = NOpNode.lookupByStmt(s);
    if (node != null) {
      return node;
    }
    // GetSensorObject: lhs = SensorManager.getDefaultSensor
    {
      NOpNode getSensor = createGetSensorOpNode(s);
      if (getSensor != null) {
        return getSensor;
      }
    }

    // SensorManager.registerListener(...)
    {
      NOpNode registerListener = createRegisterSensorListenerOpNode(s);
      if (registerListener != null) {
        return registerListener;
      }
    }

    // SensorManager.unregisterListener(...)
    // This should be taken care in the near future
    {
      NOpNode unregisterListener = createUnregisterSensorListenerOpNode(s);
      if (unregisterListener != null) {
        return unregisterListener;
      }
    }

//    {
//      NOpNode opNode = createRequestLocationUpdateOpNode(s);
//      if (opNode != null) {
//        return opNode;
//      }
//    }
//
//    {
//      NOpNode opNode = createRemoveLocationUpdateOpNode(s);
//      if (opNode != null) {
//        return opNode;
//      }
//    }

    {
      NOpNode opNode = createContainerGetOpNode(s);
      if (opNode != null) {
        return opNode;
      }
    }

    // Paint
    {
      NOpNode opNode = createPaintSetColorOpNode(s);
      if (opNode != null) {
        return opNode;
      }
    }
    {
      NOpNode opNode = createPaintSetAntiAliasOpNode(s);
      if (opNode != null) {
        return opNode;
      }
    }

    // Color
    {
      NOpNode opNode = createColorParseColorOpNode(s);
      if (opNode != null) {
        return opNode;
      }
    }

    // Canvas
    {
      NOpNode opNode = createCanvasDrawColorOpNode(s);
      if (opNode != null) {
        return opNode;
      }
    }

    return null;
  }

  private NOpNode createCanvasDrawColorOpNode(Stmt s) {
    InvokeExpr ie = s.getInvokeExpr();
    SootMethod callee = ie.getMethod();
    SootMethod caller = JimpleUtil.v().lookup(s);
    String sig = callee.getSignature();

    if (!ClassUtil.CanvasAPI.DRAW_COLOR_SIGS.keySet().contains(sig)) {
      return null;
    }

    Value colorVal = ie.getArg(ClassUtil.CanvasAPI.DRAW_COLOR_SIGS.get(sig));
    NNode colorNode = simpleNode(colorVal);
    if (colorNode == null) {
      LOG.warn("Null coor for " + s + "@" + caller);
      return NOpNode.NullNode;
    }
    NVarNode rcvNode = varNode(JimpleUtil.v().receiver(s));
    return new NCanvasDrawColorOpNode(colorNode, rcvNode, new Pair<>(s, caller));
  }

  private NOpNode createColorParseColorOpNode(Stmt s) {
    InvokeExpr ie = s.getInvokeExpr();
    SootMethod callee = ie.getMethod();
    SootMethod caller = JimpleUtil.v().lookup(s);

    if (!callee.getSignature().equals(Color.PARSE_COLOR_SIG)) {
      return null;
    }

    Value colorValue = ie.getArg(0);
    NNode colorNode = simpleNode(colorValue);
    if (colorNode == null) {
      LOG.warn("Null color for " + s + "@" + caller);
      return NOpNode.NullNode;
    }
    NVarNode lhsVarNode = varNode(JimpleUtil.v().lhsLocal(s));
    return new NColorParseColorOpNode(colorNode, lhsVarNode, new Pair<>(s, caller));
  }

  private NOpNode createPaintSetColorOpNode(Stmt s) {
    InvokeExpr ie = s.getInvokeExpr();
    SootMethod callee = ie.getMethod();
    SootMethod caller = JimpleUtil.v().lookup(s);

    if (!callee.getSignature().equals(ClassUtil.PaintAPI.SET_COLOR_SIG)) {
      return null;
    }
    Value colorValue = ie.getArg(0);
    NNode colorNode = simpleNode(colorValue);
    if (colorNode == null) {
      LOG.warn("Null color for " + s + "@" + caller);
      return NOpNode.NullNode;
    }
    NVarNode rcvVarNode = varNode(JimpleUtil.v().receiver(ie));
    return new NPaintSetColorOpNode(colorNode, rcvVarNode, new Pair<>(s, caller));
  }

  private NOpNode createPaintSetAntiAliasOpNode(Stmt s) {
    InvokeExpr ie = s.getInvokeExpr();
    SootMethod callee = ie.getMethod();
    SootMethod caller = JimpleUtil.v().lookup(s);

    if (!callee.getSignature().equals(ClassUtil.PaintAPI.SET_ANTI_ALIAS_SIG)) {
      return null;
    }
    Value boolValue = ie.getArg(0);
    NNode boolNode = simpleNode(boolValue);
    if (boolNode == null) {
      LOG.warn("Null color for " + s + "@" + caller);
      return NOpNode.NullNode;
    }
    NVarNode rcvVarNode = varNode(JimpleUtil.v().receiver(ie));
    return new NPaintSetAntiAliasOpNode(boolNode, rcvVarNode, new Pair<>(s, caller));
  }

  private NOpNode createContainerGetOpNode(Stmt s) {
    InvokeExpr ie = s.getInvokeExpr();
    SootMethod callee = ie.getMethod();
    SootMethod caller = JimpleUtil.v().lookup(s);
    String sig = callee.getSignature();

    if (!sig.equals("<java.util.List: java.lang.Object get(int)>")) {
      return null;
    }

    if (!(s instanceof DefinitionStmt)) {
      return null;
    }
    NVarNode rcvVarNode = varNode(JimpleUtil.v().receiver(ie));
    NVarNode lhsVarNode = varNode(JimpleUtil.v().lhsLocal(s));
    return new NContainerGetOpNode(rcvVarNode, lhsVarNode, new Pair<>(s, caller));
  }

  private NOpNode createGetSensorOpNode(Stmt s) {
    InvokeExpr ie = s.getInvokeExpr();
    SootMethod callee = ie.getMethod();
    SootMethod caller = JimpleUtil.v().lookup(s);
    String sig = callee.getSignature();

    if (!ClassUtil.SensorManagerAPI.GET_SENSOR_SIGS.keySet().contains(sig)) {
      return null;
    }

    if (!(s instanceof DefinitionStmt)) {
      return null;
    }

    //The return value is always an instance of Sensor.
    //Don't need to worry about it.
    Value sensorIdValue = ie.getArg(ClassUtil.SensorManagerAPI.GET_SENSOR_SIGS.get(sig));
    NNode sensorIdNode = simpleNode(sensorIdValue);
    if (sensorIdNode == null) {
      LOG.warn("Null Sensor Id for " + s + "@" + caller);
      return NOpNode.NullNode;
    }
    NVarNode lhsVarNode = varNode(JimpleUtil.v().lhsLocal(s));
    return new NSensorGetSensorOpNode(sensorIdNode, lhsVarNode, new Pair<>(s, caller), false);
  }

  private NOpNode createRegisterSensorListenerOpNode(Stmt s) {
    InvokeExpr ie = s.getInvokeExpr();
    SootMethod callee = ie.getMethod();
    String sig = callee.getSignature();
    SootMethod caller = JimpleUtil.v().lookup(s);

    if (!ClassUtil.SensorAPI.REGISTER_LISTENER_SIGS.keySet().contains(sig)) {
      return null;
    }

    //It's a sensor registration
    Value sensorListenerObj = ie.getArg(ClassUtil.SensorAPI.REGISTER_LISTENER_SIGS.get(sig).getMiddle());
    if (!(sensorListenerObj instanceof Local)) {
      throw new RuntimeException("listener object in registration is not local at " + s);
    }
    Value sensorObjVar = ie.getArg(ClassUtil.SensorAPI.REGISTER_LISTENER_SIGS.get(sig).getRight());
    if (!(sensorObjVar instanceof Local)) {
      throw new RuntimeException("sensor object in registration is not local at " + s);
    }

    NVarNode sensorNode = varNode((Local) sensorObjVar);
    NVarNode listenerNode = varNode((Local) sensorListenerObj);

    return new NSensorRegisterListenerOpNode(sensorNode, listenerNode, new Pair<>(s, caller), false);
  }

  private NOpNode createUnregisterSensorListenerOpNode(Stmt s) {
    InvokeExpr ie = s.getInvokeExpr();
    SootMethod callee = ie.getMethod();
    String sig = callee.getSignature();
    SootMethod caller = JimpleUtil.v().lookup(s);

    if (!ClassUtil.SensorAPI.UNREGISTER_LISTENER_SIGS.keySet().contains(sig)) {
      return null;
    }

    //It's a sensor unregistration
    Value sensorListenerObj = ie.getArg(ClassUtil.SensorAPI.UNREGISTER_LISTENER_SIGS.get(sig).getMiddle());
    if (!(sensorListenerObj instanceof Local)) {
      throw new RuntimeException("listener object in unregistration is not local at " + s);
    }
    Value sensorObjVar = null;
    if (ClassUtil.SensorAPI.UNREGISTER_LISTENER_SIGS.get(sig).getRight() != -1) {
      sensorObjVar = ie.getArg(ClassUtil.SensorAPI.UNREGISTER_LISTENER_SIGS.get(sig).getRight());
      if (!(sensorObjVar instanceof Local)) {
        throw new RuntimeException("sensor object in unregistration is not local at " + s);
      }
    }

    NVarNode sensorNode = varNode((Local) sensorObjVar);
    NVarNode listenerNode = varNode((Local) sensorListenerObj);

    return new NSensorUnregisterListenerOpNode(sensorNode, listenerNode, new Pair<>(s, caller), false);
  }

  private NOpNode createRequestLocationUpdateOpNode(Stmt s) {
    InvokeExpr ie = s.getInvokeExpr();
    SootMethod callee = ie.getMethod();
    String sig = callee.getSignature();
    SootMethod caller = JimpleUtil.v().lookup(s);

    if (!ClassUtil.LocationAPI.REQUEST_UPDATES_SIGS.keySet().contains(sig)) {
      return null;
    }

    Integer listenerPos = ClassUtil.LocationAPI.REQUEST_UPDATES_SIGS.get(sig).getMiddle();
    if (listenerPos == -1) {
      return null;
    }
    Value listenerObj = ie.getArg(listenerPos);
    if (!(listenerObj instanceof Local)) {
      throw new RuntimeException("listener object in registration is not local at " + s);
    }
    NVarNode listenerNode = varNode((Local) listenerObj);

    return new NLocationRequestUpdateOpNode(listenerNode, new Pair<>(s, caller), false);
  }

  private NOpNode createRemoveLocationUpdateOpNode(Stmt s) {
    InvokeExpr ie = s.getInvokeExpr();
    SootMethod callee = ie.getMethod();
    String sig = callee.getSignature();
    SootMethod caller = JimpleUtil.v().lookup(s);

    if (!ClassUtil.LocationAPI.REMOVE_UPDATES_SIGS.keySet().contains(sig)) {
      return null;
    }

    Integer listenerPos = ClassUtil.LocationAPI.REMOVE_UPDATES_SIGS.get(sig).getMiddle();
    if (listenerPos == -1) {
      return null;
    }
    Value listenerObj = ie.getArg(listenerPos);
    if (!(listenerObj instanceof Local)) {
      throw new RuntimeException("listener object in registration is not local at " + s);
    }
    NVarNode listenerNode = varNode((Local) listenerObj);

    return new NLocationRemoveUpdateOpNode(listenerNode, new Pair<>(s, caller), false);
  }

  public NNode lookupNode(Value x) {
    if (x instanceof FieldRef) {
      return allNFieldNodes.get(((FieldRef) x).getField());
    }
    if (x instanceof Local) {
      return allNVarNodes.get(x);
    }
    if (x instanceof IntConstant) {
      return allIntegerConstNodes.get(((IntConstant) x).value);
    }
    return null;
  }

  /***********************
   * debug purpose
   ***********************/
  private void writeNodes(BufferedWriter writer, Set<NNode> insterestingNode) throws IOException {
    for (NNode reach : insterestingNode) {
      Integer label = reach.id;
      String tag = reach.toString();
      writer.write("\n n" + label + " [label=\"");
      writer.write(tag.replace('"', '\'') + "\"];");
    }
  }

  private void writeSucc(BufferedWriter writer, NNode root, Set<Pair<NNode, NNode>> edges) throws IOException {
    for (NNode succ : root.getSuccessors()) {
      Pair<NNode, NNode> e = new Pair<>(root, succ);
      if (edges.contains(e))
        continue;
      writer.write("\n n" + root.id + " -> n" + succ.id + ";");
      edges.add(e);
      if (succ instanceof NOpNode)
        continue;
      writeSucc(writer, succ, edges);
    }
  }

  private void writePred(BufferedWriter writer, NNode root, Set<Pair<NNode, NNode>> edges) throws IOException {
    for (NNode pred : root.getPredecessors()) {
      Pair<NNode, NNode> e = new Pair<>(pred, root);
      if (edges.contains(e))
        continue;
      writer.write("\n n" + pred.id + " -> n" + root.id + ";");
      edges.add(e);
      if (pred instanceof NOpNode)
        continue;
      writePred(writer, pred, edges);
    }
  }

  public void dump(String dotFile) {
    try {
      FileWriter output = new FileWriter(dotFile);
      BufferedWriter writer = new BufferedWriter(output);

      writer.write("digraph G {");
      writer.write("\n rankdir=LR;");
      writer.write("\n node[shape=box];");
      // draw window nodes
      Set<NNode> interestingNodes = Sets.newHashSet();
//      for (NNode node : NOpNode.getNodes(NSensorRegisterListenerOpNode.class)) {
//        writeNodes(writer, GraphUtil.backwardReachableNodes(node));
//        interestingNodes.add(node);
//      }
//      for (NNode node : NOpNode.getNodes(NSensorUnregisterListenerOpNode.class)) {
//        writeNodes(writer, GraphUtil.backwardReachableNodes(node));
//        interestingNodes.add(node);
//      }
//      for (NNode node : NOpNode.getNodes(NSensorGetSensorOpNode.class)) {
//        writeNodes(writer, GraphUtil.backwardReachableNodes(node));
//        interestingNodes.add(node);
//      }
//      for (NNode node : NOpNode.getNodes(NPaintSetColorOpNode.class)) {
//        writeNodes(writer, GraphUtil.backwardReachableNodes(node));
//        interestingNodes.add(node);
//      }
//      for (NNode node : NOpNode.getNodes(NColorParseColorOpNode.class)) {
//        writeNodes(writer, GraphUtil.backwardReachableNodes(node));
//        interestingNodes.add(node);
//      }
//      for (NNode node : NOpNode.getNodes(NPaintSetAntiAliasOpNode.class)) {
//        writeNodes(writer, GraphUtil.backwardReachableNodes(node));
//        interestingNodes.add(node);
//      }
      for (NNode node : NOpNode.getNodes(NCanvasDrawColorOpNode.class)) {
        writeNodes(writer, GraphUtil.backwardReachableNodes(node));
        interestingNodes.add(node);
      }

      Set<Pair<NNode, NNode>> edges = Sets.newHashSet();
      for (NNode node : interestingNodes) {
        writePred(writer, node, edges);
      }
      LOG.info("---- total edges: %s", edges.size());
      // end of .dot file
      writer.write("\n}");
      writer.close();
      LOG.info("flow graph dump to file: " + dotFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

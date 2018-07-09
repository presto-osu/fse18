/*
 * Hierarchy.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import edu.osu.cse.presto.gator.Log;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.util.Set;

public class Hierarchy {
  private static final Log LOG = Log.Factory.getLog("Hierarchy");
  private final SetMultimap<SootClass, SootClass> class2subtypesInclusive = HashMultimap.create();
  private final SetMultimap<SootClass, SootClass> class2concreteSubtypesInclusive = HashMultimap.create();
  private final SetMultimap<SootClass, SootClass> class2supertypesInclusive = HashMultimap.create();

  public Set<SootClass> getConcreteSubtypesInclusive(SootClass c) {
    return class2concreteSubtypesInclusive.get(c);
  }

  public boolean isSubtype(final SootClass child, final SootClass parent) {
    Set<SootClass> superTypes = getSupertypes(child);
    if (superTypes != null) {
      return superTypes.contains(parent);
    }
    return isSubtypeOfOnDemand(child, parent);
  }

  public boolean isSubtypeOfOnDemand(final SootClass child, final SootClass parent) {
    if (parent.getName().equals("java.lang.Object")) {
      return true;
    }
    if (child.equals(parent)) {
      return true;
    }
    if (child.hasSuperclass()) {
      return isSubtypeOfOnDemand(child.getSuperclass(), parent);
    }
    return false;
  }

  // -----------------------------------------
  // Returns a set of SootClasses: all transitive SUPER types of c, including c
  public Set<SootClass> getSupertypes(SootClass c) {
    return class2supertypesInclusive.get(c);
  }

  // -------------------------------------------------------
  // recursive traversal of superclasses and superinterfaces
  private void traverse(SootClass sub, SootClass supr) {

    // sub is a subtype of supr (or possibly supr == sub)

    // first, add sub to the all_tbl set for supr
    class2subtypesInclusive.put(supr, sub);

    // also, add supr to the all_super_tbl set for sub
    class2supertypesInclusive.put(sub, supr);

    // next, if sub is a non-interface non-abstract class, add it
    // to the tbl set for supr
    if (sub.isConcrete()) {
      class2concreteSubtypesInclusive.put(supr, sub);
    }

    // traverse parent classes/interfaces of supr
    if (supr.hasSuperclass()) {
      traverse(sub, supr.getSuperclass());
    }

    for (SootClass sootClass : supr.getInterfaces()) {
      traverse(sub, sootClass);
    }
  }

  // ---------------------------------------------------------
  // This method simulates the effects of the virtual dispatch
  // performed by the JVM at run time. Precondition:
  // receiver_class.isConcrete() == true
  public SootMethod virtualDispatch(SootMethod staticTarget,
                                    SootClass receiverClass) {
    // check the precondition, just in case
    if (!receiverClass.isConcrete()) {
      LOG.debug("Hierarchy.virtualDispatch called with non-concrete receiver class" + receiverClass.getName());
    }
    // look up the method
    SootClass curr = receiverClass;
    while (curr != null) {
      if (curr.declaresMethod((staticTarget.getSubSignature()))) {
        return curr.getMethod(staticTarget.getSubSignature());
      }
      if (curr.hasSuperclass()) {
        curr = curr.getSuperclass();
      } else {
        curr = null; // for java.lang.Object
      }
    }

    LOG.debug("No match in Hierarchy.virtualDispatch: \n\tmethod = "
            + staticTarget + "\n\ttype = " + receiverClass);
    return null;
  }

  public interface CHAJob {
    void run(SootMethod target);
  }

  public void CHA(SootMethod staticTarget,
                  SootClass receiverClass,
                  CHAJob job) {
    for (SootClass sub : getConcreteSubtypesInclusive(receiverClass)) {
      SootMethod target = virtualDispatch(staticTarget, sub);
      if (target != null && target.getDeclaringClass().isApplicationClass()) {
        job.run(target);
      }
    }
  }

  // -----------------------------------
  private static Hierarchy instance;

  public static synchronized Hierarchy v() {
    if (instance == null) {
      instance = new Hierarchy();
      for (SootClass c : Scene.v().getClasses()) {
        instance.traverse(c, c);
      }
    }
    return instance;
  }
}

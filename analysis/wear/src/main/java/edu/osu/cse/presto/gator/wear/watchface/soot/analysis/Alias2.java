/*
 * Alias2.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot.analysis;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import soot.Value;
import soot.jimple.InstanceFieldRef;

import java.util.Comparator;
import java.util.Set;
import java.util.Stack;

public class Alias2 {
  private static final Comparator<Value> VALUE_COMPARATOR = new Comparator<Value>() {
    @Override
    public int compare(Value o1, Value o2) {
      if (o1 instanceof InstanceFieldRef && o2 instanceof InstanceFieldRef) {
//        return o1.equivHashCode() - o2.equivHashCode();
        return ((InstanceFieldRef) o1).getField().equivHashCode() - ((InstanceFieldRef) o2).getField().equivHashCode();
      }
      return o1.hashCode() - o2.hashCode();
    }
  };
  private static Stack<Multimap<Value, Value>> contextStack = new Stack<>();
  private static Multimap<Value, Value> context = TreeMultimap.create(VALUE_COMPARATOR, VALUE_COMPARATOR);

  public static void of(Value from, Value to) {
    context.put(from, to);
  }

  public static void clearContext() {
    contextStack.clear();
    context.clear();
  }

  public static void pushContext() {
    Multimap<Value, Value> oldContext = TreeMultimap.create(VALUE_COMPARATOR, VALUE_COMPARATOR);
    oldContext.putAll(context);
    contextStack.push(oldContext);
  }

  public static void popContext() {
    context = contextStack.pop();
  }

  public static void lookup(Value start, Visitor visitor) {
    lookup(start, visitor, Sets.newHashSet());
  }

  private static void lookup(Value start, Visitor visitor, Set<Value> visited) {
    if (visited.contains(start))
      return;
    visited.add(start);
    for (Value a : context.get(start)) {
      visitor.visit(a);
      lookup(a, visitor, visited);
    }
  }

  public interface Visitor {
    void visit(Value var);
  }
}

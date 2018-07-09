/*
 * Alias.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot.analysis;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import soot.SootField;
import soot.Value;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.InstanceFieldRef;
import soot.jimple.IntConstant;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public class Alias {
  private static final Comparator<Object> contextComp = new Comparator<Object>() {
    @Override
    public int compare(Object o1, Object o2) {
      if (o1 instanceof SootField && o2 instanceof SootField) {
        return ((SootField) o1).equivHashCode() - ((SootField) o2).equivHashCode();
      }
      // dirty hack for cases like "" and 0, which both have hash code of 0
      int strDiff = o1.toString().hashCode() - o2.toString().hashCode();
      return strDiff != 0 ? strDiff : o1.hashCode() - o2.hashCode();
    }
  };
  private static Map<Object, Alias> context = Maps.newTreeMap(contextComp);
  private Set<Alias> to;
  private Object var;

  private Alias(Object var) {
    this.var = var;
    this.to = Sets.newHashSet();
  }

  @Override
  public String toString() {
    return "<" + var + "=" + to + ">";
  }

  public static Alias of(Object var) {
    // TODO: currently tread field reference just as field, i.e., every field has exactly one entry
    if (var instanceof InstanceFieldRef) {
      var = ((InstanceFieldRef) var).getField();
    }
    Alias alias = context.getOrDefault(var, new Alias(var));
    context.put(var, alias);
    return alias;
  }

  public void to(Value var) {
    Alias alias = Alias.of(var);
    this.to.add(alias);
  }

  public void onlyTo(Value var) {
    Alias alias = Alias.of(var);
    this.to.clear();
    this.to.add(alias);
  }

  public static Integer solve(Object param) {
    return solve(param, Sets.newHashSet());
  }

  private static Integer solve(Object param, Set<Object> visited) {
    if (visited.contains(param)) // cycle in alias mapping
      return null;
    visited.add(param);
    Set<Integer> set = Sets.newHashSet();
    if (param instanceof IntConstant) {
      return ((IntConstant) param).value;
    } else if (param instanceof FloatConstant) {
      float v = ((FloatConstant) param).value;
      if (v == 0.0F) return 0;
      if (v == 1.0F) return 1;
    } else if (param instanceof DoubleConstant) {
      double v = ((DoubleConstant) param).value;
      if (v == 0.0F) return 0;
      if (v == 1.0F) return 1;
    } else {
      for (Alias t : Alias.of(param).to) {
        Integer res = solve(t.var, visited);
        if (res != null)
          set.add(res);
      }
    }
    if (set.size() == 1) {
      Object ret = set.toArray()[0];
      return ret == null ? null : (int) ret;
    }
    return null;
  }

  public static void clearContext() {
    context.clear();
  }
}

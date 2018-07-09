/*
 * ConstantPropagation.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot.analysis;

import com.google.common.collect.Maps;
import soot.Local;
import soot.Value;
import soot.jimple.IntConstant;
import soot.shimple.PhiExpr;

import java.util.Map;

public class ConstantPropagation {
  static class Result {
    Integer val;
    String name;

    Result(String name, Integer val) {
      this.val = val;
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  static final Result TOP = new Result("TOP", null); // not defined
  static final Result BOTTOM = new Result("BOTTOM", null); // not constant
  private static Map<Object, Result> result = Maps.newHashMap();

  public static String dump() {
    StringBuilder b = new StringBuilder();
    for (Object k : result.keySet()) {
      b.append("::").append(k).append("::").append(result.get(k)).append("\n");
    }
    return b.toString();
  }

  public static void record(Value lhs, Value rhs) {
    Result oldRes = result.getOrDefault(lhs, TOP);
    if (rhs instanceof IntConstant
            && (oldRes == TOP
            || oldRes.val != null && ((IntConstant) rhs).value == oldRes.val)) {
      int val = ((IntConstant) rhs).value;
      result.put(lhs, new Result(String.valueOf(val), val));
    } else if (rhs instanceof PhiExpr) {
      Integer intermediate = null;
      for (Value v : ((PhiExpr) rhs).getValues()) {
        Result res = result.getOrDefault(v, TOP);
        if (res == BOTTOM) {
          intermediate = null;
          break;
        }
        if (res.val != null) {
          if (intermediate == null) {
            intermediate = res.val;
          } else if (!intermediate.equals(res.val)) {
            intermediate = null;
            break;
          }
        }
        // TOP does not change intermediate result
      }
      result.put(lhs, intermediate == null ? BOTTOM : new Result(intermediate.toString(), intermediate));
    } else if (rhs instanceof Local) {
      Result res = result.getOrDefault(rhs, TOP);
      if (res == TOP) {
        // do noting
      } else if (res == BOTTOM) {
        result.put(lhs, BOTTOM);
      } else if (oldRes == TOP) {
        result.put(lhs, res);
      } else if (oldRes == BOTTOM) {
        // do noting
      } else if (!res.val.equals(oldRes.val)) {
        result.put(lhs, BOTTOM);
      }

    } else {
      result.put(lhs, BOTTOM);
    }
  }

  public static void clearContext() {
    result.clear();
  }

  public static Integer solve(Value param) {
    Result res = result.getOrDefault(param, TOP);
    return res.val;
  }
}

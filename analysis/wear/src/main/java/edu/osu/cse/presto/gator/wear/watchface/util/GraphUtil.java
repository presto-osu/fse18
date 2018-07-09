/*
 * GraphUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds.NNode;
import edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds.NOpNode;

import java.util.LinkedList;
import java.util.Set;

public class GraphUtil {
  // ------------------------
  // graph utilities
  // this method will help find all possible back reachable NNode
  // it will not stop traversing even it reaches NOpNode
  public static Set<NNode> backwardReachableNodes(NNode n) {
    // p("[BackwardReachable] " + n);
    Set<NNode> res = Sets.newHashSet();
    findBackwardReachableNodes(n, res);
    return res;
  }

  private static void findBackwardReachableNodes(NNode start, Set<NNode> reachableNodes) {
    LinkedList<NNode> worklist = Lists.newLinkedList();
    worklist.add(start);
    reachableNodes.add(start);
    while (!worklist.isEmpty()) {
      NNode n = worklist.remove();
      for (NNode s : n.getPredecessors()) {
        if (reachableNodes.contains(s)) {
          continue;
        }
        if (s instanceof NOpNode) {
          reachableNodes.add(s);
        } else {
          worklist.add(s);
          reachableNodes.add(s);
        }
      }
    }
  }
}

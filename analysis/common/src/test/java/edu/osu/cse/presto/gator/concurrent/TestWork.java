/*
 * TestWork.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.concurrent;

import edu.osu.cse.presto.gator.Log;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class TestWork {
  Job.Manager wm;
  Log LOG = Log.Factory.getLog(TestWork.class.getName());

  @Before
  public void setup() {
    wm = Job.Manager.newManager(3);
  }

  @Test
  public void testWork() throws ExecutionException, InterruptedException {
    Job<Integer, Integer> w1 = new Job<Integer, Integer>() {
      @Override
      public Integer process(Integer data) {
        for (int i = 0; i <= 100; i += 20) {
          // Perform some work ...
          System.out.println("Job number: " + 1
                  + ", percent complete: " + i);
          try {
            Thread.sleep((int) (Math.random() * 1000));
          } catch (InterruptedException e) {
          }
        }
        return 2;
      }

      @Override
      public String getName() {
        return "1";
      }

      @Override
      public Integer prepare() {
        return 1;
      }
    };

    Job<Integer, Integer> w2 = new Job<Integer, Integer>() {
      @Override
      public Integer process(Integer data) {
        for (int i = 0; i <= 100; i += 20) {
          // Perform some work ...
          System.out.println("Job number: " + 2
                  + ", percent complete: " + i);
          try {
            Thread.sleep((int) (Math.random() * 1000));
          } catch (InterruptedException e) {
          }
        }
        return 3;
      }

      @Override
      public String getName() {
        return "2";
      }

      @Override
      public Integer prepare() {
        return 2;
      }
    };

    Future<Integer> f1 = wm.submit(w1);
    Future<Integer> f2 = wm.submit(w2);
    wm.shutdown();
    LOG.info(f1.get());
    LOG.info(f2.get());
  }
}

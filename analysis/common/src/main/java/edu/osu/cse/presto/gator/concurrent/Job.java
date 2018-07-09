/*
 * Job.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.concurrent;

import edu.osu.cse.presto.gator.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * The interface for jobs to be submitted to {@link Manager#submit(Job)}.
 * The goal is to transform the raw data of type V to the resulting data of type T.
 *
 * @param <V> the input/source/raw data type
 * @param <T> the output/target/processed data type
 */
public interface Job<V, T> {
    /**
     * Prepare the data before being processed.
     * Any pre-processing work should be done in this method instead of {@link Job#process(Object)}.
     *
     * @return the data to be processed
     */
    V prepare();

    /**
     * Process the data and return the results.
     *
     * @param data the data to be processed
     * @return the processed data
     */
    T process(V data);

    /**
     * @return name of the job
     */
    String getName();

    /**
     * Manager of jobs.
     * The implementation does not support to be used across threads,
     * i.e., a second thread manages the thread pool directly. The
     * behavior is unclear in such situations.
     * Use {@link #newManager(int)} to create a new job manager.
     */
    class Manager {
        private final Log LOG = Log.Factory.getLog(Manager.class.getName());
        private ExecutorService es;
        private long workCount;

        private Manager(int poolSize) {
            this.es = Executors.newFixedThreadPool(poolSize);
            this.workCount = 0;
        }

        public static Manager newManager(int poolSize) {
            return new Manager(poolSize);
        }

        public <V, T> Future<T> submit(final Job<V, T> job) {
            return es.submit(new Callable<T>() {
                @Override
                public T call() {
                    synchronized (LOG) {
                        LOG.info("Job#" + ++workCount + " \"" + job.getName() + "\" is about to run...");
                    }
                    return job.process(job.prepare());
                }
            });
        }

        public <V, T> List<Future<T>> submit(final List<Job<V, T>> jobs) throws InterruptedException {
            List<Callable<T>> callables = new ArrayList<>(jobs.size());
            for (final Job<V, T> job : jobs) {
                callables.add(new Callable<T>() {
                    @Override
                    public T call() {
                        synchronized (LOG) {
                            LOG.info("Job#" + ++workCount + " \"" + job.getName() + "\" is about to run...");
                        }
                        return job.process(job.prepare());
                    }
                });
            }
            return es.invokeAll(callables);
        }

        /**
         * Don't forget to shut down the executor after finishing all jobs.
         */
        public void shutdown() {
            if (es.isShutdown() || es.isTerminated())
                return;
            es.shutdown();
        }

        public boolean awaitTermination(long seconds) throws InterruptedException {
            if (es.isTerminated())
                return true;
            return es.awaitTermination(seconds, TimeUnit.SECONDS);
        }
    }
}

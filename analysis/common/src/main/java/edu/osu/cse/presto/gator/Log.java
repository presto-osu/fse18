/*
 * Log.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator;

import java.io.PrintStream;
import java.util.Date;

/**
 * A naive logger implementation.
 * Use {@link Factory#getLog(String)} to create new Log instances.
 */
public class Log {
    public enum Level {
        debug(0),
        info(1),
        warn(2),
        error(3);

        private final int mLevel;

        Level(int level) {
            mLevel = level;
        }
    }

    public static class Factory {
        public static Log getLog(String name) {
            return new Log(name);
        }
    }

    // universal settings
    private static PrintStream ps = System.out;
    private static Level logLevel = Level.info;
    private static boolean showTime = true;

    public static void setShowTime(boolean showTime) {
        Log.showTime = showTime;
    }

    public static synchronized void setLevel(Level level) {
        if (level == logLevel) {
            return;
        }
        logLevel = level;
    }

    public static synchronized void setOutputStream(PrintStream newPs) {
        if (ps == newPs) {
            return;
        }
        ps = newPs;
    }

    // individual settings
    private String name;

    Log(String name) {
        this.name = name;
    }

    private static boolean isLoggable(Level level) {
      return level.mLevel >= logLevel.mLevel;
    }

    // convenient methods
    public void debug(Object obj) {
        log(Level.debug, "DEBUG", obj);
    }

    public void debug(String msgFormat, Object... args) {
        log(Level.debug, "DEBUG", msgFormat, args);
    }

    public void info(Object obj) {
        log(Level.info, "INFO", obj);
    }

    public void info(String msgFormat, Object... args) {
        log(Level.info, "INFO", msgFormat, args);
    }

    public void warn(Object obj) {
        log(Level.warn, "WARN", obj);
    }

    public void warn(String msgFormat, Object... args) {
        log(Level.warn, "WARN", msgFormat, args);
    }

    public void error(Object obj) {
        log(Level.error, "ERROR", obj);
        System.exit(-1);
    }

  public void error(String msgFormat, Object... args) {
    log(Level.error, "ERROR", msgFormat, args);
    System.exit(-1);
  }

    public void error(Throwable t, Object obj) {
        log(Level.error, "ERROR", obj);
        if (t != null) {
            System.err.println(String.format("Error: %1$s", t.getMessage()));
        }
        System.exit(-1);
    }

    public void error(Throwable t, String msgFormat, Object... args) {
        log(Level.error, "ERROR", msgFormat, args);
        if (t != null) {
            System.err.println(String.format("Error: %1$s", t.getMessage()));
        }
        System.exit(-1);
    }

    // generic methods
    public void log(Level level, String marker, Object obj) {
        log(level, name, marker, obj);
    }

    public void log(Level level, String marker, String msgFormat, Object... args) {
        log(level, name, marker, String.format(msgFormat, args));
    }

    public static void log(Level level, String name, String marker, Object obj) {
        if (!isLoggable(level))
            return;
        if (showTime) {
            ps.print(new Date());
            ps.print(' ');
            ps.println(name);
            ps.print(marker);
            ps.print(": ");
            ps.println(obj.toString());
        } else {
            ps.print('[');
            ps.print(marker);
            ps.print("] ");
            ps.print(name);
            ps.print(": ");
            ps.println(obj.toString());
        }
    }
}

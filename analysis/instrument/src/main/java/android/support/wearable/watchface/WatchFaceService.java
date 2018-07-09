/*
 * WatchFaceService.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

// stub of watch face service and engine
package android.support.wearable.watchface;

public abstract class WatchFaceService {

    public Engine onCreateEngine() {
        throw new RuntimeException("STUB!!!!!!");
    }

    public abstract class Engine {

        public Engine() {
            throw new RuntimeException("STUB!!!!!!");
        }

        public void onAmbientModeChanged(boolean inAmbientMode) {
            throw new RuntimeException("STUB!!!!!!");
        }

        public void onTimeTick() {
            throw new RuntimeException("STUB!!!!!!");
        }

        public void onVisibilityChanged(boolean visible) {
            throw new RuntimeException("STUB!!!!!!");
        }

        public final boolean isInAmbientMode() {
            throw new RuntimeException("STUB!!!!!!");
        }

        public boolean isVisible() {
            throw new RuntimeException("STUB!!!!!!");
        }
    }
}
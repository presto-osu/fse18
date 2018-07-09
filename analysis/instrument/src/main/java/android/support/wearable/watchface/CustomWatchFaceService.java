/*
 * CustomWatchFaceService.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package android.support.wearable.watchface;

public class CustomWatchFaceService extends WatchFaceService {
    public class Engine extends WatchFaceService.Engine {
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
        }

        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
        }

        public void onTimeTick() {
            super.onTimeTick();
        }
    }
}

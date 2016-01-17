/*
 * Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.watchme;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;

import com.google.android.apps.watchme.grabbers.AudioFrameGrabber;
import com.google.android.apps.watchme.grabbers.VideoFrameGrabber;


public class VideoStreamingConnection implements VideoStreamingInterface {
    // CONSTANTS.
    private static final int AUDIO_SAMPLE_RATE = 44100;

    // Member variables.
    private VideoFrameGrabber videoFrameGrabber;
    private AudioFrameGrabber audioFrameGrabber;
    private final Object frame_mutex = new Object();

    @Override
    public void open(String url, Camera camera, Surface previewSurface) {
        Log.d(MainActivity.APP_NAME, "open");

        videoFrameGrabber = new VideoFrameGrabber();
        audioFrameGrabber = new AudioFrameGrabber();
        synchronized (frame_mutex) {
            Size previewSize = videoFrameGrabber.start(camera, url);
            audioFrameGrabber.start(AUDIO_SAMPLE_RATE, videoFrameGrabber.getRecorder());
        }
    }

    @Override
    public void close() {
        Log.i(MainActivity.APP_NAME, "close");

        videoFrameGrabber.stop();
        audioFrameGrabber.stop();
    }
}

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

package com.google.android.apps.watchme.grabbers;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.AudioRecord;
import android.util.Log;

import com.google.android.apps.watchme.StreamerActivity;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;

import java.nio.ByteBuffer;

/**
 * @author Ibrahim Ulukaya <ulukaya@google.com>
 *         <p/>
 *         VideoFrameGrabber class which grabs video frames to buffer.
 */
public class VideoFrameGrabber {
    // Member variables
    private final String LOG_TAG = VideoFrameGrabber.class.getName();
    private boolean recording;
    private Camera camera;
    private FFmpegFrameRecorder recorder;
    private Frame[] images;
    private int frameRate = 30;
    private int imagesIndex;
    private Frame yuvImage = null;
    private long[] timestamps;
    private long startTime = 0;

    public VideoFrameGrabber() {
        imagesIndex = 0;
        images = new Frame[StreamerActivity.RECORD_LENGTH * 2];
        timestamps = new long[images.length];
        for (int i = 0; i < images.length; i++) {
            Log.i(LOG_TAG, String.format("create image i = %d", i));
            images[i] = new Frame(StreamerActivity.CAMERA_WIDTH, StreamerActivity.CAMERA_HEIGHT, Frame.DEPTH_UBYTE, 16);
            timestamps[i] = -1;
        }
        recording = true;
    }

    public FFmpegFrameRecorder getRecorder() {
        return recorder;
    }

    /**
     * Starts camera recording to buffer.
     *
     * @param camera - Camera to be recorded.
     * @return preview size.
     */

    public Size start(Camera camera, String url) {
        this.camera = camera;
        this.startTime = System.currentTimeMillis();

        Camera.Parameters params = camera.getParameters();
        params.setPreviewSize(StreamerActivity.CAMERA_WIDTH, StreamerActivity.CAMERA_HEIGHT);
        camera.setParameters(params);
        Size previewSize = params.getPreviewSize();
        int bufferSize = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(params.getPreviewFormat());
        camera.addCallbackBuffer(new byte[bufferSize]);

        camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
//                if (audioRecord == null || audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
//                    startTime = System.currentTimeMillis();
//                    return;
//                }
                int i = imagesIndex++ % images.length;
                yuvImage = images[i];
                Log.i(LOG_TAG, String.format("i = %d, imagesIndex = %d", i, imagesIndex));
                timestamps[i] = 1000 * (System.currentTimeMillis() - startTime);
                if (yuvImage != null && recording) {
                    ((ByteBuffer)yuvImage.image[0].position(0)).put(data);
                    try {
                        Log.i(LOG_TAG,"Writing Frame");
                        long t = 1000 * (System.currentTimeMillis() - startTime);
                        if (t > recorder.getTimestamp()) {
                            recorder.setTimestamp(t);
                        }
                        recorder.record(yuvImage);
                    } catch (FFmpegFrameRecorder.Exception e) {
                        Log.v(LOG_TAG,e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        });
        recorder = new FFmpegFrameRecorder(url, previewSize.width, previewSize.height, 1);
        recorder.setFormat("flv");
        recorder.setSampleRate(44100);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setFrameRate(frameRate);
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.setAudioBitrate(128000);
        try {
            recorder.start();
        } catch (FrameRecorder.Exception e) {
            Log.i(VideoFrameGrabber.class.getName(), e.getMessage());
        }

        return previewSize;
    }

    public void stop() {
        camera.setPreviewCallbackWithBuffer(null);
        camera = null;
        /*
        if (recorder != null && recording) {
            Log.i(LOG_TAG,"Writing frames");
            try {
                int firstIndex = imagesIndex % samples.length;
                int lastIndex = (imagesIndex - 1) % images.length;
                if (imagesIndex <= images.length) {
                    firstIndex = 0;
                    lastIndex = imagesIndex - 1;
                }
                if ((startTime = timestamps[lastIndex] - RECORD_LENGTH * 1000000L) < 0) {
                    startTime = 0;
                }
                if (lastIndex < firstIndex) {
                    lastIndex += images.length;
                }
                for (int i = firstIndex; i <= lastIndex; i++) {
                    long t = timestamps[i % timestamps.length] - startTime;
                    if (t >= 0) {
                        if (t > recorder.getTimestamp()) {
                            recorder.setTimestamp(t);
                        }
                        recorder.record(images[i % images.length]);
                    }
                }

                firstIndex = samplesIndex % samples.length;
                lastIndex = (samplesIndex - 1) % samples.length;
                if (samplesIndex <= samples.length) {
                    firstIndex = 0;
                    lastIndex = samplesIndex - 1;
                }
                if (lastIndex < firstIndex) {
                    lastIndex += samples.length;
                }
                for (int i = firstIndex; i <= lastIndex; i++) {
                    recorder.recordSamples(samples[i % samples.length]);
                }
            } catch (FFmpegFrameRecorder.Exception e) {
                Log.v(LOG_TAG,e.getMessage());
                e.printStackTrace();
            }

            recording = false;
            Log.v(LOG_TAG,"Finishing recording, calling stop and release on recorder");
            try {
                recorder.stop();
                recorder.release();
            } catch (FFmpegFrameRecorder.Exception e) {
                e.printStackTrace();
            }
            recorder = null;

        }*/
        try {
            recorder.stop();
            recorder.release();
        } catch (FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        }
        recorder = null;
    }
}

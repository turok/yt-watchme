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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.google.android.apps.watchme.MainActivity;
import com.google.android.apps.watchme.StreamerActivity;

import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.nio.ShortBuffer;

/**
 * @author Ibrahim Ulukaya <ulukaya@google.com>
 *         <p/>
 *         AudioFrameGrabber class which records audio.
 */
public class AudioFrameGrabber {
    private Thread thread;
    private AudioRecord audioRecord;
    private AudioRecordRunnable audioRecordRunnable;
    private FFmpegFrameRecorder recorder;
    private boolean runAudioThread = false;
    private int frequency;
    private final String LOG_TAG = AudioFrameGrabber.class.getName();
    private int samplesIndex;
    ShortBuffer[] samples;
    private boolean recording;

    /**
     * Starts recording.
     *
     * @param frequency - Recording frequency.
     */
    public void start(int frequency, FFmpegFrameRecorder recorder) {
        Log.d(MainActivity.APP_NAME, "start");

        this.frequency = frequency;
        this.recorder = recorder;
        recording = true;
        runAudioThread = true;
        thread = new Thread(audioRecordRunnable);
        thread.start();
    }

    /**
     * Stops recording.
     */
    public void stop() {
        Log.d(MainActivity.APP_NAME, "stop");

        runAudioThread = false;
        recording = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            Log.e(MainActivity.APP_NAME, "", e);
        }
        thread = null;
    }

    class AudioRecordRunnable implements Runnable {

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            // Audio
            int bufferSize;
            ShortBuffer audioData;
            int bufferReadResult;
            bufferSize = AudioRecord.getMinBufferSize(frequency, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            samplesIndex = 0;
            samples = new ShortBuffer[StreamerActivity.RECORD_LENGTH * frequency * 2 / bufferSize + 1];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = ShortBuffer.allocate(bufferSize);
            }

            Log.i(LOG_TAG, "audioRecord.startRecording()");
            audioRecord.startRecording();

            /* ffmpeg_audio encoding loop */
            while (runAudioThread) {
                audioData = samples[samplesIndex++ % samples.length];
                audioData.position(0).limit(0);
                //Log.v(LOG_TAG,"recording? " + recording);
                bufferReadResult = audioRecord.read(audioData.array(), 0, audioData.capacity());
                audioData.limit(bufferReadResult);
                if (bufferReadResult > 0) {
                    Log.v(LOG_TAG,"bufferReadResult: " + bufferReadResult);
                    // If "recording" isn't true when start this thread, it never get's set according to this if statement...!!!
                    // Why?  Good question...
                    if (recording) {
                       try {
                            recorder.recordSamples(audioData);
                            //Log.v(LOG_TAG,"recording " + 1024*i + " to " + 1024*i+1024);
                        } catch (FFmpegFrameRecorder.Exception e) {
                            Log.v(LOG_TAG,e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
            Log.v(LOG_TAG,"AudioThread Finished, release audioRecord");

            /* encoding finish, release recorder */
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                Log.v(LOG_TAG,"audioRecord released");
            }
        }
    }
}

/*
 * Copyright (C) 2015 Jesse Gu
 * 未经作者许可，禁止将该程序用于商业用途
 * 
 *************************************************************************
 **                   作者信息                                            **
 *************************************************************************
 ** Email: sinvoice@126.com                                             **
 ** QQ   : 29600731                                                     **
 ** Home: http://www.sinvoice.com或者http://112.74.216.30                **
 *************************************************************************
 */
package com.libra.sinvoice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

public class XRecord extends Worker {
    private final static String TAG = "SVRecord";

    private final static String PCM_PATH = "/sinvoice_record/sinvoice.pcm";

    private int mSampleRate = 441000;
    private int mBufferSize;

    private String mFilePath = null;

    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    public XRecord(int sampleRate, int channel, int bits, int bufferSize) {

        mSampleRate = sampleRate;
        mBufferSize = bufferSize;

        mChannelConfig = channel;
        mAudioEncoding = bits;

        File dir = Environment.getExternalStorageDirectory();
        if (null != dir) {
            mFilePath = dir.toString() + PCM_PATH;
        }

        LogHelper.d(TAG, "filepath:" + mFilePath);
    }

    @Override
    protected void doStart(Object param) {
        boolean isReadFromFile = false;

        if (param instanceof Boolean) {
            isReadFromFile = (Boolean) param;
        }

        if (isReadFromFile) {
            recordFromFile();
        } else {
            recordFromDevice();
        }
    }

    @Override
    protected void doStop() {
    }

    private void recordFromDevice() {
        LogHelper.d(TAG, "recordFromDevice Start");

        if (mBufferSize > 0) {
            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    mSampleRate, mChannelConfig, mAudioEncoding,
                    mBufferSize * 5);
            if (null != record) {
                try {
                    mState = STATE_START;
                    LogHelper.d(TAG, "record start");
                    record.startRecording();
                    LogHelper.d(TAG, "record start 1");

                    if (null != mCallback) {
                        if (null != mListener) {
                            mListener.onStart(this);
                        }

                        while (STATE_START == mState) {
                            BufferData data = mCallback.getBuffer(this);
                            if (null != data) {
                                if (null != data.mData) {
                                    int bufferReadResult = record.read(
                                            data.mData, 0, mBufferSize);
                                    LogHelper.d(TAG, "read record:"
                                            + bufferReadResult);
                                    data.setFilledSize(bufferReadResult);

                                    mCallback.freeBuffer(this, data);
                                } else {
                                    // end of input
                                    LogHelper.d(TAG,
                                            "get end input data, so stop");
                                    break;
                                }
                            } else {
                                LogHelper.e(TAG, "get null data");
                                break;
                            }
                        }

                        if (null != mListener) {
                            mListener.onStop(this);
                        }
                    }

                    LogHelper.d(TAG, "stop record");
                    record.stop();
                    LogHelper.d(TAG, "release record");
                    record.release();

                    LogHelper.d(TAG, "record stop");
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    LogHelper.e(TAG, "start record error");
                }
                mState = STATE_STOP;
            }
        } else {
            LogHelper.e(TAG, "bufferSize is too small");
        }

        LogHelper.d(TAG, "recordFromDevice End");
    }

    private void recordFromFile() {
        LogHelper.d(TAG,
                "recordFromFile Start thread id:" + Thread.currentThread());

        mState = STATE_START;
        if (null != mCallback) {
            if (null != mListener) {
                mListener.onStart(this);
            }

            File file = new File(mFilePath);
            FileInputStream fis;
            try {
                fis = new FileInputStream(file);
                while (STATE_START == mState) {
                    BufferData data = mCallback.getBuffer(this);
                    if (null != data) {
                        if (null != data.mData) {
                            LogHelper.d(TAG, "recordFromFile read start");
                            int bufferReadResult = fis.read(data.mData);
                            if (bufferReadResult >= 0) {
                                LogHelper.d(TAG, "recordFromFile read size:"
                                        + bufferReadResult + "  data len:"
                                        + data.mData.length);

                                data.setFilledSize(bufferReadResult);

                                mCallback.freeBuffer(this, data);
                            } else {
                                LogHelper.d(TAG, "recordFromFile end of file");
                                // stop
                                // mState = STATE_STOP;
                                break;
                            }
                        } else {
                            // end of input
                            LogHelper.d(TAG, "get end input data, so stop");
                            break;
                        }
                    } else {
                        LogHelper.e(TAG, "get null data");
                        break;
                    }
                }

                fis.close();
            } catch (FileNotFoundException e1) {
                // Toast.makeText(null, "fefef", Toast.LENGTH_SHORT).show();
                e1.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (null != mListener) {
                mListener.onStop(this);
            }
        }

        LogHelper.d(TAG, "recordFromFile End");
    }

}

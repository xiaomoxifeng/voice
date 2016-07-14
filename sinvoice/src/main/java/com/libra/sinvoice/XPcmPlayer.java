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

import android.media.AudioManager;
import android.media.AudioTrack;

public class XPcmPlayer extends Worker {
    private final static String TAG = "PcmPlayer";

    private boolean mStarted;

    private int mSampleRate;
    private int mChannel;
    private int mFormat;
    private int mBufferSize;

    public XPcmPlayer(int sampleRate, int channel, int format, int bufferSize) {
        mSampleRate = sampleRate;
        mChannel = channel;
        mFormat = format;
        mBufferSize = bufferSize;
    }

    @Override
    protected void doStart(Object param) {
        mStarted = false;

        if (null != mCallback) {
            mState = STATE_START;
            LogHelper.d(TAG, "start");
            if (null != mListener) {
                mListener.onStart(this);
            }

            AudioTrack mAudio = new AudioTrack(AudioManager.STREAM_MUSIC,
                    mSampleRate, mChannel, mFormat, 3 * mBufferSize,
                    AudioTrack.MODE_STREAM);

            while (STATE_START == mState) {
                LogHelper.d(TAG, "start getbuffer");

                long start = System.currentTimeMillis();
                BufferData data = mCallback.getBuffer(this);
                long d = System.currentTimeMillis() - start;
                LogHelper.d(TAG, "PcmPlayerTime getBuffer:" + d);
                if (null != data) {
                    if (null != data.mData) {

                        start = System.currentTimeMillis();
                        int len = mAudio.write(data.mData, 0,
                                data.getFilledSize());
                        if (len != data.getFilledSize()) {
                            LogHelper.e(
                                    TAG,
                                    "PcmPlayerTime writedata, write is invalidate len:"
                                            + len + "   filledSize:"
                                            + data.getFilledSize());
                        }
                        d = System.currentTimeMillis() - start;
                        LogHelper.d(TAG, "PcmPlayerTime writedata:" + d);

                        if (!mStarted) {
                            mStarted = true;
                            mAudio.play();
                        }
                        start = System.currentTimeMillis();
                        mCallback.freeBuffer(this, data);
                        d = System.currentTimeMillis() - start;
                        LogHelper.d(TAG, "PcmPlayerTime freeBuffer:" + d);
                    } else {
                        // it is the end of input, so need stop
                        LogHelper
                                .d(TAG, "it is the end of input, so need stop");
                        break;
                    }
                } else {
                    LogHelper.e(TAG, "get null data");
                    break;
                }
            }

            LogHelper.e(TAG, "audio stop");
            if (null != mAudio) {
                mAudio.flush();
                mAudio.stop();
                mAudio.release();
                mAudio = null;
            }

            mState = STATE_STOP;
            if (null != mListener) {
                mListener.onStop(this);
            }
            LogHelper.d(TAG, "pcm end");
        }
    }

    @Override
    protected void doStop() {
    }

}

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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

public class XRecognition extends Worker implements VoiceDecoder.Callback {
    private final static String TAG = "XRecognition";

    private int mSampleRate;
    private FileOutputStream mFileOut;
    private VoiceDecoder mVoiceDecoder;
    private Context mContext;

    public static interface Listener extends Worker.Listener {
        void onRecognition(Worker self, int index);
    }

    public XRecognition(int SampleRate) {
        mSampleRate = SampleRate;

        mVoiceDecoder = new VoiceDecoder(this);
    }

    public void init(Context context) {
        mContext = context;
    }

    public void uninit() {
    }

    @Override
    protected void doStart(Object param) {
        int tokenCount = 0;
        if (param instanceof Integer) {
            tokenCount = (Integer) param;
        }

        mVoiceDecoder.initVR(mContext, "com.sinvoice.demo", "SinVoice");
        LogHelper.d(TAG,
                "Voice Recogintiono start threadid:" + Thread.currentThread());
        if (null != mCallback) {
            mState = STATE_START;

            if (null != mListener) {
                mListener.onStart(this);
            }
            try {
                String sdcardPath = Environment.getExternalStorageDirectory()
                        .getPath();
                if (!TextUtils.isEmpty(sdcardPath)) {
                    mFileOut = new FileOutputStream(String.format(
                            "%s/record.pcm", sdcardPath));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            LogHelper.d(TAG, "Voice Recogintion startVR");
            mVoiceDecoder.startVR(mSampleRate, tokenCount);
            LogHelper.d(TAG, "Voice Recogintion start VR End");
            while (STATE_START == mState) {
                BufferData data = mCallback.getBuffer(this);
                if (null != data) {
                    if (null != data.mData) {
                        LogHelper.d(TAG, "putData data:" + data
                                + " filledSize:" + data.getFilledSize());
                        mVoiceDecoder.putData(data.mData, data.getFilledSize());

                        mCallback.freeBuffer(this, data);
                        if (null != mFileOut) {
                            try {
                                mFileOut.write(data.mData);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        LogHelper.d(TAG, "end input buffer, so stop");
                        break;
                    }
                } else {
                    LogHelper.e(TAG, "get null recognition buffer");
                    break;
                }
            }

            mVoiceDecoder.stopVR();
            if (null != mFileOut) {
                try {
                    mFileOut.close();
                    mFileOut = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mState = STATE_STOP;
            if (null != mListener) {
                mListener.onStop(this);
            }
        }

        mVoiceDecoder.uninitVR();
    }

    @Override
    protected void doStop() {
    }

    @Override
    public void onVoiceDecoderResult(int index) {
        LogHelper.d("VoiceRecognition", "onRecognized:" + index);
        if (null != mListener) {
            LogHelper.d("jichengtoken", "receive  token:" + index);

            if (mListener instanceof Listener) {
                ((Listener) mListener).onRecognition(this, index);
            }
        }
    }

}

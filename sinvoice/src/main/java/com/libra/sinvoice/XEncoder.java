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
import java.io.InputStream;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

public class XEncoder extends Worker implements VoiceEncoder.Callback {
    private final static String TAG = "XEncoder";

    private VoiceEncoder mVoiceEncoder;
    private Context mContext;
    private FileOutputStream mFileOut;
    private BufferData mBuf;
    private byte[] mMask;
    private int mMaskCursor = 0;
    private int mCurrentEffectIndex = -1;

    public static class Param {
        public Param(int sampleRate, int bufferSize, int[] tokens,
                int tokenLen, boolean repeat, int muteInterval, int effectIndex) {
            mSampleRate = sampleRate;
            mBufferSize = bufferSize;
            mTokens = tokens;
            mTokenLen = tokenLen;
            mMuteInterval = muteInterval;
            mRepeat = repeat;
            mEffectIndex = effectIndex;
        }

        public int mSampleRate;
        public int mBufferSize;
        public int[] mTokens;
        public int mTokenLen;
        public int mMuteInterval;
        public boolean mRepeat;
        public int mEffectIndex;
    }

    public static interface Listener extends Worker.Listener {
        void onEncoderToken(Worker self, int[] tokens);
    }

    public XEncoder() {
        mVoiceEncoder = new VoiceEncoder(this);
    }

    public void getFromAssets(String fileName) {
        try {
            InputStream in = mContext.getResources().getAssets().open(fileName);
            if (null != in) {
                int length = in.available();
                if (length > 0) {
                    mMask = new byte[length];
                    in.read(mMask);
                    LogHelper.d(TAG, "mask len:" + mMask.length);
                } else {
                    mMask = null;
                }
                in.close();
            } else {
                mMask = null;
            }
        } catch (Exception e) {
            mMask = null;
            e.printStackTrace();
        }
    }

    public void init(Context context) {
        mContext = context;
    }

    public void uninit() {
    }

    @Override
    protected void doStart(Object param) {
        if (null != param && param instanceof Param) {
            mMaskCursor = 0;
            Param p = (Param) param;

            if (mCurrentEffectIndex != p.mEffectIndex) {
                mCurrentEffectIndex = p.mEffectIndex;
                getFromAssets(String.format("sound_effect/mask%d.pcm", mCurrentEffectIndex));
            }

            mVoiceEncoder.initSV(mContext, "com.sinvoice.demo", "SinVoice");

            LogHelper.d("gujicheng", "encode start SV");
            boolean needRepeat = false;
            mVoiceEncoder.startSV(p.mSampleRate, p.mBufferSize);

            try {
                String sdcardPath = Environment.getExternalStorageDirectory()
                        .getPath();
                if (!TextUtils.isEmpty(sdcardPath)) {
                    mFileOut = new FileOutputStream(String.format(
                            "%s/player.pcm", sdcardPath));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (null != mListener) {
                mListener.onStart(this);
            }

            do {
                LogHelper.d(TAG, "encode start");
                encode(p.mTokens, p.mTokenLen, p.mMuteInterval, needRepeat);
                needRepeat = p.mRepeat;
                LogHelper.d(TAG, "encode end");
            } while (p.mRepeat && (STATE_START == mState));
            stop();
            mVoiceEncoder.stopSV();

            if (null != mFileOut) {
                try {
                    mFileOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mVoiceEncoder.uninitSV();
        }
    }

    private void encode(int[] tokens, int tokenLen, int muteInterval,
            boolean isRepeat) {
        if (STATE_START == mState) {
            for (int i = 0; i < tokenLen; ++i) {
                LogHelper.d("jichengtoken", "send i" + i + "  token:"
                        + tokens[i]);
            }
            mVoiceEncoder.genRate(tokens, tokenLen);

            // for mute
            if (muteInterval > 0) {
                int interval = 0;
                while (STATE_START == mState && interval < muteInterval) {
                    interval += 20;
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (STATE_START == mState) {
                // genRateMute(muteInterval);
            } else {
                LogHelper.d(TAG, "encode force stop");
            }
        }
    }

    @Override
    protected void doStop() {
        LogHelper.d("gujicheng", "encode stop SV");
        // mVoiceEncoder.stopSV();

        if (null != mListener) {
            mListener.onStop(this);
        }
        // for last pcmplay
        mCallback.freeBuffer(this, null);
    }

    @Override
    public void freeVoiceEncoderBuffer(int filledBytesSize) {
        if (null != mCallback) {
            LogHelper.d(TAG, "onFreeBuffer start");
            if (null != mFileOut) {
                try {
                    mFileOut.write(mBuf.mData, 0, filledBytesSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != mMask) {
                if (mMaskCursor + filledBytesSize <= mMask.length) {
                    for (int i = 0; i < filledBytesSize - 1; ++i) {
                        short sh1 = mBuf.mData[i];
                        sh1 &= 0xff;
                        short sh2 = mBuf.mData[i + 1];
                        sh2 <<= 8;
                        short s1 = (short) ((sh1) | (sh2));

                        sh1 = mMask[mMaskCursor + i];
                        sh1 &= 0xff;
                        sh2 = mMask[mMaskCursor + i + 1];
                        sh2 <<= 8;
                        short s2 = (short) ((sh1) | (sh2));

                        s1 = (short) (((int) s1 + s2) / 2);

                        mBuf.mData[i] = (byte) (s1 & 0xff);
                        mBuf.mData[i + 1] = (byte) ((s1 >> 8) & 0xff);
                        ++i;
                    }

                    mMaskCursor += filledBytesSize;
                }
            }

            mBuf.setFilledSize(filledBytesSize);
            mCallback.freeBuffer(this, mBuf);
            LogHelper.d(TAG, "onFreeBuffer end ");
        }
    }

    @Override
    public byte[] getVoiceEncoderBuffer() {
        LogHelper.d(TAG, "onGetBuffer");
        if (null != mCallback) {
            LogHelper.d(TAG, "onGetBuffer start");
            BufferData data = mCallback.getBuffer(this);
            if (null != data) {
                mBuf = data;
                LogHelper.d(TAG, "onGetBuffer end :" + data.mData + "   len:"
                        + data.mData.length);
                return data.mData;
            }
        }

        return null;
    }

    @Override
    public void onVoiceEncoderToken(int[] tokens) {
        LogHelper.d(TAG, "onEncoderToken");
        if (null != tokens && null != mListener) {
            LogHelper.d(TAG, "onSinToken " + tokens.length);

            if (mListener instanceof Listener) {
                ((Listener) mListener).onEncoderToken(this, tokens);
            }
        }
    }

}

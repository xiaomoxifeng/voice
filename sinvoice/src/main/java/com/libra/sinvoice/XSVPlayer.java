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

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;

import com.libra.sinvoice.XEncoder.Param;

public class XSVPlayer extends Prosumer implements XEncoder.Listener {
    private final static String TAG = "SinVoicePlayer";

    private XEncoder mEncoder;
    private XPcmPlayer mPlayer;

    private int mSampleRate;
    private int mBufferSize;
    private Handler mHanlder;

    public static final int BITS_8 = 128;
    public static final int BITS_16 = 32768;

    private static final int MSG_END = 2;

    public static interface Listener extends Prosumer.Listener {
        void onSinToken(Prosumer self, int[] tokens);
    }

    public XSVPlayer() {
        this(Common.DEFAULT_SAMPLE_RATE, Common.DEFAULT_BUFFER_COUNT);
    }

    public XSVPlayer(int sampleRate, int bufferCount) {
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        LogHelper.d(TAG, "AudioTrackMinBufferSize: " + bufferSize
                + "  sampleRate:" + sampleRate);

        mSampleRate = sampleRate;
        mBufferSize = bufferSize;

        mEncoder = new XEncoder();
        mEncoder.setListener(this);

        mPlayer = new XPcmPlayer(sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        mPlayer.setListener(this);

        init(mEncoder, mPlayer, bufferSize, bufferCount);

        mHanlder = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case MSG_END:
                    stop();
                    break;
                }
            }
        };
    }

    public void init(Context context) {
        mEncoder.init(context);
    }

    public void uninit() {
        mEncoder.uninit();
        mHanlder.removeMessages(MSG_END);
    }

    public void play(final String text, final boolean repeat,
            final int muteInterval, int effectIndex) {
        if (null != text) {
            int tokenLen = text.length();
            if (tokenLen > 0) {
                int[] tokens = new int[tokenLen];
                int i;
                for (i = 0; i < tokenLen; ++i) {
                    int index = Common.DEFAULT_CODE_BOOK
                            .indexOf(text.charAt(i));
                    if (index > -1) {
                        tokens[i] = index;
                    } else {
                        break;
                    }
                }

                if (i >= tokenLen) {
                    play(tokens, tokenLen, repeat, muteInterval, effectIndex);
                }
            }
        }
    }

    public void play(int[] tokens, int tokenLen, final String text) {
        play(tokens, tokenLen, false, 0, 0);
    }

    public void play(final int[] tokens, final int tokenLen,
            final boolean repeat, final int muteInterval, int effectIndex) {
        Param productorParam = new Param(mSampleRate, mBufferSize, tokens,
                tokenLen, repeat, muteInterval, effectIndex);

        start(productorParam, null);
    }

    @Override
    public void onStart(Worker self) {
        LogHelper.d(TAG, "onStart:" + self);
    }

    @Override
    public void onStop(Worker self) {
        LogHelper.d(TAG, "onStop:" + self);
        if (null != mListener) {
            if (self == mPlayer) {
                LogHelper.d(TAG, "onPlayStop");
                mHanlder.sendEmptyMessage(MSG_END);
            }
        }
    }

    @Override
    public void onEncoderToken(Worker self, int[] tokens) {
        if (null != mListener && null != tokens
                && mListener instanceof Listener) {
            LogHelper.d(TAG, "onEncoderToken " + tokens.length);
            ((Listener) mListener).onSinToken(this, tokens);
        }
    }

}

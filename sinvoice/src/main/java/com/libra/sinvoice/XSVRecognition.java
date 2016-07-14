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
import android.media.AudioRecord;
import android.text.TextUtils;

public class XSVRecognition extends Prosumer implements XRecognition.Listener {
    private final static String TAG = "SVRecog";
    private XRecord mRecord;
    private XRecognition mRecognition;
    private String mCodeBook;

    public static interface Listener extends Prosumer.Listener {
        void onSinVoiceRecognitionStart(Prosumer self);

        void onSinVoiceRecognition(Prosumer self, char ch);

        void onSinVoiceRecognitionEnd(Prosumer self, int result);
    }

    public XSVRecognition() {
        this(Common.DEFAULT_CODE_BOOK);
    }

    public XSVRecognition(String codeBook) {
        this(codeBook, Common.DEFAULT_SAMPLE_RATE, Common.DEFAULT_BUFFER_COUNT);
    }

    public XSVRecognition(String codeBook, int sampleRate, int bufferCount) {
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        LogHelper.d(TAG, "AudioRecordMinBufferSize:" + bufferSize
                + "  sampleRate:" + sampleRate);

        mRecord = new XRecord(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        mRecord.setListener(this);

        mRecognition = new XRecognition(sampleRate);
        mRecognition.setListener(this);

        init(mRecord, mRecognition, bufferSize, bufferCount);
        setCodeBook(codeBook);
    }

    public void init(Context context) {
        if (null != mRecognition) {
            mRecognition.init(context);
        }
    }

    public void uninit() {
        if (null != mRecognition) {
            mRecognition.uninit();
        }
    }

    public void setCodeBook(String codeBook) {
        if (!TextUtils.isEmpty(codeBook)) {
            mCodeBook = codeBook;
        }
    }

    @Override
    public void onStart(Worker self) {
    }

    @Override
    public void onStop(Worker self) {
    }

    @Override
    public void onRecognition(Worker self, int index) {
        LogHelper.d(TAG, "recognition:" + index);
        if (null != mListener && mListener instanceof Listener ) {
            Listener lis = (Listener)mListener;
            if (self == mRecognition) {
                if (index >= 0) {
                    // mListener.onSinVoiceRecognition((char)index);
                    lis.onSinVoiceRecognition(this, mCodeBook.charAt(index));
                } else {
                    LogHelper.d(TAG, "recognition: gIsError" + index);
                    if (VoiceDecoder.VOICE_DECODER_START == index) {
                        lis.onSinVoiceRecognitionStart(this);
                    } else if (VoiceDecoder.VOICE_DECODER_END == index) {
                        lis.onSinVoiceRecognitionEnd(this, -1);
                    } else if (index <= -3) {
                        lis.onSinVoiceRecognitionEnd(this, -3 - index);
                    } else {
                        LogHelper.d(TAG, "onRecognition error index" + index);
                    }
                }
            }
        }
    }

}

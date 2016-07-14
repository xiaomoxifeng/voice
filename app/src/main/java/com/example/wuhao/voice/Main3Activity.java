package com.example.wuhao.voice;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.libra.sinvoice.LogHelper;
import com.libra.sinvoice.Prosumer;
import com.libra.sinvoice.XSVRecognition;

import java.io.UnsupportedEncodingException;

public class Main3Activity extends Activity implements XSVRecognition.Listener{
    private final static String TAG = "MainActivity";
    private final static int MSG_SET_RECG_TEXT = 1;
    private final static int MSG_RECG_START = 2;
    private final static int MSG_RECG_END = 3;
    private final static int MSG_PLAY_TEXT = 4;
    private final static int[] TOKENS = { 32, 32, 32, 32, 32, 32 };
    private TextView mRecognisedTextView;
    private char mRecgs[] = new char[100];
    private Handler mHanlder;
    private int mRecgCount;
    private XSVRecognition mRecognition;
    static {
        System.loadLibrary("sinvoice");
        LogHelper.d(TAG, "sinvoice jnicall loadlibrary");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        mRecognisedTextView = (TextView) findViewById(R.id.tv);
        mRecognition = new XSVRecognition();
        mRecognition.init(this);
        mRecognition.setListener(this);
        mHanlder = new RegHandler(this);
        Button recognitionStart = (Button) findViewById(R.id.bt);
        recognitionStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mRecognition.start(false, TOKENS.length);
            }
        });
    }
    @Override
    public void onPause() {
        super.onPause();

        mRecognition.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRecognition.uninit();
    }
    @Override
    public void onSinVoiceRecognitionStart(Prosumer self) {
        mHanlder.sendEmptyMessage(MSG_RECG_START);
    }

    @Override
    public void onSinVoiceRecognition(Prosumer self, char ch) {
        mHanlder.sendMessage(mHanlder.obtainMessage(MSG_SET_RECG_TEXT, ch, 0));
    }

    @Override
    public void onSinVoiceRecognitionEnd(Prosumer self, int result) {
        mHanlder.sendMessage(mHanlder.obtainMessage(MSG_RECG_END, result, 0));
    }

    @Override
    public void onProdumerStart(Prosumer self) {

    }

    @Override
    public void onProdumerStop(Prosumer self) {

    }
    private static class RegHandler extends Handler {
        private StringBuilder mTextBuilder = new StringBuilder();
        private Main3Activity mAct;

        public RegHandler(Main3Activity act) {
            mAct = act;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_RECG_TEXT:
                    char ch = (char) msg.arg1;
                    // mTextBuilder.append(ch);
                    mAct.mRecgs[mAct.mRecgCount++] = ch;
                    break;

                case MSG_RECG_START:
                    // mTextBuilder.delete(0, mTextBuilder.length());
                    mAct.mRecgCount = 0;
                    break;

                case MSG_RECG_END:
                    LogHelper.d(TAG, "recognition end gIsError:" + msg.arg1);
                    if (mAct.mRecgCount > 0) {
                        byte[] strs = new byte[mAct.mRecgCount];
                        for (int i = 0; i < mAct.mRecgCount; ++i) {
                            strs[i] = (byte) mAct.mRecgs[i];
                        }
                        try {
                            String strReg = new String(strs, "UTF8");
                            if (msg.arg1 >= 0) {
                                Log.d(TAG, "reg ok!!!!!!!!!!!!");
                                if (null != mAct) {
                                    mAct.mRecognisedTextView.setText(strReg);
                                    // mAct.mRegState.setText("reg ok(" + msg.arg1 +
                                    // ")");
                                }
                            } else {
                                Log.d(TAG, "reg error!!!!!!!!!!!!!");
                                mAct.mRecognisedTextView.setText(strReg);
                                // mAct.mRegState.setText("reg err(" + msg.arg1 +
                                // ")");
                                // mAct.mRegState.setText("reg err");
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                case MSG_PLAY_TEXT:
                    // mAct.mPlayTextView.setText(mAct.mPlayText);
                    break;
            }
            super.handleMessage(msg);
        }
    }
}

package com.example.wuhao.voice;

import android.app.Activity;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.libra.sinvoice.Common;
import com.libra.sinvoice.LogHelper;
import com.libra.sinvoice.Prosumer;
import com.libra.sinvoice.XSVPlayer;

import java.io.UnsupportedEncodingException;

public class Main2Activity extends Activity implements
        XSVPlayer.Listener  {
    //最多只能发送18位
    private final static String TAG = "MainActivity";
    private final static int[] TOKENS = { 32, 32, 32, 32, 32, 32 };
    private EditText mPlayTextView;
    private XSVPlayer mSinVoicePlayer;
    static {
        System.loadLibrary("sinvoice");
        LogHelper.d(TAG, "sinvoice jnicall loadlibrary");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mPlayTextView = (EditText) findViewById(R.id.et);
        mSinVoicePlayer = new XSVPlayer();
        mSinVoicePlayer.init(this);
        mSinVoicePlayer.setListener(this);
        Button playStart = (Button) findViewById(R.id.button);
        playStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    byte[] strs = mPlayTextView.getText().toString()
                            .getBytes("UTF8");
                    if (null != strs) {
                        int len = strs.length;
                        int[] tokens = new int[len];
                        for (int i = 0; i < len; ++i) {
                            // tokens[i] = strs[i];
                            String xx = mPlayTextView.getText().toString();
                            tokens[i] = Common.DEFAULT_CODE_BOOK.indexOf(xx
                                    .charAt(i));
                            // tokens[i] =
                            // Common.DEFAULT_CODE_BOOK.indexOf(TOKENS_str.charAt(i));
                        }
                        mSinVoicePlayer.play(tokens, len, false, 2000, 3);
                    } else {
                        mSinVoicePlayer.play(TOKENS, TOKENS.length, false, 2000, 2);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        mSinVoicePlayer.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSinVoicePlayer.uninit();
    }

    @Override
    public void onSinToken(Prosumer self, int[] tokens) {

    }

    @Override
    public void onProdumerStart(Prosumer self) {

    }

    @Override
    public void onProdumerStop(Prosumer self) {

    }
}

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

public abstract class Worker {
    protected final static int STATE_START = 1;
    protected final static int STATE_STOP = 2;

    protected int mState;
    protected Listener mListener;
    protected Callback mCallback;

    public static interface Listener {
        void onStart(Worker self);

        void onStop(Worker self);
    }

    public static interface Callback {
        BufferData getBuffer(Worker self);

        boolean freeBuffer(Worker self, BufferData buffer);
    }

    public Worker() {
        mState = STATE_STOP;
    }

    final public void setListener(Listener listener) {
        mListener = listener;
    }

    final public void setCallback(Callback callback) {
        mCallback = callback;
    }

    abstract protected void doStart(Object param);

    abstract protected void doStop();

    final public void start(Object param) {
        if (STATE_STOP == mState) {
            mState = STATE_START;

            doStart(param);
        }
    }

    final public void stop() {
        if (STATE_START == mState) {
            mState = STATE_STOP;

            doStop();
        }
    }
}

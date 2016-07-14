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

public class Prosumer implements Worker.Callback {
    private final static String TAG = "Prosumer";

    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;

    private int mState;

    private BufferQueue mBufferQueue;

    private Worker mProducer;
    private Thread mProducerThread;

    private Worker mConsumer;
    private Thread mConsumerThread;

    protected Listener mListener;

    public static interface Listener {
        void onProdumerStart(Prosumer self);

        void onProdumerStop(Prosumer self);
    }

    public Prosumer() {
        mState = STATE_STOP;
    }

    final public void setListener(Listener lis) {
        mListener = lis;
    }

    protected void init(Worker producer, Worker consumer, int bufferSize,
            int bufferCount) {
        mProducer = producer;
        if (null != mProducer) {
            mProducer.setCallback(this);
        }

        mConsumer = consumer;
        if (null != mConsumer) {
            mConsumer.setCallback(this);
        }

        mBufferQueue = new BufferQueue(bufferCount, bufferSize);
    }

    protected void uninit() {
        stop();
    }

    public void start(final Object producerParam, final Object consumerParam) {
        if (STATE_STOP == mState) {
            mState = STATE_START;

            if (null != mBufferQueue) {
                mBufferQueue.set();
            }

            mConsumerThread = new Thread() {
                @Override
                public void run() {
                    if (null != mConsumer) {
                        mConsumer.start(consumerParam);
                    }
                }
            };
            if (null != mConsumerThread) {
                mConsumerThread.start();
            }

            mProducerThread = new Thread() {
                @Override
                public void run() {
                    if (null != mProducer) {
                        mProducer.start(producerParam);
                    }
                }
            };
            if (null != mProducerThread) {
                mProducerThread.start();
            }

            if (null != mListener) {
                mListener.onProdumerStart(this);
            }
        }
    }

    public void stop() {
        if (STATE_START == mState) {
            mState = STATE_STOP;

            LogHelper.d(TAG, "stop start");

            if (null != mProducer) {
                mProducer.stop();
            }

            if (null != mConsumer) {
                mConsumer.stop();
            }

            if (null != mBufferQueue) {
                mBufferQueue.reset();
            }

            if (null != mProducerThread) {
                try {
                    LogHelper.d(TAG, "wait record thread exit");
                    mProducerThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mProducerThread = null;
                }
            }

            if (null != mConsumerThread) {
                try {
                    LogHelper.d(TAG, "wait recognition thread exit");
                    mConsumerThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mConsumerThread = null;
                }
            }

            LogHelper.d(TAG, "stop end");
            if (null != mListener) {
                mListener.onProdumerStop(this);
            }
        }
    }

    @Override
    public BufferData getBuffer(Worker self) {
        BufferData buffer = null;

        if (self == mProducer) {
            buffer = mBufferQueue.getEmpty();
        } else if (self == mConsumer) {
            buffer = mBufferQueue.getFull();
        }

        if (null == buffer) {
            LogHelper.e(TAG, "get null empty buffer");
        }

        return buffer;
    }

    @Override
    public boolean freeBuffer(Worker self, BufferData buffer) {
        boolean ret = false;

        if (self == mProducer) {
            ret = mBufferQueue.putFull(buffer);
        } else if (self == mConsumer) {
            ret = mBufferQueue.putEmpty(buffer);
        }

        if (!ret) {
            LogHelper.e(TAG, "put empty buffer failed");
        }

        return ret;
    }
}

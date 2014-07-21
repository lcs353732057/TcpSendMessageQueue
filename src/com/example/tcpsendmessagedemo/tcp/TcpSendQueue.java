package com.example.tcpsendmessagedemo.tcp;

import com.example.tcpsendmessagedemo.GoddessPlanApplication;
import com.example.tcpsendmessagedemo.util.Logger;
import com.example.tcpsendmessagedemo.util.Util;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Administrator on 2014/6/27.
 */
public class TcpSendQueue implements TimerInstance.NotifyTimer {
    public static final int LONG_TIME_OUT = 1000 * 60;

    public static final int SEND_TIME_OUT = 10 * 1000;

    public static final int SEND_KEEP_LIVE_TIME_OUT = 5 * 1000;

    private static TcpSendQueue ourInstance = new TcpSendQueue();

    private LinkedList<Object> mMsgQueue = new LinkedList<Object>();

    private Logger log = Logger.getLogger();

    private volatile boolean isRunning = false;

    private Thread mWorkerThread = null;

    public static TcpSendQueue getInstance() {
        return ourInstance;
    }

    private TcpSendQueue() {
        TimerInstance.getInstance().register(this);
    }

    /**
     * 开启发送队列
     */
    public void start() {
        isRunning = true;
        mWorkerThread = new Thread(mWorker);
        mWorkerThread.start();
    }

    public void stop() {
        isRunning = false;
        mWorkerThread.interrupt();
    }

    /**
     * @param obj
     * @return
     */
    public boolean offer(Object obj) {
        if (!Util.isNetworkAvailable(GoddessPlanApplication.getApplication())) {
            return false;
        }
        if (obj instanceof NSMessage) {
            NSMessage msgBean = (NSMessage) obj;
            msgBean.setRetry(0);
            msgBean.setSend(2);
            msgBean.setLocalTime(System.currentTimeMillis());
        }
        mMsgQueue.offer(obj);
        if (TcpClient.getInstance().getConnectState() == TcpClient.ConnectState.CONNECTED) {
            synchronized (mMsgQueue) {
                log.d("notify the queue");
                mMsgQueue.notifyAll();
            }
        }
        return true;
    }

    public LinkedList<Object> getmMsgQueue() {
        return mMsgQueue;
    }

    Runnable mWorker = new Runnable() {
        @Override
        public void run() {
            while (isRunning) {
                try {
                    LinkedList<Object> queues = mMsgQueue;
                    synchronized (queues) {
                        while (queues.isEmpty() && isRunning) {
                            try {
                                log.d("the queue is empty");
                                queues.wait();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    log.d("fetch object send...");
                    Object obj = mMsgQueue.peek();
                    if (obj != null) {
                        synchronized (obj) {
                            if (obj instanceof NSMessage) {
                                NSMessage msgBean = (NSMessage) obj;
                                msgBean.setRetry(msgBean.getRetry() + 1);
                                boolean result = TcpClient.getInstance().sendMsg(msgBean);
                                log.d("send msg result: " + result);
                                if (result) {
                                    log.d("wait msg back timeout: " + SEND_TIME_OUT + "ms");
                                    msgBean.wait(SEND_TIME_OUT);
                                    log.d("the msg notify: ");
                                    if (msgBean.getSend() == 1) {
                                        log.d("the msg send successful: ");
                                        remove(obj);
                                    } else if (msgBean.getSend() == 2) {
                                        if (msgBean.getRetry() < 3) {
                                            log.d("add keeplive to queue...");
                                            mMsgQueue.addFirst(new Heartbeat(2, true));//发心跳
                                        } else {
                                            log.e("the msg send failure: ");
                                            msgBean.setSend(0);
                                            remove(obj);
//                                            MessageObserver.getInstance().notifyDataChanged(null,
//                                                    new ObserverFilter(CMDBean.ACTION_REFRESH_UI));
                                            GoddessPlanApplication.getApplication().send("消息发送失败 localtime： "+msgBean.getLocalTime());
                                        }
                                    } else {
                                        remove(obj);
//                                        MessageObserver.getInstance().notifyDataChanged(null,
//                                                new ObserverFilter(CMDBean.ACTION_REFRESH_UI));
                                        GoddessPlanApplication.getApplication().send("消息发送失败 localtime： "+msgBean.getLocalTime());
                                    }
                                } else {
                                    log.d("the msg send failure: ");
                                    msgBean.setSend(0);
                                    remove(obj);
//                                    MessageObserver.getInstance().notifyDataChanged(null,
//                                            new ObserverFilter(CMDBean.ACTION_REFRESH_UI));
                                    GoddessPlanApplication.getApplication().send("消息发送失败 localtime： "+msgBean.getLocalTime());
                                }

                            } else {
                                Heartbeat heartbeat = (Heartbeat) obj;
                                log.d("send keeplive to server...");
                                boolean result = TcpClient.getInstance().sendMsg(heartbeat.heartbeat);
                                if (heartbeat.isDetect) {
                                    if (result) {
                                        log.d("wait keeplive back timeout: " + SEND_KEEP_LIVE_TIME_OUT + "ms");
                                        heartbeat.wait(SEND_KEEP_LIVE_TIME_OUT);
                                        log.d("the msg notify: ");
                                        remove(obj);
                                        if (heartbeat.send == 2) {
                                            log.d("not received keeplive");
                                            TcpClient.getInstance().reset();
                                            return;
                                        } else {
                                            Object obj1 = mMsgQueue.remove();
                                            if (obj1 instanceof NSMessage) {
                                                NSMessage msgBean = (NSMessage) obj1;
                                                msgBean.setSend(0);
//                                                MessageObserver.getInstance().notifyDataChanged(null,
//                                                        new ObserverFilter(CMDBean.ACTION_REFRESH_UI));
                                                GoddessPlanApplication.getApplication().send("消息发送失败 localtime： "+msgBean.getLocalTime());
                                            } else {
                                                Heartbeat heartbeat1 = (Heartbeat) obj1;
                                                heartbeat1.send = 0;
                                            }
                                        }
                                    } else {
                                        remove(obj);
                                    }
                                } else {
                                    remove(obj);
                                    if (!result) {
                                        TcpClient.getInstance().reset();
                                        return;
                                    }
                                }
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            log.i("the send queue is die...");
        }
    };

    /**
     * 确认消息
     *
     * @param msgBean
     * @param localId
     * @return
     */
    public NSMessage confirmMsg(NSMessage msgBean, long localId) {
//        LinkedList<Object> queues = mMsgQueue;
//        Object obj = queues.peek();
//        if (obj == null)
//            return null;
//        if (obj instanceof MsgBean) {
//            MsgBean oldMsgBean = (MsgBean) obj;
//            if (oldMsgBean.getGroupId() == msgBean.getGroupId() && oldMsgBean.getLocalId() == localId) {
//                oldMsgBean.setMsgId(msgBean.getMsgId());
//                oldMsgBean.setTimeStamp(msgBean.getTimeStamp());
//                oldMsgBean.setSend(1);
//                synchronized (oldMsgBean) {
//                    oldMsgBean.notifyAll();
//                }
//                return oldMsgBean;
//            }
//        }
        return null;
    }

//    /**
//     * @param msgBean
//     * @param localid
//     * @return
//     */
//    public boolean hasOfflineMsg(MsgBean msgBean, long localid) {
//        Object obj = mMsgQueue.peek();
//        if (obj == null)
//            return false;
//        if (obj instanceof MsgBean) {
//            MsgBean msgBean1 = (MsgBean) obj;
//            if (msgBean1.getGroupId() == msgBean.getGroupId() && localid == msgBean1.getLocalId()) {
//                log.d("exist local id :" + msgBean1.getLocalId());
//                mMsgQueue.remove();
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * @param msgBean
     * @return
     */
    public synchronized NSMessage findSendQueueExistByMsgBean(NSMessage msgBean, int groupId) {
//        LinkedList<Object> queue = TcpSendQueue.getInstance().getmMsgQueue();
//        Iterator itr = queue.iterator();
//        while (itr.hasNext()) {
//            Object obj = itr.next();
//            if (obj instanceof NSMessage) {
//                NSMessage msgBean1 = (NSMessage) obj;
//                if (msgBean1.getGroupId() == groupId && msgBean.getLocalId() == msgBean1.getLocalId()) {
//                    log.d("exist local id :" + msgBean1.getLocalId());
//                    return msgBean1;
//                }
//            }
//        }
        return null;
    }

    @Override
    public void notifyTimer() {
        Object obj = mMsgQueue.peek();
        if (obj == null)
            return;
        synchronized (obj) {
            if (obj instanceof NSMessage) {
                NSMessage msgBean = (NSMessage) obj;
                if (System.currentTimeMillis() - msgBean.getLocalTime() >= LONG_TIME_OUT) {
                    log.d("the timer timeout!");
                    msgBean.setSend(0);
                    remove(obj);
//                    MessageObserver.getInstance().notifyDataChanged(null,
//                            new ObserverFilter(CMDBean.ACTION_REFRESH_UI));
                    GoddessPlanApplication.getApplication().send("消息发送失败 localtime： "+msgBean.getLocalTime());
                }
            } else {
                //心跳
                Heartbeat heartbeat = (Heartbeat) obj;
                if (System.currentTimeMillis() - heartbeat.timeStamp >= LONG_TIME_OUT) {
                    heartbeat.send = 0;
                    remove(obj);
                }
            }
        }
    }

    /**
     * 加入消息队列
     *
     * @param obj
     */
    public void addFirst(Object obj) {
        mMsgQueue.addFirst(obj);
        if (TcpClient.getInstance().getConnectState() == TcpClient.ConnectState.CONNECTED) {
            synchronized (mMsgQueue) {
                log.d("notify the queue");
                mMsgQueue.notifyAll();
            }
        }
    }

    private boolean remove(Object obj) {
        if (mMsgQueue.contains(obj)) {
            mMsgQueue.remove();
            return true;
        }
        return false;
    }

    public void clear() {
        mMsgQueue.clear();
    }
}

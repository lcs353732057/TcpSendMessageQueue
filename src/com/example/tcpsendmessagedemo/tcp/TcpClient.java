package com.example.tcpsendmessagedemo.tcp;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.example.tcpsendmessagedemo.GoddessPlanApplication;
import com.example.tcpsendmessagedemo.util.Const;
import com.example.tcpsendmessagedemo.util.Logger;
import com.example.tcpsendmessagedemo.util.Util;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
import java.nio.channels.UnresolvedAddressException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by Administrator on 14-1-14.
 */
public class TcpClient implements Runnable, TimerInstance.NotifyTimer {
    private static TcpClient ourInstance = new TcpClient();

    private static HashMap<String, String> cmdHandlerMap = new HashMap<String, String>();

    public void setListener(ConnectListener listener) {
        this.listener = listener;
    }

    public ConnectListener getListener() {
        return this.listener;
    }

    @Override
    public void notifyTimer() {
        if (mConnectState != ConnectState.CONNECTED) {
            int diff = (int) (System.currentTimeMillis() - mLastKeepLiveTime);
            if (diff >= keep_alive_interval) {
                TcpSendQueue.getInstance().offer(new Heartbeat(2, false));
                no_receive_keep_alive++;
            }
            if (no_receive_keep_alive == 2) {
                log.e("the connection is disconnected");
                reset();
            }
        }
    }

    public interface ConnectListener {
        public void connect(ConnectState state);
    }

    public enum ConnectState implements Serializable {
        INIT, NOCONNECT, CONNECTING, CONNECTED, CLOSE
    }

    private static final int RECONNECT_MSG_WHAT = 1;

    private static final int CONNECT_STATE_WHAT = 2;

    public static final int KEEP_ALIVE_MSG_ID = 202;

    private Logger log = Logger.getLogger();

    private Context cxt;

    /**
     * The connector
     */
    private IoConnector connector;

    private ConnectFuture connFuture;

    private int keep_alive_interval = 60 * 1000;

    private ConnectState mConnectState = ConnectState.INIT;

    private int retry = 0;
    /**
     * 未收到心跳次数
     */
    private int no_receive_keep_alive = 0;

    private ConnectListener listener;

    private Object lock = new Object();

    private long mLastKeepLiveTime = 0l;

    public static TcpClient getInstance() {
        return ourInstance;
    }

    private TcpClient() {
        connector = new NioSocketConnector();
        connector.setHandler(mIoHandlerAdapter);
        SocketSessionConfig dcfg = (SocketSessionConfig) connector
                .getSessionConfig();
        dcfg.setSendBufferSize(1024 * 32);
        dcfg.setWriteTimeout(1000 * 60);
        dcfg.setTcpNoDelay(true);
        dcfg.setIdleTime(IdleStatus.BOTH_IDLE, 500 * 1000);
        dcfg.setWriterIdleTime(500 * 1000);
        dcfg.setReaderIdleTime(500 * 1000);
        dcfg.setKeepAlive(true);
        dcfg.setReadBufferSize(1024 * 1024);
        dcfg.setReceiveBufferSize(1024 * 1024);
        connector.getFilterChain().addLast("logging", new LoggingFilter());
        connector.getFilterChain().addLast("codec",
                new ProtocolCodecFilter(new KXCodecFactory()));
        connector.setConnectTimeoutMillis(30 * 1000);
    }

    public void init(Context cxt) {
        this.cxt = cxt;
    }

    public ConnectState getConnectState() {
        return mConnectState;
    }

    public void setConnectState(ConnectState connectState) {
        this.mConnectState = connectState;
    }

    public synchronized void start() {
        this.mConnectState = ConnectState.NOCONNECT;
        if (!checkTokenValidity()) {
            log.e("login params eror");
            return;
        }
        reset();
        // Establish an connection
        mConnectState = ConnectState.CONNECTING;
        new Thread(this).start();
    }

    private void stopKeepAlives() {
        TimerInstance.getInstance().unregister(this);
    }

    public void retry(boolean isNewSign) {
        mConnectState = ConnectState.NOCONNECT;
        stopKeepAlives();
        if (Util.isNetworkAvailable(cxt)) {
            retry++;
            log.e("retry connect : " + retry);
            int time = retry * 5 * 1000;
            if (time >= 20 * 1000) {
                notifyConnectState(mConnectState);
                scheduleReconnect(isNewSign, 1000 * 20);
            } else {
                scheduleReconnect(isNewSign, time);
            }
        }
    }

    private void scheduleReconnect(boolean isNewSign, int i) {
        cancelReconnect();
        Message msg = mHandler.obtainMessage(RECONNECT_MSG_WHAT);
        msg.obj = isNewSign;
        mHandler.sendMessageDelayed(msg, i);
    }

    private void cancelReconnect() {
        mHandler.removeMessages(RECONNECT_MSG_WHAT);
    }

    public synchronized void reconnectIfNecessary(boolean isForce) {
        log.d("reconnectIfNecessary");
        if (!isForce && mConnectState != ConnectState.NOCONNECT) {
            log.i("chat connecting...");
            return;
        }
        mConnectState = ConnectState.CONNECTING;
        cancelReconnect();
        if (!checkTokenValidity()) {
            log.e("login params eror");
            retry(true);
            return;
        }
        new Thread(this).start();
    }

    @Override
    public void run() {
        log.i("start Connecting...");
        connect();
    }

    /**
     * 连接
     */
    private void connect() {
        synchronized (lock) {
            try {
                notifyConnectState(mConnectState);
                connFuture = connector.connect(new InetSocketAddress(
                        Const.HOST, Const.PORT));
                // 等待连接是否成功,相当于是转异步为同步执行
                connFuture.awaitUninterruptibly(30 * 1000);
                log.d("connect end.........");
                if (connFuture.isConnected()) {
                    log.i("connect chat server successful");
                } else {
                    log.d("connect chat server fail");
                    connFuture.cancel();
                    retry(false);
                }
            } catch (UnresolvedAddressException e) {
                e.printStackTrace();
                log.d("connect chat server fail");
                retry(false);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                log.d("connect chat server fail");
                retry(false);
            }
        }
    }

    private IoHandlerAdapter mIoHandlerAdapter = new IoHandlerAdapter() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void exceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            log.e("exceptionCaught");
            cause.printStackTrace();
            log.e(cause.toString());
            if (cause instanceof BufferOverflowException) {
                mConnectState = ConnectState.CLOSE;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
//            log.d("session: " + session);
            log.i("messageReceived..." + message.toString());
            GoddessPlanApplication.getApplication().send(message.toString());
            JSONObject jsonObject = (JSONObject) message;
            if (!jsonObject.has("cmd"))
                return;
            try {
                if ("S_LOGIN".equals(jsonObject.getString("cmd"))) {
                    parserLogin(jsonObject);
                } else if ("S_KA".equals(jsonObject.getString("cmd"))) {
                    parserKeepAlive(jsonObject);
                } else if ("S_CLOSE".equals(jsonObject.getString("cmd"))) {
                    if (jsonObject.getInt(ECODE) == 499) {
                        occupy();
                    } else if (jsonObject.getInt(ECODE) == 401) {
                        doFail401();
                    } else if (jsonObject.getInt(ECODE) == 497) {
                        kicked();
                    } else {
                        log.e("close long connection");
                        doFail();
                    }
                } else {
                    no_receive_keep_alive = 0;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void messageSent(IoSession session, Object message)
                throws Exception {
            log.i("messageSent...");
//            log.d("session: " + session);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sessionClosed(IoSession session) throws Exception {
            log.e("sessionClosed...");
//            log.d("session: " + session);
            if (mConnectState != ConnectState.CLOSE) {
                mConnectState = ConnectState.NOCONNECT;
                retry(false);
            }
            notifyConnectState(mConnectState);

            TcpSendQueue.getInstance().stop();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sessionCreated(IoSession session) throws Exception {
            log.i("sessionCreated...");
//            log.d("session: " + session);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sessionIdle(IoSession session, IdleStatus status)
                throws Exception {
            log.e("sessionIdle...");
            reset();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void sessionOpened(IoSession session) throws Exception {
            log.i("sessionOpened...");
            byte[] pushTokenParse = TokenPacket.encodeTokenPacket(Const.UID, Const.TIMESTAMP,
                    Const.SIGN, Const.TOKEN);
            if (pushTokenParse != null) {
                sendMsg(pushTokenParse);
            }
        }
    };

    /**
     * 重复登录
     */
    private void occupy() {
        log.e("the line is occupied");
        mConnectState = ConnectState.CLOSE;
    }

    /**
     * 被踢下线
     */
    private void kicked() {
        log.e("the user Kicked offline");
        mConnectState = ConnectState.CLOSE;
    }

    public boolean sendMsg(byte[] bytes) {
        if (connFuture.getSession() != null) {
            log.d("send msg: " + new String(bytes));
            connFuture.getSession().write(bytes);
            return true;
        } else {
            log.e("message send fail,because session is null");
            reset();
            return false;
        }
    }

    /**
     * @param msgBean
     * @return
     */
    public boolean sendMsg(NSMessage msgBean) {
        return sendMsg(TokenPacket.sendMsgPacket(msgBean));
    }

    public void close() {
        stopKeepAlives();
        if (mConnectState == ConnectState.CONNECTED || mConnectState == ConnectState.CONNECTING) {
            mConnectState = ConnectState.CLOSE;
            if (connFuture != null) {
                connFuture.cancel();
                if (connFuture.getSession() != null)
                    connFuture.getSession().close(true);
            }
        } else {
            mConnectState = ConnectState.CLOSE;
        }
    }

    /**
     * 重置连接
     */
    public void reset() {
        stopKeepAlives();
        if (connFuture != null && connFuture.getSession() != null)
            connFuture.getSession().close(true);
    }

    public static final String ECODE = "ecode";

    private void parserLogin(JSONObject jsonObject) {
        try {
            if (jsonObject.getInt(ECODE) == 200) {
                retry = 0;
                mConnectState = ConnectState.CONNECTED;
                notifyConnectState(mConnectState);
                keep_alive_interval = jsonObject.getInt("ttl") * 1000;
                log.d("keep alive: " + keep_alive_interval);
                TcpSendQueue.getInstance().start();
                cancelReconnect();
                startKeepAlives();
            } else {
                doFail();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理失败
     */
    private void doFail() {
        log.e("login chat server fail");
    }

    private void doFail401() {
        log.e("login chat server fail");
    }

    private void startKeepAlives() {
        TimerInstance.getInstance().register(this);
    }

    /**
     * 通知前台刷新ui
     *
     * @param state
     */
    private void notifyConnectState(ConnectState state) {
        Message msg = mHandler.obtainMessage(CONNECT_STATE_WHAT);
        msg.obj = state;
        mHandler.sendMessage(msg);
    }

    private void parserKeepAlive(JSONObject jsonObject) {
        try {
            if (jsonObject.getInt(ECODE) == 200) {
                no_receive_keep_alive = 0;
                LinkedList<Object> queues = TcpSendQueue.getInstance().getmMsgQueue();
                Object obj = queues.peek();
                if (obj instanceof Heartbeat) {
                    synchronized (obj) {
                        Heartbeat heartbeat = (Heartbeat) obj;
                        heartbeat.send = 1;
                        heartbeat.notifyAll();
                    }
                }
            } else {
                log.e("receive chat server fail");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查用户信息有效性
     *
     * @return
     */
    private boolean checkTokenValidity() {
        return true;
    }

    /*
    * 添加要接收的tcp指令
    * */
    static {
        cmdHandlerMap.put("OFFMSG", "OFFMSGResponse");
        cmdHandlerMap.put("USER_MSG", "UserMsgResponse");
        cmdHandlerMap.put("GROUP_MSG", "GroupMsgResponse");
        cmdHandlerMap.put("QUIT_GROUP", "PersonCMDResponse");
        cmdHandlerMap.put("JOIN_GROUP", "PersonCMDResponse");
        cmdHandlerMap.put("DISBAND_GROUP", "PersonCMDResponse");
        cmdHandlerMap.put("KICK_GROUP", "PersonCMDResponse");
        cmdHandlerMap.put("MSG_ERROR", "MsgErrorResponse");
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECONNECT_MSG_WHAT:
                    if (Util.isNetworkAvailable(cxt)) {
                        mConnectState = ConnectState.NOCONNECT;
                        reconnectIfNecessary(false);
                    }
                    break;
                case CONNECT_STATE_WHAT:
                    if (listener != null) {
                        listener.connect(mConnectState);
                    }
                    break;
                default:
                    break;
            }
        }
    };
}

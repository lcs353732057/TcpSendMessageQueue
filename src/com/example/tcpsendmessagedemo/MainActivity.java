package com.example.tcpsendmessagedemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.tcpsendmessagedemo.tcp.Heartbeat;
import com.example.tcpsendmessagedemo.tcp.NSMessage;
import com.example.tcpsendmessagedemo.tcp.TcpClient;
import com.example.tcpsendmessagedemo.tcp.TcpSendQueue;
import com.example.tcpsendmessagedemo.util.Logger;

public class MainActivity extends Activity implements View.OnClickListener, TcpClient.ConnectListener {
    Logger log = Logger.getLogger();
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button).setOnClickListener(this);
        findViewById(R.id.button2).setOnClickListener(this);
        findViewById(R.id.button3).setOnClickListener(this);
        tv = GoddessPlanApplication.getApplication().tv = (android.widget.TextView) findViewById(R.id.textView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        TcpClient.getInstance().setListener(this);
        registerReceiver(mConnectivityChanged, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mConnectivityChanged);
        GoddessPlanApplication.getApplication().tv = null;
    }

    @Override
    public void onClick(View view) {
        if (R.id.button == view.getId()) {
            TcpClient.getInstance().start();
        } else if (R.id.button2 == view.getId()) {
            int localId = 2006;
            int groupId = 456;
            String text = "发送一条消息";
            TcpSendQueue.getInstance().offer(new NSMessage(2, 0, System.currentTimeMillis(), localId, groupId, text));
        } else {
            TcpClient.getInstance().close();
        }
    }

    // connection
    // accordingly
    private BroadcastReceiver mConnectivityChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get network info
            NetworkInfo info = (NetworkInfo) intent
                    .getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

            // Is there connectivity?
            boolean hasConnectivity = (info != null && info.isConnected()) ? true
                    : false;

            log.i("Connectivity changed: connected=" + hasConnectivity);
            TcpClient.ConnectState connectState = TcpClient.getInstance()
                    .getConnectState();
            log.i("the tcp connect state: " + connectState);
            if (connectState == TcpClient.ConnectState.CLOSE) {
                return;
            } else if (hasConnectivity
                    && connectState == TcpClient.ConnectState.NOCONNECT) {
                TcpClient.getInstance().reconnectIfNecessary(true);
            } else if (!hasConnectivity
                    && connectState == TcpClient.ConnectState.CONNECTED) {
                TcpSendQueue.getInstance().addFirst(new Heartbeat(2, true));
            }
            TcpClient.ConnectListener listener = TcpClient.getInstance().getListener();
            if (listener != null) {
                listener.connect(connectState);
            }
        }
    };

    @Override
    public void connect(TcpClient.ConnectState state) {
        if (state == TcpClient.ConnectState.CONNECTING) {
            tv.setText("连接中...");
        } else if (state == TcpClient.ConnectState.CONNECTED) {
            tv.setText("连接成功");
        } else if (state == TcpClient.ConnectState.NOCONNECT || state == TcpClient.ConnectState.CLOSE) {
            tv.setText("连接断开");
        }
    }
}

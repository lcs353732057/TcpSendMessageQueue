package com.example.tcpsendmessagedemo;

import android.app.Application;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.example.tcpsendmessagedemo.tcp.TcpClient;
import com.example.tcpsendmessagedemo.tcp.TimerInstance;

import org.w3c.dom.Text;

public class GoddessPlanApplication extends Application {

    private static GoddessPlanApplication mApplication;

    public TextView tv;

    @Override
    public void onCreate() {
        super.onCreate();

        mApplication = this;
        TcpClient.getInstance().init(this);
        TimerInstance.getInstance().startTimer();

    }

    public static GoddessPlanApplication getApplication() {
        return mApplication;
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (tv != null) {
                tv.setText((CharSequence) msg.obj);
            }
        }
    };

    public void send(String text) {
        Message msg = handler.obtainMessage(0);
        msg.obj = text;
        handler.sendMessage(msg);
    }

}

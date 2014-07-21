package com.example.tcpsendmessagedemo.tcp;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2014/7/3.
 */
public class TimerInstance {
    public static final int PERIOD = 1000;

    public interface NotifyTimer {
        public void notifyTimer();
    }

    private static TimerInstance ourInstance = new TimerInstance();

    private ArrayList<NotifyTimer> list = new ArrayList<NotifyTimer>();

    private Timer mTimer;

    private TimerTask mTimerTask;

    public static TimerInstance getInstance() {
        return ourInstance;
    }

    private TimerInstance() {
    }

    public void register(NotifyTimer notifyTimer) {
        synchronized (list) {
            if (!list.contains(notifyTimer))
                list.add(notifyTimer);
        }
    }

    public void unregister(NotifyTimer notifyTimer) {
        synchronized (list) {
            if (list.contains(notifyTimer))
                list.remove(notifyTimer);
        }
    }

    public void startTimer() {
        if (mTimer == null) {
            mTimer = new Timer();
        }
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    for (NotifyTimer notifyTimer : list) {
                        notifyTimer.notifyTimer();
                    }
                }
            };
        }

        if (mTimer != null && mTimerTask != null)
            mTimer.schedule(mTimerTask, 0, PERIOD);
    }

    public void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }
}

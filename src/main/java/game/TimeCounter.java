package game;

import javax.swing.Timer;

public class TimeCounter {
    private Timer timer;
    long startTime;

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void start(long time) {
        if (time > 0)
            startTime = System.currentTimeMillis() - time;
        else start();
    }

    public String getFormatedTime() {
        return formatTime(getTime());
    }

    public static String formatTime(long millis) {
        long milliSeconds = millis % 1000;
        long seconds = millis / 1000 % 60;
        long minutes = millis / (1000 * 60) % 60;
        long hours = millis / (1000 * 60 * 60) % 24;
        return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, milliSeconds);
    }

    public long getTime() {
        return System.currentTimeMillis() - startTime;
    }
}

package game;

import java.io.IOException;

public class MusicPlayer {
    public static void play(String name) { //偷懒了，应该用getResourceAsStream
        stop();
        try {
            String currentDir = MusicPlayer.class.getResource("/music/").getPath().substring(1).replace("/","\\");
            Runtime.getRuntime().exec("cmd /c " + currentDir + "player\\MODPLUG.EXE " + currentDir + name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        try {
            Process process= Runtime.getRuntime().exec("taskkill /IM MODPLUG.EXE");
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

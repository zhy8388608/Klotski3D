package login;

import com.fasterxml.jackson.databind.ObjectMapper;
import game.MainThread;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Intermediary {
    public static int level;
    public static User user;

    // 保存用户数据
    public static void save(String historyStepNumbers, String historyStepDirections, long historyTime) {

        System.out.println("已保存");
        while (user.getLevels().size() < level)
            user.getLevels().add(new Level());
        Level l = user.getLevels().get(level - 1);
        l.setStepNumbers(historyStepNumbers);
        l.setStepDirections(historyStepDirections);
        l.setHistoryTime(historyTime);
        if (user.getName() != "Guest")
            ArchiveManager.writeUserArchive(LoginFrame.userArchive);
    }

    // 处理通关信息
    public static void win(int step, long time) {
        Level l = user.getLevels().get(level - 1);
        if (l.getBestSteps() == -1 || l.getBestSteps() > step)
            l.setBestSteps(step);
        if (l.getBestTime() == -1 || l.getBestTime() > time)
            l.setBestTime(time);
        user.setProgress(Math.max(user.getProgress(), level + 1));
        if (user.getName() != "Guest")
            ArchiveManager.writeUserArchive(LoginFrame.userArchive);
        LoginFrame.loginFrame.showWinning(level);
    }

    // 加载用户数据
    public static void load(User user1, int level1) {
        user = user1;
        level = level1;
        String historyStepNumbers;
        String historyStepDirections;
        long historyTime;
        if (user.getName() != "Guest" && user.getLevels().size() >= level) {
            historyStepNumbers = user.getLevels().get(level - 1).getStepNumbers();
            historyStepDirections = user.getLevels().get(level - 1).getStepDirections();
            historyTime = user.getLevels().get(level - 1).getHistoryTime();
        } else {
            historyStepDirections = "";
            historyStepNumbers = "";
            historyTime = 0;
        }

        new Thread(() -> { //必须开新线程，否则Swing会阻塞
            MainThread mainThread = new MainThread();
            mainThread.startLevel(level - 1, historyStepNumbers, historyStepDirections, historyTime);
        }).start();
    }
}
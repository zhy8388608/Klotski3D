package login;

import java.util.ArrayList;
import java.util.List;

// 用户数据模型
public class User {
    private String name;
    private String password;
    private int progress;
    private List<Level> levels;

    public User() {
        this.levels = new ArrayList<>();
    }

    public User(String name, String password, int progress, List<Level> levels) {
        this.name = name;
        this.password = password;
        this.progress = progress;
        this.levels = levels;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public List<Level> getLevels() {
        return levels;
    }

    public void setLevels(List<Level> levels) {
        this.levels = levels;
    }
}
package login;

// 关卡数据模型
public class Level {
    private Integer bestSteps;
    private long bestTime;
    private String stepNumbers;
    private String stepDirections;


    private long historyTime;

    public Level(){
        this.bestSteps = -1;
        this.stepNumbers = "";
        this.bestTime = -1;
        this.stepDirections = "";
    }

    public Level(Integer bestSteps, String n, long bestTime, String d) {
        this.bestSteps = bestSteps;
        this.stepNumbers = n;
        this.bestTime = bestTime;
        this.stepDirections = d;
    }

    public long getBestTime() {
        return bestTime;
    }

    public void setBestTime(long bestTime) {
        this.bestTime = bestTime;
    }


    public long getHistoryTime() {
        return historyTime;
    }

    public void setHistoryTime(long historyTime) {
        this.historyTime = historyTime;
    }

    public Integer getBestSteps() {
        return bestSteps;
    }

    public void setBestSteps(Integer bestSteps) {
        this.bestSteps = bestSteps;
    }

    public String getStepNumbers() {
        return stepNumbers;
    }

    public void setStepNumbers(String stepNumbers) {
        this.stepNumbers = stepNumbers;
    }

    public String getStepDirections() { // 新增 getter 方法
        return stepDirections;
    }

    public void setStepDirections(String stepDirections) { // 新增 setter 方法
        this.stepDirections = stepDirections;
    }
}
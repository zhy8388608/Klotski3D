package game;

import core.Camera;
import core.Vector3D;

import java.util.ArrayList;

public class BoxManager {
    //合集
    public static Box[] boxes = new Box[1000];
    public static Box[] targetBoxes = new Box[100];
    public static int totalBoxes, maxNumber, targetBoxesCount;
    public static Boolean isFoucusModeOn;
    public static Boolean isXrayModeOn;
    public static boolean isShowingTarget = false;
    public static ArrayList<ArrayList<Integer>> stepNumber; //要考虑撤销，所以一步移动多个都要记录
    public static ArrayList<Vector3D> stepDirection;
    public static int totalSteps;
    public static boolean isRecordingSteps;
    public static Box box = new Box();
    public static BoxMap boxMap = new BoxMap();

    public static void init(int[][][] map) {
        boxMap.init(map);
        totalBoxes = maxNumber = totalSteps = 0;
        isFoucusModeOn = false;
        isXrayModeOn = false;
        stepNumber = new ArrayList<>();
        stepDirection = new ArrayList<>();
        isRecordingSteps = true;

        for (int i = 0; i < boxMap.xLength; i++)
            for (int j = 0; j < boxMap.yLength; j++)
                for (int k = 0; k < boxMap.zLength; k++)
                    if (boxMap.boxes[i][j][k] > 0 || boxMap.boxes[i][j][k] == -1) {
                        int number = boxMap.boxes[i][j][k];
                        boxes[totalBoxes] = (Box) box.clone();
                        boxes[totalBoxes].number = number;
                        boxes[totalBoxes].textureIndex = 10 + (number <= 10 ? number : Algorithms.clampCircular(number,1,10));
                        boxes[totalBoxes].place.set(i, j, k);

                        totalBoxes++;
                        maxNumber = Math.max(maxNumber, number);
                    }

    }

    public static void setTargetBoxes(int[][][] map) {
        targetBoxesCount = 0;
        int xLength = map.length;
        int yLength = map[0].length;
        int zLength = map[0][0].length;
        for (int i = 0; i < xLength; i++)
            for (int j = 0; j < yLength; j++)
                for (int k = 0; k < zLength; k++)
                    if (map[i][j][k] > 0 || map[i][j][k] == -1) {
                        int number = map[i][j][k];
                        targetBoxes[targetBoxesCount] = (Box) box.clone();
                        targetBoxes[targetBoxesCount].number = number;
                        targetBoxes[targetBoxesCount].textureIndex = 10 + (number <= 10 ? number : Algorithms.clampCircular(number,1,10));
                        targetBoxes[targetBoxesCount].place.set(i, j, k);
                        targetBoxes[targetBoxesCount].localTranslation.set(i, j, k);
                        targetBoxesCount++;
                    }
    }

    public static boolean checkWin() {
        for (int i = 0; i < targetBoxesCount; i++) {
            Vector3D v = targetBoxes[i].place;
            if (boxMap.boxes[(int) v.x][(int) v.y][(int) v.z] != targetBoxes[i].number)
                return false;
        }
        return true;
    }

    //step[0,1]
    public static void animate(float step) {
        for (int i = 0; i < totalBoxes; i++)
            boxes[i].localTranslation.add(new Vector3D(boxes[i].place).subtract(boxes[i].localTranslation).scale(step));
    }

    public static void undo() {
        if (totalSteps <= 0)
            return;
        isRecordingSteps = false;
        totalSteps--;
        move(stepNumber.get(totalSteps), new Vector3D(stepDirection.get(totalSteps)).scale(-1));
        isRecordingSteps = true;
    }

    public static void redo() {
        if (totalSteps >= stepDirection.size())
            return;
        isRecordingSteps = false;
        move(stepNumber.get(totalSteps), new Vector3D(stepDirection.get(totalSteps)));
        totalSteps++;
        isRecordingSteps = true;
    }

    //number<0的箱子无法移动
    public static int moveByScreenDirection(int number, Vector3D direction) {
        if (number <= 0)
            return -1;
        //确定移动方向
        direction.rotate_X(Camera.X_angle);
        direction.rotate_Y(Camera.Y_angle);
        direction.unit();
        //按照分量进行优先级排序
        float sortBy[] = new float[]{Math.abs(direction.x), Math.abs(direction.y), Math.abs(direction.z)};
        Vector3D sortTo[] = new Vector3D[]{new Vector3D(direction.x > 0 ? 1 : -1, 0, 0), new Vector3D(0, direction.y > 0 ? 1 : -1, 0), new Vector3D(0, 0, direction.z > 0 ? 1 : -1)};
        for (int i = sortBy.length - 1; i > 0; i--) //按照不同方向分量大小进行冒泡排序
            for (int j = 0; j < i; j++)
                if (sortBy[j] < sortBy[j + 1]) {
                    float temp1 = sortBy[j];
                    sortBy[j] = sortBy[j + 1];
                    sortBy[j + 1] = temp1;
                    Vector3D temp2 = sortTo[j];
                    sortTo[j] = sortTo[j + 1];
                    sortTo[j + 1] = temp2;
                }
        //逐个尝试，增加容错
        int res = 0;
        for (int i = 0; i < sortBy.length; i++) {
            if (sortBy[i] < 0.2f) //太偏就不容错
                break;
            res = move(number, sortTo[i]);
            if (res == 0)
                break;
        }
        return res;
    }

    public static int move(int number, Vector3D direction) {
        if (number <= 0)
            return -1;
        ArrayList<Integer> numbers = new ArrayList<Integer>();
        numbers.add(number);
        return move(numbers, direction);
    }

    public static int move(ArrayList<Integer> numbers, Vector3D direction) {
        //移动地图标记
        int res = boxMap.move(numbers, direction);
        if (res < 0)
            return res;
        //移动箱子位置
        for (int i = 0; i < totalBoxes; i++) {
            if (numbers.contains(boxes[i].number)) //由于Java的传参机制，此时numbers自带所有成功移动的方块编号
                boxes[i].place.add(direction);
        }
        //记录步骤
        if (isRecordingSteps)
            if (totalSteps == stepDirection.size()) { //全新步骤
                totalSteps++;
                stepDirection.add(direction);
                stepNumber.add(numbers);
            } else if (stepDirection.get(totalSteps).equals(direction) && stepNumber.get(totalSteps).equals(numbers)) { //回退后重复一样的步骤
                totalSteps++;
            } else { //回退后做不一样的步骤
                while (stepDirection.size() > totalSteps)
                    stepDirection.remove(stepDirection.size() - 1);
                while (stepNumber.size() > totalSteps)
                    stepNumber.remove(stepNumber.size() - 1);
                totalSteps++;
                stepDirection.add(direction);
                stepNumber.add(numbers);
            }
        return 0;
    }

    //不透明/透明图层分配
    public static void distributeLayerByCut(Vector3D cut, int[] screen1, float[] zBuffer1, int[] screen2, float[] zBuffer2) {
        float x1 = 0, y1 = 0, z1 = 0, x2 = boxMap.xLength - 1, y2 = boxMap.yLength - 1, z2 = boxMap.zLength - 1;
        if (cut.x > 0) x1 += cut.x;
        else x2 += cut.x;
        if (cut.y > 0) y1 += cut.y;
        else y2 += cut.y;
        if (cut.z > 0) z1 += cut.z;
        else z2 += cut.z;
        for (int i = 0; i < totalBoxes; i++) {
            Vector3D pos = boxes[i].place;
            if (pos.x >= x1 && pos.x <= x2 && pos.y >= y1 && pos.y <= y2 && pos.z >= z1 && pos.z <= z2) {
                boxes[i].setScreen(screen1, zBuffer1);
                boxes[i].hasShadow = true;
            } else {
                boxes[i].setScreen(screen2, zBuffer2);
                boxes[i].hasShadow = false;
            }
        }
    }

    public static void distributeLayerByNumber(int number, int[] screen1, float[] zBuffer1, int[] screen2, float[] zBuffer2) {
        ArrayList<Integer> numbers = new ArrayList<Integer>();
        numbers.add(number);
        distributeLayerByNumber(numbers, screen1, zBuffer1, screen2, zBuffer2);
    }

    public static void distributeLayerByNumber(ArrayList<Integer> numbers, int[] screen1, float[] zBuffer1, int[] screen2, float[] zBuffer2) {
        //计算相邻的所有数字
        ArrayList<Integer> nearNumbers = new ArrayList<Integer>();
        int x, y, z;
        int[][] step = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int i = 0; i < totalBoxes; i++)
            if (numbers.contains(boxes[i].number)) {
                x = (int) boxes[i].place.x;
                y = (int) boxes[i].place.y;
                z = (int) boxes[i].place.z;
                for (int j = 0; j < 6; j++)
                    if (boxMap.fits(x + step[j][0], y + step[j][1], z + step[j][2])) {
                        int number1 = boxMap.boxes[x + step[j][0]][y + step[j][1]][z + step[j][2]];
                        if (!numbers.contains(number1) && !nearNumbers.contains(number1))
                            nearNumbers.add(number1);
                    }
            }
        //分配图层
        for (int i = 0; i < totalBoxes; i++)
            if (numbers.contains(boxes[i].number)) {
                boxes[i].setScreen(screen1, zBuffer1);
                boxes[i].isHide = false;
                boxes[i].hasShadow = true;
            } else if (nearNumbers.contains(boxes[i].number)) {
                boxes[i].setScreen(screen2, zBuffer2);
                boxes[i].isHide = false;
                boxes[i].hasShadow = false;
            } else
                boxes[i].isHide = true;
    }

    public static void distributeLayerByVoid(int[] screen1, float[] zBuffer1, int[] screen2, float[] zBuffer2) {
        int[][] step = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        ArrayList<Integer> focusNumbers = new ArrayList<>();
        for (int i = 0; i < boxMap.xLength; i++)
            for (int j = 0; j < boxMap.yLength; j++)
                for (int k = 0; k < boxMap.zLength; k++)
                    if (boxMap.boxes[i][j][k] == 0)
                        for (int l = 0; l < 6; l++)
                            if (boxMap.fits(i + step[l][0], j + step[l][1], k + step[l][2])) {
                                int number1 = boxMap.boxes[i + step[l][0]][j + step[l][1]][k + step[l][2]];
                                if (number1 != 0 && !focusNumbers.contains(number1))
                                    focusNumbers.add(number1);
                            }
        distributeLayerByNumber(focusNumbers, screen1, zBuffer1, screen2, zBuffer2);
    }

    public static void showAll(int[] screen, float[] zBuffer) {
        for (int i = 0; i < totalBoxes; i++) {
            boxes[i].setScreen(screen, zBuffer);
            boxes[i].hasShadow = true;
        }
    }

    public static void setShowTargetScreen(int[] screen1, float[] zBuffer1, int[] screen2, float[] zBuffer2) {
        for (int i = 0; i < totalBoxes; i++) {
            boxes[i].setScreen(screen2, zBuffer2);
            boxes[i].hasShadow = false;
        }
        for (int i = 0; i < targetBoxesCount; i++)
            targetBoxes[i].setScreen(screen1, zBuffer1);
    }

    public static void switchFoucusMode(boolean on) {
        if (on)
            isFoucusModeOn = true;
        else {
            isFoucusModeOn = false;
            for (int i = 0; i < totalBoxes; i++) {
                boxes[i].isHide = false;
                boxes[i].hasShadow = true;
            }
        }
    }

    public static void setShadow(int number, boolean on) {
        for (int i = 0; i < totalBoxes; i++)
            if (boxes[i].number == number)
                boxes[i].hasShadow = on;
        for (int i = 0; i < targetBoxesCount; i++)
            if (targetBoxes[i].number == number)
                targetBoxes[i].hasShadow = on;
    }


}

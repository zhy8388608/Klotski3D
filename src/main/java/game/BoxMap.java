package game;

import core.Vector3D;

import java.util.ArrayList;

public class BoxMap implements Cloneable{
    public int[][][] boxes;
    public int xLength;
    public int yLength;
    public int zLength;

    @Override
    public Object clone() {
        BoxMap clone = null;
        try {
            clone = (BoxMap) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        clone.boxes = new int[xLength][yLength][zLength];
        for (int i = 0; i < xLength; i++)
            for (int j = 0; j < yLength; j++)
                for (int k = 0; k < zLength; k++)
                    clone.boxes[i][j][k] = boxes[i][j][k];
        return clone;
    }

    public void init(int[][][] boxes1) {
        boxes = boxes1;
        xLength = boxes.length;
        yLength = boxes[0].length;
        zLength = boxes[0][0].length;
    }

    public boolean fits(int x, int y, int z) {
        return Algorithms.fits(x, 0, xLength - 1) && Algorithms.fits(y, 0, yLength - 1) && Algorithms.fits(z, 0, zLength - 1);
    }

    public ArrayList<Integer> move(int number, Vector3D direction) {
        if (number <= 0)
            return null;
        ArrayList<Integer> numbers = new ArrayList<Integer>();
        numbers.add(number);
        if(move(numbers, direction)==0)
            return numbers;
        return null;
    }

    public int move(ArrayList<Integer> numbers, Vector3D direction) {
        int success=-1;
        if (numbers.contains(-1)) //-1是不可移动的方块
            return -1;
        //判断能否移动
        for (int i = 0; i < xLength; i++)
            for (int j = 0; j < yLength; j++)
                for (int k = 0; k < zLength; k++)
                    if (numbers.contains(boxes[i][j][k])) {
                        if (!fits(i + (int) direction.x, j + (int) direction.y, k + (int) direction.z))
                            return -1;
                        int n1 = boxes[i + (int) direction.x][j + (int) direction.y][k + (int) direction.z];
                        if (n1 != 0 && !numbers.contains(n1)) {
                            numbers.add(n1);
                            return move(numbers, direction);
                        }
                    }
        //移动地图标记
        int easyFor[][] = new int[3][3];
        if (direction.x < 0) easyFor[0] = new int[]{0, xLength, 1};
        else easyFor[0] = new int[]{xLength - 1, -1, -1};
        if (direction.y < 0) easyFor[1] = new int[]{0, yLength, 1};
        else easyFor[1] = new int[]{yLength - 1, -1, -1};
        if (direction.z < 0) easyFor[2] = new int[]{0, zLength, 1};
        else easyFor[2] = new int[]{zLength - 1, -1, -1};
        for (int i = easyFor[0][0]; i != easyFor[0][1]; i += easyFor[0][2])
            for (int j = easyFor[1][0]; j != easyFor[1][1]; j += easyFor[1][2])
                for (int k = easyFor[2][0]; k != easyFor[2][1]; k += easyFor[2][2])
                    if (numbers.contains(boxes[i][j][k])) {
                        boxes[i + (int) direction.x][j + (int) direction.y][k + (int) direction.z] = boxes[i][j][k];
                        boxes[i][j][k] = 0;
                        success=0;
                    }
        return success;
    }
}
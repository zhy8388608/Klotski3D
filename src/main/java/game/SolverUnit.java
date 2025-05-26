package game;

import java.util.ArrayList;
import java.util.Arrays;

public class SolverUnit implements Cloneable {
    public BoxMap boxMap;
    public int[][] quickMap, sortedQuickMap;
    public ArrayList<Integer> stepNumber = new ArrayList<>(); //作记录用
    public ArrayList<Integer> stepDirection = new ArrayList<>(); //作记录用

    @Override
    public SolverUnit clone() {
        SolverUnit clone = new SolverUnit();
        try {
            clone = (SolverUnit) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        clone.boxMap = (BoxMap) boxMap.clone();
        clone.stepNumber = (ArrayList<Integer>) stepNumber.clone();
        clone.stepDirection = (ArrayList<Integer>) stepDirection.clone();
        clone.quickMap = new int[quickMap.length][quickMap[0].length];
        clone.sortedQuickMap = new int[sortedQuickMap.length][sortedQuickMap[0].length];
        for (int i = 0; i < quickMap.length; i++)
            for (int j = 0; j < quickMap[0].length; j++) {
                clone.quickMap[i][j] = quickMap[i][j];
                clone.sortedQuickMap[i][j] = sortedQuickMap[i][j];
            }
        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        return Arrays.deepEquals(((SolverUnit) obj).sortedQuickMap, sortedQuickMap);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(sortedQuickMap);
    }

    //剪枝换位情况
    public void sortQuickMap(ArrayList<ArrayList<Integer>> boundedNumbers) {
        if(boundedNumbers==null){ //没有同形状的盒子
            sortedQuickMap = quickMap;
            return;
        }
        for (int i = 0; i < quickMap.length; i++)
            for (int j = 0; j < quickMap[0].length; j++)
                sortedQuickMap[i][j] = quickMap[i][j];
        int[] tmp;
        for (ArrayList<Integer> bound : boundedNumbers)
            for (int i = 0; i < 3; i++) //依次针对xyz坐标排序
                for (int a = 0; a < bound.size() - 1; a++) //冒泡排序
                    for (int b = 0; b < bound.size() - 1 - a; b++)
                        if (sortedQuickMap[bound.get(b)][i] > sortedQuickMap[bound.get(b + 1)][i]) {
                            tmp = sortedQuickMap[bound.get(b)];
                            sortedQuickMap[bound.get(b)] = sortedQuickMap[bound.get(b + 1)];
                            sortedQuickMap[bound.get(b + 1)] = tmp;
                        }
    }
}

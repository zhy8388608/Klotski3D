package game;

import core.Vector3D;

import java.util.*;

public class Solver {
    static int[][][] targetMap;
    static int xLength;
    static int yLength;
    static int zLength;
    static int solveMode = 0; //0=华容道，1=puzzle。

    public static Vector3D[] steps = new Vector3D[]{
            new Vector3D(-1, 0, 0), new Vector3D(1, 0, 0),
            new Vector3D(0, -1, 0), new Vector3D(0, 1, 0),
            new Vector3D(0, 0, -1), new Vector3D(0, 0, 1)
    };

    //maxNumber最好不要超，否则很慢。mode=0针对华容道，所有特定块达到终点位置才停；mode=1针对puzzle，任意块达到终点区域就停。boundedNumbers表示形状相同的盒子。
    public static SolverUnit solve(int[][][] boxMap1, int[][][] targetMap1, int maxNumber, int solveMode1, ArrayList<ArrayList<Integer>> boundedNumbers) {
        targetMap = targetMap1;
        xLength = targetMap.length;
        yLength = targetMap[0].length;
        zLength = targetMap[0][0].length;
        solveMode = solveMode1;

        BoxMap boxMap = new BoxMap();
        boxMap.init(boxMap1);
        SolverUnit solverUnit = new SolverUnit();
        solverUnit.boxMap = boxMap;
        int x = boxMap1.length, y = boxMap1[0].length, z = boxMap1[0][0].length;
        solverUnit.quickMap = new int[maxNumber + 1][3];
        solverUnit.sortedQuickMap = new int[maxNumber + 1][3];
        for (int i = 0; i < x; i++) //存储方块的角落位置
            for (int j = 0; j < y; j++)
                for (int k = 0; k < z; k++) {
                    int number = boxMap1[i][j][k];
                    if (number > 0) {
                        solverUnit.quickMap[number][0] = i;
                        solverUnit.quickMap[number][1] = j;
                        solverUnit.quickMap[number][2] = k;
                    }
                }
        solverUnit.sortQuickMap(boundedNumbers);

        //特殊情况
        if (solveMode == 0)
            if (checkAtTarget(solverUnit.boxMap.boxes))
                return null;

        Queue<SolverUnit> openList = new LinkedList<SolverUnit>();
        openList.add(solverUnit);
        Set<SolverUnit> closedUnits = new HashSet<SolverUnit>();
        closedUnits.add(solverUnit);

        ArrayList<Integer> inTargetNumbers = new ArrayList<Integer>();
        if (solveMode == 1)
            inTargetNumbers = checkInTarget(solverUnit.boxMap.boxes);
        int lastSolvedCount = inTargetNumbers.size();

        int lastSteps = 0;
        while (!openList.isEmpty()) {
            if (openList.peek().stepNumber.size() != lastSteps)
                System.out.println(++lastSteps + " " + closedUnits.size());

            SolverUnit currentUnit = openList.remove(); //初始情况
            SolverUnit tryUnit = currentUnit.clone(); //用于检测移动
            for (int i = 0; i <= maxNumber; i++) {
                if (solveMode == 1 && inTargetNumbers.contains(i)) //就位的就不动了
                    continue;
                for (int j = 0; j < steps.length; j++) {
                    ArrayList<Integer> moved = tryUnit.boxMap.move(i, steps[j]);
                    if (moved != null) {
                        for (int n : moved) { //移动小地图
                            tryUnit.quickMap[n][0] += (int) steps[j].x;
                            tryUnit.quickMap[n][1] += (int) steps[j].y;
                            tryUnit.quickMap[n][2] += (int) steps[j].z;
                        }
                        tryUnit.sortQuickMap(boundedNumbers);
                        if (closedUnits.contains(tryUnit)) { //查找情况是否存在
                            tryUnit = currentUnit.clone();
                            continue;
                        }
                        SolverUnit saveUnit = tryUnit.clone(); //用于存储
                        tryUnit = currentUnit.clone();
                        saveUnit.stepNumber.add(i);
                        saveUnit.stepDirection.add(j);
                        if (solveMode == 0)
                            if (checkAtTarget(saveUnit.boxMap.boxes))
                                return saveUnit;
                        if (solveMode == 1) {
                            inTargetNumbers = checkInTarget(saveUnit.boxMap.boxes);
                            if (inTargetNumbers.size()>lastSolvedCount) //默认搜一步，不要的话改成(inTargetNumbers.size() == mode1SearchDepth)
                                return saveUnit;
                            if(inTargetNumbers.size()>lastSolvedCount){
                                lastSolvedCount=inTargetNumbers.size();
                                openList = new LinkedList<SolverUnit>();
                                closedUnits = new HashSet<SolverUnit>();
                            } else if (inTargetNumbers.size()<lastSolvedCount) {
                                closedUnits.add(saveUnit);
                                continue;
                            }
                        }
                        closedUnits.add(saveUnit);
                        openList.add(saveUnit);
                    }
                }
            }
        }
        return null;
    }

    public static boolean checkAtTarget(int[][][] boxMap1) {
        for (int i = 0; i < xLength; i++)
            for (int j = 0; j < yLength; j++)
                for (int k = 0; k < zLength; k++)
                    if (targetMap[i][j][k] > 0 && targetMap[i][j][k] != boxMap1[i][j][k])
                        return false;
        return true;
    }

    public static ArrayList<Integer> checkInTarget(int[][][] boxMap1) {
        ArrayList<Integer> inTargetNumbers = new ArrayList<>();
        for (int i = 0; i < xLength; i++)
            for (int j = 0; j < yLength; j++)
                for (int k = 0; k < zLength; k++)
                    if (targetMap[i][j][k] > 0 && boxMap1[i][j][k] > 0)
                        if (!inTargetNumbers.contains(boxMap1[i][j][k]))
                            inTargetNumbers.add(boxMap1[i][j][k]);
        return inTargetNumbers;
    }

    public static void sendStepsToBoxManager(SolverUnit solverUnit) {
        //一步可能移动多个方块，二者存储格式不同，最快转换格式的方法就是move再undo
        for (int i = 0; i < solverUnit.stepNumber.size(); i++)
            BoxManager.move(solverUnit.stepNumber.get(i), steps[solverUnit.stepDirection.get(i)]);
        for (int i = 0; i < solverUnit.stepNumber.size(); i++)
            BoxManager.undo();
    }
}

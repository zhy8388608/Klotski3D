package game;

import core.*;
import login.Intermediary;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class MainThread extends JFrame {

    //屏幕的分辨率
    public static int screen_w = 960;
    public static int screen_h = 540;
    public static int half_screen_w = screen_w / 2;
    public static int half_screen_h = screen_h / 2;
    public static int screenSize = screen_w * screen_h;

    //用Jpanel作为画板
    public static JPanel panel;

    //使用一个int数组存处屏幕上像素的数值
    public static int[] screen;

    //使用一个float数组来存储屏幕的深度缓冲值
    public static float[] zBuffer;

    //屏幕图像缓冲区。它提供了在内存中操作屏幕中图像的方法
    public static BufferedImage screenBuffer;

    //记载目前已渲染的 帧数
    public static int frameIndex;

    //希望达到的每频之间的间隔时间 (毫秒)
    public static int frameInterval = 16;

    //cpu睡眠时间，数字越小说明运算效率越高
    public static int sleepTime, averageSleepTime;

    //刷新率，及计算刷新率所用到一些辅助参数
    public static int framePerSecond;
    public static long lastDraw;
    public static double thisTime, lastTime;

    //总共渲染的三角形数
    public static int triangleCount;

    //渲染用到的贴图
    public static Texture[] textures;

    //视角的原点到屏幕的距离 （以像素为单位）， 这个值越大视角就越狭窄。常用的值为屏宽的2/3
    public static int screenDistance = 815;

    //选中的方块
    public static int noSelectedNumber = 0;
    public static int selectedNumber;

    //渲染图层
    int[] screen1 = new int[screenSize];
    float[] zBuffer1 = new float[screenSize];
    int[] screen2 = new int[screenSize];
    float[] zBuffer2 = new float[screenSize];
    int[] screen3 = new int[screenSize];
    float[] zBuffer3 = new float[screenSize];

    //离合器
    boolean shoulUpdateScreenSize = false;
    int newWidth, newHeight;

    //光影深度
    public static float[] shadowMap = Sunshine.shadowMap;

    //状态
    public static boolean isSolving = false;
    public static boolean isRestarting = false;
    public static boolean isExiting = false;
    public static boolean isWinning = false;
    public static boolean DEBUG = false;

    //计时
    TimeCounter timeCounter = new TimeCounter();

    //程序的入口点
    public static void main(String[] args) {
        DEBUG = true;
        new MainThread().startLevel(2, "", "", 0);
    }

    public void initScreen(int width, int height) {
        screen_w = width;
        screen_h = height;
        half_screen_w = screen_w / 2;
        half_screen_h = screen_h / 2;
        screenSize = screen_w * screen_h;
        screenBuffer = new BufferedImage(screen_w, screen_h, BufferedImage.TYPE_INT_RGB);
        DataBuffer dest = screenBuffer.getRaster().getDataBuffer();
        screen = ((DataBufferInt) dest).getData();
        zBuffer = new float[screenSize];
        screen1 = new int[screenSize];
        zBuffer1 = new float[screenSize];
        screen2 = new int[screenSize];
        zBuffer2 = new float[screenSize];
        screen3 = new int[screenSize];
        zBuffer3 = new float[screenSize];
        Rasterizer.setScreenSize(screen_w, screen_h);
    }

    public void initWindow() {
        //弹出一个宽 为screen_w高为screen_h的Jpanel窗口，并把它放置屏幕中间。
        setTitle("Klotski 3D");
        panel = (JPanel) this.getContentPane();
        panel.setPreferredSize(new Dimension(screen_w, screen_h));
        panel.setMinimumSize(new Dimension(screen_w, screen_h));
        panel.setLayout(null);

        setResizable(true);
        pack();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); //EXIT_ON_CLOSE

        // 添加组件适配器监听窗口尺寸变化
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                shoulUpdateScreenSize = true;
                newWidth = getWidth();
                newHeight = getHeight();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                isExiting = true;
                try {
                    while (isExiting) //等待主线程结束
                        Thread.sleep(10);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });

        setVisible(true);
    }

    public void initCore() {
        //用TYPE_INT_RGB来创建BufferedImage，然后把屏幕的像素数组指向BufferedImage中的DataBuffer。
        //这样通过改变屏幕的像素数组(screen[])中的数据就可以在屏幕中渲染出图像
        screenBuffer = new BufferedImage(screen_w, screen_h, BufferedImage.TYPE_INT_RGB);
        DataBuffer dest = screenBuffer.getRaster().getDataBuffer();
        screen = ((DataBufferInt) dest).getData();

        zBuffer = new float[screenSize];

        //初始化查找表
        LookupTables.init();

        //初始化光栅渲染器
        newWidth = 960;
        newHeight = 540;
        shoulUpdateScreenSize=true;

        //初始化视角
        Camera.init(0, 0, 0);

        //添加按键监听器
        addMouseListener(new Mouse());
        addMouseMotionListener(new Mouse());
        addMouseWheelListener(new Mouse());
        addKeyListener(new Keyboard());

        //开始一个守护线程，该线程将永远处于睡眠状态，用来稳定刷新率
        Thread dt = new Thread(new DaemonThread());
        dt.setDaemon(true);
        dt.start();

    }

    public void startGame(int[][][] boxMap, int[][][] targetMap, String historyStepNumbers, String historyStepDirections, long historyTime) {
        //加载地图
        BoxManager.init(boxMap);
        BoxManager.setTargetBoxes(targetMap);
        loadSteps(historyStepNumbers, historyStepDirections);

        //读取纹理贴图
        textures = new Texture[21];
        textures[7] = new Texture("selector.png");
        if (theme == 0) {
            textures[2] = new Texture("mc/steve.png");
            textures[3] = new Texture("mc/ground.png");
            textures[5] = new Texture("mc/sky.png");
            textures[9] = new Texture("mc/box-1.png");
            for (int i = 1; i <= 10; i++)
                textures[i + 10] = new Texture("mc/box" + i + ".png");
        } else if (theme == 1) {
            textures[5] = new Texture("sp/sky.jpg");
            textures[3] = new Texture("sp/capsule0.jpg");
            textures[2] = new Texture("sp/bunny.jpg");
            textures[9] = new Texture("sp/box-1.png");
            for (int i = 1; i <= 10; i++)
                textures[i + 10] = new Texture("sp/box" + i + ".png");
        } else if (theme == 2) {
            textures[5] = new Texture("hrd/sky1.jpg");
            textures[3] = new Texture("hrd/transparent.png");
            textures[2] = new Texture("hrd/caocao.png");
            textures[9] = new Texture("hrd/box-1.png");
            for (int i = 1; i <= 10; i++)
                textures[i + 10] = new Texture("hrd/box" + i + ".png");
        }

        //定义光源
        Light lightSource1 = new Light(-100f, 100f, -100f, 1f);
        float kd = 0.15f, ks = 0.2f;

        //由obj文件构建兔子模型
        Mesh modelMesh = null;
        if (theme == 0)
            modelMesh = new Mesh("steve.obj", "clockwise");
        else if (theme == 1)
            modelMesh = new Mesh("null.obj", "clockwise"); //bunny.obj比较费资源
        else if (theme == 2)
            modelMesh = new Mesh("caocao.obj", "clockwise");
        VertexBufferObject model;
        model = new VertexBufferObject(modelMesh, lightSource1, kd, ks);
        model.textureIndex = 2;
        model.renderType = VertexBufferObject.barycentric_textured;
        if (theme == 0)
            model.scale = 0.07f;
        else if (theme == 1)
            model.scale = 15f;
        else if (theme == 2)
            model.scale = 0.01f;

        //由obj文件构建天空盒
        Mesh skyMesh = new Mesh("sky.obj", "clockwise");
        VertexBufferObject sky = new VertexBufferObject(skyMesh);
        sky.textureIndex = 5;
        sky.renderType = VertexBufferObject.barycentric_textured;
        sky.scale = 1f;
        sky.hasShadow = false;
        if (theme == 2)
            sky.localRotationY = 180;

        //由obj文件构建selector
        Mesh boxMesh = new Mesh("box.obj", "clockwise");
        VertexBufferObject selector = new VertexBufferObject(boxMesh);
        selector.textureIndex = 7;
        selector.renderType = VertexBufferObject.barycentric_textured;
        selector.scale = 0.51f; //略大，避免穿模
        selector.localTranslation = new Vector3D(0, 0, 0);
        selector.hasShadow = false;

        //构建地面贴图的模型
        Vector3D[] groundVertices = new Vector3D[]{
                new Vector3D(-3.5f, -0.5f, BoxManager.boxMap.zLength + 2.5f),
                new Vector3D(-3.5f, -0.5f, -3.5f),
                new Vector3D(BoxManager.boxMap.xLength + 2.5f, -0.5f, -3.5f),
                new Vector3D(BoxManager.boxMap.xLength + 2.5f, -0.5f, BoxManager.boxMap.zLength + 2.5f)
        };
        Vector3D[] groundNormals = new Vector3D[]{new Vector3D(0, 1, 0), new Vector3D(0, 1, 0), new Vector3D(0, 1, 0), new Vector3D(0, 1, 0)};
        Vector3D[][] groundUVDirections = new Vector3D[2][];
        groundUVDirections[0] = new Vector3D[]{new Vector3D(1f, 0, 0), new Vector3D(0, 0f, -1)};
        groundUVDirections[1] = new Vector3D[]{new Vector3D(1f, 0, 0), new Vector3D(0, 0f, -1)};
        int[] groundIndices = new int[]{0, 2, 1, 0, 3, 2};
        VertexBufferObject ground = new VertexBufferObject(groundVertices, groundUVDirections, groundNormals, groundIndices);
        ground.textureIndex = 3;
        ground.renderType = VertexBufferObject.textured;
        ground.hasShadow = false;
        if (theme == 0)
            ground.textureScale = new float[][]{new float[]{0.5f, 0.5f}, new float[]{0.5f, 0.5f}};
        else if (theme == 1)
            ground.textureScale = new float[][]{new float[]{0.069f, 0.069f}, new float[]{0.069f, 0.069f}};
        else if (theme == 2)
            ground.textureScale = new float[][]{new float[]{1f, 1f}, new float[]{1f, 1f}};


        //移动相机
        float rotatingRadiusRatio = 1.2f;
        Camera.rotatingRadius = (int) (new Vector3D(BoxManager.boxMap.xLength, BoxManager.boxMap.yLength, BoxManager.boxMap.zLength).getLength() * 1.2f);
        Camera.rotatingCenter = new Vector3D(0.5f * (BoxManager.boxMap.xLength - 1), 0.5f * (BoxManager.boxMap.yLength - 1) + 0.5f, 0.5f * (BoxManager.boxMap.zLength - 1));
        Camera.Y_angle_temp = 0;
        Camera.X_angle_temp = 271;

        //修改阴影绘制范围
        Sunshine.setRange(-3, BoxManager.boxMap.zLength + 3, BoxManager.boxMap.xLength + 3, -3);

        //清空选中的方块
        selectedNumber = noSelectedNumber;

        //用于标记左键随意拖动
        boolean shouldRestoreSelectedNumber = false;

        //X-Ray模式隔板位置记录
        int cutX1 = 0, cutX2 = 0, cutY1 = 0, cutY2 = 0, cutZ1 = 0, cutZ2 = 0;

        //开始计时
        timeCounter.start(historyTime);

        //主循环
        while (true) {
            //更新窗口尺寸
            if (shoulUpdateScreenSize) {
                Rasterizer.interruptShaders();
                Rasterizer.waitForShaders();
                System.out.println(getWidth() + " " + getHeight());
                Rasterizer.newShaders();
                initScreen(getWidth(), getHeight());
                Rasterizer.startShaders();
                shoulUpdateScreenSize = false;
            }
            //重开
            if (isRestarting) {
                while (BoxManager.totalSteps > 0)
                    BoxManager.undo();
                BoxManager.stepNumber.clear();
                BoxManager.stepDirection.clear();
                timeCounter.start();
                isRestarting = false;
            }
            //自动求解
            if (isSolving) {
                solve();
                isSolving = false;
            }
            //关掉
            if (isExiting) {
                isExiting = false;
                return;
            }

            //三角形数归零
            triangleCount = 0;

            //更新视角
            Camera.update();

            Rasterizer.prepare();

            //######鼠标实现######

            //选择框实现
            //为加速计算，使用上一帧数据
            Vector3D selectedLocation = new Vector3D(Selector.getSelectedLocation(Mouse.X, Mouse.Y - 30, zBuffer1)); //Windows标题栏宽30像素
            Selector.round(selectedLocation);
            if (Mouse.isMouseClicked) {
                Mouse.isMouseClicked = false;
                selectedNumber = Selector.getSelectedNumber(selectedLocation);
            }

            //左键拖动实现
            Vector3D moveDirection = new Vector3D(Mouse.leftXDrag, -Mouse.leftYDrag, 0);
            if (moveDirection.getLength() > 70) { //最小移动距离
                Mouse.leftXDrag = Mouse.leftYDrag = 0;
                if (selectedNumber <= 0) {
                    selectedLocation.set(Selector.getSelectedLocation(Mouse.pressX, Mouse.pressY - 30, zBuffer1)); //Windows标题栏宽30像素
                    Selector.round(selectedLocation);
                    selectedNumber = Selector.getSelectedNumber(selectedLocation);
                    shouldRestoreSelectedNumber = true;
                } //不能用else，因为selectedNumber发生了变化
                if (selectedNumber > 0)
                    BoxManager.moveByScreenDirection(selectedNumber, moveDirection);
            }
            if (shouldRestoreSelectedNumber && !Mouse.isLeftButtonPressed) {
                shouldRestoreSelectedNumber = false;
                selectedNumber = MainThread.noSelectedNumber;
            }

            //######图层初始化######

            //清屏
            Rasterizer.clearScreen(screen1, zBuffer1, 0);
            Rasterizer.clearScreen(screen2, zBuffer2, 0);
            Rasterizer.clearScreen(screen3, zBuffer3, 0);
            shadowMap[0] = -1;
            for (int i = 1; i < shadowMap.length; i += i)
                System.arraycopy(shadowMap, 0, shadowMap, i, Math.min(shadowMap.length - i, i));

            //分配图层 1是可以被selector选中图层，2是含透明度图层，3是不能被selector选中的图层
            sky.setScreen(screen1, zBuffer1);
            sky.hasZBuffer = false;
            ground.setScreen(screen3, zBuffer3);
            model.setScreen(screen3, zBuffer3);
            selector.setScreen(screen3, zBuffer3);

            //######模型渲染######

            //渲染天空盒
            sky.localTranslation.set(Camera.position);
            //优化 Rasterizer.addVBO(sky);
            for (int i = 0; i < sky.triangleCount; i++) {
                VertexBufferObject skyTriangle = (VertexBufferObject) sky.clone();
                skyTriangle.triangleCount = 1;
                skyTriangle.indexBuffer = new int[3];
                skyTriangle.uvCoordinates = new float[3][];
                for (int j = 0; j < 3; j++) {
                    skyTriangle.indexBuffer[j] = sky.indexBuffer[i * 3 + j];
                    skyTriangle.uvCoordinates[j] = sky.uvCoordinates[i * 3 + j];
                }
                Rasterizer.addVBO(skyTriangle);
            }

            //渲染地面
            //优化 Rasterizer.addVBO(ground);
            for (int i = 0; i < 2; i++) {
                VertexBufferObject halfGround = (VertexBufferObject) ground.clone();
                halfGround.triangleCount = 1;
                halfGround.indexBuffer = new int[3];
                halfGround.UVDirections = new Vector3D[1][];
                for (int j = 0; j < 3; j++)
                    halfGround.indexBuffer[j] = ground.indexBuffer[i * 3 + j];
                halfGround.UVDirections[0] = ground.UVDirections[i];
                Rasterizer.addVBO(halfGround);
            }

            //渲染兔子
            model.localRotationY = (frameIndex >> 2) % 360;
            model.localTranslation.set(0f, 0.5f, 7f);
            if (level == 0)
                model.localTranslation.set(new Vector3D(0.5f, 0.5f, 0.5f).add(BoxManager.boxes[17].localTranslation));
            Rasterizer.addVBO(model);

            //动画移动
            BoxManager.animate(0.2f);

            //渲染选择框
            for (int i = 0; i < BoxManager.totalBoxes; i++)
                if (BoxManager.boxes[i].number == selectedNumber) {
                    Vector3D p = BoxManager.boxes[i].localTranslation;
                    VertexBufferObject selector1 = (VertexBufferObject) selector.clone();
                    selector1.localTranslation.set(p.x, p.y, p.z);
                    Rasterizer.addVBO(selector1);
                }
            selector.localTranslation.set(selectedLocation);
            Rasterizer.addVBO(selector);

            //######不同透视模式实现######

            //如果专注模式开就只渲染选中的，X光模式开就渲染分层的，否则全部渲染
            if (BoxManager.isFoucusModeOn) {
                //滚轮实现，切换选中的方块
                if (Math.abs(Mouse.scroll) > 1) {
                    if (selectedNumber == noSelectedNumber)
                        selectedNumber = 0;
                    selectedNumber = Algorithms.clampCircular(selectedNumber + (Mouse.scroll > 0 ? 1 : -1), -1, BoxManager.maxNumber);
                    if (selectedNumber == 0)
                        selectedNumber = noSelectedNumber;
                    Mouse.scroll = 0;
                }
                //有选中的就强调选中的，没有选中的就强调空位
                if (selectedNumber != noSelectedNumber) {
                    BoxManager.distributeLayerByNumber(selectedNumber, screen1, zBuffer1, screen2, zBuffer2);
                } else
                    BoxManager.distributeLayerByVoid(screen1, zBuffer1, screen2, zBuffer2);
            } else if (BoxManager.isXrayModeOn) {
                //求当前视角最接近的坐标轴
                Vector3D viewAxis = new Vector3D(Camera.viewDirection);
                Algorithms.roundToAxisDirection(viewAxis);

                //滚轮实现，分别存储各个方向的透视深度。
                if (Math.abs(Mouse.scroll) > 1) {
                    int step = Mouse.scroll > 0 ? 1 : -1;
                    if (viewAxis.equals(1, 0, 0))
                        cutX1 = Algorithms.clampCircular(cutX1 + step, 0, BoxManager.boxMap.xLength);
                    else if (viewAxis.equals(-1, 0, 0))
                        cutX2 = Algorithms.clampCircular(cutX2 + step, 0, BoxManager.boxMap.xLength);
                    else if (viewAxis.equals(0, 1, 0))
                        cutY1 = Algorithms.clampCircular(cutY1 + step, 0, BoxManager.boxMap.yLength);
                    else if (viewAxis.equals(0, -1, 0))
                        cutY2 = Algorithms.clampCircular(cutY2 + step, 0, BoxManager.boxMap.yLength);
                    else if (viewAxis.equals(0, 0, 1))
                        cutZ1 = Algorithms.clampCircular(cutZ1 + step, 0, BoxManager.boxMap.zLength);
                    else if (viewAxis.equals(0, 0, -1))
                        cutZ2 = Algorithms.clampCircular(cutZ2 + step, 0, BoxManager.boxMap.zLength);
                    Mouse.scroll = 0;
                }

                //分层渲染
                Vector3D cut = new Vector3D(0, 0, 0);
                if (viewAxis.equals(1, 0, 0)) cut.x = cutX1;
                else if (viewAxis.equals(-1, 0, 0)) cut.x = -cutX2;
                else if (viewAxis.equals(0, 1, 0)) cut.y = cutY1;
                else if (viewAxis.equals(0, -1, 0)) cut.y = -cutY2;
                else if (viewAxis.equals(0, 0, 1)) cut.z = cutZ1;
                else if (viewAxis.equals(0, 0, -1)) cut.z = -cutZ2;

                BoxManager.distributeLayerByCut(cut, screen1, zBuffer1, screen2, zBuffer2);
            } else {
                if (BoxManager.isShowingTarget)
                    BoxManager.setShowTargetScreen(screen1, zBuffer1, screen2, zBuffer2);
                else
                    BoxManager.showAll(screen1, zBuffer1);
                //滚轮缩放实现
                if (Math.abs(Mouse.scroll) > 1) {
                    float step = Mouse.scroll > 0 ? 0.1f : -0.1f;
                    rotatingRadiusRatio = Algorithms.clamp(rotatingRadiusRatio + step, 0.1f, 2f);
                    Camera.rotatingRadius = (int) (new Vector3D(BoxManager.boxMap.xLength, BoxManager.boxMap.yLength, BoxManager.boxMap.zLength).getLength() * rotatingRadiusRatio);
                    Mouse.scroll = 0;
                }
            }

            //渲染盒子
            for (int i = 0; i < BoxManager.totalBoxes; i++)
                if (!BoxManager.boxes[i].isHide)
                    Rasterizer.addVBO(BoxManager.boxes[i]);
            if (BoxManager.isShowingTarget)
                for (int i = 0; i < BoxManager.targetBoxesCount; i++)
                    Rasterizer.addVBO(BoxManager.targetBoxes[i]);
            BoxManager.setShadow(-1, false);

            //######混合图层######

            Rasterizer.renderScene();
            System.arraycopy(screen1, 0, screen, 0, screenSize);
            System.arraycopy(zBuffer1, 0, zBuffer, 0, screenSize);
            Rasterizer.mixScreen(screen, zBuffer, screen3, zBuffer3);

            //3x3像素为单位计算阴影，进行加速
            int pixel = 3;
            for (int i = pixel / 2; i < screen_h; i += pixel)
                for (int j = pixel / 2; j < screen_w; j += pixel) {
                    Vector3D location = Selector.getSelectedLocation(j, i, zBuffer);
                    if (Float.isInfinite(location.z)) //背景无阴影
                        continue;
                    if (Sunshine.getShadowHeight(location) > location.y + 0.2f) { //让光射进面内，便于区分
                        for (int a = 0; a < Math.min(pixel, screen_h - i); a++)
                            for (int b = 0; b < Math.min(pixel, screen_w - j); b++) {
                                int p = (i + a - pixel / 2) * screen_w + j + b - pixel / 2;
                                int color = screen[p];
                                screen[p] = Rasterizer.mixCover(color, 0x50000000);
                            }
                    }
                }
            Rasterizer.mixScreenWithAlpha(screen, zBuffer, screen2, zBuffer2, 70);

            /*//test shadow
            for (int i = 0; i < Sunshine.height; i++) {
                for (int j = 0; j < Sunshine.width; j++) {
                    int c = ((int) (255 * (shadowMap[i * Sunshine.width + j] + 0.5) / 1) & 0xff) << 16;
                    for (int a = 0; a < 1; a++)
                        for (int b = 0; b < 1; b++)
                            screen[(i * 1 + a) * screen_w + j * 1 + b] = c;
                }
            }*/

            //######获胜######

            //计算获胜
            if (frameIndex % 10 == 0)
                if (BoxManager.checkWin()) {
                    System.out.println("Win");
                    isWinning = true;
                    return;
                }

            //######帧率记录######

            //loop每运行一边，帧数就+1
            frameIndex++;

            //尽量让刷新率保持恒定。
            int mySleepTime = 0;
            int processTime = (int) (System.currentTimeMillis() - lastDraw);
            if (processTime < frameInterval && lastDraw != 0) {
                mySleepTime = frameInterval - processTime;
                try {
                    Thread.sleep(mySleepTime);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }

            sleepTime += mySleepTime;
            lastDraw = System.currentTimeMillis();
            //计算当前的刷新率
            if (frameIndex % 30 == 0) {
                double thisTime = System.currentTimeMillis();
                framePerSecond = (int) (1000 / ((thisTime - lastTime) / 30));
                lastTime = thisTime;
                averageSleepTime = sleepTime / 30;
                sleepTime = 0;
            }

            //显示当前刷新率
            Graphics2D g2 = (Graphics2D) screenBuffer.getGraphics();
            g2.setColor(Color.GREEN);
            g2.setFont(new Font("幼圆", Font.PLAIN, 16));
            g2.drawString("用时：" + timeCounter.getFormatedTime(), 5, 35);
            g2.setFont(new Font("幼圆", Font.BOLD, 26));
            g2.drawString("步数: " + BoxManager.totalSteps, 5, 65);
            //test
            g2.setFont(new Font("幼圆", Font.PLAIN, 14));
            g2.drawString("帧率：" + framePerSecond + "   " + "三角形总数：" + triangleCount, 5, 15);

            //把图像发画到显存里，这是唯一要用到显卡的地方
            panel.getGraphics().drawImage(screenBuffer, 0, 0, this);

        }
    }

    //myMap：长宽高+每层数据（按行输入）
    public int[][][] transMap(String myMap) {
        Scanner sc = new Scanner(myMap);
        int z = sc.nextInt(), x = sc.nextInt(), y = sc.nextInt();
        int zAir = sc.nextInt(), xAir = sc.nextInt(), yAir = sc.nextInt();
        int[][][] boxMap = new int[x + xAir * 2][y + yAir * 2][z + zAir * 2];
        for (int i = 0; i < y; i++)
            for (int j = 0; j < x; j++)
                for (int k = 0; k < z; k++)
                    boxMap[j + xAir][i + yAir][k + zAir] = sc.nextInt();
        return boxMap;
    }

    public String printMap(int[][][] myMap) {
        String res = "";
        int x = myMap.length, y = myMap[0].length, z = myMap[0][0].length;
        res += z + " " + x + " " + y + " 0 0 0 ";
        for (int i = 0; i < y; i++)
            for (int j = 0; j < x; j++)
                for (int k = 0; k < z; k++)
                    res += myMap[j][i][k] + " ";
        return res;
    }

    static Vector3D[] steps = new Vector3D[]{
            new Vector3D(-1, 0, 0), new Vector3D(1, 0, 0),
            new Vector3D(0, -1, 0), new Vector3D(0, 1, 0),
            new Vector3D(0, 0, -1), new Vector3D(0, 0, 1)
    };

    public void loadSteps(String historyStepNumbers, String historyStepDirections) {
        ArrayList<ArrayList<Integer>> numbers = new ArrayList<ArrayList<Integer>>();
        ArrayList<Vector3D> directions = new ArrayList<Vector3D>();

        Scanner nSc = new Scanner(historyStepNumbers);
        Scanner dSc = new Scanner(historyStepDirections);
        while (dSc.hasNext()) {
            directions.add(steps[dSc.nextInt()]);
            ArrayList<Integer> number = new ArrayList<Integer>();
            int n = 0;
            while ((n = nSc.nextInt()) != 0)
                number.add(n);
            numbers.add(number);
        }

        while (BoxManager.totalSteps > 0)
            BoxManager.undo();
        for (int i = 0; i < numbers.size(); i++)
            BoxManager.move(numbers.get(i), directions.get(i));
    }

    public static String printStepNumbers() {
        String res = "";
        for (ArrayList<Integer> stepNumber : BoxManager.stepNumber) {
            //stepNumber.get(0)就是最早挪动的数字
            for (int n : stepNumber)
                res += n + " ";
            res += "0 "; //表示间隔
        }
        return res;
    }

    public static String printStepDirections() {
        String res = "";
        for (Vector3D stepDirection : BoxManager.stepDirection)
            for (int i = 0; i < steps.length; i++)
                if (steps[i].equals(stepDirection)) {
                    res += i + " ";
                    break;
                }
        return res;
    }

    public void solve() {
        if (level == 0) { //华容道
            String s = "5 2 1 1 2 0 2 2 0 0 0 2 2 0 0 0";
            int[][][] targetMap = transMap(s);

            ArrayList<ArrayList<Integer>> boundedNumbers = new ArrayList<ArrayList<Integer>>();
            boundedNumbers.add(new ArrayList<Integer>(Arrays.asList(7, 8, 9, 10)));
            boundedNumbers.add(new ArrayList<Integer>(Arrays.asList(1, 3, 4, 5)));
            SolverUnit solution = Solver.solve(BoxManager.boxMap.boxes, targetMap, 10, 0, boundedNumbers);
            if (solution == null)
                return;
            System.out.println(solution.stepNumber);
            System.out.println(solution.stepDirection);
            Solver.sendStepsToBoxManager(solution);
            BoxManager.redo(); //方便看出来计算结束
            return;
        } else if (level == 1) { //小盒子
            int a = 15, b = 6, c = 15;
            int[][][] wallMap = new int[a][b][c];
            for (int i = 0; i < a; i++)
                for (int j = 0; j < b; j++)
                    for (int k = 0; k < c; k++)
                        wallMap[i][j][k] = 10;
            for (int i = 1; i < a - 1; i++)
                for (int j = 0; j < b - 1; j++)
                    for (int k = 1; k < c - 1; k++)
                        wallMap[i][j][k] = 0;

            SolverUnit solution = Solver.solve(BoxManager.boxMap.boxes, wallMap, 4, 1, null);
            if (solution == null)
                return;
            System.out.println(solution.stepNumber);
            System.out.println(solution.stepDirection);
            Solver.sendStepsToBoxManager(solution);
            BoxManager.redo(); //方便看出来计算结束
            return;
        } else if (level == 2) { //计算时间太长了，直接放结果
            String numbers = "1 0 5 0 3 5 0 3 5 8 4 0 4 0 6 2 0 2 0 6 0 3 0 3 0 4 8 5 0 2 0 2 5 8 4 0 1 0 1 6 0 2 4 8 5 0 2 0 3 0 4 0 4 0 1 0 1 0 6 0 7 0 7 0 1 0 5 8 0 3 0 6 0 7 0 2 0 3 4 8 5 0 3 0 3 8 5 0 3 5 8 4 0 2 0 6 0 7 0 3 0 3 0 3 0 3 0 8 5 0 1 0 1 0 7 0 7 0 6 0 1 0 5 8 0 3 0 6 1 2 0 8 0 3 0 6 0 6 0 6 0 3 0 3 0 4 8 0 2 0 3 0 8 4 2 0 2 4 5 0 7 0 7 0 7 0 1 6 3 0 2 4 8 5 0 3 0 2 0 2 0 5 8 4 0 3 0 3 5 4 0 3 0 2 0 1 0 2 0 2 0 2 0 3 0 3 0 3 0 4 0 8 0 1 0 1 0 1 6 3 2 0 1 0 4 0 8 0 3 0 3 0 6 0 6 0 2 0 2 0 6 0 6 0 3 0 3 0 4 0 8 0 1 0 1 0 1 0 7 0 7 0 7 0 7 0 \n" +
                    "1 0 2 0 4 0 8 0 3 0 3 0 3 0 6 0 8 0 3 0 3 4 8 5 0 3 0 3 5 8 0 3 5 8 4 0 9 2 0 9 4 0 2 0 2 0 9 0 9 0 1 6 2 0 1 6 2 0 2 0 2 0 2 0 \n" +
                    "6 0 6 0 6 0 6 0 6 0 6 0 6 0 \n" +
                    "3 2 0 3 2 0 9 8 4 0 1 0 1 0 1 0 1 0 1 0 \n" +
                    "3 0 3 0 3 1 0 3 1 0 3 0 3 0 3 0 3 0 3 0 3 0 \n" +
                    "4 5 0 4 9 0 4 5 8 9 0 9 0 9 5 0 8 0 8 0 8 0 8 0 \n" +
                    "4 6 9 0 5 0 5 8 9 0 9 0 5 0 5 0 5 0 5 0 \n" +
                    "9 0 9 0 9 0 9 0 9 0 9 0 9 0 9 0 9 0 9 0 \n" +
                    "4 0 4 0 4 0 4 0 4 0 4 0 4 0 4 0 \n";
            String directions = "4 1 5 0 5 1 2 1 3 3 1 2 0 5 5 1 3 2 0 4 2 5 4 0 0 3 0 2 0 0 3 1 4 4 0 2 1 1 3 4 4 3 1 2 4 1 1 5 3 0 2 4 5 2 0 0 0 3 3 1 2 1 0 5 1 1 1 5 1 0 3 3 0 2 4 2 0 4 5 1 2 3 5 3 0 1 2 4 4 3 1 0 2 2 1 1 5 5 0 0 3 3 0 1 2 5 5 1 1 1 1\n" +
                    "3 3 1 0 2 5 5 1 4 2 1 5 5 0 2 5 3 0 2 2 5 5 5 5 5 \n" +
                    "0 0 0 0 0 0 0 \n" +
                    "3 3 0 5 5 5 5 5 \n" +
                    "1 1 1 1 5 5 5 5 5 5 \n" +
                    "0 3 0 4 4 0 0 0 0 \n" +
                    "3 0 5 5 0 0 0 0 \n" +
                    "1 1 4 3 3 3 3 3 3 3 \n" +
                    "1 1 5 5 5 5 5 5 \n";
            loadSteps(numbers, directions);
            while (BoxManager.totalSteps > 0)
                BoxManager.undo();
            System.out.println(printStepNumbers());
            System.out.println(printStepDirections());
            BoxManager.redo(); //方便看出来计算结束
            return;
        }
    }

    int level; //012 hrd/mc/sp
    int theme; //012 mc/sp/hrd

    public void startLevel(int level1, String historyStepNumbers, String historyStepDirections, long historyTime) {
        level = level1;
        if (level == 0 || level == 3)
            theme = 2;
        else if (level == 1)
            theme = 0;
        else if (level == 2)
            theme = 1;

        initWindow();
        initCore();

        String[] level1Map = new String[]{
                "7 6 1 0 0 0 -1 -1 -1 -1 -1 -1 -1 -1 7 4 4 1 1 -1 -1 0 8 6 2 2 -1 -1 0 9 6 2 2 -1 -1 10 5 5 3 3 -1 -1 -1 -1 -1 -1 -1 -1",
                "7 6 1 0 0 0 -1 -1 -1 -1 -1 -1 -1 -1 4 4 7 1 1 -1 -1 0 8 6 2 2 -1 -1 0 9 6 2 2 -1 -1 5 5 10 3 3 -1 -1 -1 -1 -1 -1 -1 -1",
                "7 6 1 0 0 0 -1 -1 -1 -1 -1 -1 -1 -1 4 4 7 1 1 -1 -1 0 6 8 2 2 -1 -1 0 6 9 2 2 -1 -1 5 5 10 3 3 -1 -1 -1 -1 -1 -1 -1 -1",
                "7 6 1 0 0 0 -1 -1 -1 -1 -1 -1 -1 -1 4 4 1 1 7 -1 -1 0 8 6 2 2 -1 -1 0 9 6 2 2 -1 -1 5 5 3 3 10 -1 -1 -1 -1 -1 -1 -1 -1",
                "7 6 1 0 0 0 -1 -1 -1 -1 -1 -1 -1 -1 6 8 1 1 0 -1 -1 6 4 4 2 2 -1 -1 9 5 5 2 2 -1 -1 10 7 3 3 0 -1 -1 -1 -1 -1 -1 -1 -1"
        };
        String[] levelBoxMap = new String[3];
        if (level == 0) //华容道
            levelBoxMap[0] = level1Map[0];
        else if (level == 3) { //随机华容道
            level = 0;
            historyStepNumbers = historyStepDirections = "";
            historyTime = 0;
            levelBoxMap[0] = level1Map[(int) (Math.random() * level1Map.length)];
        }
        levelBoxMap[1] = "5 5 6 5 5 0 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 1 1 1 -1 2 0 1 0 2 2 0 1 0 2 2 0 1 0 2 -1 1 1 1 -1 -1 1 0 4 -1 2 2 2 2 2 0 0 0 0 0 3 0 0 0 3 -1 1 0 4 -1 -1 1 0 4 -1 2 2 0 2 2 0 0 0 0 0 3 3 3 3 3 -1 1 0 4 -1 -1 4 4 4 -1 3 4 4 0 3 3 0 4 0 3 3 4 4 0 3 -1 4 4 4 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1";
        levelBoxMap[2] = "7 7 7 8 8 6 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 9 9 9 -1 -1 -1 -1 9 9 9 -1 -1 -1 -1 9 9 9 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 5 5 8 8 8 -1 3 5 5 0 8 8 3 3 5 5 0 8 8 3 4 5 0 0 8 8 4 4 5 0 9 0 8 4 4 5 0 0 0 8 4 -1 5 5 8 8 8 -1 -1 5 5 8 8 8 -1 3 0 0 0 0 0 3 3 0 0 3 3 3 3 4 4 4 4 4 0 4 4 4 0 9 4 0 4 4 0 0 0 4 4 4 -1 5 5 8 8 8 -1 -1 5 5 0 7 7 -1 3 0 0 0 0 0 3 3 3 3 3 0 3 3 0 0 0 9 0 0 0 1 0 0 9 0 0 1 1 1 1 1 1 1 1 -1 5 5 0 7 7 -1 -1 6 6 6 7 7 -1 2 2 2 2 2 2 2 2 0 0 0 0 0 2 2 0 0 9 0 0 2 1 6 0 0 7 0 1 1 6 0 6 0 0 1 -1 6 6 6 7 7 -1 -1 6 6 6 7 7 -1 2 6 0 0 0 7 2 2 6 0 0 0 7 2 2 6 0 9 7 7 2 1 6 0 0 7 0 1 1 6 0 0 7 0 1 -1 6 6 6 7 7 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 9 9 9 -1 -1 -1 -1 9 9 9 -1 -1 -1 -1 9 9 9 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1";
        int[][][] boxMap = transMap(levelBoxMap[level]);
        String[] levelTargetMap = new String[3];
        levelTargetMap[0] = "7 6 1 0 0 0 -1 -1 -1 -1 -1 -1 -1 -1 0 0 0 0 0 -1 -1 2 2 0 0 0 -1 -1 2 2 0 0 0 -1 -1 0 0 0 0 0 -1 -1 -1 -1 -1 -1 -1 -1";
        levelTargetMap[1] = "9 1 1 0 0 2 0 0 0 0 0 0 0 0 4";
        levelTargetMap[2] = "1 1 19 10 10 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 9";
        int[][][] targetMap = transMap(levelTargetMap[level]);


        String[] musicList = new String[]{
                "dead_feelings.mod",
                "jesper_k_.mod",
                "toilet4.xm",
                "unreeeal_superhero_3.xm",
                "external.xm"
        };

        MusicPlayer.play(musicList[(int) (Math.random() * musicList.length)]);

        startGame(boxMap, targetMap, historyStepNumbers, historyStepDirections, historyTime);
        onExit();
    }

    public void onExit() {
        //关音乐
        MusicPlayer.stop();
        if (DEBUG) //不存档
            System.exit(0);
        //存档
        while (BoxManager.stepNumber.size() > BoxManager.totalSteps) { //删除未重做步骤
            BoxManager.stepNumber.remove(BoxManager.stepNumber.size() - 1);
            BoxManager.stepDirection.remove(BoxManager.stepDirection.size() - 1);
        }
        if (!isWinning) {
            Intermediary.save(printStepNumbers(), printStepDirections(), timeCounter.getTime());
        } else {
            isWinning = false;
            //留一手存档
            BoxManager.stepNumber.remove(BoxManager.stepNumber.size() - 1);
            BoxManager.stepDirection.remove(BoxManager.stepDirection.size() - 1);
            Intermediary.save(printStepNumbers(), printStepDirections(), timeCounter.getTime());
            Intermediary.win(BoxManager.totalSteps, timeCounter.getTime());
        }
        //退出
        Rasterizer.interruptShaders();
        dispose();
        System.out.println("Exit");
    }
}

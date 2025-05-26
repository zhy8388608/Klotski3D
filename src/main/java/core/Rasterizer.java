package core;

import game.MainThread;

//只用于三角形的渲染
public class Rasterizer {

    //顶点缓冲对象数组，设定最多可以处理1000个顶点缓冲对象
    public static VertexBufferObject[] VBOs = new VertexBufferObject[1000];

    //已绘制的VBO数
    public static int VBODrawCount = 0;

    //记载每一频加载的VBO个数
    public static int numberOfVBOs;

    //用来处理着色的线程
    public static Shader[] shaders = new Shader[7];

    static {
        newShaders();
    }

    public static void newShaders() {
        for (int i = 0; i < shaders.length; i++)
            shaders[i] = new Shader("shader" + i);
    }

    //初始化光栅渲染器
    public static void startShaders() {
        //初始化着色器线程并让它们运行起来
        for (int i = 0; i < shaders.length; i++)
            shaders[i].start();
    }

    public static void setScreenSize(int screen_w, int screen_h) {
        Object[] zBufferLock = new Object[screen_h];
        for (int i = 0; i < zBufferLock.length; i++)
            zBufferLock[i] = new Object();
        for (int i = 0; i < shaders.length; i++) {
            shaders[i].setScreenSize(screen_w, screen_h);
            shaders[i].setZBufferLock(zBufferLock);
        }
    }

    public static void interruptShaders() {
        for (int i = 0; i < shaders.length; i++)
            shaders[i].interrupt();
    }

    public static void waitForShaders() {
        //等着色器线程完成工作
        for (int i = 0; i < shaders.length; i++) {
            synchronized (shaders[i].workLock) {
                if (shaders[i].isWorking) {
                    try {
                        shaders[i].workLock.wait();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }

    }


    //在每一频的开始，把渲染器上一频的信息清除
    public static void prepare() {
        numberOfVBOs = 0;
        VBODrawCount = 0;
    }

    //加载一个VBO
    public static void addVBO(VertexBufferObject VBO) {
        VBOs[numberOfVBOs] = VBO;
        numberOfVBOs++;
    }

    //获取一个VBO
    public static synchronized VertexBufferObject getVBO() {
        if (VBODrawCount < numberOfVBOs)
            return VBOs[VBODrawCount++];
        return null;
    }

    //渲染器的入口
    public static void renderScene() {
        //让着色器开始工作
        for (int i = 0; i < shaders.length; i++) {
            synchronized (shaders[i]) {
                shaders[i].notify();
                shaders[i].isWorking = true; //isWorking=true不能写在shader类内，否则无法即时变为true，导致waitForShaders()失效
            }
        }

        //等着色器线程完成工作
        waitForShaders();

        //计算渲染的三角形总数
        for (int i = 0; i < shaders.length; i++)
            MainThread.triangleCount += shaders[i].triangleCount;

    }

    public static int div255(int a) {
        int res;
        boolean negative = false;
        if (a < 0) {
            a = -a;
            negative = true;
        }
        for (res = 0; a != 0; a >>= 8, res += a) ;
        return negative ? -res : res;
    }

    //设置图层透明度
    public static void multiplyAlpha(int[] screen, int alpha) {
        for (int i = 0; i < MainThread.screenSize; i++) {
            if (screen[i] != 0) {
                int a = screen[i] >>> 24 & 0xFF;
                if (a == 255)
                    a = alpha;
                else
                    a = div255(a * alpha);
                screen[i] = screen[i] & 0xFFFFFF | a << 24;
            }
        }
    }

    //把screen2合并到screen1
    public static void mixScreen(int[] screen1, float[] zBuffer1, int[] screen2, float[] zBuffer2) {
        for (int i = 0; i < MainThread.screenSize; i++)
            if (screen2[i] != 0 && zBuffer1[i] <= zBuffer2[i]) {
                screen1[i] = screen2[i];
                zBuffer1[i] = zBuffer2[i];
            }
    }


    public static int mixLight(int color1, int color2) {
        int a1 = (color1 >>> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a2 = (color2 >>> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a3 = a1 + a2 - div255(a1 * a2);

        //把alpha转为亮度,建立亮度滤镜
        r2 = r2 + div255((255 - r2) * (255 - a2));
        g2 = g2 + div255((255 - g2) * (255 - a2));
        b2 = b2 + div255((255 - b2) * (255 - a2));

        //微调滤镜强度
        r2 /= 2;
        g2 /= 2;
        b2 /= 2;

        r1 = div255(r1 * r2);
        g1 = div255(g1 * g2);
        b1 = div255(b1 * b2);
        return (a3 << 24) | (r1 << 16) | (g1 << 8) | b1;
    }

    public static void mixScreenLight(int[] screen1, float[] zBuffer1, int[] screen2, float[] zBuffer2) {
        for (int i = 0; i < MainThread.screenSize; i++)
            if (screen2[i] != 0 && zBuffer1[i] < zBuffer2[i]) { //只有深度大于0才会合并
                int color1 = screen1[i];
                int color2 = screen2[i];

                screen1[i] = mixLight(color1, color2);
                //透明贴图暂时不遮挡
                //zBuffer1[i] = zBuffer2[i];
            }
    }

    public static int mixCover(int color1, int color2) {
        int a2 = (color2 >>> 24) & 0xFF;
        if (a2 == 255)
            return color2;

        int a1 = (color1 >>> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a3;
        if (a1 == 255) a3 = 255;
        else a3 = a1 + a2 - div255(a1 * a2);

        r1 += div255((r2 - r1) * a2);
        g1 += div255((g2 - g1) * a2);
        b1 += div255((b2 - b1) * a2);
        return (a3 << 24) | (r1 << 16) | (g1 << 8) | b1;
    }

    public static void mixScreenWithAlpha(int[] screen1, float[] zBuffer1, int[] screen2, float[] zBuffer2) {
        for (int i = 0; i < MainThread.screenSize; i++)
            if (screen2[i] != 0 && zBuffer1[i] <= zBuffer2[i]) { //只有深度大于0才会合并,透明贴图暂时不遮挡
                int color1 = screen1[i];
                int color2 = screen2[i];
                screen1[i] = mixCover(color1, color2);
                //透明贴图暂时不遮挡
                //zBuffer1[i] = zBuffer2[i];
            }
    }

    public static void mixScreenWithAlpha(int[] screen1, float[] zBuffer1, int[] screen2, float[] zBuffer2, int alpha) {
        for (int i = 0; i < MainThread.screenSize; i++)
            if (screen2[i] != 0 && zBuffer1[i] <= zBuffer2[i]) { //只有深度大于0才会合并,透明贴图暂时不遮挡
                int color1 = screen1[i];
                int color2 = screen2[i] & 0xFFFFFF | alpha << 24;
                screen1[i] = mixCover(color1, color2);
                //透明贴图暂时不遮挡
                //zBuffer1[i] = zBuffer2[i];
            }
    }

    //清屏,screenColor=-1不清屏
    public static void clearScreen(int[] screen, float[] zBuffer, int screenColor) {
        int screenSize = 0;
        //清除颜色
        screenSize = screen.length;
        if (screenColor != -1) {
            screen[0] = screenColor;
            for (int i = 1; i < screenSize; i += i)
                System.arraycopy(screen, 0, screen, i, Math.min(screenSize - i, i));
        }
        //清除深度缓冲
        screenSize = zBuffer.length;
        zBuffer[0] = 0;
        for (int i = 1; i < screenSize; i += i)
            System.arraycopy(zBuffer, 0, zBuffer, i, Math.min(screenSize - i, i));
    }

}

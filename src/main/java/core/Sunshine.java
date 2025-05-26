package core;

public class Sunshine extends Shader {
    //阳光与地面夹角
    public static float cotX = 1, cotZ = 1;
    //光屏分辨率
    public static int width=256,height=256;
    //深度光屏
    public static float[] shadowMap = new float[width * height];
    //光屏位置，高度为0
    public static float startX = -1, startZ = 21; //左上
    public static float endX = 21, endZ = -1; //右下
    public static float xStep, zStep;
    public static final Object shadowLock = new Object(); //别用zBufferLock，混用会降低效率

    public Sunshine(String name) {
        super(name);
        setScreenSize(width, height);
    }

    public static void setRange(float startX, float startZ, float endX, float endZ) {
        Sunshine.startX = startX;
        Sunshine.startZ = startZ;
        Sunshine.endX = endX;
        Sunshine.endZ = endZ;
        xStep = width / (endX - startX);
        zStep = height / (endZ - startZ);
    }

    //计算三角形投影
    public void calculateSunshine(Vector3D[] triangleVertices) {
        //计算投影坐标
        for (int i = 0; i < 3; i++) {
            Vector3D p = triangleVertices[i];
            vertices2D[i][0] = p.x + p.y * cotX;
            vertices2D[i][1] = p.z + p.y * cotZ;
            vertexDepth[i] = p.y;
        }
        //判断是否绘制阴影
        float u01 = vertices2D[1][0] - vertices2D[0][0];
        float v01 = vertices2D[1][1] - vertices2D[0][1];
        float u02 = vertices2D[2][0] - vertices2D[0][0];
        float v02 = vertices2D[2][1] - vertices2D[0][1];
        float cross = u01 * v02 - u02 * v01;
        if (cross >= 0)
            return; //三角面未朝向阳光，不绘制阴影
        //得到光屏坐标
        leftMostPosition = screen_w;
        rightMostPosition = -1;
        for (int i = 0; i < 3; i++) {
            vertices2D[i][0] = (vertices2D[i][0] - startX) * xStep;
            vertices2D[i][1] = (vertices2D[i][1] - startZ) * zStep;
            clippedLight[i] = 0;

            leftMostPosition = Math.min(leftMostPosition, vertices2D[i][0]);
            rightMostPosition = Math.max(rightMostPosition, vertices2D[i][0]);
        }
        isClippingRightOrLeft = leftMostPosition < 0 || rightMostPosition >= screen_w;
        //扫描三角形边
        scanUpperPosition = screen_h;
        scanLowerPosition = -1;
        for (int i = 0; i < 3; i++)
            scanSide(i);
        if (isClippingRightOrLeft)
            handleClipping();
        //画三角形
        //逐行渲染扫描线
        for (int i = scanUpperPosition; i <= scanLowerPosition; i++) {
            int x_left = xLeft[i];
            int x_right = xRight[i];

            float z_Left = zLeft[i];
            float z_Right = zRight[i];

            float dz = (z_Right - z_Left) / (x_right - x_left);  //算出这条扫描线上深度值改变的梯度

            x_left += i * screen_w;
            x_right += i * screen_w;

            for (int j = x_left; j < x_right; j++, z_Left += dz) {
                synchronized (shadowLock) {
                    if (shadowMap[j] < z_Left)       //如果深度浅于深度缓冲上的值
                        shadowMap[j] = z_Left;          //就更新深度缓冲上的值
                }
            }
        }
    }

    public static float getShadowHeight(Vector3D p) {
        float x = p.x + p.y * cotX;
        float z = p.z + p.y * cotZ;
        x = (x - startX) * xStep;
        z = (z - startZ) * zStep;
        if (x < 0 || x >= width || z < 0 || z >= height)
            return -1;
        return shadowMap[(int) z * width + (int) x];
    }

}

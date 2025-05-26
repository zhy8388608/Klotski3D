package core;

import game.MainThread;

public class VertexBufferObject implements Cloneable {

    //顶点缓冲
    public Vector3D[] vertexBuffer;

    //顶点法量
    public Vector3D[] normals;

    //顶点的纹理方向
    public Vector3D[][] UVDirections;

    //顶点的纹理坐标；
    public float[][] uvCoordinates;

    //索引缓冲
    public int[] indexBuffer;

    //用以存储变换后的顶点的容器
    public Vector3D[] updatedVertexBuffer;

    //用以存储变换后的顶点亮度的容器
    public float[] vertexLightLevelBuffer;

    //顶点数
    public int vertexCount;

    //三角形数
    public int triangleCount;

    //大小的变换
    public float scale = 1;

    //局部坐标系变换的角度
    public int localRotationX, localRotationY, localRotationZ;

    //局部的平移变换
    public Vector3D localTranslation = new Vector3D(0, 0, 0);

    //三角形的颜色(渲染单色三角形时才会用到)
    public int triangleColor;

    //三角形的类型
    public int renderType = 0;
    public final static int soildColor = 0;
    public final static int textured = 1;
    public final static int barycentric_textured = 2;

    //光源
    public Light lightSource;

    //漫反射系数
    public float kd;

    //镜面反射系数
    public float ks;

    //模型用到的贴图的索引
    public int textureIndex;

    //贴图纹理坐标方向的重复数
    public float textureScale[][];

    //屏幕的深度缓冲
    public float[] zBuffer = MainThread.zBuffer;

    //屏幕的像素组
    public int[] screen = MainThread.screen;

    //是否有影子
    public boolean hasShadow=true;

    //是否更新深度缓冲
    public boolean hasZBuffer=true;

    @Override
    public Object clone() {
        VertexBufferObject clone = null;
        try {
            clone = (VertexBufferObject) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        //用以存储变换后的顶点的容器
        clone.updatedVertexBuffer = new Vector3D[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            clone.updatedVertexBuffer[i] = new Vector3D(0, 0, 0);
        }
        //用以存储变换后的顶点亮度的容器
        clone.vertexLightLevelBuffer = new float[vertexCount];
        //局部的平移变换
        clone.localTranslation=new Vector3D(localTranslation);
        return clone;
    }

    //构造函数
    //无光面组
    public VertexBufferObject(Vector3D[] vertexBuffer, Vector3D[][] UVDirections, Vector3D[] normals, int[] indexBuffer) {
        this(vertexBuffer, UVDirections, normals, indexBuffer, null, 0, 0);
    }

    //有光面组
    public VertexBufferObject(Vector3D[] vertexBuffer, Vector3D[][] UVDirections, Vector3D[] normals, int[] indexBuffer, Light lightSource, float kd, float ks) {
        this.vertexBuffer = vertexBuffer;
        this.UVDirections = UVDirections;
        this.normals = normals;
        this.indexBuffer = indexBuffer;

        this.vertexCount = vertexBuffer.length;
        this.triangleCount = indexBuffer.length / 3;

        this.lightSource = lightSource;
        this.kd = kd;
        this.ks = ks;

        prepareResource();

    }

    //无光模型
    public VertexBufferObject(Mesh mesh) {
        this(mesh, null, 0, 0);
    }

    //有光模型
    public VertexBufferObject(Mesh mesh, Light lightSource, float kd, float ks) {
        this.vertexBuffer = mesh.vertices;
        this.normals = mesh.normals;
        this.indexBuffer = mesh.indices;
        this.uvCoordinates = mesh.uvCoordinates;

        this.vertexCount = vertexBuffer.length;
        this.triangleCount = indexBuffer.length / 3;

        this.lightSource = lightSource;
        this.kd = kd;
        this.ks = ks;

        prepareResource();
    }

    public void setScreen(int[] screen, float[] zBuffer) {
        this.screen = screen;
        this.zBuffer = zBuffer;
    }

    public void prepareResource() {
        //初始化用以存储变换后的顶点的容器
        updatedVertexBuffer = new Vector3D[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            updatedVertexBuffer[i] = new Vector3D(0, 0, 0);
        }

        //初始化用以存储变换后顶点亮度的容器
        vertexLightLevelBuffer = new float[vertexCount];

        //初始化纹理坐标数组
        if (uvCoordinates == null) {
            uvCoordinates = new float[vertexCount][2];
        }
    }


}

package game;

import core.Light;
import core.Mesh;
import core.Vector3D;
import core.VertexBufferObject;

public class Box extends VertexBufferObject implements Cloneable{
    //动画坐标
    public Vector3D place = new Vector3D(0,0,0);
    //编号
    public int number;
    //是否绘制
    public boolean isHide=false;

    public Box(Mesh mesh, Light lightSource, float kd, float ks) {
        super(mesh, lightSource, kd, ks);
    }

    public Box(Mesh mesh) {
        super(mesh);
    }

    public Box() {
        super(new Mesh("box.obj", "clockwise"));
        textureIndex = 9;
        renderType = VertexBufferObject.barycentric_textured;
        scale = 0.5f;
        localTranslation = new Vector3D(0, 0, 0);
    }

    @Override
    public Object clone() {
        Box box = null;
        box = (Box) super.clone();
        box.place = new Vector3D(0,0,0);
        return box;
    }
}

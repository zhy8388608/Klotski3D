package game;

import core.Camera;
import core.Vector3D;

public class Selector {
    public static Vector3D getSelectedLocation(int X, int Y, float[] zBuffer) {
        X = Algorithms.clamp(X, 0, MainThread.screen_w - 1);
        Y = Algorithms.clamp(Y, 0, MainThread.screen_h - 1);
        float ZC = zBuffer[X + Y * MainThread.screen_w];
        if (ZC == 0) //天空盒
            return new Vector3D(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        float screenZ = 1 / ZC;
        float screenX = (X - MainThread.half_screen_w) * screenZ / MainThread.screenDistance;
        float screenY = (MainThread.half_screen_h - Y) * screenZ / MainThread.screenDistance;
        Vector3D loc = new Vector3D(screenX, screenY, screenZ);
        loc.rotate_X(Camera.X_angle);
        loc.rotate_Y(Camera.Y_angle);
        loc.add(Camera.position);
        return loc;
    }

    public static void round(Vector3D location) {
        //微调选取位置，便于区分一个点对应的两个方块
        float sx = location.x + 0.5f, sy = location.y + 0.5f, sz = location.z + 0.5f;
        sx = Math.abs(Math.round(sx) - sx);
        sy = Math.abs(Math.round(sy) - sy);
        sz = Math.abs(Math.round(sz) - sz);
        if (sx < sy && sx < sz)
            location.x += Camera.viewDirection.x > 0 ? 0.1f : -0.1f;
        else if (sy < sz)
            location.y += Camera.viewDirection.y > 0 ? 0.1f : -0.1f;
        else
            location.z += Camera.viewDirection.z > 0 ? 0.1f : -0.1f;
        location.set(Math.round(location.x), Math.round(location.y), Math.round(location.z));
    }

    public static int getSelectedNumber(Vector3D location) {
        int selectedNumber;
        if (BoxManager.boxMap.fits((int) location.x, (int) location.y, (int) location.z)) {
            selectedNumber = BoxManager.boxMap.boxes[(int) location.x][(int) location.y][(int) location.z];
            if (selectedNumber == 0)
                selectedNumber = MainThread.noSelectedNumber;
        } else selectedNumber = MainThread.noSelectedNumber;
        return selectedNumber;
    }
}

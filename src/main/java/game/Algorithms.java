package game;

import core.Vector3D;

public class Algorithms {

    public static Boolean fits(int a, int min, int max) {
        return min <= a && a <= max;
    }

    public static int clampCircular(int value,int min, int max) {
        int range = max - min + 1;
        int offset = value - min;
        offset = ((offset % range) + range) % range;
        return offset + min;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
    
    public static void roundToAxisDirection(Vector3D v){
        float vx = Math.abs(v.x), vy = Math.abs(v.y), vz = Math.abs(v.z);
        if (vx > vy && vx > vz) v.set(v.x > 0 ? 1 : -1, 0, 0);
        else if (vy > vz) v.set(0, v.y > 0 ? 1 : -1, 0);
        else v.set(0, 0, v.z > 0 ? 1 : -1);
    }
}

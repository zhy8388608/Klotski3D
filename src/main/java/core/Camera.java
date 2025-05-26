package core;

import game.Algorithms;
import game.Mouse;

public class Camera {
	//视角的位置矢量
	public static Vector3D position;
	
	//视角的方向矢量
	public static Vector3D viewDirection;

	//视角在Y轴上的旋转， 用来控制向左或向右看
	public static int Y_angle;
	
	//视角在X轴上的旋转, 用来控制向上或向下看
	public static int X_angle;

	public static float Y_angle_temp, X_angle_temp;
	
	//视角改变观察方向的速率,每频旋转度
	public static int turnRate= 2;
	
	//视角改变位置的速度，每频移动单位长度
	public static float moveSpeed = 0.2f;

	public static Vector3D rotatingCenter=new Vector3D(0,1,0);
	public static int rotatingRadius=8;
	
	//初始化方法
	public static void init(float x, float y, float z){
		position = new Vector3D(x,y,z);
		viewDirection = new Vector3D(0, 0, 1);
	}


	//更新视角状态
	public static void update(){

		float angelPerPixel=0.5f;
		Y_angle_temp+= Mouse.rightXDrag *angelPerPixel;
		X_angle_temp-= Mouse.rightYDrag *angelPerPixel;
		Y_angle = (int)(Y_angle_temp);
		X_angle = (int)(X_angle_temp);
		Mouse.rightXDrag =Mouse.rightYDrag =0;
		X_angle= Algorithms.clampCircular(X_angle,0,359);
		if(X_angle > 89 && X_angle < 180){
			X_angle = 89;
			X_angle_temp = 89;
		}
		else if(X_angle < 271 && X_angle >= 180){
			X_angle = 271;
			X_angle_temp = 271;
		}
		Y_angle= Algorithms.clampCircular(Y_angle,0,359);
		
		//更新视角的方向
		viewDirection.set(0,0,1);
		viewDirection.rotate_X(X_angle);
		viewDirection.rotate_Y(Y_angle);
		viewDirection.unit();

		position.set(0,0,0);
		position.add(viewDirection, -rotatingRadius);
		position.add(rotatingCenter);
	}
}

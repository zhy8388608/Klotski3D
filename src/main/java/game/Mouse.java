package game;

import java.awt.event.*;

public class Mouse extends MouseAdapter {
    public static boolean isLeftButtonPressed = false, isRightButtonPressed = false, isMouseClicked = false;
    public static int leftXDrag = 0, leftYDrag = 0, rightXDrag = 0, rightYDrag = 0, scroll = 0, X, Y, X0, Y0, pressX, pressY;

    @Override
    public void mousePressed(MouseEvent e) {
        X = X0 = e.getX();
        Y = Y0 = e.getY();
        pressX = e.getX();
        pressY = e.getY();
        if (e.getButton() == MouseEvent.BUTTON3) {
            isRightButtonPressed = true;
        } else if (e.getButton() == MouseEvent.BUTTON1) {
            isLeftButtonPressed = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        X = e.getX();
        Y = e.getY();
        if (e.getButton() == MouseEvent.BUTTON3) {
            isRightButtonPressed = false;
        } else if (e.getButton() == MouseEvent.BUTTON1) {
            isLeftButtonPressed = false;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        X = e.getX();
        Y = e.getY();
        if (isRightButtonPressed) {
            rightXDrag += X - X0;
            rightYDrag += Y - Y0;
        } else if (isLeftButtonPressed) {
            leftXDrag += X - X0;
            leftYDrag += Y - Y0;
        }
        X0 = X;
        Y0 = Y;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        X = e.getX();
        Y = e.getY();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        isMouseClicked = true;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int rotation = e.getWheelRotation(); // 获取滚动方向
        int scrollAmount = e.getScrollAmount(); // 获取滚动量（单位/块）
        if (rotation > 0)  //往下
            scroll += scrollAmount;
        else
            scroll -= scrollAmount;
    }

}

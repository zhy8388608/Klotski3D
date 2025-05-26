package game;

import core.Vector3D;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Keyboard extends KeyAdapter {
    private static long lastTypedTime = 0;
    private static final long MIN_INTERVAL = 100; // 最小间隔时间（毫秒）

    @Override
    public void keyPressed(KeyEvent e) {
        boolean shouldMoveBox = true;
        Vector3D direction = new Vector3D(0, 0, 0);
        if (e.getKeyChar() == 'w' || e.getKeyChar() == 'W')
            direction.set(0, 1, 0);
        else if (e.getKeyChar() == 's' || e.getKeyChar() == 'S')
            direction.set(0, -1, 0);
        else if (e.getKeyChar() == 'a' || e.getKeyChar() == 'A')
            direction.set(-1, 0, 0);
        else if (e.getKeyChar() == 'd' || e.getKeyChar() == 'D')
            direction.set(1, 0, 0);
        else if (e.getKeyChar() == 'q' || e.getKeyChar() == 'Q')
            direction.set(0, 0, 1);
        else if (e.getKeyChar() == 'e' || e.getKeyChar() == 'E')
            direction.set(0, 0, -1);
        else
            shouldMoveBox = false;
        if (shouldMoveBox)
            BoxManager.moveByScreenDirection(MainThread.selectedNumber, direction);
        else if (e.getKeyChar() == 'f' || e.getKeyChar() == 'F')
            BoxManager.switchFoucusMode(true);
        else if (e.getKeyChar() == 'r' || e.getKeyChar() == 'R')
            BoxManager.isXrayModeOn = true;
        else if (e.getKeyChar() == 't' || e.getKeyChar() == 'T')
            BoxManager.isShowingTarget = true;
    }


    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyChar() == 'f' || e.getKeyChar() == 'F')
            BoxManager.switchFoucusMode(false);
        else if (e.getKeyChar() == 'r' || e.getKeyChar() == 'R')
            BoxManager.isXrayModeOn = false;
        else if (e.getKeyChar() == 't' || e.getKeyChar() == 'T')
            BoxManager.isShowingTarget = false;
        else if (e.getKeyChar() == 'h' || e.getKeyChar() == 'H')
            MainThread.isSolving = true;
        else if (e.getKeyChar() == 'n' || e.getKeyChar() == 'N')
            MainThread.isRestarting = true;
        else if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
            MainThread.isExiting=true;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTypedTime < MIN_INTERVAL)
            return;
        lastTypedTime = currentTime;
        if (e.getKeyChar() == 'z' || e.getKeyChar() == 'Z')
            BoxManager.undo();
        else if (e.getKeyChar() == 'y' || e.getKeyChar() == 'Y')
            BoxManager.redo();
    }
}



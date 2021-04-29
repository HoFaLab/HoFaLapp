package de.hofalab.hofalapp;

import java.util.LinkedList;

/**
 * Created by piet on 27.07.19.
 */

class CNCDevice {
    private static Vector3 pos = new Vector3();
    private static double step = 1;
    private static int horizontalAxis = 0, verticalAxis = 1;
    private static LinkedList<Stroke> strokesDone = new LinkedList<Stroke>();

    public static double getStep() {
        return step;
    }

    public static void setStep(double step) {
        CNCDevice.step = step;
    }

    static synchronized Vector3 getPos(){
        return pos;
    }

    static synchronized void setPos(Vector3 val){
        CNCDevice.pos = val;
    }

    public static int getHorizontalAxis() {
        return horizontalAxis;
    }

    public static void setHorizontalAxis(int horizontalAxis) {
        CNCDevice.horizontalAxis = horizontalAxis;
    }

    public static int getVerticalAxis() {
        return verticalAxis;
    }

    public static void setVerticalAxis(int verticalAxis) {
        CNCDevice.verticalAxis = verticalAxis;
    }

    public static LinkedList<Stroke> getStrokesDone() {
        return strokesDone;
    }

    // Constant hardware restrictions of CNC device
    // TODO get correct values here
    static final Vector3 wrkspcMin = new Vector3(-75,-75, -38);
    static final Vector3 wrkspcMax = new Vector3(75,75,2);

    public static Vector3 getWrkspcSize(){
        Vector3 res = CNCDevice.wrkspcMax.copy();
        res.sub(CNCDevice.wrkspcMin);
        return res;
    }

}

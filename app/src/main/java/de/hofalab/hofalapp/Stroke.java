package de.hofalab.hofalapp;

import java.util.Vector;

/**
 * Created by piet on 27.07.19.
 */

// A list of 2 or more positions
class Stroke {
    static double width = 4; // todo configure
    Vector<Vector3> data;

    Stroke(Vector3 startpoint){
        data = new Vector<Vector3>();
        data.add(startpoint);
    }


}

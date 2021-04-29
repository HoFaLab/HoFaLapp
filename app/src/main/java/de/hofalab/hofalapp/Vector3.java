package de.hofalab.hofalapp;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Created by piet on 26.07.19.
 */

class Vector3 {
    private double[] data;
    private static DecimalFormat f = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance( Locale.ENGLISH ));

    Vector3(){
        this(0);
    }

    Vector3(double val){
        this(val, val, val);
    }

    Vector3(double x, double y, double z){
        data = new double[3];
        data[0] = x;
        data[1] = y;
        data[2] = z;
    }

    double getX() {return data[0];}
    double getY() {return data[1];}
    double getZ() {return data[2];}

    double get(int i){return data[i];}
    double[] getData(){return data;}
    void set(int i, double val){data[i] = val;}
    void setData(double[] d){data = d;}

    @Override
    public String toString() {
        return "X"+f.format(data[0]) + "Y"+f.format(data[1]) + "Z"+f.format(data[2]);
    }

    Vector3 copy(){
        return new Vector3(data[0], data[1], data[2]);
    }

    void add(Vector3 other){
        data[0] += other.data[0];
        data[1] += other.data[1];
        data[2] += other.data[2];
    }

    void sub(Vector3 other){
        data[0] -= other.data[0];
        data[1] -= other.data[1];
        data[2] -= other.data[2];
    }

    void mult(double val){
        data[0] *= val;
        data[1] *= val;
        data[2] *= val;
    }

    void neg(){
        mult(-1);
    }

    double dot(Vector3 other){
        return
            data[0] * other.data[0] +
            data[1] * other.data[1] +
            data[2] * other.data[2];
    }

    double lengthSqr(){
        return dot(this);
    }

    double length(){
        return Math.sqrt(lengthSqr());
    }

    void min(Vector3 other){
        data[0] = Math.min(data[0], other.data[0]);
        data[1] = Math.min(data[1], other.data[1]);
        data[2] = Math.min(data[2], other.data[2]);
    }

    void max(Vector3 other){
        data[0] = Math.max(data[0], other.data[0]);
        data[1] = Math.max(data[1], other.data[1]);
        data[2] = Math.max(data[2], other.data[2]);
    }

    void clamp(Vector3 lower, Vector3 upper){
        max(lower);
        min(upper);
    }

    public boolean equals(Vector3 other) {
        return data.equals(other.data);
    }
}

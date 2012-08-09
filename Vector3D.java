/*
 * Vector3D.java
 * 3D Vector class. Used to create 3D model.
 *
 * Created on March 17, 2006, 2:17 PM
 */

/**
 * @author  Chen-Fu Liao
 * Sr. Systems Engineer
 * ITS Institute, ITS Laboratory
 * Center For Transportation Studies
 * University of Minnesota
 * 200 Transportation and Safety Building
 * 511 Washington Ave. SE
 * Minneapolis, MN 55455
 */
import java.lang.Math ;

public class Vector3D {
    public float X ;
    public float Y ;
    public float Z ;
    /** Creates a new instance of Vector3D */
    public Vector3D() {
    }
    public Vector3D(float _x, float _y, float _z) {
        X = _x ;
        Y = _y ;
        Z = _z ;
    }
    public Vector3D vAdd(Vector3D vec) {
        Vector3D v ;
        v = new Vector3D(X + vec.X, Y + vec.Y, Z + vec.Z) ;
        return v ;
    }
    public Vector3D vSub(Vector3D vec) {
       Vector3D v ;
        v = new Vector3D(X - vec.X, Y - vec.Y, Z - vec.Z);
        return v;
    }
    public float vDot(Vector3D vec) {
        return (X * vec.X + Y * vec.Y + Z * vec.Z);
    }
    public Vector3D vCross(Vector3D vec) {
        Vector3D v;
        v = new Vector3D((Y * vec.Z - Z * vec.Y), (Z * vec.X - X * vec.Z), (X * vec.Y - Y * vec.X));
        return v;
    }
    public Vector3D vScale(float sf ) {
        return new Vector3D(sf * X, sf * Y, sf * Z);
    }
    public float vLen() {
        return new Double(Math.sqrt(X*X + Y*Y + Z*Z)).floatValue() ;
    }
    public Vector3D vUnit() {
        float sf ;
        sf = 1 / vLen() ;
        return new Vector3D(sf * X, sf * Y, sf * Z) ;
    }
    public String toStr() {
        return Float.toString(X) + " " + Float.toString(Y) + " " + Float.toString(Z) + " " ;
    }
}



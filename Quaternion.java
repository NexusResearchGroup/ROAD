/*
 * Quaternion.java
 *
 * Created on March 17, 2006, 2:32 PM
 */

/**
 * Chen-Fu Liao
 * Sr. Systems Engineer
 * ITS Institute, ITS Laboratory
 * Center For Transportation Studies
 * University of Minnesota
 * 200 Transportation and Safety Building
 * 511 Washington Ave. SE
 * Minneapolis, MN 55455
 */
import java.lang.Math;

public class Quaternion {
    public Vector3D vectPart ;
    public float realPart ;
  
    /** Creates a new instance of Quaternion */
    public Quaternion() {
    }
    
    public Quaternion(Vector3D vec, float angle) {
        vectPart = vec ;
        realPart = angle ;
    }
    public Quaternion QQMul(Quaternion q2) {
        Quaternion r ;
        Vector3D tempV ;
        r = new Quaternion(vectPart.vCross(q2.vectPart), this.realPart * q2.realPart - this.vectPart.vDot(q2.vectPart)) ;
        tempV = this.vectPart.vScale(q2.realPart);
        r.vectPart = tempV.vAdd(r.vectPart);
        tempV = q2.vectPart.vScale(this.realPart);
        r.vectPart = tempV.vAdd(r.vectPart);
        return r;
    }
    public String toAxisAngle() {
        float halfAngle, sinHalfAngle ;
        float rotAngle ;
        Vector3D rotAxis ;
        halfAngle = new Double(Math.acos(this.realPart)).floatValue();
        sinHalfAngle = new Double(Math.sin(halfAngle)).floatValue();
        rotAngle = 2.0f * halfAngle;
        if ( (sinHalfAngle < 0.00000001) && sinHalfAngle > -0.00000001 ){
            rotAxis = new Vector3D(1, 0, 0);
        } else {
            sinHalfAngle = 1f / sinHalfAngle;
            rotAxis = this.vectPart.vScale(sinHalfAngle);
        }
        return rotAxis.toStr() + Float.toString(rotAngle) ;
    }

}

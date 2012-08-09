/*
 * mPointF.java
 * A floating point 2D point class.
 *
 * Created on March 17, 2006, 12:19 PM
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

public class mPointF {
    public float X ;
    public float Y ;
    /** Creates a new instance of mPoint */
    public mPointF() {
    }
    public mPointF(float _x, float _y) {
        X=_x;
        Y=_y;
    }
    public float getX() {
        return X ;
    }
    public float getY() {
        return Y ;
    }
}

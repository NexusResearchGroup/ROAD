/*
 * mPoint.java
 * An integer point class.
 *
 * Created on March 17, 2006, 12:18 PM
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
public class mPoint {
    public int X ;
    public int Y ;
    /** Creates a new instance of mPoint */
    public mPoint() {
    }
    public mPoint(int _x, int _y) {
        X=_x;
        Y=_y;
    }
    public int getX() {
        return X ;
    }
    public int getY() {
        return Y ;
    }
}
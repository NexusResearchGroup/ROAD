/*
 * RoadGeoDB.java
 * This class is used to store road geometry info every 10 ft or 10 meter
 * that will later be used to create 3D VRML model DB
 *
 * Created on March 17, 2006, 1:33 PM
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

public class RoadGeoDB {
    public float X ;          // X coordinate
    public float Y ;          // Y coordinate
    public float Ele ;        // Elevation
    public float Se ;         // superelevation
    
    /** Creates a new instance of RoadGeoDB */
    public RoadGeoDB() {
    }
    public RoadGeoDB(float _x, float _y, float _ele,float _se ){
        X = _x;
        Y = _y;
        Ele = _ele;
        Se = _se;
}
    public void Load(float _x, float _y, float _ele,float _se ){
        X = _x;
        Y = _y;
        Ele = _ele;
        Se = _se;
}
    public String toTextString() {
        String str ;
        str = Float.toString(X) + "," + Float.toString(Y) + "," + 
            Float.toString(Ele) + "," + Float.toString(Se) ;
        return str;
    }
}

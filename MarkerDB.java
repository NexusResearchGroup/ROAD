/*
 * MarkerDB.java
 * Station database class.
 *
 * Created on March 17, 2006, 1:23 PM
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

public class MarkerDB {
    private mPointF location  ; // marker/station location
    private float elevation = -1 ;  // elevation data in ft ormeter
    private byte type = 0 ;   // 1-line, 2-curve, 3-tangent
    private float distance ;  // distance from starting landmark
    private byte parent ;     // index/id of parent segment from hRoadData DB
    private float grade ;     // grade from last to current landmark
    public boolean PVTnC_Overlap = false ;  // 4/4/06 added
    
    /** Creates a new instance of MarkerDB */
    public MarkerDB() {
        location = new mPointF(0f, 0f);
    }

    public void setMarker(mPointF loc, float ele , byte index ){
        location = loc;
        elevation = ele;
        type = 0;
        parent = index;
    }
    public void setMarker(mPointF loc , float ele , byte index, byte seg_type){
        location = loc;
        elevation = ele;
        type = seg_type;
        parent = index;
    }

    public mPointF getLocation(){
        return location;
    }
    public float getElevation(){
        return elevation;
    }
    public byte getSegmentType() {
        return type;
    }
    public void RESET(){    // reset station data
        location = new mPointF(0f, 0f);
        elevation = -1;
        type = 0;
    }
    public void setLocation(float px , float py){
        location = new mPointF(px, py);
    }
    public void setElevation(float ele ){
        elevation = ele;
    }
    public void setSegmentType(byte stype ){
        type = stype;
    }
    public void setDistance(float dist){
        distance = dist;
    }
    public float getDistance() {
        return distance;
    }
    public void setParentIndex(byte pID ) {
        parent = pID;
    }
    public byte getParentIndex() {
        return parent;
    }
    public void setGrade(float val ){
        grade = val;
    }
    public float getGrade() {
        return grade;
    }
}

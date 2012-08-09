/*
 * StationInfo.java
 * Store station info specified by user mouse click.
 *
 * Created on March 21, 2006, 3:53 PM
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

// Landmark station info class
public class StationInfo {
    public String title = "" ;
    public float elevation = 0f ;
    public boolean CheckBox_edit = false ;
    public int parentId = 0 ;   // parent segment ID
    public int dataIndex = 0 ;
    public boolean tangent_option = false ;
    public boolean curve_option = false ;
    public boolean line_option = false ;
    public int initial_state = -1 ; // initial optioni, 1-line, 2-curve, 3-tangent
    public int insert = -1 ;        // index for insertion
    
    //public byte segment_type = 0 ;
    public mPointF location = new mPointF(0f,0f);
    
    /** Creates a new instance of StationInfo */
    public StationInfo() {
    }
    
    public void optionInit(){
        line_option=false;
        curve_option=false;
        tangent_option=false;
    }
}

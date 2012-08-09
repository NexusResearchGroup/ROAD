/*
 * SHARED.java
 * Shard data variable class.
 *
 * Created on March 20, 2006, 9:16 AM
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
/** Revision Log:
    Version 0.1: Mar. 14, 2006
         - Release Windows .NET version.
    Version 0.2: Apr. 04, 2006
         - Release web-based version.
         - Fix metric unit display error in [Design Settings] screen.
         - Fix 3D animation error when choosing metric unit.
         - Fix vehicle speed to reflect design speed in 3D animation.
         - Fix horizontal curve setting scaling factor error when updating image 
           resolution and map scale.
         - Add [edit - clear curves] and allow PVI modification in vertical curve 
           design.
         - Add [file - save report] in vertical curve design for saving report to 
           local disk drive.
    Version 0.2.1: April 21, 2006
         - Add [file - delete] function in horizontal design
    Version 0.2.2: May 1, 2006
         - Update contour image resolution & scale with corresponding unit selection
         - Update horizontal design curve radius & elevation data with corresponding unit selection
         - Include unit info while saving vertical curve file
         - Automatically convert vertical curve data to corresponding unit selected
    Version 0.2.3: July 1, 31 2006
         - Add view mass diagram feature in vertical curve design
         - Add print/save report file menu under report screen
         - Add print/save landmark data file menu under view station landmark window in horizontal design
         - Add print/save station data file menu under view stations window in vertical design
         - Add Cortona User's Guide web link under help menu in vertical design screen, 8/2
         - Add road base & walls in 3D model, 8/10/06
    Version 0.2.4: Oct. 10, 2006
         - Add min grade
         - Change image scale and map scale from integer to float
         - Allow users to specify desired vertical curve length and check min curve len, Lm
         - Add option to view & save mass diagram and cut-fill data to a text file
    Version 0.2.5: Nov. 1, 2006
         - Upgrade toolbar buttons to 3D style
         - Add horizontal curve alignment toolbar icon #7
         - Add designed vertical curve elevation data button & options in elevation profile screen
         - Remove deleted item(s), line or curve, when saving horizontal design to a file
         - Modify popElevationMarkerForm in hDrawArea to correct error line segment 
           selection when users select curve segment
         - Modify getLineMarkLocation & getCurveMarkLocation subroutines
         - Ask for saving horizontal design & vertical design before close 7 exit
         - Add view horizontal PC, PT data screen
         - Allow users to edit elevation in view station/landmark screen by addinig 
           Update Elevation option under Data file menu 
         - Save temperary roaddesign.html & vrml_db file to Windows Desktop, 
           "C:\\Documents and Settings\\All Users\\Desktop\\". Use "C:\\" dir,
           if desktop directory is not available or not found
         - Fix import contour image then cancel error
         - Add view "Road Design Ctl+A" and "Road Only Ctl+Z" options under horizontal design
           file-view menu
         - Add "check station data" option under horizontal design file-tool menu option
         - Add index help manual (using JavaHelp), Web contents & User's Guide under Help menu.
         - Comment out JavaHelp stuff in Geometry_DEsign & hDrawArea files; cause error when using 
           JRE1.6.0 2/12/07
         - Change min curve distance from 20 to 40 and add popup message if curve is farther 
           than 40 pixels from lines in hDrawArea subroutine, 2/12/07
    Version 0.3.0: Feb. 12, 2007
         - Comment additional function call updateVCurve(PVI_index) at line 925 in vDrawArea, 2/28/07
         - Change refresh icon to landmark insert icon to allow insert landmark stations, 3/1/07
         - Check station landmarks before entering vertical design, 3/1/07
         - Allow using right mouse click to edit/delete landmark when choosing insert landmark tool,
           3/1/07
         - Modify saveDesignFile subroutine in Geometry_Design class to update parentIndex accordingly
           when removing deleted segments, 3/1/07
    Version 0.3.1: Mar. 15, 2007 released
         - add mouse right click to delete items feature in horizontal design (See mouse_click() under
           default option in hDrawArea.java file. 12/21/07
         - Remove tangent points when curve is deleted. 12/21/07
    Version 0.3.2: Feb. 11, 2008 to be released
         - Change horizontal view landmark (X,Y) and view PC/PT data scale to reflect actual scale in ft or m.
 
 */

import java.awt.*;

public class SHARED {
    public static final String VERSION = "Ver.0.3.2, Feb. 2008 (C)" ;   // Version string, recompile abouttextbox
    public static final String MANUAL_PATH = "http://street.umn.edu/Road/jmanualv032.pdf" ;  // user's manual file path
    public static final String CONTENTS_PATH = "http://street.umn.edu/Road/javahelp/roadWebMain.html" ;  // help set file path
    static final int MAX_CURVES = 32  ;                     // MAX CURVES
    static final int MAX_MARKERS = 100  ;                   // MAX NUMBER OF ELEVATION MARKERS
    static final int MAX_SEGMENTS = 100   ;                 // MAX NUMBER OF LINE?CURVE SEGMENTS
    static final float FT2M = 0.3048f       ;               // convert from foot to meter
    static final float MPH2Kmh = 1.609344f   ;              // convert from MPH to Km/h

    public int myUnit = 1 ;                                 // 1 - US Customary, 2 - Metric, default to 1
    public float ContourImageResolution = 300f;  // DPI, 71 ; // deafult contour map resolution, DPI
    public float ContourScale = 1000f;   //769 ;            // default contour map scale
    public float imageScale = 0.0f ;                        // overall image scale in GUI display
    public Color myPenColor = Color.blue ;                  // this is a color the user selects
    public float myRoadLaneSizes = 2f ;                     // set pen/road lane # variable
    public float myLaneWidth = 12f ;                        // design lane width, 12 ft
    public float myShoulderWidth = 6f ;                     // design shoulder width, 6 ft
    public Color elevationMarkerColor = Color.green ;       // this is a color the user selects for markers
    public float elevationMarkerSize = 2f;                  // set marker size
    public float curveRadius;                               // curve radius
    public mPointF currentElevationMarker ;                 // current Mark XY position

    public Data2D[] hRoadData = new Data2D[MAX_SEGMENTS];           // horizontal road design database
    public MarkerDB[] hAlignMarks = new MarkerDB[2 * MAX_CURVES];   // land marks DB up to 16 curves, horizontal curve tangent points
    public int hAlignMarkCount ;                                    // number of hAlignMarks
    public MarkerDB[] elevationMarks = new MarkerDB[MAX_MARKERS];   // elevation marks DB - horizontal design
    public int elevationMarkCount ;                                 // number of elevation marks
    public MarkerDB[] vConstructMarks = new MarkerDB[MAX_MARKERS] ; //  ' proposed construction marks DB - vertical design
    public int vConstructMarkCount ;                                // number of proposed construction marks
    public VCurve[] verticalCurves = new VCurve[MAX_MARKERS - 2] ;  //' vertical curves
    public int vCurveCount ;                                        // vertical curve count

    // design control parameters
    public float gradeLimit = 0.05f ;                     // max grade limit default 5%
    public float speedLimit = 40f ;                       // speed limit MPH default 40MPH
    public float maxCut = 15f ;                           // depth of maximum cut, default 10 ft
    public float maxFill = 15f ;                          // height of maximum fill, default 10 ft
    public float vehDecel = 11.2f ;                       // vehicle deceleration rate, 11.2 ft/s/s
    public float reactionTime = 2.5f ;                    // drive reaction time 2.5 sec
    public float frictionCoef = 0.3f ;                    // friction coefficient
    public float sideFrictionCoef = 0.13f ;               // side friction coefficient
    public float minVCurveLen = 560f ;                    // minimum vertical curve length
    public float minHCurveRadius = 500f ;                 // minimum horizontal curve radius
    public float maxSuperelevation = 0.06f ;              // maximum superelevation, 6%
    public float minGrade = 0.0f ;                        // minimum grade 0.0%

    /** Creates a new instance of SHARED */
    public SHARED() {
    }
    
    public void RESET() {
        hRoadData = new Data2D[MAX_SEGMENTS];
    }
}

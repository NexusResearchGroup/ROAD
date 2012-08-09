/*
 * vDrawArea.java
 * Vertical curve design class.
 *
 * Created on March 23, 2006, 1:14 PM
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
import java.awt.*;
import java.applet.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.URL; 
import java.io.*;
import java.io.FilenameFilter;
import javax.swing.*;

public class vDrawArea extends DoubleBufferedPanel 
    implements MouseListener,  MouseMotionListener
{
    public Applet myApplet = new Applet() ;         // applet pointer from parent, hDesign
    public SHARED myDB = new SHARED();              // shared data variables
    public int hRoadDataCount ;                     // horizontal design data count
    toolbarV tb;                                    // toolbar
    statusbarV sb;                                  // status bar
    final int grid = 8;                             // drawarea grid size
    final int offsetX = 80 ;                        // draw area offset X
    final int offsetY = 30;                         // draw area offset Y
    final int data_viewer_Height = 360;             // draw area width
    final int data_viewer_Width = 480;              // draw area height
    final String newLine = "\n";                    // line separator
    int toolbarIndex = 0 ;                          // toolbar index
    final int GRID_HDIV = 60;                       // horizontal grid dividion
    final int GRID_VDIV = 60;                       // vertical grid division
    float ComputeStepSize = 10f;                    // 10 ft or 10 m
    Font myFont = new Font("Times New Roman", Font.PLAIN, 12);

    int gradeLogIndex = -1;
    int[] gradeLogBuffer = new int[16] ;            // undo, redo log
    //boolean display_dot_max = false;
    //boolean display_dot_min =false;
    float max_ele, min_ele ;                        // min/max elevation in elevation profile 
    float plotScaleX=0f, plotScaleY=0f ;            // plot scale X & Y
    int Xmin, Ymin, gridSizeX, gridSizeY ;          // drawing variables
    boolean isGradeConstructOn  = false;            // grade construction flag
    boolean viewElevationProfile = true;            // view elevation profile flag
    boolean viewMassDiagram = false;                // view mass diagram flag, 5/2/06 added
    float SSD;                                      // stop sight distance
    float Lm_crestK, Lm_sagK ;                      // horizontal distance required to effect 1% of change in slope
    boolean valid_vCurve = false;                   // valid vertical curve flag
    float max_hDist ;                               // horizontal/projected road length
    float total_roadLen ;                           // total road length
    MouseEvent me0, me1 ;                           // DB Null
    final int marker_size = 3;                      // end mark draw size
    float[] CutandFill ;                            // cur and fill data array
    float[] accuMass ;                              // accumulated mass
    float[] designElevation ;                       // design elevation profile
    int fillCutSteps ;                              // # of cut & fill steps
    RoadGeoDB[] vrmlPoints_roadCenter;              // Road center geometry DB
    Image eleImg = null;                            // elevation profile label image
    Image fillcutImg = null;                        // cut/fill profile label image
    // 4/4/06 added
    int PVI_index = -1;                             // PVI modification index
    
    // window frame =================
    myWindow frame_msgbox, frame_msgboxYesNo, frame_msgbox_viewVRML ;
    myWindow frame_msgboxClearAll, frame_msgboxClearCurves;
    myWindow frame_report = null ;

    myWindow frame_editVertCurveLen ;   // 10/9/06
    JFrame frmStationTable = new JFrame("View Station Data") ;
    JFrame frmDesignedElevationTable = new JFrame("View Designed Elevation Data") ;
    JFrame frmMassTable = new JFrame("View Mass Data") ;
    JFrame frmCutFillTable = new JFrame("View Cut and Fill Data") ;
    
    Choice listVertCurves ;         // Vertical Curve Index
    Label lblSSD ;                  // SSD label display in popVertCurveLenEdit
    TextField txtCurveLen;          // vertical curvev length
    
    // mouse double click variables
    private boolean displayDoubleClickString = false;
    // The number of milliseconds that should bound a double click.
    static private long mouseDoubleClickThreshold = 300L;
    // The previous mouseUp event//s time.
    private long mouseUpClickTime = 0;
    private String stationPrintStr ;
    private String elevationPrintStr ;
    private String massPrintStr ;
    private String fill_cutPrintStr ;
    
    Runnable runThread0 = null ;    // stop on red light
    public Thread tSetReport ;
    public boolean setReport_flag = false ; // accessed from toolbarV
    public boolean setCurvelenEdit_flag = false ;   // accessed from toolbarV
    private boolean noCurve2Edit_flag = false ;
    private boolean popMsgBox_flag = false ;
    //private boolean popMsgBox_viewVRML_flag = false ;   // 11/15/06 added
    private String msgBox_title = "" ;
    private String msgBox_message = "" ;
    //private String msgBox_viewVRML_title = "" ;     // 11/15/06 added
    //private String msgBox_viewVRML_message = "" ;   // 11/15/06 added

    vDrawArea(toolbarV t, statusbarV s)
    {
	tb = t;
        sb = s;
	setBackground(Color.white);
	t.parent = this;  
        s.parent = this;
        this.setBackground(new Color(220,220,220));
    }    
    
    // Object initialization
    public void init() {
        addMouseListener(this);
        addMouseMotionListener(this);
        
        // compute elevation step size
   //     if (myDB.myUnit==1) {   // US unit
   //         ComputeStepSize = 10f ; // 10 ft
   //     } else if (myDB.myUnit==2) {    // metric unit
  //          ComputeStepSize = 3.0f ;    // 3 meter
  //      }
        
        // prepare X, Y axis label min & max
        int i ;
        float my_ele=0f ;
        float dist=0f, accu_dist=0f ;
        min_ele = 99999f;
        max_ele = 0f;
        mPointF lastMark ;
        byte lastMarkType ;

        myDB.vConstructMarkCount = 0;
        valid_vCurve = false;
        myDB.vCurveCount = 0;
        me0 = null;
        myDB.imageScale = (float)myDB.ContourImageResolution / (float)myDB.ContourScale;  //  // pixel/ft
        for (i=0;i<myDB.elevationMarkCount;i++){
            // determine max min elevation
            my_ele = myDB.elevationMarks[i].getElevation();
            if( my_ele > max_ele) { 
                max_ele = my_ele;
            }
            if (my_ele < min_ele) { 
                min_ele = my_ele;
            }

            // calculate accumulated distance, based on line/curve/tangent point
            if (i == 0) { 
                accu_dist = 0f;
            } else {
                lastMark = myDB.elevationMarks[i - 1].getLocation();
                //System.out.println("X="+CStr(lastMark.X)+", Y="+CStr(lastMark.Y)+", scale="+CStr(myDB.imageScale));
                
                lastMarkType = myDB.elevationMarks[i-1].getSegmentType();
                switch (myDB.elevationMarks[i].getSegmentType()) {
                    case 1:  // line
                        // linear distance
                        dist = distanceOf(lastMark, myDB.elevationMarks[i].getLocation()) / myDB.imageScale;
                        break;
                    case 2:  // curve
                        dist = calculateArcLength(i);    // radius in feet already
                        break;
                    case 3:  // tangent point, i>0
                        if (lastMarkType == 1 || lastMarkType == 3 ) {
                            //previous point belongs to a line
                            // linear distance
                            dist = distanceOf(lastMark, myDB.elevationMarks[i].getLocation()) / myDB.imageScale;
                        } else if (lastMarkType == 2) { 
                            // previous point belongs to a curve
                            dist = calculateArcLength(i);
                        }
                        break;
                }   // switch
            //System.out.println("dist="+CStr(dist));
                accu_dist += dist;   // distance from landmark 0
            }
            myDB.elevationMarks[i].setDistance(accu_dist);
            //System.out.println("accudist="+CStr(myDB.elevationMarks[i].getDistance()));
        } // for 
        max_hDist = accu_dist;   // in  ft or meter
        
            //System.out.println("max_hDist="+CStr(max_hDist));

        // determine plot scale in both X & Y directions
        gridSizeY = CInt(Math.ceil((max_ele - min_ele) / 4f));
        Ymin = CInt(10 * Math.floor((min_ele-gridSizeY/2) / 10f));  // 4/5/06 modified
        
        gridSizeX = CInt(Math.ceil(max_hDist / 7f));
        plotScaleX = (float)GRID_HDIV / (float)gridSizeX;
        plotScaleY = (float)GRID_VDIV / (float)gridSizeY;

        float d1, d2, e1, e2, grade ;
        boolean gradeLimitFlag = false;
        d1 = myDB.elevationMarks[0].getDistance();
        e1 = myDB.elevationMarks[0].getElevation();
        for (i=1; i< myDB.elevationMarkCount; i++) {
            d2 = myDB.elevationMarks[i].getDistance();
            e2 = myDB.elevationMarks[i].getElevation();
            grade = (e2 - e1) / (d2 - d1);
            myDB.elevationMarks[i].setGrade(grade);
            e1 = e2;
            d1 = d2;
            if ( ((Math.abs(grade) > myDB.gradeLimit) || (Math.abs(grade) < myDB.minGrade) )
                && (gradeLimitFlag==false) ) {
                gradeLimitFlag = true;
                //sb.setStatusBarText(1, "Grade calculation from horizontal design exceed grade limit.") ;
            }
        }

        push2GradeLogBuffer(myDB.vConstructMarkCount);
        // ---------------------------------------------------
        float V1 ;
        if (myDB.myUnit == 1) {
            V1 = myDB.speedLimit * 5280f / 3600f; // ft/sec
            SSD = V1 * myDB.reactionTime + V1 * V1 / (2 * myDB.vehDecel);
            // ceil to 5
            SSD = CInt(Math.ceil(SSD / 5)) * 5;

            // compute crest curve length using AASHTO formula, assume Lm > SSD
            Lm_crestK = SSD*SSD / 2158f;              // US unit
            Lm_sagK = SSD*SSD / (400f + 3.5f * SSD);   // US unit
        } else if (myDB.myUnit == 2) {
            V1 = myDB.speedLimit * 1000f / 3600f; // m/sec
            SSD = V1 * myDB.reactionTime + V1 * V1 / (2f * myDB.vehDecel);
            // ceil to 5
            SSD = CInt(Math.ceil(SSD / 5)) * 5;

            // compute crest curve length using AASHTO formula, assume Lm > SSD
            Lm_crestK = SSD*SSD / 658f ;              // metric unit
            Lm_sagK = SSD*SSD / (120f + 3.5f * SSD);   // metric unit
        }
        URL url = getClass().getResource("elevationtext.png");
        eleImg = Toolkit.getDefaultToolkit().getImage(url);
        url = getClass().getResource("fillcuttext.png");
        fillcutImg = Toolkit.getDefaultToolkit().getImage(url);

        repaint();
        
        // =======================================================================
        // bring vertical design to top display thread
        // =====================================================================
        runThread0 = new Runnable() {
            public void run() {
                while (true) {
                    if (popMsgBox_flag){
                        popMessageBox1(msgBox_title, msgBox_message);
                        popMsgBox_flag = false ;
                //    } else if (popMsgBox_viewVRML_flag){
                //        popMessageBoxViewVRML1(msgBox_viewVRML_title, msgBox_viewVRML_message);
                //        popMsgBox_viewVRML_flag = false ;
                    } else if (setCurvelenEdit_flag) {
                        newstatus(3, " Modify Curve Length") ;
                        setCurvelenEdit_flag = false ;
                    } else if (setReport_flag){
                        newstatus(7, " Generate Report");
                        setReport_flag = false ;
                    } else if (noCurve2Edit_flag) {
                        if (!valid_vCurve && myDB.vCurveCount>0) {
                            // construction lines exist but no curves generated
                            popMessageBox("No Vertical Curve(s)", "Please generate vertical curve(s) first!") ;
                        } else {
                            popMessageBox("No Vertical Curve(s)", "Please design your vertical curve first!") ;
                        }
                        noCurve2Edit_flag = false ;
                    } else {
                        tSetReport.yield();
                        try {Thread.sleep(200) ;}
                        catch (InterruptedException ie) {} ;
                    }
                }
             }   // void run
        } ; // runThread 0
        tSetReport = new Thread(runThread0, "VertCurveDesign") ;
        tSetReport.start() ;
    }
    
    public void paint(Graphics gr) 
    {
        int i, x1=0, x2=0, y1=0, y2=0 ;
        Graphics2D g = (Graphics2D)gr ;
        Rectangle r = bounds();
        if(grid>0)
        {
            g.setColor(Color.black);
            g.setStroke(new BasicStroke(1));
            g.drawRect(5,5, 600, 430);  // border
            g.setColor(Color.blue);
            g.setStroke(new BasicStroke(2));
            g.drawRect(80,30, 480, 360);
            // graph background color
            g.setColor(new Color(255,227,206)); //255,227,206
            g.fillRect(82, 32, 476, 356);
            
            // grid line
            g.setColor(Color.lightGray);
            g.setStroke(new BasicStroke(1));
            
            for (i=1;i<8;i++){
                g.drawLine(80+i*GRID_HDIV, 32, 80+i*GRID_HDIV, 388);
            }
            for (i=1;i<6;i++){
                g.drawLine(82, 30+i*GRID_VDIV, 558, 30+i*GRID_VDIV);
            }
            
            // paint X, Y axes labels
            g.setColor(Color.blue);
            
            //System.out.println("gridSizeX="+CStr(gridSizeX));
            //System.out.println("gridSizeY="+CStr(gridSizeY));
            for (i=0;i<7;i++){  // Y axis
                if (viewMassDiagram) {
                    g.drawString(CStr(i*gridSizeY + Ymin), 30, 35+(6-i)*GRID_VDIV);
                } else {
                    g.drawString(CStr(i*gridSizeY + Ymin), 40, 35+(6-i)*GRID_VDIV);
                }
            }
            for (i=0;i<9; i++) {    // X axis
                g.drawString(CStr(i*gridSizeX), 70+i*GRID_HDIV, 410) ;
            }
            g.setColor(Color.black);
            g.setStroke(new BasicStroke(2));
            String unitStrY = "";
            String unitStr = "";
            if (myDB.myUnit==1){
                if (viewMassDiagram) {
                    unitStrY="yd^3";
                } else {
                    unitStrY="ft";
                }
                unitStr="ft";
            } else if (myDB.myUnit==2){
                if (viewMassDiagram) {
                    unitStrY="m^3";
                } else {
                    unitStrY="m";
                }
                unitStr="m";
            }
            g.drawString("("+unitStrY+")", 40, 20); // y axis unit label
            g.drawString("Distance ("+unitStr+")", 275, 430);
 
            // ===============================================
            if (viewElevationProfile) {
                // plot elevation data
                // Y Label
                g.drawImage(eleImg, 10,210-47,this) ;
                //plot out elevation vs. distance curve
                float grade ;
                for (i=0; i<myDB.elevationMarkCount; i++) {
                    if (i == 0) { 
                        // draw a starting mark only
                        x1 = transform2DrawX(myDB.elevationMarks[i].getDistance());
                        y1 = transform2DrawY(myDB.elevationMarks[i].getElevation());
                        g.setColor(Color.magenta);
                        g.setStroke(new BasicStroke(2));
                        g.drawPolygon(getDnTriangleShape(x1, y1, marker_size));
                    } else {
                        // draw a line & end mark
                        x2 = transform2DrawX(myDB.elevationMarks[i].getDistance());
                        y2 = transform2DrawY(myDB.elevationMarks[i].getElevation());
                        //draw same color on elevation landmarks, comment out on 2/23/06
                        grade = Math.abs(myDB.elevationMarks[i].getGrade());
                        g.setColor(Color.orange);
                        if ( (grade > myDB.gradeLimit) || (grade < myDB.minGrade) ) {
                            g.drawLine( x1, y1, x2, y2);
                        } else {
                            //g.DrawLine(blu_pen2, x1, y1, x2, y2)
                            g.drawLine( x1, y1, x2, y2);
                        }
                        g.setColor(Color.magenta);
                        g.drawPolygon( getDnTriangleShape(x2, y2, marker_size));
                        x1 = x2;
                        y1 = y2;
                    }
                } // for elevationMarkCount
                // legend
                g.drawPolygon(getDnTriangleShape(offsetX+20, offsetY+10, marker_size));
                g.setFont(myFont);
                g.drawString("Station Landmark", offsetX+30, offsetY+10+myFont.getSize() / 2);
                // title
                g.setColor(Color.blue);
                g.setStroke(new BasicStroke(2));
                g.drawString("Grade Design", 250, 20);
                
                float d1, d2, e1, e2, my_grade ;
                int dg_x1=0, dg_y1=0, dg_x2=0, dg_y2=0;
                // redraw constructed grade lines &/ design curves
                if (myDB.vConstructMarkCount > 0) {
                    for (i=0;i<myDB.vConstructMarkCount;i++) {
                        if (i == 0) { 
                            x1 = transform2DrawX(myDB.vConstructMarks[i].getDistance());
                            y1 = transform2DrawY(myDB.vConstructMarks[i].getElevation());
                            g.setColor(Color.green);
                            g.setStroke(new BasicStroke(2));
                            g.drawPolygon(getUpTriangleShape(x1, y1, marker_size));
                        } else {
                            x2 = transform2DrawX(myDB.vConstructMarks[i].getDistance());
                            y2 = transform2DrawY(myDB.vConstructMarks[i].getElevation());
                            my_grade = Math.abs(myDB.vConstructMarks[i].getGrade());
                            if ( (my_grade > myDB.gradeLimit) || (my_grade < myDB.minGrade)|| myDB.vConstructMarks[i].PVTnC_Overlap) { 
                                g.setColor(Color.red);
                                g.drawLine(x1, y1, x2, y2);
                            } else {
                                g.setColor(Color.darkGray);
                                g.drawLine(x1, y1, x2, y2);
                            }
                            g.setColor(Color.green);
                            g.drawPolygon(getUpTriangleShape(x2, y2, marker_size));
                            x1 = x2;
                            y1 = y2;
                        }
                        g.setColor(Color.green);
                        g.drawPolygon( getUpTriangleShape(offsetX+150, offsetY+10, marker_size));
                        g.setFont(myFont);
                        g.drawString("PVI", offsetX+160, offsetY+10 + myFont.getSize() / 2);

                        if(valid_vCurve && i > 0 ) {
                            if (i == 1) { 
                                // draw 1st line from start point to PVC0
                                dg_x1 = transform2DrawX(myDB.vConstructMarks[0].getDistance());
                                dg_y1 = transform2DrawY(myDB.vConstructMarks[0].getElevation());
                                dg_x2 = transform2DrawX(myDB.verticalCurves[0].getPVC());
                                dg_y2 = transform2DrawY(myDB.verticalCurves[0].getPVC_Elevation());
                                g.setColor(Color.blue);
                                g.setStroke(new BasicStroke(2));
                                g.drawLine(dg_x1, dg_y1, dg_x2, dg_y2);
                                dg_x1 = dg_x2;
                                dg_y1 = dg_y2;
                            } else { // i>0
                                // draw designed vertical curve
                                int steps, j ;
                                float step_size ;
                                // draw PVC mark
                                g.setColor(Color.red);
                                g.setStroke(new BasicStroke(2));
                                g.drawRect( dg_x1 - marker_size, dg_y1 - marker_size, CInt(2*marker_size), CInt(2*marker_size));
                                g.drawPolygon( getRectangleShape( offsetX+220, offsetY+10, marker_size));
                                g.drawString("PVC",  offsetX+230, offsetY+10+CInt(myFont.getSize() / 2));

                                steps = CInt(0.5f * myDB.verticalCurves[i-2].getCurveLen() * plotScaleX);
                                step_size = myDB.verticalCurves[i-2].getCurveLen() / steps;
                                for (j=1; j<=steps; j++) {
                                    dg_x2 = transform2DrawX(myDB.verticalCurves[i - 2].getPVC() + j * step_size);
                                    dg_y2 = transform2DrawY(myDB.verticalCurves[i - 2].getDX_Elevation(j * step_size));
                                    g.setColor(Color.blue);
                                    g.setStroke(new BasicStroke(2));
                                    g.drawLine(dg_x1, dg_y1, dg_x2, dg_y2);
                                    dg_x1 = dg_x2;
                                    dg_y1 = dg_y2;
                                }   //Next j
                                // draw PVT mark
                                g.setColor(Color.black);
                                g.setStroke(new BasicStroke(2));
                                g.drawPolygon(getDiamondShape(dg_x1, dg_y1, marker_size));
                                g.drawPolygon(getDiamondShape(offsetX+290, offsetY+10, marker_size));
                                g.drawString("PVT", offsetX+300, offsetY+10+CInt(myFont.getSize() / 2));

                                // draw tangent line to next curve, PVT(i-1) to PVC(i)
                                if (i == myDB.vConstructMarkCount - 1) { 
                                    // last condtruction point not PVI
                                    dg_x2 = transform2DrawX(myDB.vConstructMarks[i].getDistance());
                                    dg_y2 = transform2DrawY(myDB.vConstructMarks[i].getElevation());
                                } else {
                                    dg_x2 = transform2DrawX(myDB.verticalCurves[i - 1].getPVC());
                                    dg_y2 = transform2DrawY(myDB.verticalCurves[i - 1].getPVC_Elevation());
                                }
                                g.setColor(Color.blue);
                                g.setStroke(new BasicStroke(2));
                                g.drawLine(dg_x1, dg_y1, dg_x2, dg_y2); 
                                dg_x1 = dg_x2;
                                dg_y1 = dg_y2;
                            } // if i==1
                        } // if valid_vCurve
                    } //Next i
                }   // if vConstructMarkCount
            
                // draw temporary construction line
                if (isGradeConstructOn && me0 != null &&  me1 != null) { 
                    //g.DrawRectangle(grn_pen2, me0.X - 2, me0.Y - 2, 4, 4)
                    //g.DrawRectangle(grn_pen2, me1.X - 2, me1.Y - 2, 4, 4)
                    d1 = transform2Distance(me0.getX());
                    e1 = transform2Elevation(me0.getY());
                    d2 = transform2Distance(me1.getX());
                    e2 = transform2Elevation(me1.getY());

                    my_grade = (e2 - e1) / (d2 - d1);

                    //Dim currentPen As Pen
                    if ( (Math.abs(my_grade)>myDB.gradeLimit) || (Math.abs(my_grade)<myDB.minGrade) ){
                        g.setColor(Color.red);
                    } else {
                        g.setColor(Color.black);
                    }
                    g.setStroke(new BasicStroke(CInt(myDB.myRoadLaneSizes)));
                    g.drawLine(me0.getX(), me0.getY(), me1.getX(), me1.getY());
                 //   ToolTip1.SetToolTip(data_viewer, "grade= " & (Math.Round(my_grade * 1000) / 1000).ToString() & _
                 //   ", dist= " & Math.Round(d2).ToString & ", ele= " & Math.Round(e2).ToString)
                }
                // data button ============================
                if (valid_vCurve && !isGradeConstructOn) {
                    g.setColor(Color.yellow);

                    g.fillRect(570,9,31,14);  // data button
                    g.setStroke(new BasicStroke(1));
                    g.setColor(Color.black);
                    //g.drawRect(569,8,33,16);
                    g.drawLine(602,8,602,24) ;
                    g.drawLine(569,24,602,24) ;
                    g.setColor(Color.white);
                    g.drawLine(569,8,602,8) ;
                    g.drawLine(569,8,569,24) ;

                    g.setStroke(new BasicStroke(2));
                    g.setFont(myFont);
                    g.setColor(Color.blue);
                    g.drawString("Data", 575, 20);
                }
            // ==================================================
            } else if (viewMassDiagram) {    // view mass diagram
                // Y Label
                //g.drawImage(massdiagramImg, 10,210-47,this) ;
                // title
                
                g.setColor(Color.yellow);
                
                g.fillRect(570,9,31,14);  // data button
                g.setStroke(new BasicStroke(1));
                g.setColor(Color.black);
                //g.drawRect(569,8,33,16);
                g.drawLine(602,8,602,24) ;
                g.drawLine(569,24,602,24) ;
                g.setColor(Color.white);
                g.drawLine(569,8,602,8) ;
                g.drawLine(569,8,569,24) ;
                g.setStroke(new BasicStroke(2));
                g.setFont(myFont);
                g.setColor(Color.blue);
                g.drawString("Data", 575, 20);
                
                g.setStroke(new BasicStroke(2));
                g.drawString("Mass Diagram", 250, 20);
                
                // draw a zero line
                y1 = transform2DrawY(0);
                g.setColor(Color.green);
                g.setStroke(new BasicStroke(1));
                g.drawLine(offsetX+1, y1, offsetX+data_viewer_Width-1, y1);
                g.setColor(Color.blue);
                g.setStroke(new BasicStroke(2));
                g.drawString("0", offsetX+5, y1);

                // view mass diagram mode
                if (fillCutSteps > 0) { 
                    // calculation is ready
                    // view cut/fill profile
                    x1 = transform2DrawX(0);
                    y1 = transform2DrawY(accuMass[0]);
                    for (i = 1 ; i<fillCutSteps ; i++) {
                        x2 = transform2DrawX(i * ComputeStepSize);
                        y2 = transform2DrawY(accuMass[i]);
                        g.setColor(Color.red);
                        g.setStroke(new BasicStroke(2));
                        g.drawLine( x1, y1, x2, y2);
                        x1 = x2;
                        y1 = y2;
                    }
                }   // if fillCutSteps > 0
                
            // ==================================================
            } else {    // view cut fill profile
                
                g.setColor(Color.yellow);
                
                g.fillRect(570,9,31,14);  // data button
                g.setStroke(new BasicStroke(1));
                g.setColor(Color.black);
                //g.drawRect(569,8,33,16);
                g.drawLine(602,8,602,24) ;
                g.drawLine(569,24,602,24) ;
                g.setColor(Color.white);
                g.drawLine(569,8,602,8) ;
                g.drawLine(569,8,569,24) ;
                g.setStroke(new BasicStroke(2));
                g.setFont(myFont);
                g.setColor(Color.blue);
                g.drawString("Data", 575, 20);
                
                // Y Label
                g.drawImage(fillcutImg, 10,210-47,this) ;
                // title
                g.setFont(myFont);
                g.setColor(Color.blue);
                g.setStroke(new BasicStroke(2));
                g.drawString("Fill-Cut Profile", 250, 20);
                // draw a zero line
                y1 = transform2DrawY(0);
                g.setColor(Color.green);
                g.setStroke(new BasicStroke(1));
                g.drawLine(offsetX+1, y1, offsetX+data_viewer_Width-1, y1);
                g.setColor(Color.blue);
                g.setStroke(new BasicStroke(2));
                g.drawString("0", offsetX+5, y1);
                // max fill/cut
                y1 = transform2DrawY(myDB.maxFill);
                g.setColor(Color.red);
                g.setStroke(new BasicStroke(1));
                g.drawLine(offsetX+1, y1, offsetX+data_viewer_Width-1, y1);
                y1 = transform2DrawY(-myDB.maxCut);
                g.drawLine(offsetX+1, y1, offsetX+data_viewer_Width-1, y1);
                // draw legend
                g.drawLine( offsetX+20, offsetY+10, offsetX+40, offsetY+10);
                g.setColor(Color.red);
                g.drawString("Max Cut / Fill Limit", offsetX+55, offsetY+10 + myFont.getSize() / 2);
                // draw legend
                g.setColor(Color.blue);
                g.setStroke(new BasicStroke(2));
                g.drawLine( offsetX+220, offsetY+10, offsetX+240, offsetY+10);
                g.drawString("Cut / Fill Curve", offsetX+255, offsetY+10 + myFont.getSize() / 2);

                // view cut/fill profile mode, // viewElevationProfile = false
                if (fillCutSteps > 0) { 
                    // calculation is ready
                    // view cut/fill profile
                    x1 = transform2DrawX(0);
                    y1 = transform2DrawY(CutandFill[0]);
                    for (i = 1 ; i<fillCutSteps ; i++) {
                        x2 = transform2DrawX(i * ComputeStepSize);
                        y2 = transform2DrawY(CutandFill[i]);
                        g.setColor(Color.blue);
                        g.setStroke(new BasicStroke(2));
                        g.drawLine( x1, y1, x2, y2);
                        x1 = x2;
                        y1 = y2;
                    }
                }   // if fillCutSteps > 0
            }   // end if elevation or mass diagram or cut/fill profile
        /*
            g.setColor(new Color(113,255,113)); // light green

            for(int i=grid;i<r.height;i+=grid)
                g.drawLine(0,i,r.width,i);
            for(int i=grid;i<r.width;i+=grid)
                g.drawLine(i,0,i,r.height);

            g.setColor(new Color(208,208,208));

            for(int i=grid*10;i<r.height;i+=grid*10)
                g.drawLine(0,i,r.width,i);
            for(int i=grid*10;i<r.width;i+=grid*10)
                g.drawLine(i,0,i,r.height);
             */
        }   // draw grid
   }    
public void newstatus(int index, String str)
    {
        toolbarIndex = index ;
        sb.setStatusBarText(0, str) ;

        switch (toolbarIndex) {
            case 0: // construct on
                if (!viewElevationProfile) {
                    viewElevation();
                }
                isGradeConstructOn = true;
                //sb.setStatusBarText(0, "Grade construction tool ON") ; //Status: 
                //edit_undo.Enabled = True
                //edit_redo.Enabled = True
                break ;
            case 1: // construct off
                if (!viewElevationProfile) {
                    viewElevation();
                }
                isGradeConstructOn = false;
                //sb.setStatusBarText(0, "Grade construction tool OFF"); //Status: 
                //edit_undo.Enabled = False
                //edit_redo.Enabled = False
                break ;
            case 2: // calc PVI
                if (!viewElevationProfile) {
                    viewElevation();
                }
                vertAlign();
                //sb.setStatusBarText(0, str) ; //Status: 
                break ;
            case 3: // modify vertical curve
                if (!viewElevationProfile) {
                    viewElevation();
                }
                popVertCurveLenEdit();
                break ;
            case 4: // elevation profile
                viewElevation();
                break ;
            case 5: // fill and cut profile
                viewFillCut();
                break ;
            case 6: // mass diagram
                view_MassDiagram(); 
                break ;
            case 7: // report
                popReport();
                //sb.setStatusBarText(0, str) ; //Status: 
                break ;
            case 8: // anomation
                popAnimation3D();
                //sb.setStatusBarText(0, str) ; //Status: 
                break ;
        }
	repaint();
    }

    // generate 3D animation model & display it on web browser
    public void popAnimation3D() {
        if (valid_vCurve) {
            // generate 3D VRML model
            createVRMLFile();
            try
            {
                String osinfo = System.getProperty("os.name");
                String osarch = System.getProperty("os.arch");
            //System.out.println(osinfo+","+osarch);
                String filename="" ;
                if (osinfo.indexOf("Windows")>=0) {
                //    filename = "c:\\roaddesign.html" ;  // "C:\\Documents and Settings\\All Users\\Desktop\\roaddesign.html"
                    String username = System.getProperty("user.name");
                    filename =  "C:\\Documents and Settings\\"+username+"\\Desktop\\roaddesign.html" ;
                    boolean exists = (new File(filename)).exists();
                    if (!exists) { 
                        // if desktop is not available, use default directory
                        filename = "c:\\roaddesign.html" ; 
                    }
                } else {    //if (osinfo.indexOf("Linux")>=0){
                    filename = "roaddesign.html" ;
                }
                AppletContext ac = myApplet.getAppletContext(); 
                File file = new File(filename);
                URL u = file.toURL();   //new URL("file:/c:/roaddesign.html");
//          System.out.println(myApplet.toString() + ", url="+u.toString());
                ac.showDocument(u, "_blank");
                //_blank to open page in new window		
            popMessageBox("3D Animation", 
                "*** If 3D animation window doesn't pop up \n" + 
                "automatically, please open roaddesign.html file\n" +
                "manually.\n" +
                "Windows: on PC Destop or in C:\\ directory, \n" + 
                "    Mac: in HD directory, \n" +
                "  Linux: in root directory"
                 );
            }
            catch (Exception e){
                    //do nothing
                System.out.println(e.toString());
                sb.setStatusBarText(1, e.toString()) ; //"Error: "+

            } // end of try
        } else {
            // invalid vertical curve
                popMessageBox("Vertical Curve Design","No construction lines. \nPlease use the construction button to \ncreate vertical curve construction lines first!");
        }    
    }
    /** Creates a new instance of vDrawArea */
    public vDrawArea() {
    }
    
    public void mouseClicked(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public void mouseDragged(java.awt.event.MouseEvent mouseEvent) {
        if (PVI_index >= 0) {
            // update PVI end point
            myDB.vConstructMarks[PVI_index].setDistance(transform2Distance(mouseEvent.getX()));
            myDB.vConstructMarks[PVI_index].setElevation(transform2Elevation(mouseEvent.getY()));
            
            // 10/13/06 added
            myDB.vConstructMarks[PVI_index].PVTnC_Overlap = false ;
            if ( (PVI_index+1<myDB.MAX_MARKERS) && (myDB.vConstructMarks[PVI_index+1] != null) ) {
                myDB.vConstructMarks[PVI_index+1].PVTnC_Overlap = false ;
            }
            //myDB.vConstructMarks[PVI_index+2].PVTnC_Overlap = false ;

            repaint();
        //System.out.println("here");
        }
    }
    
    public void mouseEntered(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public void mouseExited(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public void mouseMoved(java.awt.event.MouseEvent mouseEvent) {
        if (isGradeConstructOn) { 
            //Me.Cursor.Current = System.Windows.Forms.Cursors.Cross
        } else {
            //Me.Cursor.Current = System.Windows.Forms.Cursors.Arrow
        }
        me1 = mouseEvent;
        repaint();
        //update();
    }
    
    public void mousePressed(java.awt.event.MouseEvent mouseEvent) {
        if ((isGradeConstructOn==false) && (viewElevationProfile==true)) { 
            // not in cut/fill construction mode
            int i, x0=0, y0=0, x1, y1, indexFound, segFound ;
            float cosine, sf ;
            mPointF v1, v2 ;
            indexFound = -1;
            segFound = -1;
            for (i=0;i<myDB.elevationMarkCount;i++) {
                x1 = transform2DrawX(myDB.elevationMarks[i].getDistance());
                y1 = transform2DrawY(myDB.elevationMarks[i].getElevation());
                if (i == 0) { 
                    x0 = x1;
                    y0 = y1;
                }
                if (distanceOf(new mPointF(x1, y1), new mPointF(mouseEvent.getX(), mouseEvent.getY())) <= 2 * marker_size) { 
                    indexFound = i;
                    break;

                } else if (i > 0) { 
                    // check if segment selected
                    v1 = vector(new mPointF(x0, y0), new mPointF(x1, y1));
                    v2 = vector(new mPointF(x0, y0), new mPointF(mouseEvent.getX(), mouseEvent.getY()));
                    cosine = getCosTheta(v1, v2);
                    sf = Math.abs(v2.X / v1.X);
                    if ((cosine >= 0.99f) && (sf <= 1)) { // Then // < 1 degree, item selected
                        sb.setStatusBarText(0, "(" + CStr(i + 1) + ") grade= " + CStr(myDB.elevationMarks[i].getGrade()));
                        break;
                    }
                    x0 = x1;
                    y0 = y1;

                }
            } // for i
            if (indexFound >= 0) { 
                sb.setStatusBarText(0, "(" + CStr(indexFound + 1) + ") Dist=" + CStr(myDB.elevationMarks[indexFound].getDistance()) + ", Ele=" + CStr(myDB.elevationMarks[indexFound].getElevation()));
            } else {
                for (i=0;i<myDB.vCurveCount;i++) {
                    x1 = transform2DrawX(myDB.verticalCurves[i].getPVC());
                    y1 = transform2DrawY(myDB.verticalCurves[i].getPVC_Elevation());
                    if (distanceOf(new mPointF(x1, y1), new mPointF(mouseEvent.getX(), mouseEvent.getY())) <= 2 * marker_size) { 
                        indexFound = i;
                        sb.setStatusBarText(0, "(" + CStr(indexFound + 1) + ") PVC=" + CStr(myDB.verticalCurves[indexFound].getPVC())+ ", Ele=" + CStr(myDB.verticalCurves[indexFound].getPVC_Elevation()));
                        break;
                    }
                    x1 = transform2DrawX(myDB.verticalCurves[i].getPVT());
                    y1 = transform2DrawY(myDB.verticalCurves[i].getPVT_Elevation());
                    if (distanceOf(new mPointF(x1, y1), new mPointF(mouseEvent.getX(), mouseEvent.getY())) <= 2 * marker_size) { 
                        indexFound = i;
                        sb.setStatusBarText(0, "(" + CStr(indexFound + 1)+ ") PVT=" + CStr(myDB.verticalCurves[indexFound].getPVT())+ ", Ele=" + CStr(myDB.verticalCurves[indexFound].getPVT_Elevation()));
                        break;
                    }
                    x1 = transform2DrawX(myDB.verticalCurves[i].getPVI());
                    y1 = transform2DrawY(myDB.verticalCurves[i].getPVI_e());
                    if (distanceOf(new mPointF(x1, y1), new mPointF(mouseEvent.getX(), mouseEvent.getY())) <= 2 * marker_size) { 
                        indexFound = i;
                        sb.setStatusBarText(0, "(" + CStr(indexFound + 1)+ ") PVI=" + CStr(myDB.verticalCurves[indexFound].getPVI())+ ", Ele=" + CStr(myDB.verticalCurves[indexFound].getPVI_e()));
                        break;
                    }

                }   //Next i

            }   // if indexFound >= 0
            // 4/4/06 added
            if (valid_vCurve == false) { 
                // no vertical curve was generated yet, 4/4/06 added
                for (i=0; i<myDB.vConstructMarkCount;i++) {
                    x1 = transform2DrawX(myDB.vConstructMarks[i].getDistance());
                    y1 = transform2DrawY(myDB.vConstructMarks[i].getElevation());
                    if (distanceOf(new mPointF(x1, y1), new mPointF(mouseEvent.getX(), mouseEvent.getY())) <= 2 * marker_size) { 
                        // found vConstructMark end point, 4/4/06
                        PVI_index = i;
              //System.out.println("Found "+PVI_index);
                        break ;
                    }   //End If
                }   //Next i
            }   // valid_vCurve
            
        } //if ((isGradeConstructOn==false) && (viewElevationProfile==true))
   }
    
    public void mouseReleased(java.awt.event.MouseEvent mouseEvent) {
        long eventTime = System.currentTimeMillis();
	long timeDiff;
	
	timeDiff = eventTime - mouseUpClickTime;

        if (timeDiff<300L) {
            // double click
            // Display the event time information.
            //System.out.println ("Current event :" + eventTime +
		//    ":   Previous event :" + mouseUpClickTime +
		//    ":   Difference :" + timeDiff + ":");
            isGradeConstructOn = false;
            sb.setStatusBarText(0, "Grade construction tool OFF"); //Status: 
        } else {
            mouseUp(mouseEvent);
        }
	mouseUpClickTime = eventTime;
    }

    public void mouseUp(java.awt.event.MouseEvent mouseEvent) {   
        // added 4/4/06
        if (PVI_index > 0) { 
            // update previous grade info
            float d1, d2, e1, e2, my_grade ;
            d1 = myDB.vConstructMarks[PVI_index - 1].getDistance();
            e1 = myDB.vConstructMarks[PVI_index - 1].getElevation();
            d2 = myDB.vConstructMarks[PVI_index].getDistance();
            e2 = myDB.vConstructMarks[PVI_index].getElevation();
            my_grade = (e2 - e1) / (d2 - d1);
            myDB.vConstructMarks[PVI_index].setGrade(my_grade);
            // update next grade
            if (PVI_index < myDB.vConstructMarkCount - 1) {
                d1 = myDB.vConstructMarks[PVI_index + 1].getDistance();
                e1 = myDB.vConstructMarks[PVI_index + 1].getElevation();
                my_grade = (e1 - e2) / (d1 - d2);
                myDB.vConstructMarks[PVI_index + 1].setGrade(my_grade);
            }

            //updateVCurve(PVI_index);  // comment out 2/28/07
            if (PVI_index > 1) {    // update previous curve
                updateVCurve(PVI_index - 1);
            }
            if (PVI_index < myDB.vConstructMarkCount - 2) {
                // update next curve
                updateVCurve(PVI_index + 1);
            }
            PVI_index = -1;  // reset PVI_index
        }   // PVI_index>0
        
        // 10/10/06 added
        // ========================
        // view mass diagram or view fill cut
        int mx, my ;
        mx = mouseEvent.getX() ;
        my = mouseEvent.getY() ;
        if (mx>=570 && mx <=600 && my>=9 && my<=23) {
            if (viewElevationProfile == false) {
                // click Data
                if (viewMassDiagram) {
                    //saveMassDiagram() ;
                    popMassData() ;
                    //popMessageBox("Save Mass Diagram", "Save Mass Diagram") ;
                } else {
                    // view fill amd cut
                    //saveCutAndFill() ;
                    popCutAndFillData() ;
                    //popMessageBox("Save Fill Cut Data", "Save Fill Cut Data") ;
                }
            } else {
                // designed elevation profile
                popDesignedElevationData() ;
            }
        }   // if click data box
        
        // ========================
        if (isGradeConstructOn) { //
            if ((me0==null)&&(myDB.vConstructMarkCount > 1)) { 
                me0 = mouseEvent;
                popVertCurveExists("Vertical Curve Design","Current vertical curve design exists.\nStart new design?");
               
            } else if (mouseEvent.getX()>=82 && mouseEvent.getX()<= 558
                && mouseEvent.getY()>=32 && mouseEvent.getY()<= 388 ) {
                    
            // cut/fill construction mode
            myDB.vConstructMarks[myDB.vConstructMarkCount] = new MarkerDB();
            myDB.vConstructMarks[myDB.vConstructMarkCount].setDistance(transform2Distance(mouseEvent.getX()));
            myDB.vConstructMarks[myDB.vConstructMarkCount].setElevation(transform2Elevation(mouseEvent.getY()));
            if (myDB.vConstructMarkCount > 0) { 
                // save grade info 
                float d1, d2, e1, e2, my_grade ;
                d1 = transform2Distance(me0.getX());
                e1 = transform2Elevation(me0.getY());
                d2 = transform2Distance(mouseEvent.getX());
                e2 = transform2Elevation(mouseEvent.getY());

                my_grade = (e2 - e1) / (d2 - d1);
                myDB.vConstructMarks[myDB.vConstructMarkCount].setGrade(my_grade);
                if (myDB.vConstructMarkCount > 1 ) {
                    // initialize vertical curve element
                    myDB.verticalCurves[myDB.vCurveCount] = new VCurve(myDB.minVCurveLen);
                    // calculate length of crest/sag curves
                    float G1, G2, grade_diff_A ;
                    G1 = 100f * myDB.vConstructMarks[myDB.vConstructMarkCount - 1].getGrade();
                    G2 = 100f * myDB.vConstructMarks[myDB.vConstructMarkCount].getGrade();
                    if ( (G1 != 0) && (G2 != 0) ) { 
                        grade_diff_A = G1 - G2;
                        if (grade_diff_A > 0) { 
                            myDB.verticalCurves[myDB.vCurveCount].setCurveLen(Lm_crestK * grade_diff_A, SSD);
                        } else if (grade_diff_A < 0) { 
                            myDB.verticalCurves[myDB.vCurveCount].setCurveLen(Lm_sagK * Math.abs(grade_diff_A), SSD);
                        } else {
                            // no grade changes, no vertical alignment
                            myDB.verticalCurves[myDB.vCurveCount].setCurveLen(0, SSD);
                        }
                    } else {
                        //both grades are 0, no vertical alignment
                        myDB.verticalCurves[myDB.vCurveCount].setCurveLen(0, SSD);
                    }   // if G1, G2
                    myDB.vCurveCount += 1;
                }   // if myDB.vConstructMarkCount > 1
                repaint();
                //data_viewer.Invalidate()
             }   //if myDB.vConstructMarkCount > 0
          
                myDB.vConstructMarkCount += 1;
                me0 = mouseEvent;
                // save # of data in log buffer
                push2GradeLogBuffer(myDB.vConstructMarkCount);

            }   // if me != null
        } // if isGradeConstructOn
    }
    
    public void updateVCurve(int index) {
        // calculate length of crest/sag curves
        float G1, G2, grade_diff_A ;
        G1 = 100 * myDB.vConstructMarks[index - 1].getGrade();
        G2 = 100 * myDB.vConstructMarks[index].getGrade();
        if (G1 != 0 && G2 != 0) { 
            grade_diff_A = G1 - G2;
            if (grade_diff_A > 0 ) {
                //System.out.println("1 index="+index) ;
                myDB.verticalCurves[index-1].setCurveLen(Lm_crestK * grade_diff_A, SSD);
            } else if (grade_diff_A < 0) { 
                //System.out.println("2 index="+index) ;
                myDB.verticalCurves[index-1].setCurveLen(Lm_sagK * Math.abs(grade_diff_A), SSD);
            } else {
                // no grade changes, no vertical alignment
                myDB.verticalCurves[index-1].setCurveLen(0, SSD);
            }
        } else {
            //both grades are 0, no vertical alignment
            myDB.verticalCurves[index-1].setCurveLen(0, SSD) ;
        }

    }   // update VCurve

    public float checkVCurveLen(int index, float L) {
        // calculate length of crest/sag curves
        float Lm = 0f ;
        float G1, G2, grade_diff_A ;
        G1 = 100 * myDB.vConstructMarks[index - 1].getGrade();
        G2 = 100 * myDB.vConstructMarks[index].getGrade();
        if (G1 != 0 && G2 != 0) { 
            grade_diff_A = G1 - G2;
            if (grade_diff_A > 0 ) {
                Lm = myDB.verticalCurves[index - 1].checkCrestLm(myDB.myUnit, grade_diff_A, SSD, L); 
            } else if (grade_diff_A < 0) { 
                Lm = myDB.verticalCurves[index - 1].checkSagLm(myDB.myUnit, Math.abs(grade_diff_A), SSD, L); 
            } else {
                // no grade changes, no vertical alignment
                Lm = myDB.minVCurveLen ;
            }
        } else {
            //both grades are 0, no vertical alignment
            Lm = myDB.minVCurveLen ;
        }
        return Lm ;
    }   // check min vertical Curve length
    
    public float calculateArcLength(int idx) {
        mPointF curve_ctr ;
        int segIndex = myDB.elevationMarks[idx].getParentIndex();
        //System.out.println("idx="+idx+", parent index="+segIndex) ;
        float myRadius = myDB.hRoadData[segIndex].getRadius();
        double dist ;
        if (myRadius <= 0f ) {
            // wrong landmark DB, pop error message
            String str = new Integer(idx+1).toString();
            popMessageBox("Station Elevation Data","Invalid station elevation data (" + str + "). Please check station elevation data.");
            return -999;
        } else {
            curve_ctr = myDB.hRoadData[segIndex].getPoint1();
            double theta ;
            mPointF vec1, vec2 ;
            vec1 = vector(curve_ctr, myDB.elevationMarks[idx].getLocation());
            vec2 = vector(curve_ctr, myDB.elevationMarks[idx-1].getLocation());
            theta = Math.acos(getCosTheta(vec1, vec2));
            dist = myRadius * theta ;// curve distance
            return new Float(dist).floatValue();
        }
    } // calculateArcLength
    
    /** Pop up a window to display message */   
    public void popMessageBox(String caption, String message) {
        msgBox_title = caption ;
        msgBox_message = message ;
        popMsgBox_flag = true ;
    }
    private void popMessageBox1(String caption, String message) {
        // open a frame
        frame_msgbox = new myWindow(caption) ;
        frame_msgbox.setLocation(500,5) ;
        frame_msgbox.setSize(300,150) ;
        frame_msgbox.validate() ;
        frame_msgbox.setVisible(true) ;
        frame_msgbox.setResizable(false);
        //frame_msgbox.show() ;
/*
        ActionListener frame_msgbox_ok_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                
                frame_msgbox.dispose() ;
            }
        } ;
*/
        frame_msgbox.setLayout(new BorderLayout(1,1)) ;
        TextArea myTitle = new TextArea(message, 3, 60) ;
        myTitle.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
        myTitle.setForeground(new Color(0,0,218)) ;
        frame_msgbox.setBackground(new Color(200, 200, 200)) ;
        frame_msgbox.add("Center",myTitle) ;
        
        //Button btn_ok = new Button(" OK ") ;
        //frame_msgbox.add("South",btn_ok) ;
        //btn_ok.addActionListener(frame_msgbox_ok_listener) ;
   //     frame_msgbox.invalidate();
        frame_msgbox.show() ;
        frame_msgbox.toFront() ;
    } // popMessageBox1    
 
    /** Pop up a window to display message */   
/*
    public void popMessageBoxViewVRML(String caption, String message) {
        msgBox_viewVRML_title = caption ;
        msgBox_viewVRML_message = message ;
        popMsgBox_viewVRML_flag = true ;
    }
    
    private void popMessageBoxViewVRML1(String caption, String message) {
        // open a frame
        frame_msgbox_viewVRML = new myWindow(caption) ;
        frame_msgbox_viewVRML.setLocation(500,5) ;
        frame_msgbox_viewVRML.setSize(300,200) ;
        frame_msgbox_viewVRML.validate() ;
        frame_msgbox_viewVRML.setVisible(true) ;
        frame_msgbox_viewVRML.setResizable(false);
        //frame_msgbox_viewVRML.show() ;

        ActionListener frame_msgbox_ok_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                // view VRML file
                 try
                {
                    AppletContext a = myApplet.getAppletContext(); 
                    URL u = new URL("file:/c:/roaddesign.html"); 
                    a.showDocument(u,"_blank");
                    //_blank to open page in new window		
                }
                catch (Exception e){
                        //do nothing
                    sb.setStatusBarText(1, "Error: Manual file "+e.toString()) ;
                }   // try
                frame_msgbox_viewVRML.dispose() ;
            }
        } ;
        ActionListener frame_msgbox_close_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_msgbox_viewVRML.dispose() ;
            }
        } ;
        
        frame_msgbox_viewVRML.setLayout(new GridBagLayout()) ;
        GridBagConstraints c = new GridBagConstraints() ;
        c.fill = GridBagConstraints.BOTH ; // component grows in both directions
        c.weightx = 1.0 ; c.weighty = 1.0 ;

        c.gridx = 0 ; c.gridy = 0; c.gridwidth = 4 ; c.gridheight = 4 ;
        c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides

        
        TextArea myTitle = new TextArea(message, 3, 60) ;
        myTitle.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
        myTitle.setForeground(new Color(0,0,218)) ;
        frame_msgbox_viewVRML.setBackground(new Color(200, 200, 200)) ;
        frame_msgbox_viewVRML.add(myTitle, c) ;
        
        c.gridx = 0 ; c.gridy = 4; c.gridwidth = 1 ; c.gridheight = 1 ;
        frame_msgbox_viewVRML.add(new Label(" "),c) ;
        c.gridx = 1 ; c.gridy = 4; c.gridwidth = 1 ; c.gridheight = 1 ;
        frame_msgbox_viewVRML.add(new Label(" "),c) ;
        c.gridx = 2 ; c.gridy = 4; c.gridwidth = 1 ; c.gridheight = 1 ;
        Button btn_ok = new Button(" View ") ;
        frame_msgbox_viewVRML.add(btn_ok, c) ;
        btn_ok.addActionListener(frame_msgbox_ok_listener) ;
        Button btn_close = new Button(" Close ") ;
        c.gridx = 3 ; c.gridy = 4; c.gridwidth = 1 ; c.gridheight = 1 ;
        frame_msgbox_viewVRML.add(btn_close, c) ;
        btn_close.addActionListener(frame_msgbox_close_listener) ;
        
   //     frame_msgbox.invalidate();
        frame_msgbox_viewVRML.show() ;
        frame_msgbox_viewVRML.toFront() ;
    } // popMessageBoxViewVRML 
*/
    public mPointF vector( mPointF p1, mPointF p2) {
        mPointF _vec ;
        _vec = new mPointF(p2.X - p1.X, p2.Y - p1.Y);
        return _vec;
    }
    public float vectorLen(mPointF vec ) {
        float dist;
        dist = new Float(Math.sqrt(vec.X * vec.X + vec.Y * vec.Y)).floatValue();
        return dist;
    }

    public float getCosTheta(mPointF v1, mPointF v2) {
        double cos_theta, dot, v1_len, v2_len ;
        dot = v1.X * v2.X + v1.Y * v2.Y;
        v1_len = Math.sqrt(v1.X*v1.X + v1.Y*v1.Y);
        v2_len = Math.sqrt(v2.X*v2.X + v2.Y*v2.Y);
        cos_theta = dot / (v1_len * v2_len);
        return new Float(cos_theta).floatValue();
    }
    public float distanceOf(mPointF p1 , mPointF p2 ) {
        float dist, dx, dy ;
        dx = p1.X - p2.X;
        dy = p1.Y - p2.Y;
        dist = new Double(Math.sqrt(dx*dx + dy*dy)).floatValue();
        return dist;
    }
    public int CInt(double val){
        return new Double(val).intValue();
    }
    public int CInt(float val){
        return new Float(val).intValue();
    }
    public int CInt(String str){
        return new Float(str).intValue();
    }
    public float CFloat(String str){
        return new Float(str).floatValue();
    }   
    public String CStr(float val){
        return new Float(val).toString();
    }   
    public String CStr(int val){
        return new Integer(val).toString();
    }   
    public void push2GradeLogBuffer(int _myLandmarkCount){

        // save # of data into log buffer
        if (gradeLogIndex == gradeLogBuffer.length - 1) { 
            // buffer fulled
            // shift forward by 1
            int i ;
            for (i=0; i<gradeLogIndex; i++) {
                gradeLogBuffer[i] = gradeLogBuffer[i + 1];
            }
            gradeLogBuffer[gradeLogIndex] = _myLandmarkCount;
        } else {
            gradeLogIndex += 1;
            gradeLogBuffer[gradeLogIndex] = _myLandmarkCount;
        }
    }

    public int popGradeLogBuffer() {
        // pop the current # of landmark data from log buffer
        if (gradeLogIndex > 0) {
            gradeLogIndex -= 1;
            return gradeLogBuffer[gradeLogIndex];
        } else {
            return -99;
        }
    }  // popGradeLogBuffer
    
    private int transform2DrawY(float value) {
            return offsetY+data_viewer_Height - CInt((value - Ymin) * plotScaleY);
    }

    private int transform2DrawX(float value) {
        return offsetX+CInt(value*plotScaleX);
    }
    // transform from screen clicked y pixel to elevation in ft/m
    private float transform2Elevation(int value) {
        return Ymin + (offsetY+data_viewer_Height - value) / plotScaleY;
    }
    // transform from screen clicked x pixel to distance // ft/m
    private float transform2Distance(int value) {
        return ((float)(value-offsetX) / plotScaleX);
    }
    public Polygon getDiamondShape(int x , int y , int size ) {
        Polygon myPolygon = new Polygon();
        
        myPolygon.addPoint(x + size, y) ;
        myPolygon.addPoint(x, y + size) ;
        myPolygon.addPoint(x - size, y);
        myPolygon.addPoint(x, y - size);
        myPolygon.addPoint(x + size, y) ;
        return myPolygon;
    }
    public Polygon getRectangleShape(int x , int y , int size ) { 
        Polygon myPolygon = new Polygon();
        myPolygon.addPoint(x + size, y + size);
        myPolygon.addPoint(x - size, y + size);
        myPolygon.addPoint(x - size, y - size);
        myPolygon.addPoint(x + size, y - size);
        myPolygon.addPoint(x + size, y + size);
        return myPolygon;
    }
    public Polygon getDnTriangleShape(int x , int y , int size ) { 
        Polygon myPolygon = new Polygon();
        myPolygon.addPoint(x, y + size);
        myPolygon.addPoint(x - size, y - size);
        myPolygon.addPoint(x + size, y - size);
        myPolygon.addPoint(x, y + size);
        return myPolygon;
    }
    public Polygon getUpTriangleShape(int x , int y , int size ) { 
         Polygon myPolygon = new Polygon();
        myPolygon.addPoint(x + size, y + size);
        myPolygon.addPoint(x - size, y + size);
        myPolygon.addPoint(x, y - size);
        myPolygon.addPoint(x + size, y + size);
        return myPolygon;
    }
    public void popVertCurveExists(String caption, String message) {
        // open a frame
        frame_msgboxYesNo = new myWindow(caption) ;
        frame_msgboxYesNo.setLocation(400,200) ;
        frame_msgboxYesNo.setSize(300,150) ;
        frame_msgboxYesNo.validate() ;
        frame_msgboxYesNo.setVisible(true) ;
        frame_msgboxYesNo.setResizable(false) ;

        ActionListener frame_msgbox_yes_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                myDB.vConstructMarkCount = 1;
                valid_vCurve = false;
                myDB.vCurveCount = 0;
                frame_msgboxYesNo.dispose() ;
                me0=null;
                repaint();
            }
        } ;
        ActionListener frame_msgbox_no_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_msgboxYesNo.dispose() ;
            }
        } ;

        frame_msgboxYesNo.setLayout(new GridBagLayout()) ;
        // Create a constrains object, and specify default values
        GridBagConstraints c = new GridBagConstraints() ;
        c.fill = GridBagConstraints.BOTH ; // component grows in both directions
        c.weightx = 1.0 ; c.weighty = 1.0 ;

        c.gridx = 0 ; c.gridy = 0; c.gridwidth = 2 ; c.gridheight = 1 ;
        c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
        TextArea myMsg = new TextArea(message,3,60) ;
        //myMsg.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
        //myMsg.setForeground(new Color(0,0,218)) ;
        frame_msgboxYesNo.setBackground(new Color(200, 200, 200)) ;
        frame_msgboxYesNo.add(myMsg,c) ;
        c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
        Button btn_ok = new Button(" Yes ") ;
        frame_msgboxYesNo.add(btn_ok, c) ;
        btn_ok.addActionListener(frame_msgbox_yes_listener) ;
        c.gridx = 1 ; c.gridy = 1;
        Button btn_no = new Button(" No ") ;
        frame_msgboxYesNo.add(btn_no, c) ;
        btn_no.addActionListener(frame_msgbox_no_listener) ;

        frame_msgboxYesNo.invalidate();
        frame_msgboxYesNo.show() ;
        frame_msgboxYesNo.toFront() ;
 
    } // popClearLandMark
    
    /** Pop vertical curve edit screen if curves exist */    
    public void popVertCurveLenEdit() { // 10/9/06
        if (myDB.vCurveCount>0 && valid_vCurve) {
            // open a frame
            frame_editVertCurveLen = new myWindow("Edit Vertical Curve") ;
            frame_editVertCurveLen.setLocation(250,40) ;
            frame_editVertCurveLen.setSize(280,180) ;
            frame_editVertCurveLen.validate() ;
            frame_editVertCurveLen.setVisible(true) ;
            frame_editVertCurveLen.setResizable(false) ;

            ActionListener frame_editVertCurveLen_save_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    int index = listVertCurves.getSelectedIndex();
                    float val = new Float(txtCurveLen.getText()).floatValue() ;
                    if (val<myDB.minVCurveLen) {
                        String unitStr = "" ;
                        if (myDB.myUnit==1) {
                            unitStr = " (ft)";
                        } else if (myDB.myUnit==2) {
                            unitStr = " (m)";
                        } 
                        popMessageBox("Vertical Curve Length", "Value less than minimum vertical curve length, \n"+CStr(myDB.minVCurveLen)+unitStr) ;
                        txtCurveLen.setText(CStr(myDB.verticalCurves[index].getCurveLen())) ;
                    } else {
                        myDB.verticalCurves[index].setCurveLen(val, SSD) ;
                        vertAlign();
                    }
                    //frame_editVertCurveLen.dispose() ;
                  
                }
            } ;
            // check Lm based on AASHTO green book, chapter 3
            ActionListener frame_editVertCurveLen_check_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    int index = listVertCurves.getSelectedIndex();
                    float val = new Float(txtCurveLen.getText()).floatValue() ;
                    float Lm = checkVCurveLen(index+1, val) ;
                    String unitStr = "" ;
                    if (myDB.myUnit==1) {
                        unitStr = " (ft)";
                    } else if (myDB.myUnit==2) {
                        unitStr = " (m)";
                    } 
                    popMessageBox("Vertical Curve Length", "Minimum vertical curve length,\nLm = "+CStr(Lm)+unitStr) ;                  
                }
            } ;
            ActionListener frame_editVertCurveLen_done_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    // update fill cut profile after modifying vertical curve
                    fillCutSteps = calcFillCutProfile() ;   // 10/13/06 added
                    frame_editVertCurveLen.dispose() ;
                    //frame_report.dispose() ;
                }
            } ;
            ItemListener curveIndex_listener = new ItemListener() {
                public void itemStateChanged(ItemEvent ie) {
                    int index = listVertCurves.getSelectedIndex();
                    txtCurveLen.setText(CStr(myDB.verticalCurves[index].getCurveLen())) ;
                    //System.out.println("index="+index) ;
                }
            } ;
        
            frame_editVertCurveLen.setLayout(new GridBagLayout()) ;
            // Create a constrains object, and specify default values
            GridBagConstraints c = new GridBagConstraints() ;
            c.fill = GridBagConstraints.BOTH ; // component grows in both directions

            c.gridx = 0 ; c.gridy = 0; c.gridwidth = 1 ; c.gridheight = 1 ;
            c.insets = new Insets(5,5,0,5) ; // 5-pixel margins on all sides
            String unitStr = "";
            if (myDB.myUnit==1) {
                unitStr = " (ft)";
            } else if (myDB.myUnit==2) {
                unitStr = " (m)";
            } 
            
            lblSSD = new Label(CStr(SSD)+ unitStr) ;
            txtCurveLen = new TextField(CStr(myDB.verticalCurves[0].getCurveLen())) ;
            frame_editVertCurveLen.add(new Label("Curve Index"),c) ;
            
            c.insets = new Insets(1,5,5,5) ; 
            c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
            listVertCurves = new Choice();
            for (int i=0; i<myDB.vCurveCount; i++) {
                listVertCurves.add("Curve #" + (i+1));
            }
            listVertCurves.addItemListener(curveIndex_listener) ;
            frame_editVertCurveLen.add(listVertCurves, c) ;
            
            c.insets = new Insets(5,5,0,5) ; 
            c.gridx = 0; c.gridy = 2; 
            frame_editVertCurveLen.add(new Label("Stop Sight Distance" ), c) ;
            c.gridy = 3 ; 
            c.insets = new Insets(1,5,5,5) ; 
            frame_editVertCurveLen.add(lblSSD, c) ;
            
            c.insets = new Insets(5,5,0,5) ; 
            c.gridx = 1 ; c.gridy = 0; c.gridwidth = 1 ;
            frame_editVertCurveLen.add(new Label("Curve Length " + unitStr),c) ;
            c.insets = new Insets(1,5,5,5) ; 
            c.gridy = 1; 
            frame_editVertCurveLen.add(txtCurveLen, c) ;
            
            c.insets = new Insets(5,5,0,5) ; 
            c.gridx = 1 ; c.gridy = 2; c.gridwidth = 1 ;
            Button btn_save = new Button("Save") ;
            frame_editVertCurveLen.add(btn_save, c) ;
            btn_save.addActionListener(frame_editVertCurveLen_save_listener) ;

            c.gridx = 1 ; c.gridy = 3;
            c.insets = new Insets(5,5,0,5) ; 
            Button btn_check = new Button("Check Min Len") ;
            frame_editVertCurveLen.add(btn_check, c) ;
            btn_check.addActionListener(frame_editVertCurveLen_check_listener) ;

            c.gridx = 1 ; c.gridy = 4;
            c.insets = new Insets(5,5,5,5) ; 
            Button btn_done = new Button("Done") ;
            frame_editVertCurveLen.add(btn_done, c) ;
            btn_done.addActionListener(frame_editVertCurveLen_done_listener) ;

            frame_editVertCurveLen.invalidate();
            frame_editVertCurveLen.show() ;
            frame_editVertCurveLen.toFront() ;
             
        } else {
            noCurve2Edit_flag = true ;
        }
           
    } // popVertCurveLenEdit
    
    public void vertAlign(){
        if (myDB.vCurveCount > 0) { 
            fillCutSteps = 0;
            // at least 1 vertical curve exists
            // compute locations of PVC, PVT using lengths and ensure that PVC(i) > PVT(i-1)
            int i, vCurveFlag ;
            vCurveFlag = 0;
            valid_vCurve = false;
            float dist, x1=0f, y1=0f, x2=0f, y2=0f ;
            for (i=0; i<myDB.vCurveCount;i++) {
                myDB.verticalCurves[i].setPVI(myDB.vConstructMarks[i + 1].getDistance());
                myDB.verticalCurves[i].calcPVI(myDB.vConstructMarks[i + 1].getElevation(), 
                    myDB.vConstructMarks[i + 1].getGrade(), myDB.vConstructMarks[i + 2].getGrade());

                // check if PVC(i) & PVT(i-1) overlaps
                if (i > 0) { // 
                    if (myDB.verticalCurves[i].getPVC() < myDB.verticalCurves[i-1].getPVT()) { 
                        popMessageBox("Construct PVC, PVT", "Adjacent curves " + CStr(i) + " and " + CStr(i + 1) + " overlap!\nPlease redesign vertical curves.");
                        vCurveFlag = 1;
                        myDB.vConstructMarks[i].PVTnC_Overlap = true;
                        myDB.vConstructMarks[i + 1].PVTnC_Overlap = true;
                        myDB.vConstructMarks[i + 2].PVTnC_Overlap = true;
                    }   // overlap occurs
                }   // i>0

                // calculate road length/PVC, PVT distance
                if (i == 0) { 
                    // first curve
                    x1 = myDB.vConstructMarks[i].getDistance();
                    y1 = myDB.vConstructMarks[i].getElevation();
                    x2 = myDB.verticalCurves[i].getPVC();
                    y2 = myDB.verticalCurves[i].getPVC_Elevation();
                    dist = new Float(Math.sqrt((x2 - x1)*(x2 - x1) + (y2 - y1)*(y2 - y1))).floatValue();
                    myDB.verticalCurves[i].setPVC_Distance(dist);
                } else {
                    x1 = myDB.verticalCurves[i - 1].getPVT();
                    y1 = myDB.verticalCurves[i - 1].getPVT_Elevation();
                    x2 = myDB.verticalCurves[i].getPVC();
                    y2 = myDB.verticalCurves[i].getPVC_Elevation();
                    dist = new Float(Math.sqrt((x2 - x1)*(x2 - x1) + (y2 - y1)*(y2 - y1))).floatValue();
                    myDB.verticalCurves[i].setPVC_Distance(myDB.verticalCurves[i - 1].getPVT_Distance() + dist);

                }
                dist = calcCurveRoadLength(i);
                myDB.verticalCurves[i].setPVT_Distance(myDB.verticalCurves[i].getPVC_Distance() + dist);

            }   // Next i
            // last linear segment
            // last curve
            x1 = myDB.verticalCurves[myDB.vCurveCount - 1].getPVT();
            y1 = myDB.verticalCurves[myDB.vCurveCount - 1].getPVT_Elevation();
            x1 = myDB.vConstructMarks[myDB.vConstructMarkCount - 1].getDistance();
            y1 = myDB.vConstructMarks[myDB.vConstructMarkCount - 1].getElevation();
            dist = new Float(Math.sqrt((x2 - x1)*(x2 - x1) + (y2 - y1)*(y2 - y1))).floatValue();
            total_roadLen = myDB.verticalCurves[myDB.vCurveCount - 1].getPVT_Distance() + dist;

            // if curve is valid, create our own construction curve
            if (vCurveFlag == 0) { 
                valid_vCurve = true;
                //data_viewer.Invalidate()
                // calculate cut/fill profile
                fillCutSteps = calcFillCutProfile() ;
                //btnRoad3D.Enabled = True
            }
        } else {
            //btnRoad3D.Enabled = False
            popMessageBox("Vertical Curve Design","Cannot construct vertical curve. \nRequire at least 2 construction lines!");
        }   //if (myDB.vCurveCount > 0)
    }   // vertAlign
    private float calcCurveRoadLength(int index) {
        int i, numSteps ;
        float stepSize, xi, yi, x1, y1, x2, y2 ;
        float len = 0f;
        x1 = myDB.verticalCurves[index].getPVC();
        y1 = myDB.verticalCurves[index].getPVC_Elevation();
        x2 = myDB.verticalCurves[index].getPVT();

        numSteps = CInt(Math.ceil((x2 - x1) / ComputeStepSize));  // calculate every 10 ft/2m
        stepSize = (x2 - x1) / numSteps;
        for (i=1;i<numSteps;i++) {
            xi = i * stepSize + x1;
            yi = calcDesignElevation(xi);
            len += Math.sqrt(stepSize*stepSize  + (yi - y1)*(yi - y1));
            y1 = yi;
        } // Next
        return len;
    }    // calcCurveRoadLength
    
    // proposed road elevation
    public float calcDesignElevation(float distx) {
        // input distance from start point, in ft/m
        // return designed elevation in ft/m
        float elev = 0f;
        if (distx > max_hDist) {
            elev = myDB.vConstructMarks[myDB.vConstructMarkCount - 1].getElevation();
        } else {
            int i ;
            float xPVC, xPVT ;
            float dx ;
            for (i=0;i<myDB.vCurveCount;i++) {
                xPVC = myDB.verticalCurves[i].getPVC();
                xPVT = myDB.verticalCurves[i].getPVT();
                dx = distx - xPVC;
                if (dx <= 0) { 
                    elev = myDB.verticalCurves[i].getPVC_Elevation() + dx * myDB.verticalCurves[i].get_G1();
                    break;
                } else if (dx > 0 && distx < xPVT) { // vertical curve
                    elev = myDB.verticalCurves[i].getDX_Elevation(dx);
                    break;
                } else if ((i == myDB.vCurveCount - 1) && (distx >= xPVT)) {   // last grade
                    dx = distx - xPVT;
                    elev = myDB.verticalCurves[i].getPVT_Elevation() + dx * myDB.verticalCurves[i].get_G2();
                }
            }   //Next i
        }
        return elev;
    }   //calcDesignElevation
    
    public int calcFillCutProfile() {
        // create a cut/fill profile with increments of every 10 ft/m
        int fillcut_steps ;
        fillcut_steps = CInt(Math.ceil(max_hDist / ComputeStepSize));
        CutandFill = new float[fillcut_steps] ;
        accuMass = new float[fillcut_steps] ;
        designElevation = new float[fillcut_steps] ;
        
        int i ;
        for (i=0;i<fillcut_steps;i++) {
            CutandFill[i] = 0f;
            CutandFill[i] = calcOriginalElevation(i * ComputeStepSize) - calcDesignElevation(i * ComputeStepSize);
            // 5/2/06 added
            if (i == 0) { 
                if (myDB.myUnit == 1) {   // US unit
                    accuMass[i] = CutandFill[i] * ComputeStepSize * (myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth * 2f) / 27f ;
                } else if (myDB.myUnit == 2) { // metric
                    accuMass[i] = CutandFill[i] * ComputeStepSize * (myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth * 2f);
                }
            } else {
                if (myDB.myUnit == 1) {   // US unit
                    accuMass[i] = CutandFill[i] * ComputeStepSize * (myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth * 2f) / 27f + accuMass[i-1];
                } else if (myDB.myUnit == 2) { // metric
                    accuMass[i] = CutandFill[i] * ComputeStepSize * (myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth * 2f) + accuMass[i-1];
                }
            }
        } // Next

        return fillcut_steps;
    } // calcFillCutProfile
    
    // original land elevation
    public float calcOriginalElevation(float distx) {
        float elev = 0f;
        if (distx > max_hDist) { 
            elev = myDB.elevationMarks[myDB.elevationMarkCount - 1].getElevation();
        } else {
            int i ;
            float dx ;
            for (i=1;i<myDB.elevationMarkCount;i++) {
                if ((distx >= myDB.elevationMarks[i - 1].getDistance()) && (distx < myDB.elevationMarks[i].getDistance())) {
                    // do linear interpolation
                    dx = distx - myDB.elevationMarks[i - 1].getDistance();
                    elev = myDB.elevationMarks[i - 1].getElevation() + dx * myDB.elevationMarks[i].getGrade();
                    break;
                }
            }   //Next i
        }
        return elev;
    }   // calcOriginalElevation
    
    public void viewFillCut() {
        if (CutandFill==null) {
            fillCutSteps = calcFillCutProfile();
        }
        if (valid_vCurve) { 
            tb.setConstructEnabled(false) ;
            //btnFillCut.Image = ImageList1.Images(2)
            viewElevationProfile = false;
            viewMassDiagram=false ;     // 5/2/06 added
            sb.setStatusBarText(0, "View fill/cut profile") ; //Status: 
            //ele_data.Text = "Fill-Cut Profile"
            int i ;
            float my_fc, max_fc, min_fc ;
            min_fc = 99999f;
            max_fc = 0f;
            for (i=0;i<fillCutSteps;i++) {
                my_fc = CutandFill[i];
                if (my_fc > max_fc) {
                    max_fc = my_fc;
                } else if (my_fc < min_fc) {
                    min_fc = my_fc;
                }
            }   //Next i

            // update plot scale in Y directions
            gridSizeY = CInt(Math.ceil((max_fc - min_fc) / 4));
            Ymin = CInt(10f * Math.floor((min_fc-gridSizeY/2) / 10f));
            
            plotScaleY = (float)GRID_VDIV / (float)gridSizeY;

            // update X, Y axes labels
            /*
            elevation_0.Text = Ymin.ToString
            elevation_1.Text = (1 * gridSizeY + Ymin).ToString()
            elevation_2.Text = (2 * gridSizeY + Ymin).ToString()
            elevation_3.Text = (3 * gridSizeY + Ymin).ToString()
            elevation_4.Text = (4 * gridSizeY + Ymin).ToString()
            elevation_5.Text = (5 * gridSizeY + Ymin).ToString()
            elevation_6.Text = (6 * gridSizeY + Ymin).ToString()
            lbl_ele.Visible = False
             */
            //ToolTip1.SetToolTip(btnFillCut, "View elevation profile")
            //data_viewer.Invalidate()
            repaint();
        } else {
            // no valid vertical curves created
            if (myDB.vCurveCount > 0) { 
                popMessageBox("Vertical Curve Design","Please perform vertical curve alignment first!");
            } else {
                popMessageBox("Vertical Curve Design","No construction lines. \nPlease use the construction button to \ncreate vertical curve construction lines first!");
            }
        }
    }   // vire fillcut
    public void view_MassDiagram() {
        if (accuMass==null) {
            fillCutSteps = calcFillCutProfile();
        }
        if (valid_vCurve) { 
            tb.setConstructEnabled(false) ;
            //btnFillCut.Image = ImageList1.Images(2)
            viewElevationProfile = false;
            viewMassDiagram=true ;
            sb.setStatusBarText(0, "View mass diagram") ; //Status: 
            //ele_data.Text = "Fill-Cut Profile"
            int i ;
            float my_fc, max_fc, min_fc ;
            min_fc = 99999f;
            max_fc = 0f;
            for (i=0;i<fillCutSteps;i++) {
                my_fc = accuMass[i];
                if (my_fc > max_fc) {
                    max_fc = my_fc;
                } else if (my_fc < min_fc) {
                    min_fc = my_fc;
                }
            }   //Next i

            // update plot scale in Y directions
            gridSizeY = CInt(Math.ceil((max_fc - min_fc) / 4));
            Ymin = CInt(1000f * Math.floor((min_fc-gridSizeY/2) / 1000f));
            
            plotScaleY = (float)GRID_VDIV / (float)gridSizeY;

            // update X, Y axes labels
            /*
            elevation_0.Text = Ymin.ToString
            elevation_1.Text = (1 * gridSizeY + Ymin).ToString()
            elevation_2.Text = (2 * gridSizeY + Ymin).ToString()
            elevation_3.Text = (3 * gridSizeY + Ymin).ToString()
            elevation_4.Text = (4 * gridSizeY + Ymin).ToString()
            elevation_5.Text = (5 * gridSizeY + Ymin).ToString()
            elevation_6.Text = (6 * gridSizeY + Ymin).ToString()
            lbl_ele.Visible = False
             */
            //ToolTip1.SetToolTip(btnFillCut, "View elevation profile")
            //data_viewer.Invalidate()
            repaint();
        } else {
            // no valid vertical curves created
            if (myDB.vCurveCount > 0) { 
                popMessageBox("Vertical Curve Design","Please perform vertical curve alignment first!");
            } else {
                popMessageBox("Vertical Curve Design","No construction lines. \nPlease use the construction button to \ncreate vertical curve construction lines first!");
            }
        }
    }   // view mass diagram
    
    public void viewElevation(){
        tb.setConstructEnabled(true) ;
        if (valid_vCurve) { 
            //btnFillCut.Image = ImageList1.Images(3)
            viewElevationProfile = true;
            viewMassDiagram=false ;     // 5/2/06 added
            //ele_data.Text = "Grade Design"
            sb.setStatusBarText(0, "View elevation profile"); //Status: 

            // update plot scale in Y directions
            gridSizeY = CInt(Math.ceil((max_ele - min_ele) / 4));
            Ymin = CInt(10f * Math.floor((min_ele-gridSizeY/2) / 10f));
            
            plotScaleY = (float)GRID_VDIV / (float)gridSizeY;

            // update X, Y axes labels
            /*
            elevation_0.Text = Ymin.ToString
            elevation_1.Text = (1 * gridSizeY + Ymin).ToString()
            elevation_2.Text = (2 * gridSizeY + Ymin).ToString()
            elevation_3.Text = (3 * gridSizeY + Ymin).ToString()
            elevation_4.Text = (4 * gridSizeY + Ymin).ToString()
            elevation_5.Text = (5 * gridSizeY + Ymin).ToString()
            elevation_6.Text = (6 * gridSizeY + Ymin).ToString()
            lbl_ele.Visible = True
            ToolTip1.SetToolTip(btnFillCut, "View fill/cut profile")
             */
            //data_viewer.Invalidate()
            repaint();
        } else {
            // no valid vertical curves created
            if (myDB.vCurveCount > 0) {// Then
                popMessageBox("Vertical Curve Design","Please perform vertical curve alignment first!");
            } else {
                popMessageBox("Vertical Curve Design","No construction lines. \nPlease use the construction button to \ncreate vertical curve construction lines first!");
            }
        }
    }   // view elevation
    public void tool_gradeON(){
        isGradeConstructOn = true;
        sb.setStatusBarText(0,  "Grade construction tool ON"); //Status: 
    }

    public void tool_gradeOFF() {
        isGradeConstructOn = false;
        sb.setStatusBarText(0, "Grade construction tool OFF"); //Status: 
    }

    public void edit_undo() {
        int bufData ;
        bufData = popGradeLogBuffer();
        if (bufData >= 0) { 
            myDB.vConstructMarkCount = bufData;
            myDB.vCurveCount = myDB.vConstructMarkCount-1;
            sb.setStatusBarText(0, "Undo last construction point") ; //Status: 
        } else {
            sb.setStatusBarText(0, "Cannot undo") ; //Status: 
        }
        repaint();
    }
    
    public void setStatusBarText(int id, String message){
        sb.setStatusBarText(id, message) ;
    }

    public void edit_redo() {
        if (gradeLogIndex < gradeLogBuffer.length - 1) { 
            if (gradeLogBuffer[gradeLogIndex + 1] > 0 ) {
                // log info exists
                gradeLogIndex += 1;
                myDB.vConstructMarkCount = gradeLogBuffer[gradeLogIndex];
                 myDB.vCurveCount = myDB.vConstructMarkCount-1;
                 sb.setStatusBarText(0, "Redo last construction point") ; //Status: 
            } else {
            sb.setStatusBarText(0, "Cannot redo") ; //Status: 
            }
        } else {
            sb.setStatusBarText(0, "Cannot redo") ; //Status: 
        }
        repaint();
    }   // edit_redo
 /** Pop up a window to display message */    
    public void popClearAllDesign(String caption, String message) {
        // open a frame
        frame_msgboxClearAll = new myWindow(caption) ;
        //frame_msgboxClearAll.setLocation(400,200) ;
        frame_msgboxClearAll.setSize(300,150) ;
        frame_msgboxClearAll.setCenter() ;
        frame_msgboxClearAll.validate() ;
        frame_msgboxClearAll.setVisible(true) ;

        ActionListener frame_msgbox_yes_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                myDB.vConstructMarkCount = 0;
                valid_vCurve = false;
                myDB.vCurveCount = 0;
                me0 = null;
                frame_msgboxClearAll.dispose() ;

                sb.setStatusBarText(0, "Design cleared") ; //Status: 
            }
        } ;
        ActionListener frame_msgbox_no_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_msgboxClearAll.dispose() ;
            }
        } ;

        frame_msgboxClearAll.setLayout(new GridBagLayout()) ;
        // Create a constrains object, and specify default values
        GridBagConstraints c = new GridBagConstraints() ;
        c.fill = GridBagConstraints.BOTH ; // component grows in both directions
        c.weightx = 1.0 ; c.weighty = 1.0 ;

        c.gridx = 0 ; c.gridy = 0; c.gridwidth = 2 ; c.gridheight = 1 ;
        c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
        Label myMsg = new Label(message) ;
        //myMsg.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
        //myMsg.setForeground(new Color(0,0,218)) ;
        frame_msgboxClearAll.setBackground(new Color(200, 200, 200)) ;
        frame_msgboxClearAll.add(myMsg,c) ;
        c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
        Button btn_ok = new Button(" Yes ") ;
        frame_msgboxClearAll.add(btn_ok, c) ;
        btn_ok.addActionListener(frame_msgbox_yes_listener) ;
        c.gridx = 1 ; c.gridy = 1;
        Button btn_no = new Button(" No ") ;
        frame_msgboxClearAll.add(btn_no, c) ;
        btn_no.addActionListener(frame_msgbox_no_listener) ;

        frame_msgboxClearAll.invalidate();
        frame_msgboxClearAll.show() ;
        frame_msgboxClearAll.toFront() ;
            
    } // popClearAll
 /** Pop up a window to display message */    
    public void popClearVCurves(String caption, String message) {
        if (!viewElevationProfile) {
            viewElevation() ;
        }
        // open a frame
        frame_msgboxClearCurves = new myWindow(caption) ;
        //frame_msgboxClearCurves.setLocation(400,200) ;
        frame_msgboxClearCurves.setSize(300,150) ;
        frame_msgboxClearCurves.setCenter() ;
        frame_msgboxClearCurves.validate() ;
        frame_msgboxClearCurves.setVisible(true) ;

        ActionListener frame_msgbox_yes_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                valid_vCurve = false;
                /*
                for (int i=0; i<myDB.vCurveCount+2; i++) {
                    myDB.vConstructMarks[i].PVTnC_Overlap = false ;
                }
                sb.setStatusBarText(1, "") ; // clear error if any previously
                */
                sb.setStatusBarText(0, "Vertical curves cleared") ; //Status: 
                frame_msgboxClearCurves.dispose() ;
                repaint();
            }
        } ;
        ActionListener frame_msgbox_no_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_msgboxClearCurves.dispose() ;
            }
        } ;

        frame_msgboxClearCurves.setLayout(new GridBagLayout()) ;
        // Create a constrains object, and specify default values
        GridBagConstraints c = new GridBagConstraints() ;
        c.fill = GridBagConstraints.BOTH ; // component grows in both directions
        c.weightx = 1.0 ; c.weighty = 1.0 ;

        c.gridx = 0 ; c.gridy = 0; c.gridwidth = 2 ; c.gridheight = 1 ;
        c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
        Label myMsg = new Label(message) ;
        //myMsg.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
        //myMsg.setForeground(new Color(0,0,218)) ;
        frame_msgboxClearCurves.setBackground(new Color(200, 200, 200)) ;
        frame_msgboxClearCurves.add(myMsg,c) ;
        c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
        Button btn_ok = new Button(" Yes ") ;
        frame_msgboxClearCurves.add(btn_ok, c) ;
        btn_ok.addActionListener(frame_msgbox_yes_listener) ;
        c.gridx = 1 ; c.gridy = 1;
        Button btn_no = new Button(" No ") ;
        frame_msgboxClearCurves.add(btn_no, c) ;
        btn_no.addActionListener(frame_msgbox_no_listener) ;

        frame_msgboxClearCurves.invalidate();
        frame_msgboxClearCurves.show() ;
        frame_msgboxClearCurves.toFront() ;
            
    } // pop Clear Vertical curves

    public void file_open(){
        FileInputStream fis=null;
        DataInputStream br=null;
        int i=0;
        float x=0f, y=0f ;
        try
        {
            FileDialog fd=new FileDialog(new Frame(),"Load Vertical Curve Design", FileDialog.LOAD);
            fd.setFile("*.vcw");
            fd.show();
            String dir = fd.getDirectory() ;
            String filename = fd.getFile() ;
            String fullpath = dir + filename ;
            fd.dispose();

            if (filename != null && dir != null) {
                //System.out.println("open filename="+fullpath);
                //reset draw settings

                fis = new FileInputStream(fullpath);
                br = new DataInputStream( new BufferedInputStream(fis,512)); 
                
                // 1 - get saved vertical construction grade lines
                myDB.vConstructMarkCount = br.readInt();
       //     System.out.println("vConstructMarkCount"+myDB.vConstructMarkCount);
                push2GradeLogBuffer(myDB.vConstructMarkCount);
                for (i=0;i<myDB.vConstructMarkCount;i++) {
                    if (myDB.vConstructMarks[i]==null) { 
                        myDB.vConstructMarks[i] = new MarkerDB();
                    }
                    x = br.readFloat();
                    y = br.readFloat();
                    myDB.vConstructMarks[i].setLocation(x, y);
                    myDB.vConstructMarks[i].setElevation(br.readFloat());
                    myDB.vConstructMarks[i].setParentIndex(br.readByte());
                    myDB.vConstructMarks[i].setSegmentType(br.readByte());
                    myDB.vConstructMarks[i].setDistance(br.readFloat());
                    myDB.vConstructMarks[i].setGrade(br.readFloat());
                }
                // 2 - get vertical curves
                myDB.vCurveCount = br.readInt();
                for (i=0; i<myDB.vCurveCount; i++) {
                    if (myDB.verticalCurves[i]==null) { 
                        myDB.verticalCurves[i] = new VCurve(myDB.minVCurveLen);
                    }
                    myDB.verticalCurves[i].setCurveLen(br.readFloat(), 0);
                    myDB.verticalCurves[i].setPVC(br.readFloat());
                    myDB.verticalCurves[i].setPVC_Elevation(br.readFloat());
                    myDB.verticalCurves[i].setPVC_Distance(br.readFloat());
                    myDB.verticalCurves[i].setPVT(br.readFloat());
                    myDB.verticalCurves[i].setPVT_Elevation(br.readFloat());
                    myDB.verticalCurves[i].setPVT_Distance(br.readFloat());
                    myDB.verticalCurves[i].setPVI(br.readFloat());
                    myDB.verticalCurves[i].setPVI_e(br.readFloat());
                    myDB.verticalCurves[i].setMinMaxElevation(br.readFloat());
                    myDB.verticalCurves[i].setMinMaxEleDist(br.readFloat());
                    myDB.verticalCurves[i].setPara_a(br.readFloat());
                    myDB.verticalCurves[i].setPara_b(br.readFloat());
                    myDB.verticalCurves[i].set_G1(br.readFloat());
                    myDB.verticalCurves[i].set_G2(br.readFloat());
                }
                // 3- 5/1/06 added to load unit info
                int savedUnit ;
                if (br.available() >0 ) {
                    savedUnit = br.readInt() ;
                    if (savedUnit == 1 && myDB.myUnit == 2) { 
                        // change from US to metric
                        for (i=0; i<myDB.vConstructMarkCount; i++) {
                            myDB.vConstructMarks[i].setElevation(myDB.vConstructMarks[i].getElevation() * myDB.FT2M);
                            myDB.vConstructMarks[i].setDistance(myDB.vConstructMarks[i].getDistance() * myDB.FT2M);
                        }
                        for (i=0; i<myDB.vCurveCount; i++) {
                            myDB.verticalCurves[i].setCurveLen(myDB.verticalCurves[i].getCurveLen() * myDB.FT2M, 0);
                        }

                    } else if (savedUnit == 2 && myDB.myUnit == 1) { 
                            // change from metric to US
                        for (i=0; i<myDB.vConstructMarkCount; i++) {
                            myDB.vConstructMarks[i].setElevation(myDB.vConstructMarks[i].getElevation() / myDB.FT2M);
                            myDB.vConstructMarks[i].setDistance(myDB.vConstructMarks[i].getDistance() / myDB.FT2M);
                        }
                        for (i=0; i<myDB.vCurveCount; i++) {
                            myDB.verticalCurves[i].setCurveLen(myDB.verticalCurves[i].getCurveLen() / myDB.FT2M, 0);
                        }

                    }   // if (savedUnit == 1 && myDB.myUnit == 2) 
                }   //End If br.available
                vertAlign() ; // calc total road length, etc
                valid_vCurve = true;

                br.close();
                fis.close();
            }   // end if fullpath
            repaint();
        }   // try
        catch (Exception e){
                //do nothing
            System.out.println("Load Vertical Curve Design File:"+e.toString());
            sb.setStatusBarText(1, "Loading Vertical Curve, "+e.toString()) ; //Error: 

        } // try        
    }   // file open
    public void file_save(){
        FileOutputStream fos=null;
        DataOutputStream w=null;

        try
        {
            FileDialog fd=new FileDialog(new Frame(),"Save Vertical Curve Design", FileDialog.SAVE);
            fd.setFile("*.vcw");
 /*           
             fd.setFilenameFilter(new FilenameFilter(){
                public boolean accept(File dir, String name){
                  return (name.endsWith(".vcw")) ;  // || name.endsWith(".gif"));
                  }
            });
  */
            fd.show();
            
            String fullpath=fd.getDirectory()+fd.getFile();
            
//System.out.println("filepath="+fullpath);
            if(fullpath!=null) {
                if (fullpath.indexOf(".vcw")<0) {
                    fullpath += ".vcw" ;
                }
                fos = new FileOutputStream(fullpath);
                w = new DataOutputStream( new BufferedOutputStream(fos,512)); 

                // 1 - save grade construction lines
                int i ;
                w.writeInt(myDB.vConstructMarkCount);
                for (i=0; i<myDB.vConstructMarkCount;i++) {
                    w.writeFloat(myDB.vConstructMarks[i].getLocation().X);
                    w.writeFloat(myDB.vConstructMarks[i].getLocation().Y);
                    w.writeFloat(myDB.vConstructMarks[i].getElevation());
                    w.writeByte(myDB.vConstructMarks[i].getParentIndex());
                    w.writeByte(myDB.vConstructMarks[i].getSegmentType());
                    w.writeFloat(myDB.vConstructMarks[i].getDistance());
                    w.writeFloat(myDB.vConstructMarks[i].getGrade());
                }   //Next
                w.flush();
                // 2 - vertical curves DB
                w.writeInt(myDB.vCurveCount);
                for (i=0;i<myDB.vCurveCount;i++) { 
                    w.writeFloat(myDB.verticalCurves[i].getCurveLen());
                    w.writeFloat(myDB.verticalCurves[i].getPVC());
                    w.writeFloat(myDB.verticalCurves[i].getPVC_Elevation());
                    w.writeFloat(myDB.verticalCurves[i].getPVC_Distance());
                    w.writeFloat(myDB.verticalCurves[i].getPVT());
                    w.writeFloat(myDB.verticalCurves[i].getPVT_Elevation());
                    w.writeFloat(myDB.verticalCurves[i].getPVT_Distance());
                    w.writeFloat(myDB.verticalCurves[i].getPVI());
                    w.writeFloat(myDB.verticalCurves[i].getPVI_e());
                    w.writeFloat(myDB.verticalCurves[i].getMinMaxElevation());
                    w.writeFloat(myDB.verticalCurves[i].getMinMaxEleDist());
                    w.writeFloat(myDB.verticalCurves[i].getPara_a());
                    w.writeFloat(myDB.verticalCurves[i].getPara_b());
                    w.writeFloat(myDB.verticalCurves[i].get_G1());
                    w.writeFloat(myDB.verticalCurves[i].get_G2());
                }
                // 3 - 5/1/06 added to save unit info
                w.writeInt(myDB.myUnit);

                w.flush();
                w.close();
            }
            fos.close();
            fd.dispose();
        }
        catch (Exception e){
                //do nothing
            System.out.println("Save Vertical Curve Design File:"+e.toString());
            sb.setStatusBarText(1, "Saving Vertical Curve, "+e.toString()) ; //Error: 
        } // try
        
    }// file save

    public void saveElevationProfile(){  // save elevation profile data to a file
        try
        {
            FileDialog fd=new FileDialog(new Frame(),"Save Designed Elevation Data", FileDialog.SAVE);
            fd.setFile("*.txt");
            fd.show();
            String fullpath=fd.getDirectory()+fd.getFile();
            fd.dispose();
            //System.out.println("filepath="+fullpath);
            if(fullpath!=null) {
                BufferedWriter out = new BufferedWriter(new FileWriter(fullpath));
                String unitStr ;
                if (myDB.myUnit==1) {
                    unitStr = " (ft)" ;
                } else {
                    unitStr=" (m)" ;
                }
                String dataStr = "Distance"+unitStr+", Elevation"+unitStr+"\n";
                if (fillCutSteps > 0) {
                    for (int i=0; i<fillCutSteps; i++) {
                        dataStr += CStr(i*ComputeStepSize) + "," + CStr(designElevation[i]) + "\n" ;
                    }
                }
                out.write(dataStr);
                out.flush();
                out.close();
            }
        }
        catch (Exception e){
            System.out.println("Save Designed Elevation Data:"+e.toString());
            sb.setStatusBarText(1, "Saving Designed Elevation Data, "+e.toString()) ; //Error: 
        } // try

       
    }// file save elevation profile
    
    public void saveMassDiagram(){  // save mass diagram data to a file
        try
        {
            FileDialog fd=new FileDialog(new Frame(),"Save Mass Diagram Data", FileDialog.SAVE);
            fd.setFile("*.txt");
            fd.show();
            String fullpath=fd.getDirectory()+fd.getFile();
            fd.dispose();
            //System.out.println("filepath="+fullpath);
            if(fullpath!=null) {
                BufferedWriter out = new BufferedWriter(new FileWriter(fullpath));
                String unitStr="", unitStr3="" ; 
                if (myDB.myUnit==1) {
                    unitStr = " (ft)" ;
                    unitStr3 = " (yd^3)" ;
                } else {
                    unitStr=" (m)" ;
                    unitStr3=" (m^3)" ;
                }
                String dataStr = "Distance"+unitStr+", Mass"+unitStr3+"\n";
                if (fillCutSteps > 0) {
                    for (int i=0; i<fillCutSteps; i++) {
                        dataStr += CStr(i*ComputeStepSize) + "," + CStr(accuMass[i]) + "\n" ;
                    }
                }
                out.write(dataStr);
                out.flush();
                out.close();
            }
        }
        catch (Exception e){
            System.out.println("Save Mass Diagram Data:"+e.toString());
            sb.setStatusBarText(1, "Saving Mass Diagram Data, "+e.toString()) ; //Error: 
        } // try

       
    }// file save mass diagram

    public void saveCutAndFill(){  // save mass diagram data to a file
        try
        {
            FileDialog fd=new FileDialog(new Frame(),"Save Cut and Fill Data", FileDialog.SAVE);
            fd.setFile("*.txt");
            fd.show();
            String fullpath=fd.getDirectory()+fd.getFile();
            fd.dispose();
            //System.out.println("filepath="+fullpath);
            if(fullpath!=null) {
                BufferedWriter out = new BufferedWriter(new FileWriter(fullpath));
                String unitStr="" ; 
                if (myDB.myUnit==1) {
                    unitStr = " (ft)" ;
                } else {
                    unitStr=" (m)" ;
                }
                String dataStr = "Distance"+unitStr+", Cut/Fill"+unitStr+"\n";
                if (fillCutSteps > 0) {
                    for (int i=0; i<fillCutSteps; i++) {
                        dataStr += CStr(i*ComputeStepSize) + "," + CStr(CutandFill[i]) + "\n" ;
                    }
                }
                out.write(dataStr);
                out.flush();
                out.close();
            }
        }
        catch (Exception e){
            System.out.println("Save Cut and Fill Data:"+e.toString());
            sb.setStatusBarText(1, "Saving Cut and Fill Data, "+e.toString()) ; //Error: 
        } // try

       
    }// file save cut and fill
    
    public void file_saveReport(){  // save report file
        if (valid_vCurve) { 
            try
            {
                FileDialog fd=new FileDialog(new Frame(),"Save Report", FileDialog.SAVE);
                fd.setFile("*.txt");
                fd.show();
                String fullpath=fd.getDirectory()+fd.getFile();
                fd.dispose();
    //System.out.println("filepath="+fullpath);
                if(fullpath!=null) {
                    BufferedWriter out = new BufferedWriter(new FileWriter(fullpath));
                    String reportStr = generateReport();
                    out.write(reportStr);
                    out.flush();
                    out.close();
                }
            }
            catch (Exception e){
                    //do nothing
                System.out.println("Save Report File:"+e.toString());
                sb.setStatusBarText(1, "Saving Report, "+e.toString()) ; //Error: 
            } // try
        } else {
            // no valid vertical curves created
            if (myDB.vCurveCount > 0) {// Then
                popMessageBox("Vertical Curve Design","Please perform vertical curve alignment first!");
            } else {
                popMessageBox("Vertical Curve Design","No construction lines. \nPlease use the construction button to \ncreate vertical curve construction lines first!");
            }
        }
       
    }// file save report
   
    public void file_printReport(){  // print report file
        if (valid_vCurve) { 
            String reportStr = generateReport();
            try
            {
                //PrintSimpleText printReport = new PrintSimpleText(reportStr) ;
                PrintText printReport = new PrintText() ;
                printReport.print(reportStr) ;

            }
            catch (Exception e){
                    //do nothing
                System.out.println("Print Report:"+e.toString());
                sb.setStatusBarText(1, "Print Report, "+e.toString()) ; //Error: 
            } // try
        } else {
            // no valid vertical curves created
            if (myDB.vCurveCount > 0) {// Then
                popMessageBox("Vertical Curve Design","Please perform vertical curve alignment first!");
            } else {
                popMessageBox("Vertical Curve Design","No construction lines. \nPlease use the construction button to \ncreate vertical curve construction lines first!");
            }
        }
       
    }// file print report
    
    /** Pop up a window to display report */   
    public void popReport() {
        if (valid_vCurve) { 
            if (frame_report != null) {
                frame_report.dispose() ;
            }
            frame_report = new myWindow("Roadway Geometry Design Report") ;
            frame_report.setSize(450,600) ;
            frame_report.setCenter() ;
            String message = "" ;
            TextArea myReport = new TextArea() ;
            if (frame_report.isShowing()==false)
            {
                message = generateReport();
                
                //System.out.println(message) ;
                //frame_report.setLocation(250,5) ;

                frame_report.setSize(450,600) ;
                //frame_report.validate() ;
                frame_report.setVisible(true) ;
                frame_report.setResizable(true);
                //frame_report.setCenter() ;
                //frame_report.show() ;
                // file menu
                MenuBar menu_bar = new MenuBar() ;
                Menu menu_file = new Menu("File") ;
                MenuItem file_savereport = new MenuItem("Save Report") ;
                MenuItem file_printreport = new MenuItem("Print") ;
                MenuItem separator = new MenuItem("-") ;
                MenuItem file_close = new MenuItem("Close") ;
                menu_file.add(file_savereport) ;   // add menu items
                menu_file.add(file_printreport) ;   // add menu items
                menu_file.add(separator) ;   // add menu items
                menu_file.add(file_close) ;   // add menu items

                menu_bar.add(menu_file) ;     // add menu
                frame_report.setMenuBar(menu_bar) ;

                 file_savereport.addActionListener(
                        new ActionListener() {
                            public void actionPerformed(ActionEvent aev) {
                                file_saveReport();  
                                setStatusBarText(0, "Save Report") ;
                            } // actionPerformed
                        } // ActionListener
                 ) ; // file save report
                 file_printreport.addActionListener(
                        new ActionListener() {
                            public void actionPerformed(ActionEvent aev) {
                                file_printReport();  
                                setStatusBarText(0, "Print Report") ;
                            } // actionPerformed
                        } // ActionListener
                 ) ; // file save report
                 file_close.addActionListener(
                        new ActionListener() {
                            public void actionPerformed(ActionEvent aev) {
                                frame_report.dispose();
                            } // actionPerformed
                        } // ActionListener
                 ) ; // file Close
        /*
                ActionListener frame_report_ok_listener = new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {

                        frame_report.dispose() ;
                    }
                } ;
        */
                frame_report.setLayout(new BorderLayout(1,1)) ;
                myReport.setText(message) ;
                myReport.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
                myReport.setForeground(new Color(0,0,218)) ;
                frame_report.setBackground(new Color(200, 200, 200)) ;
                frame_report.add("Center",myReport) ;

                //Button btn_ok = new Button(" OK ") ;
                //frame_report.add("South",btn_ok) ;
                //btn_ok.addActionListener(frame_report_ok_listener) ;
                //frame_report.invalidate();
                //System.out.println(message) ;
                frame_report.show() ;
            }   // frmReport is showing
            //frame_report.toFront() ;
        } else {
            // no valid vertical curves created
            if (myDB.vCurveCount > 0) { 
                popMessageBox("Vertical Curve Design","Please perform vertical curve alignment first!");
            } else {
                popMessageBox("Vertical Curve Design","No construction lines. \nPlease use the construction button to \ncreate vertical curve construction lines first!");
            }
        }
    } // popReport
    
    public String generateReport() {
        String rpt  = "";
        String unit_str="" ;

        rpt = "Roadway Geometry Design Report" + newLine + newLine;
        rpt += "Vertical Curve Design Summary" + newLine;
        if (myDB.myUnit == 1) { 
            unit_str = " ft";
            rpt += "Total road length = " + CStr(total_roadLen) + " ft = " + CStr(total_roadLen / 5280f) + " miles." + newLine;
        } else if (myDB.myUnit == 2) { 
            unit_str = " m";
            rpt += "Total road length = " + CStr(total_roadLen) + " m = " + CStr(total_roadLen / 1000f) + " Km." + newLine;
        }
        int i ;
        for (i = 1; i< myDB.vConstructMarkCount; i++) {
            rpt += "Grade(" + i + ") = " + CStr(myDB.vConstructMarks[i].getGrade() * 100f) + "%" + newLine;
        }   //Next i
        rpt += newLine + "Curves Location and Elevation " + newLine;
        float pvcd, pvtd, clen ;
        for (i = 0 ; i<myDB.vCurveCount ; i++) {
            pvcd = myDB.verticalCurves[i].getPVC_Distance();
            pvtd = myDB.verticalCurves[i].getPVT();
            clen = pvtd - pvcd;
            rpt += "Curve(" + CStr(i + 1) + ") Length = " + CStr(clen) + unit_str + newLine;
            rpt += "             PVC (distance, elevation) = (" + CStr(pvcd) + ", " + CStr(myDB.verticalCurves[i].getPVC_Elevation()) + ")" + unit_str + newLine;
            rpt += "             PVI (dist_prj, elevation) = (" + CStr(myDB.verticalCurves[i].getPVI()) + ", " + CStr(myDB.verticalCurves[i].getPVI_e()) + ")" + unit_str + newLine;
            rpt += "             PVT (distance, elevation) = (" + CStr(pvtd) + ", " + CStr(myDB.verticalCurves[i].getPVT_Elevation()) + ")" + unit_str + newLine;
            if (myDB.verticalCurves[i].get_G2() - myDB.verticalCurves[i].get_G1() > 0) { 
                rpt += "  Min. ";
            } else {
                rpt += "  Max. ";
            }
            rpt += "elevation(distance, elevation) = (" + CStr(myDB.verticalCurves[i].getMinMaxEleDist()) + ", " + CStr(myDB.verticalCurves[i].getMinMaxElevation()) + ")" + unit_str + newLine;
            rpt += newLine;
        }   //Next i

        rpt += newLine;
        rpt += getHorizonDesignSummary();
        rpt += newLine;
        rpt += "Cut and Fill Summary" + newLine;
        int size ;
        float totalCut = 0f ;
        float totalFill = 0f ;
        size = CutandFill.length ;
        for (i = 0; i< size ; i++) {   // actual elevation profile - designed curve
            if (CutandFill[i] < 0 ) {
                totalFill += CutandFill[i];
            } else if (CutandFill[i] > 0) { 
                totalCut += CutandFill[i];
            }
        }
        
        if (myDB.myUnit == 1) { 
            totalFill *= ComputeStepSize * (myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth * 2f) / 27f;
            totalCut *= ComputeStepSize * (myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth * 2f) / 27f;
            rpt += "Total Volume to Fill = " + CStr(totalFill) + " cu. yd" + newLine;
            rpt += "Total Volume to Cut  = " + CStr(totalCut) + " cu. yd" + newLine;
            rpt += "Cut and Fill Balance = " + CStr(totalCut + totalFill) + " cu. yd" + newLine;
        } else if (myDB.myUnit == 2) { 
            totalFill *= ComputeStepSize * (myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth * 2f);
            totalCut *= ComputeStepSize * (myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth * 2f);
            rpt += "Total Volume to Fill = " + CStr(totalFill) + " cu. meter" + newLine;
            rpt += "Total Volume to Cut  = " + CStr(totalCut) + " cu. meter" + newLine;
            rpt += "Cut and Fill Balance = " + CStr(totalCut + totalFill) + " cu. meter" + newLine;
        }
        //System.out.println("total_cut="+totalCut) ;
        //System.out.println("total_fill="+totalFill) ;
        //System.out.println("balance="+CStr(totalCut + totalFill)) ;

        rpt += newLine ;
        return rpt;
    }   // generate report
    
    public String getHorizonDesignSummary() {
        int i, curveidx, hAlignIdx ;
        String rpt, unit_str="" ;
        float dist, cord_dist, myRadius ;
        mPointF t1=new mPointF(0f,0f);
        mPointF t2=new mPointF(0f,0f);
        rpt = "Horizontal Geometry Design Summary" + newLine;
        curveidx = 0;
        if (myDB.myUnit == 1) { 
            unit_str = " ft";
        } else if (myDB.myUnit == 2) { 
            unit_str = " m";
        }
        for (i = 0 ; i< hRoadDataCount; i++) {
            myRadius = myDB.hRoadData[i].getRadius();
            if (myRadius > 0) { 
                curveidx += 1;
                rpt += "Curve(" + curveidx + ")" + newLine;
                rpt += "   Radius = " + CStr(myDB.hRoadData[i].getRadius()) + unit_str + newLine;
                hAlignIdx = findTangentDBIndex(i);
                if (hAlignIdx >= 0) {
                    t1 = myDB.hAlignMarks[hAlignIdx].getLocation();
                    t2 = myDB.hAlignMarks[hAlignIdx + 1].getLocation();
                }
                cord_dist = distanceOf(t1, t2) / myDB.imageScale ;  // ft

                mPointF curve_ctr ;
                curve_ctr = myDB.hRoadData[i].getPoint1();
                float theta, theta_deg ;
                mPointF vec1, vec2 ;
                vec1 = vector(curve_ctr, t1);
                vec2 = vector(curve_ctr, t2);
                theta = new Float(Math.acos(getCosTheta(vec1, vec2))).floatValue();
                theta_deg = CInt(theta * 18000 / Math.PI) / 100;  // degree 2 decimal points
                dist = myRadius * theta;  // curve distance

                rpt += "   Curve Length = " + CStr(dist) + unit_str + newLine;
                rpt += "   Central Angle = " + CStr(theta_deg) + " degrees" + newLine;
                rpt += "   Cord Length = " + CStr(cord_dist) + unit_str + newLine;
                float superE=0f ;
                if (myDB.myUnit == 1) { 
                    superE = (myDB.speedLimit*myDB.speedLimit / 15f / myDB.hRoadData[i].getRadius() - myDB.sideFrictionCoef) * 100f;
                } else if (myDB.myUnit == 2) { 
                    superE = (myDB.speedLimit*myDB.speedLimit / 127f / myDB.hRoadData[i].getRadius() - myDB.sideFrictionCoef) * 100f;
                }
                // or using MPH speed, AASHTO 2004, pp.146 Eq 3-10
                //superE = (speedLimit ^ 2 / 15 / hRoadData(i).getRadius() - sideFrictionCoef) * 100
                // check if superelevation <0
                if (superE < 0) {
                    superE = 0;
                }

                superE = CInt(Math.ceil(superE / 2)) * 2; // round to 2%//s
                rpt += "   Superelevation = " + CStr(superE) + "% " + newLine;
                if (superE > myDB.maxSuperelevation * 100f) { 
                    rpt += "*** Warning: Exceed max. superelevation " + CStr(myDB.maxSuperelevation * 100f)+ "% ***" + newLine;
                }

                float maxCSpd ;
                if (myDB.myUnit == 1 ) {
                    maxCSpd = new Double(Math.sqrt(myDB.hRoadData[i].getRadius() * 15f * (myDB.sideFrictionCoef + superE / 100f))).floatValue();
                    rpt += "   Max. Curve Speed = " + CStr(Math.round(maxCSpd)) + " MPH" + newLine;
                } else if (myDB.myUnit == 2) { 
                    maxCSpd = new Double(Math.sqrt(myDB.hRoadData[i].getRadius() * 127f * (myDB.sideFrictionCoef + superE / 100f))).floatValue();
                    rpt += "   Max. Curve Speed = " + CStr(Math.round(maxCSpd)) + " Km/h" + newLine;
                }
                rpt += newLine;
            } // if
        }
        return rpt;
    }   // getHorizonDesignSummary
    
    public int findTangentDBIndex(int parent) {
        int i, foundIdx ;
        foundIdx = -1;
        for (i = 0 ; i<myDB.hAlignMarkCount; i++) {
            if (myDB.hAlignMarks[i].getParentIndex() == parent) { 
                foundIdx = i;
                break;
            }
        }   //Next
        return foundIdx;
    }    //findTangentDBIndex

    // VRML file generation =================================
    public void createVRMLFile(){
        int i, db_size ;
        float x1, y1, x2, y2, dist=0f ;
        int dbCount = 0;
        mPointF lastMark ;
        byte lastMarkType ;
        boolean isLineSeg =false ;
        // compute # of data points needed to store 3D data at every ComputeStepSize
        db_size = CInt(Math.ceil(total_roadLen / ComputeStepSize));
        vrmlPoints_roadCenter = new RoadGeoDB[db_size];

        ProgressBar ProgressBar1  =
                new ProgressBar("Creating 3D Model ...", 400);
        ProgressBar1.show();
        // init starting point data
        lastMark = myDB.elevationMarks[0].getLocation();
        lastMarkType = myDB.elevationMarks[0].getSegmentType();
        x1 = lastMark.X / myDB.imageScale;
        y1 = lastMark.Y / myDB.imageScale;
 //       frmProgress.ProgressBar1.Maximum = elevationMarkCount
        dbCount=0 ;
        for (i = 1 ; i< myDB.elevationMarkCount ; i++) {
            ProgressBar1.updateProgress();
            x2 = myDB.elevationMarks[i].getLocation().X / myDB.imageScale; // ft/m
            y2 = myDB.elevationMarks[i].getLocation().Y / myDB.imageScale;
            switch(myDB.elevationMarks[i].getSegmentType()) {
                case 1:  // line
                    // linear distance
                    dist = distanceOf(lastMark, myDB.elevationMarks[i].getLocation()) / myDB.imageScale;
                    isLineSeg = true;
                    break;
                case 2:  // curve
                    dist = calculateArcLength(i) ;   // radius in feet already
                    isLineSeg = false;
                    break;
                case 3:  // tangent point, i>0
                    if ((lastMarkType == 1) || (lastMarkType == 3)) { 
                        //previous point belongs to a line
                        // linear distance
                        dist = distanceOf(lastMark, myDB.elevationMarks[i].getLocation()) / myDB.imageScale;
                        isLineSeg = true;
                    } else if (lastMarkType == 2) { //Then
                        // previous point belongs to a curve
                        dist = calculateArcLength(i);
                        isLineSeg = false;
                    }
                    break;
            }   //End Select
            int numSteps, j ;
            if (isLineSeg) { //Then
                numSteps = CInt(Math.floor(dist / ComputeStepSize));
                // line segment interpolation
                float vx, vy, dist_j ;
                vx = (x2 - x1) / dist;   // line vector for segment i
                vy = (y2 - y1) / dist;
//System.out.println("dbCount="+dbCount);
                
                for (j = 0; j<= numSteps; j++) {
                    vrmlPoints_roadCenter[dbCount] = new RoadGeoDB();
                    dist_j = myDB.elevationMarks[i - 1].getDistance() + j * ComputeStepSize;
                    vrmlPoints_roadCenter[dbCount].Load(x1 + vx * j * ComputeStepSize, y1 + vy * j * ComputeStepSize, calcDesignElevation(dist_j), 0);
                    dbCount += 1;
                }   //Next j
            } else {
                // curve segment interpolation every ComputeStepSize
                int parentIndex ;
                float cRadius, d_theta, dx, dy, cx, cy ;
                float alpha1, alpha2, d_alpha ;
                mPointF center ;
                parentIndex = myDB.elevationMarks[i].getParentIndex();
                cRadius = myDB.hRoadData[parentIndex].getRadius();
                center = myDB.hRoadData[parentIndex].getPoint1();
                cx = center.X / myDB.imageScale;  // convert to ft or m
                cy = center.Y / myDB.imageScale;
                d_theta = ComputeStepSize / cRadius; // angle increment in radian, unsigned
                dx = x1 - cx;
                dy = y1 - cy;
                alpha1 = new Double(Math.atan2(dy, dx)).floatValue();  // return start point angle of curve [-pi, pi]
                dx = x2 - cx;
                dy = y2 - cy;
                alpha2 = new Double(Math.atan2(dy, dx)).floatValue();  // return start point angle of curve [-pi, pi]
                d_alpha = alpha2 - alpha1;
                if (d_alpha > Math.PI ) {   //Then
                    d_alpha = new Double(d_alpha - 2 * Math.PI).floatValue();
                } else if (d_alpha < -Math.PI) {    // Then
                    d_alpha = new Double(2 * Math.PI + d_alpha).floatValue();
                }   //End If
                numSteps = CInt(Math.floor(Math.abs(d_alpha / d_theta))) ;
                d_theta = d_alpha / numSteps;    // signed delta theta
                float rx, ry, dist_j, superE=0f ;
                // calculate superelevation
                if (myDB.myUnit==1) {
                    superE = ((myDB.speedLimit * 1.467f)*(myDB.speedLimit * 1.467f) / 32.2f / cRadius - myDB.sideFrictionCoef) * 100f;
                } else if (myDB.myUnit==2) {
                    superE = ((myDB.speedLimit*myDB.speedLimit) / 127f / cRadius - myDB.sideFrictionCoef) * 100f;                    
                }
                superE = (CInt(superE / 2) + 1) * 0.02f; // ceiling round to 2%//s

                for (j=0;j<numSteps;j++) {
                    rx = new Double(cx + cRadius * Math.cos(alpha1 + j * d_theta)).floatValue();
                    ry = new Double(cy + cRadius * Math.sin(alpha1 + j * d_theta)).floatValue();
                    vrmlPoints_roadCenter[dbCount] = new RoadGeoDB();
                    dist_j = myDB.elevationMarks[i - 1].getDistance() + cRadius * Math.abs(j * d_theta);
                    vrmlPoints_roadCenter[dbCount].Load(rx, ry, calcDesignElevation(dist_j), superE);
                    dbCount += 1;
                }   //Next j

            }   // End If
            x1 = x2;
            y1 = y2;
            lastMark = myDB.elevationMarks[i].getLocation();
            lastMarkType = myDB.elevationMarks[i].getSegmentType();

        }   //Next i
        // save last data point
        vrmlPoints_roadCenter[dbCount] = new RoadGeoDB();
        vrmlPoints_roadCenter[dbCount].Load(x1, y1, myDB.vConstructMarks[myDB.vConstructMarkCount - 1].getElevation(), 0);
        dbCount += 1;

 //       frmProgress.ProgressBar1.Maximum = dbCount
        // smooth superelevation if abs(difference) greater that 0.01
        float d_se ;
        // up edge
        for (i=1; i<dbCount; i++) {
            d_se = vrmlPoints_roadCenter[i].Se - vrmlPoints_roadCenter[i - 1].Se;
            if (d_se > 0.01f) { // Then
                vrmlPoints_roadCenter[i].Se = vrmlPoints_roadCenter[i - 1].Se + 0.01f;
            }   //End If
  //          frmProgress.ProgressBar1.Value = i
            ProgressBar1.updateProgress();
        }   //Next
        // down edge
        for (i=dbCount-1;i>=1; i--) {
            d_se = vrmlPoints_roadCenter[i].Se - vrmlPoints_roadCenter[i - 1].Se;
            if (d_se < -0.01f) {// Then
                vrmlPoints_roadCenter[i - 1].Se = vrmlPoints_roadCenter[i].Se + 0.01f;
            }
//            frmProgress.ProgressBar1.Value = i
            ProgressBar1.updateProgress();
        }   //Next

        // ====================================
        // create left & right shoulder curve/boundary
        RoadGeoDB[] vrmlPoints_roadLeft = new RoadGeoDB[dbCount] ;
        RoadGeoDB[] vrmlPoints_roadRight = new RoadGeoDB[dbCount] ;
        RoadGeoDB temp_pointR = new RoadGeoDB();
        RoadGeoDB temp_pointL = new RoadGeoDB();
        float vec_px, vec_py ; // vector perpenticular to p(i-1)-p(i)
        double rdx, rdy, sLen ;  // road segment length
        vrmlPoints_roadRight[0] = new RoadGeoDB();
        vrmlPoints_roadLeft[0] = new RoadGeoDB();
        for (i=1; i<dbCount; i++) {
    //        frmProgress.ProgressBar1.Value = i
            ProgressBar1.updateProgress();
            vrmlPoints_roadRight[i] = new RoadGeoDB();
            vrmlPoints_roadLeft[i] = new RoadGeoDB();

            rdx = vrmlPoints_roadCenter[i].X - vrmlPoints_roadCenter[i - 1].X;
            rdy = vrmlPoints_roadCenter[i].Y - vrmlPoints_roadCenter[i - 1].Y;
            sLen = Math.sqrt(rdx*rdx + rdy*rdy);
            vec_px = new Double(rdy / sLen).floatValue();
            vec_py = new Double(-rdx / sLen).floatValue();
            // right side ---------------
            vrmlPoints_roadRight[i].Load(vrmlPoints_roadCenter[i].X + (0.5f * myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth) * vec_px, 
                vrmlPoints_roadCenter[i].Y + (0.5f * myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth) * vec_py, 
                vrmlPoints_roadCenter[i].Ele, 
                vrmlPoints_roadCenter[i].Se);

            temp_pointR.Load(vrmlPoints_roadCenter[i - 1].X + (0.5f * myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth) * vec_px, 
                vrmlPoints_roadCenter[i - 1].Y + (0.5f * myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth) * vec_py, 
                vrmlPoints_roadCenter[i - 1].Ele, 
                vrmlPoints_roadCenter[i - 1].Se);
            if (i > 1) {    // Then
                vrmlPoints_roadRight[i - 1].X = 0.5f * (vrmlPoints_roadRight[i - 1].X + temp_pointR.X);
                vrmlPoints_roadRight[i - 1].Y = 0.5f * (vrmlPoints_roadRight[i - 1].Y + temp_pointR.Y);
                vrmlPoints_roadRight[i - 1].Ele = 0.5f * (vrmlPoints_roadRight[i - 1].Ele + temp_pointR.Ele);
            } else if (i == 1) {	// Then
                vrmlPoints_roadRight[0].Load(temp_pointR.X, temp_pointR.Y, temp_pointR.Ele, temp_pointR.Se);
            }   //End If
            // left side ---------------
            vrmlPoints_roadLeft[i].Load(vrmlPoints_roadCenter[i].X - (0.5f * myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth) * vec_px, 
                vrmlPoints_roadCenter[i].Y - (0.5f * myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth) * vec_py, 
                vrmlPoints_roadCenter[i].Ele, 
                vrmlPoints_roadCenter[i].Se);

            temp_pointL.Load(vrmlPoints_roadCenter[i - 1].X - (0.5f * myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth) * vec_px, 
                vrmlPoints_roadCenter[i - 1].Y - (0.5f * myDB.myRoadLaneSizes * myDB.myLaneWidth + myDB.myShoulderWidth) * vec_py,
                vrmlPoints_roadCenter[i - 1].Ele, 
                vrmlPoints_roadCenter[i - 1].Se);
            if (i > 1)  {// Then
                vrmlPoints_roadLeft[i - 1].X = 0.5f * (vrmlPoints_roadLeft[i - 1].X + temp_pointL.X);
                vrmlPoints_roadLeft[i - 1].Y = 0.5f * (vrmlPoints_roadLeft[i - 1].Y + temp_pointL.Y);
                vrmlPoints_roadLeft[i - 1].Ele = 0.5f * (vrmlPoints_roadLeft[i - 1].Ele + temp_pointL.Ele);
            } else if (i == 1) {    // Then
                vrmlPoints_roadLeft[0].Load(temp_pointL.X, temp_pointL.Y, temp_pointL.Ele, temp_pointL.Se);
            }   //End If

        }   //Next

        // =====================================================
        // create VRML file
        String myVrml_str="", point_str="", base_point_str = "", index_str="" ;
        String texCoordIndexStr="" ;
        String imageStr="" ;
        float init_vehPos=0.0f ;
        if (myDB.myUnit == 1) { 
            init_vehPos = (0.5f*myDB.myRoadLaneSizes * myDB.myLaneWidth  - 0.65f * myDB.myLaneWidth) * myDB.FT2M;
        } else if (myDB.myUnit == 2) {  // Then // metric
            init_vehPos = 0.5f*myDB.myRoadLaneSizes * myDB.myLaneWidth  - 0.65f * myDB.myLaneWidth;
        }   //End If

        myVrml_str = "#VRML V2.0 utf8" + newLine;
        myVrml_str += "WorldInfo {title \"ITS Interdisciplinary Lab Course Prototype\"" + newLine;
        myVrml_str += "info [\"(c) Copyright 2006 ITS Lab, Center For Transportation Studies, University of Minnesota\" ]}";
        myVrml_str += "NavigationInfo {headlight FALSE avatarSize [.2 1.6 .7]}" + newLine;
        myVrml_str += "Background {skyColor [0.0 0.2 0.7, 0.0 0.5 1.0, 1.0 1.0 1.0]" + newLine;
        myVrml_str += "skyAngle [ 1.309, 1.571 ]" + newLine ;
        myVrml_str += "groundColor [0.1 0.10 0.0, 0.4 0.25 0.2, 0.6 0.60 0.6,]}" + newLine;
        myVrml_str += "DirectionalLight { ambientIntensity 0.9  intensity 0.9 color 0.7 0.7 0.6 direction -1 -1 -1 on TRUE }";

        //myVrml_str += " Background {skyColor [0.0 0.2 0.7,0.0 0.5 1.0,1.0 1.0 1.0]"
        //myVrml_str += " skyAngle [ 1.309, 1.571 ] groundColor [0.1 0.10 0.0,0.4 0.25 0.2,0.6 0.60 0.6,]"
        //myVrml_str += " groundAngle [ 1.309, 1.571 ] "
        //myVrml_str += " topUrl ""sky_top.jpg"" "
        //myVrml_str += " frontUrl ""sky_fb.jpg"" "
        //myVrml_str += " leftUrl ""sky_lr.jpg"" "
        //myVrml_str += " backUrl ""sky_fb.jpg"" "
        //myVrml_str += " rightUrl ""sky_lr.jpg""} "

        int mid = CInt(dbCount / 2);
        Vector3D startPos=null, endPos=null ;
        Vector3D lookat=null ;
        if (myDB.myUnit == 1) {// Then
            startPos = new Vector3D(init_vehPos + vrmlPoints_roadCenter[0].X * myDB.FT2M, 1 + vrmlPoints_roadCenter[0].Ele * myDB.FT2M, vrmlPoints_roadCenter[0].Y * myDB.FT2M);
            endPos = new Vector3D(init_vehPos + vrmlPoints_roadCenter[dbCount - 1].X * myDB.FT2M, 1 + vrmlPoints_roadCenter[dbCount - 1].Ele * myDB.FT2M, vrmlPoints_roadCenter[dbCount - 1].Y * myDB.FT2M);
            lookat = new Vector3D(init_vehPos + vrmlPoints_roadCenter[1].X * myDB.FT2M, 1 + vrmlPoints_roadCenter[1].Ele * myDB.FT2M, vrmlPoints_roadCenter[1].Y * myDB.FT2M);
        } else if (myDB.myUnit == 2) {  // Then
            startPos = new Vector3D(init_vehPos + vrmlPoints_roadCenter[0].X, 1 + vrmlPoints_roadCenter[0].Ele, vrmlPoints_roadCenter[0].Y);
            endPos = new Vector3D(init_vehPos + vrmlPoints_roadCenter[dbCount - 1].X, 1 + vrmlPoints_roadCenter[dbCount - 1].Ele, vrmlPoints_roadCenter[dbCount - 1].Y);
            lookat = new Vector3D(init_vehPos + vrmlPoints_roadCenter[1].X, 1 + vrmlPoints_roadCenter[1].Ele, vrmlPoints_roadCenter[1].Y);
        } //End If
        Vector3D upAxis = new Vector3D(0, 1, 0);
        String viewpoint ;
        viewpoint = convertCameraModel(startPos, lookat, upAxis);

        myVrml_str += " Viewpoint { position " + startPos.toStr() + " orientation " + viewpoint + " fieldOfView 0.785 description \"Start\"} " + newLine;
        // optional 2nd viewpoint
        //If myUnit = 1 Then
        //point_str = (vrmlPoints_roadCenter(0).X * FT2M).ToString & " " _
        //    & (20 + vrmlPoints_roadCenter(0).Ele * FT2M).ToString & " " _
        //    & (vrmlPoints_roadCenter(0).Y * FT2M).ToString
        //ElseIf myUnit = 2 Then
        //    point_str = (vrmlPoints_roadCenter(0).X).ToString & " " _
        //        & (20 + vrmlPoints_roadCenter(0).Ele).ToString & " " _
        //        & (vrmlPoints_roadCenter(0).Y).ToString
        //End If
        //myVrml_str += " Viewpoint { position " & point_str & " orientation 1 0 0 -0.57 fieldOfView 0.785 description ""Aerial Camera 1""} " & newLine
        point_str = "";
        base_point_str = "" ;
        index_str = "";
        texCoordIndexStr = "";
        String keyStr, keyValStr, keyRadStr, keyValStrReverse, keyRadStrReverse ;
        keyStr = "";
        keyValStr = "" ;     // position string
        keyValStrReverse = "" ;  // reverse positioin string
        if (myDB.myUnit == 1) {// Then
            for (i=0; i<dbCount; i++) {
                // left/center/right points
                point_str += CStr(vrmlPoints_roadLeft[i].X * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Ele * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Y * myDB.FT2M) + ", " + newLine;
                point_str += CStr(vrmlPoints_roadCenter[i].X * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadCenter[i].Ele * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadCenter[i].Y * myDB.FT2M) + ", " + newLine;
                point_str += (vrmlPoints_roadRight[i].X * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadRight[i].Ele * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadRight[i].Y * myDB.FT2M) + ", " + newLine;

                base_point_str += CStr(vrmlPoints_roadLeft[i].X * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Ele * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Y * myDB.FT2M) + ", " + newLine;
                base_point_str += CStr(vrmlPoints_roadLeft[i].X * myDB.FT2M) + " 0 " 
                    + CStr(vrmlPoints_roadLeft[i].Y * myDB.FT2M) + ", " + newLine;
                base_point_str += CStr(vrmlPoints_roadCenter[i].X * myDB.FT2M) + " 0 " 
                    + CStr(vrmlPoints_roadCenter[i].Y * myDB.FT2M) + ", " + newLine;
                base_point_str += (vrmlPoints_roadRight[i].X * myDB.FT2M) + " 0 " 
                    + CStr(vrmlPoints_roadRight[i].Y * myDB.FT2M) + ", " + newLine;
                base_point_str += (vrmlPoints_roadRight[i].X * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadRight[i].Ele * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadRight[i].Y * myDB.FT2M) + ", " + newLine;
                
                // animation key fraction & key value for vehicle location
                keyStr += CStr((float)i / (float)dbCount) + ", " + newLine;

                keyValStr += CStr((vrmlPoints_roadCenter[i].X) * myDB.FT2M) + " " 
                    + CStr((vrmlPoints_roadCenter[i].Ele) * myDB.FT2M) + " " 
                    + CStr((vrmlPoints_roadCenter[i].Y) * myDB.FT2M) + ", " + newLine;
                // reverse travel path for opposit dir veh
                keyValStrReverse = CStr((vrmlPoints_roadCenter[i].X) * myDB.FT2M) + " " 
                    + CStr((vrmlPoints_roadCenter[i].Ele) * myDB.FT2M) + " " 
                    + CStr((vrmlPoints_roadCenter[i].Y) * myDB.FT2M) + ", " + keyValStrReverse + newLine;

   //             frmProgress.ProgressBar1.Value = i
                ProgressBar1.updateProgress();
            }   //Next
        } else if (myDB.myUnit == 2) {  // Then // metric
            for (i=0; i<dbCount; i++) {
                // left/center/right points
                point_str += CStr(vrmlPoints_roadLeft[i].X) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Ele) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Y) + ", " + newLine ;
                point_str += CStr(vrmlPoints_roadCenter[i].X) + " " 
                    + CStr(vrmlPoints_roadCenter[i].Ele) + " " 
                    + CStr(vrmlPoints_roadCenter[i].Y) + ", " + newLine;
                point_str += CStr(vrmlPoints_roadRight[i].X) + " " 
                    + CStr(vrmlPoints_roadRight[i].Ele) + " " 
                    + CStr(vrmlPoints_roadRight[i].Y) + ", " + newLine;
                
                base_point_str += CStr(vrmlPoints_roadLeft[i].X) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Ele) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Y) + ", " + newLine ;
                base_point_str += CStr(vrmlPoints_roadLeft[i].X) + " 0 " 
                    + CStr(vrmlPoints_roadLeft[i].Y) + ", " + newLine ;
                base_point_str += CStr(vrmlPoints_roadCenter[i].X) + " 0 " 
                    + CStr(vrmlPoints_roadCenter[i].Y) + ", " + newLine;
                base_point_str += CStr(vrmlPoints_roadRight[i].X) + " 0 " 
                    + CStr(vrmlPoints_roadRight[i].Y) + ", " + newLine;
                base_point_str += CStr(vrmlPoints_roadRight[i].X) + " " 
                    + CStr(vrmlPoints_roadRight[i].Ele) + " " 
                    + CStr(vrmlPoints_roadRight[i].Y) + ", " + newLine;

                // animation key fraction & key value for vehicle location
                keyStr += CStr((float)i / (float)dbCount) + ", " + newLine;

                keyValStr += CStr(vrmlPoints_roadCenter[i].X) + " " 
                    + CStr(vrmlPoints_roadCenter[i].Ele) + " " 
                    + CStr(vrmlPoints_roadCenter[i].Y) + ", " + newLine;

                keyValStrReverse = CStr(vrmlPoints_roadCenter[i].X) + " " 
                    + CStr(vrmlPoints_roadCenter[i].Ele) + " " 
                    + CStr(vrmlPoints_roadCenter[i].Y) + ", " + keyValStrReverse + newLine;

    //            frmProgress.ProgressBar1.Value = i
                ProgressBar1.updateProgress();
            }   //Next
        }   //End If
        // ===================================================
        // Road Barrier data points
        String barrier_point_str = "";
        float barrier_height = 1f;  // 1 meter
        if (myDB.myUnit == 1) { // Then // US customary
            // left/right barrier data points
            for (i=0;i<dbCount;i++) {
      //          frmProgress.ProgressBar1.Value = i
                ProgressBar1.updateProgress();
                barrier_point_str += CStr(vrmlPoints_roadLeft[i].X * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Ele * myDB.FT2M + barrier_height) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Y * myDB.FT2M) + ", " + newLine;
                barrier_point_str += CStr(vrmlPoints_roadLeft[i].X * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Ele * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Y * myDB.FT2M) + ", " + newLine;
                barrier_point_str += CStr(vrmlPoints_roadRight[i].X * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadRight[i].Ele * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadRight[i].Y * myDB.FT2M) + ", " + newLine;
                barrier_point_str += CStr(vrmlPoints_roadRight[i].X * myDB.FT2M) + " " 
                    + CStr(vrmlPoints_roadRight[i].Ele * myDB.FT2M + barrier_height) + " " 
                    + CStr(vrmlPoints_roadRight[i].Y * myDB.FT2M) + ", " + newLine;
            }   //Next
        } else if (myDB.myUnit == 2) {// Then // metric
            // left/right barrier data points, from 3N to 4N-1
            for (i=0; i<dbCount;i++) {
       //         frmProgress.ProgressBar1.Value = i
                ProgressBar1.updateProgress();
                barrier_point_str += CStr(vrmlPoints_roadLeft[i].X) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Ele + barrier_height) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Y) + ", " + newLine;
                barrier_point_str += CStr(vrmlPoints_roadLeft[i].X) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Ele) + " " 
                    + CStr(vrmlPoints_roadLeft[i].Y) + ", " + newLine;
                barrier_point_str += CStr(vrmlPoints_roadRight[i].X) + " " 
                    + CStr(vrmlPoints_roadRight[i].Ele) + " " 
                    + CStr(vrmlPoints_roadRight[i].Y) + ", " + newLine;
                barrier_point_str += CStr(vrmlPoints_roadRight[i].X) + " " 
                    + CStr(vrmlPoints_roadRight[i].Ele + barrier_height) + " " 
                    + CStr(vrmlPoints_roadRight[i].Y) + ", " + newLine;
            }   //Next
        }   //End If

        keyRadStr = "" ;
        keyRadStrReverse = "" ;
        float dvx, dvy ;
        String ang_str="" ;
        float ang, init_ang, end_ang ;
        dvx = (vrmlPoints_roadRight[dbCount - 1].X - vrmlPoints_roadCenter[dbCount - 1].X);
        dvy = (vrmlPoints_roadRight[dbCount - 1].Y - vrmlPoints_roadCenter[dbCount - 1].Y);
        end_ang = new Double(Math.PI - Math.atan2(dvy, dvx)).floatValue();

        dvx = (vrmlPoints_roadRight[0].X - vrmlPoints_roadCenter[0].X);
        dvy = (vrmlPoints_roadRight[0].Y - vrmlPoints_roadCenter[0].Y);
        init_ang = new Double(Math.PI - Math.atan2(dvy, dvx)).floatValue();

        // texture patching
        String barrier_index_str = "";
        String barrier_texCoordIndexStr = "";
        String base_left_wall_index_str = "" ;
        String base_right_wall_index_str = "" ;
        String base_front_wall_index_str = "" ;
        String base_rear_wall_index_str = "" ;
        String base_bottom_index_str = "" ;
        for (i = 0; i<=dbCount - 2; i++) {
            // left quad road
            index_str += CStr(3 * i) + "," + CStr(3 * i + 1) + "," + CStr(3 * (i + 1) + 1) + "," + CStr(3 * (i + 1)) + "," + CStr(3 * i) + ",-1," + newLine;
            // right quad road
            index_str +=  CStr(3 * i + 1) + "," +  CStr(3 * i + 2) + "," +  CStr(3 * (i + 1) + 2) + "," +  CStr(3 * (i + 1) + 1) + "," +  CStr(3 * i + 1) + ",-1," + newLine;

            // base left wall
            base_left_wall_index_str += CStr(5*(i+1)) + "," + CStr(5*(i+1) + 1) + "," + CStr(5 * i + 1) + "," + CStr(5 * i) + "," + CStr(5*(i+1)) + ",-1," + newLine;
            // right base wall
            base_right_wall_index_str += CStr(5*i+4) + "," + CStr(5*i + 3) + "," + CStr(5 * (i+1)+3) + "," + CStr(5 *(i+1)+4) + "," + CStr(5*i+4) + ",-1," + newLine;
            // bottom base
            base_bottom_index_str += CStr(5*i+3) + "," + CStr(5*i+1) + "," + CStr(5*(i+1)+1) + "," + CStr(5*(i+1)+3) + "," + CStr(5*i+3) + ",-1," + newLine;
            
            // left barrier quad
            barrier_index_str +=  CStr(4 * i) + "," +  CStr(4 * i + 1) + "," +  CStr(4 * (i + 1) + 1) + "," +  CStr(4 * (i + 1)) + "," +  CStr(4 * i) + ",-1," + newLine;
            // right barrier quad 
            barrier_index_str +=  CStr(4 * i + 2) + "," +  CStr(4 * i + 3) + "," +  CStr(4 * (i + 1) + 3) + "," +  CStr(4 * (i + 1) + 2) + "," +  CStr(4 * i + 2) + ",-1," + newLine;

            // texture coordinate index str
            texCoordIndexStr += "1,0,3,2,1,-1,  0,1,2,3,4,-1" + newLine;
            barrier_texCoordIndexStr += "3,0,1,2,3,-1,  1,2,3,0,1,-1" + newLine;
    //        frmProgress.ProgressBar1.Value = i
            ProgressBar1.updateProgress();

            // animation for vehicle rotation
            dvx = (vrmlPoints_roadRight[i].X - vrmlPoints_roadCenter[i].X);
            dvy = (vrmlPoints_roadRight[i].Y - vrmlPoints_roadCenter[i].Y);
            ang = new Double(Math.PI - Math.atan2(dvy, dvx)).floatValue();
            //If ang < 0 Then
            //ang += 2 * Math.PI
            //End If
            ang_str = CStr(ang) ;
            keyRadStr += " 0.0 1.0 0.0 " + ang_str + newLine;
            keyRadStrReverse = " 0.0 1.0 0.0 " + ang_str + keyRadStrReverse + newLine;
        }   //Next
        // wall front
        base_front_wall_index_str = "0,1,2,3,4" ;
        // wall rear
        base_rear_wall_index_str = CStr(5*(dbCount-1))+","+CStr(5*(dbCount-1)+1)+","+
            CStr(5*(dbCount-1)+2)+","+CStr(5*(dbCount-1)+3)+","+CStr(5*(dbCount-1)+4)+"," ;
        
        keyRadStr += " 0.0 1.0 0.0 " + ang_str + newLine;
        keyRadStrReverse = " 0.0 1.0 0.0 " + ang_str + keyRadStrReverse + newLine;

        // select road pavement image file
        switch (CInt(myDB.myRoadLaneSizes)) {
            case 2:  // 2 lane highway
                imageStr = "\"http://128.101.111.90/Lab_Mod/roadtexture1.png\", \"roadtexture1.png\""  ;   // double quote "
                break;
            case 4:  // 4 lane highway
                imageStr = "\"http://128.101.111.90/Lab_Mod/roadtexture2.png\", \"roadtexture2.png\"" ;   // double quote "
                break;
            case 6:  // 6 lane highway
                imageStr = "\"http://128.101.111.90/Lab_Mod/roadtexture3.png\", \"roadtexture3.png\"" ;   // double quote "
                break;
        } 
        // road barrier image file
        String barrier_imageStr = "\"http://128.101.111.90/Lab_Mod/roadbarrier.png\", \"roadbarrier.png\"" ;    // double quote "
        
        // road wall dirt image file
        //String dirt_imageStr = "\"http://128.101.111.90/Lab_Mod/dirt.png\", \"dirt.png\"" ;    // double quote "

        // create EXTERNPROTO reference
        myVrml_str += " EXTERNPROTO toyota [][\"http://128.101.111.90/Lab_Mod/toyota_proto.wrl\", \"toyota_proto.wrl\" ]" + newLine;
        ProgressBar1.updateProgress();
        // ================================================
        // create road geometry vrml string
        myVrml_str += getShapeTextureStr("Road", point_str, index_str, imageStr, texCoordIndexStr);
        // left/right wall
        String wall_color = "0 0.8 0" ;
        String bottom_color = "0.8 0.8 0" ;
        myVrml_str += getShapeMaterialStr("LTBaseWall", base_point_str, base_left_wall_index_str, wall_color, true);
        myVrml_str += getShapeMaterialStr("RTBaseWall", base_point_str, base_right_wall_index_str, wall_color, false);
        myVrml_str += getShapeMaterialStr("FTBaseWall", base_point_str, base_front_wall_index_str, wall_color, false);
        myVrml_str += getShapeMaterialStr("RRBaseWall", base_point_str, base_rear_wall_index_str, wall_color, false);
        myVrml_str += getShapeMaterialStr("BTBaseWall", base_point_str, base_bottom_index_str, bottom_color, false);
        
        myVrml_str += getShapeTextureStr("Barrier", barrier_point_str, barrier_index_str, barrier_imageStr, barrier_texCoordIndexStr);
        ProgressBar1.updateProgress();
        //dvx = (vrmlPoints_roadRight(0).X - vrmlPoints_roadCenter(0).X)
        //dvy = (vrmlPoints_roadRight(0).Y - vrmlPoints_roadCenter(0).Y)
        //ang_str = (0.5 * Math.PI + Math.Atan2(dvy, dvx)).ToString
        ang_str = CStr(init_ang);
        // create vehicle viewpoint vrml string
        myVrml_str += getVehicleStr("Veh", startPos.toStr(), ang_str);

        // create no viewpoint vehicle moving from opposite side
        myVrml_str += getReverseVehicleStr("VehR", endPos.toStr(), CStr(end_ang));
        ProgressBar1.updateProgress();
        // ================================================
        // animation clock
        float travelTime=0f ;
        if (myDB.myUnit == 1) { 
            travelTime = total_roadLen * 3600f / 5280f / myDB.speedLimit; // ft/MPH (sec)
        } else if (myDB.myUnit == 2) { 
            travelTime = total_roadLen * 3.6f / myDB.speedLimit; // m/Km/h (sec)
        }

        myVrml_str += "DEF Clock TimeSensor { cycleInterval " + CStr(travelTime) + " loop TRUE}" + newLine;
        myVrml_str += "DEF TravelPath PositionInterpolator { key [ " + keyStr + " ] keyValue [ " + keyValStr + " ]}" + newLine;
        myVrml_str += "DEF RotatePath OrientationInterpolator { key [ " + keyStr + " ] keyValue [ " + keyRadStr + " ]}" + newLine;
        myVrml_str += "DEF ReverseTravelPath PositionInterpolator { key [ " + keyStr + " ] keyValue [ " + keyValStrReverse + " ]}" + newLine;
        myVrml_str += "DEF ReverseRotatePath OrientationInterpolator { key [ " + keyStr + " ] keyValue [ " + keyRadStrReverse + " ]}" + newLine;

        myVrml_str += "ROUTE Clock.fraction_changed TO TravelPath.set_fraction" + newLine;
        myVrml_str += "ROUTE TravelPath.value_changed TO Veh.set_translation" + newLine;
        myVrml_str += "ROUTE Clock.fraction_changed TO RotatePath.set_fraction" + newLine;
        myVrml_str += "ROUTE RotatePath.value_changed TO Veh.set_rotation" + newLine;

        // reverse path
        myVrml_str += "ROUTE Clock.fraction_changed TO ReverseTravelPath.set_fraction" + newLine;
        myVrml_str += "ROUTE ReverseTravelPath.value_changed TO VehR.set_translation" + newLine;
        myVrml_str += "ROUTE Clock.fraction_changed TO ReverseRotatePath.set_fraction" + newLine;
        myVrml_str += "ROUTE ReverseRotatePath.value_changed TO VehR.set_rotation" + newLine;
        ProgressBar1.updateProgress();
        // database created, save to a temp file
        String filename="" ;
        try {
            // check os info, Linux, Mac or Windows
            String osinfo = System.getProperty("os.name");
            String osarch = System.getProperty("os.arch");
        //System.out.println(osinfo+","+osarch);
             
            if (osinfo.indexOf("Windows")>=0) {
                //filename = "C:\\vrml_db" ;
                String username = System.getProperty("user.name");
                filename = "C:\\Documents and Settings\\"+username+"\\Desktop\\vrml_db" ;
            } else {    //if (osinfo.indexOf("Linux")>=0){
                filename = "vrml_db" ;
            } 
            BufferedWriter out = new BufferedWriter(new FileWriter(filename));
            out.write(myVrml_str);
            out.flush();
            out.close();
        } catch (IOException e) {
             
            String err_msg = e.toString() ;
            //System.out.println("vDrawArea:createVRMLFile:"+err_msg);
            if (err_msg.indexOf("FileNotFound")>0) {
                filename = "C:\\vrml_db" ;
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(filename));
                    out.write(myVrml_str);
                    out.flush();
                    out.close();
                    sb.setStatusBarText(1, "Restore animation file in root directory!") ; 
                } catch (IOException ioe) {
                    System.out.println("vDrawArea:createVRMLFile:"+ioe.toString());   
                    sb.setStatusBarText(1, "Creating 3D Model, "+err_msg) ; //Error:
                }
            } // if file not found
        }
        ProgressBar1.dispose();
    }   // createVRMLFile
    
    // generate vehicle VRML string
    private String getVehicleStr(String vehName, String translate, String angle) {
        String veh_str ;
        float lane_width_m ;
        float camera_offset ;
        int i ;
        
        if (myDB.myUnit == 1) { 
            // US customary unit
            lane_width_m = myDB.myLaneWidth * myDB.FT2M;
        } else {
            lane_width_m = myDB.myLaneWidth;
        }
        veh_str = "DEF " + vehName + " Transform {" +
        " translation " + translate +
        " rotation 0 1 0 " + angle +
        " children [";
        camera_offset = 1.75f;
        for (i = 0 ;i< myDB.myRoadLaneSizes / 2 ;i++){
            camera_offset = 1.5f + lane_width_m * i;
            veh_str += "DEF VEH_VIEW" + CStr(i + 1) + " Viewpoint {" +
            " description \"Lane" + CStr(i + 1) + "\" orientation 1 0 0 -0.03  position " + CStr(camera_offset) + " 1.5 -1.4 }" + newLine;
        }   //Next i
        veh_str += "DEF VEH_BIRDVIEW Viewpoint {" +
        " description \"Vehicle Top Bird View\" orientation 1 0 0 -1.57  position " + CStr(camera_offset) + " 40 0 }";

        //include a vehice
        veh_str += "Transform { rotation 0 1 0 -3.14 translation 1.75 0 0 children [toyota {}]}";
        veh_str += "]}";

        return veh_str + newLine;
    }   //getVehicleStr
    
    // generate opposite direction vehicle string
    private String getReverseVehicleStr(String vehName, String translate, String angle) {
        String veh_str ;

        veh_str = "DEF " + vehName + " Transform {" +
        " translation " + translate +
        " rotation 0 1 0 " + angle +
        " children [";

        //include a vehice
        veh_str += "Transform { rotation 0 1 0 0 translation -1.75 0 0 children [toyota {}]}";
        veh_str += "]}";

        return veh_str + newLine;
    }   //getReverseVehicleStr
    
    // generate VRML shape texture string
    private String getShapeTextureStr( 
                String name , 
                String points , 
                String coordIndex , 
                String imageName , 
                String texCoordIndexStr ) {
        String vrml_str ;
        vrml_str = "DEF " + name + " Shape{";
        vrml_str += " appearance Appearance{texture ImageTexture{url[" + imageName + "]}}" + newLine;
        vrml_str += " geometry IndexedFaceSet{ ccw FALSE creaseAngle .785 " + newLine;
        vrml_str += "  coord Coordinate{point[";
        // point data here
        vrml_str += points;
        vrml_str += "]}" + newLine;
        vrml_str += "  coordIndex[";
        // coordinate index here;
        vrml_str += coordIndex;
        vrml_str += "]" + newLine;
        //vrml_str += "  normal Normal{vector [0 1 0]}" + newLine

        // texture mapping
        vrml_str += "texCoord TextureCoordinate {point [0 0, 1 0, 1 1, 0 1, 0 0]}" + newLine;
        vrml_str += "texCoordIndex [ " + texCoordIndexStr + " ]" + newLine;
        // end of geometry;
        vrml_str += " }" + newLine;
        // end of shape
        vrml_str += "}" + newLine;

        return vrml_str;
    }   //getShapeStr
    
    // create VRML shape material string
    private String getShapeMaterialStr( 
                String name , 
                String points , 
                String coordIndex , 
                String colorStr,
                boolean def_flag
                ) {
        String vrml_str ;
        vrml_str = "DEF " + name + " Shape{";
        vrml_str += " appearance Appearance{material Material{diffuseColor " + colorStr + "}}" + newLine;
        vrml_str += " geometry IndexedFaceSet{ ccw FALSE creaseAngle .785 " + newLine;
        if (def_flag) {
            vrml_str += " coord DEF road_base Coordinate{ point[";
            // point data here
            vrml_str += points;
            vrml_str += "]}" + newLine;
        } else {
            // use
            vrml_str += " coord USE road_base" + newLine;
        }
        vrml_str += "  coordIndex[";
        // coordinate index here;
        vrml_str += coordIndex;
        vrml_str += "]" + newLine;
        //vrml_str += "  normal Normal{vector [0 1 0]}" + newLine

        // end of geometry;
        vrml_str += " }" + newLine;
        // end of shape
        vrml_str += "}" + newLine;

        return vrml_str;
    }   //getShapeStr
    
    // create VRML camera look at vector
    public String convertCameraModel(Vector3D pos, Vector3D at, Vector3D up) {
        Vector3D n, tempV, v, normAxis, newY ;
        Quaternion normQuat, invNormQuat, yQuat, newYQuat, rotYQuat, rotQuat ;
        float tempD ;
        n = at.vSub(pos);
        n = n.vUnit();
        up = up.vUnit();
        tempD = up.vDot(n);
        tempV = n.vScale(tempD);
        v = up.vSub(tempV);
        v = v.vUnit();
        normAxis = new Vector3D(n.Y, -n.X, 0);
        if (normAxis.vDot(normAxis) < 0.00000001) {
            if (n.Z > 0.0) {
                normQuat = new Quaternion(new Vector3D(0.0f, 1.0f, 0.0f), 1.0f);
            } else {
                normQuat = new Quaternion(new Vector3D(0.0f, 0.0f, 0.0f), 1.0f);
            }
        } else {
            normAxis = normAxis.vUnit();
            normQuat = buildRotateQuaternion(normAxis, -n.Z);
        }
        invNormQuat = new Quaternion(normQuat.vectPart.vScale(-1), normQuat.realPart);
        yQuat = new Quaternion(new Vector3D(0.0f, 1.0f, 0.0f), 0.0f);
        newYQuat = normQuat.QQMul(yQuat);
        newYQuat = newYQuat.QQMul(invNormQuat);
        newY = newYQuat.vectPart;
        tempV = newY.vCross(v);
        if (tempV.vDot(tempV) < 0.00000001) {
            tempV = new Vector3D(0.0f, -v.Z, v.Y);
            if (tempV.vDot(tempV) < 0.00000001) {
                tempV = new Vector3D(v.Z, 0.0f, -v.X);
            }   //End If
        }   //End If
        tempV = tempV.vUnit();
        //      alert(tempV.x+" "+tempV.y+" "+tempV.z); // ***DEBUG
        rotYQuat = buildRotateQuaternion(tempV, newY.vDot(v));
        rotQuat = rotYQuat.QQMul(normQuat);
        return rotQuat.toAxisAngle();
    }   //convertCameraModel
    
    // VRML viewpoint calculation
    public Quaternion buildRotateQuaternion(Vector3D axis, float cosAngle) {
        double angle;
        float sinHalfAngle, cosHalfAngle ;
        angle = 0.0;
        sinHalfAngle = 0.0f;
        cosHalfAngle = 0.0f;
        Quaternion r ;
        if (cosAngle > 1.0) {
            cosAngle = 1.0f;
        }
        if (cosAngle < -1.0) {
            cosAngle = -1.0f;
        }
        angle = Math.acos(cosAngle);
        sinHalfAngle = new Double(Math.sin(angle / 2.0)).floatValue();;
        cosHalfAngle = new Double(Math.cos(angle / 2.0)).floatValue();
        r = new Quaternion(axis.vScale(sinHalfAngle), cosHalfAngle);
        return r;
    }   //buildRotateQuaternion
    
    // List station data in a table
    public void popStationData(){
        frmStationTable = new JFrame("View Station Data") ;
        frmStationTable.setSize(360, 300);
        //Make sure we have nice window decorations.
  //      frmStationTable.setDefaultLookAndFeelDecorated(true);
        frmStationTable.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // file menu
        MenuBar menu_bar = new MenuBar() ;
        Menu menu_file = new Menu("File") ;
        MenuItem file_savereport = new MenuItem("Save Data") ;
        MenuItem file_printreport = new MenuItem("Print") ;
        MenuItem separator = new MenuItem("-") ;
        MenuItem file_close = new MenuItem("Close") ;
        menu_file.add(file_savereport) ;   // add menu items
        menu_file.add(file_printreport) ;   // add menu items
        menu_file.add(separator) ;   // add menu items
        menu_file.add(file_close) ;   // add menu items

        menu_bar.add(menu_file) ;     // add menu
        frmStationTable.setMenuBar(menu_bar) ;

         file_savereport.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        station_saveData();  
                        sb.setStatusBarText(0, "Save Station Data") ;
                    } // actionPerformed
                } // ActionListener
         ) ; // file save report
         file_printreport.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        station_printData();  
                        sb.setStatusBarText(0, "Print Station Data") ;
                    } // actionPerformed
                } // ActionListener
         ) ; // file save report
         file_close.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        frmStationTable.dispose();
                    } // actionPerformed
                } // ActionListener
         ) ; // file Close
             
        String unitStr; 
        if (myDB.myUnit==1) {
            unitStr = " (ft)" ;
        } else {
            unitStr=" (m)" ;
        }
        String[] headers = { "Station ID", "Distance"+unitStr, "Elevation"+unitStr, "Grade" };
        int[] fieldSize = {15, 15, 15, 15} ;
        stationPrintStr = PrintText.StrFormat(0,"ID", fieldSize[0]) +
                          PrintText.StrFormat(0,"Distance"+unitStr, fieldSize[1]) +
                          PrintText.StrFormat(0,"Elavation"+unitStr, fieldSize[2]) + 
                          PrintText.StrFormat(0, "Grade", fieldSize[3]) + "\n" ;

        String[][] data = new String[myDB.elevationMarkCount][headers.length];
        int i;
        float grade ;
        for (i=0;i<myDB.elevationMarkCount; i++) {
            data[i][0] = CStr(i + 1) ;
            data[i][1] = CStr(Math.round(myDB.elevationMarks[i].getDistance()*1000f)/1000f) ;
            data[i][2] = CStr(Math.round(myDB.elevationMarks[i].getElevation()*1000f)/1000f);
            if (i==0) {
                data[i][3] = "N/A" ;
            } else {
                grade = (myDB.elevationMarks[i].getElevation()-myDB.elevationMarks[i-1].getElevation())/(myDB.elevationMarks[i].getDistance()-myDB.elevationMarks[i-1].getDistance()) ;
                data[i][3] = CStr(Math.round(grade*1000f)/1000f);
            }

            for (int j=0; j<4; j++){
                stationPrintStr += PrintText.StrFormat(0,data[i][j].toString(), fieldSize[j]) ;
            }
            stationPrintStr += "\n" ;
        }   // for i
        JTable table = new JTable(data, headers) {
        // override isCellEditable method, , 11/13/06
           public boolean isCellEditable(int row, int column) {
               // all un-editable
               return false;
           }    // isCellEditable method
        } ;
        table.setPreferredScrollableViewportSize(new Dimension(360, 300));
        table.setColumnSelectionAllowed(true) ;
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        //Add the scroll pane to this panel.
        frmStationTable.add(scrollPane);
        //Get the column model.
        javax.swing.table.TableColumnModel colModel = table.getColumnModel();
        //Get the column at index pColumn, and set its preferred width.
        colModel.getColumn(0).setPreferredWidth(24);   
        
            
        //Display the window.
        Dimension screen = getToolkit().getDefaultToolkit().getScreenSize();
        double top = 0.5*(screen.getWidth()-frmStationTable.getWidth());
        double left = 0.5*(screen.getHeight()-frmStationTable.getHeight());
        int x = new Double(top).intValue();
        int y = new Double(left).intValue();
        frmStationTable.setLocation(x, y);

        frmStationTable.pack();
        frmStationTable.setVisible(true);
        frmStationTable.show();
       
    }   // popStationData ;

    // display elevation profile data including vertical curves etc.
    public void popDesignedElevationData(){
        frmDesignedElevationTable = new JFrame("View Designed Elevation Data") ;
        frmDesignedElevationTable.setSize(300, 250);
        //Make sure we have nice window decorations.
  //      frmDesignedElevationTable.setDefaultLookAndFeelDecorated(true);
        frmDesignedElevationTable.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // file menu
        MenuBar menu_bar = new MenuBar() ;
        Menu menu_file = new Menu("File") ;
        MenuItem file_savedata = new MenuItem("Save Data") ;
        MenuItem file_printreport = new MenuItem("Print") ;
        MenuItem separator = new MenuItem("-") ;
        MenuItem file_close = new MenuItem("Close") ;
        menu_file.add(file_savedata) ;   // add menu items
        menu_file.add(file_printreport) ;   // add menu items
        menu_file.add(separator) ;   // add menu items
        menu_file.add(file_close) ;   // add menu items

        menu_bar.add(menu_file) ;     // add menu
        frmDesignedElevationTable.setMenuBar(menu_bar) ;

         file_savedata.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        saveElevationProfile();  
                        sb.setStatusBarText(0, "Save Designed Elevation Data") ;
                    } // actionPerformed
                } // ActionListener
         ) ; // file save report
         file_printreport.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        elevation_printData();  
                        sb.setStatusBarText(0, "Print Mass Data") ;
                    } // actionPerformed
                } // ActionListener
         ) ; // file save report
         file_close.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        frmDesignedElevationTable.dispose();
                    } // actionPerformed
                } // ActionListener
         ) ; // file Close
             
        String unitStr="" ; 
        if (myDB.myUnit==1) {
            unitStr = " (ft)" ;
        } else {
            unitStr=" (m)" ;
        }
        String[] headers = { "Data", "Distance"+unitStr, "Elevation"+unitStr };
        int[] fieldSize = {6, 16, 16} ;
        elevationPrintStr = PrintText.StrFormat(0,"Data", fieldSize[0]) +
                          PrintText.StrFormat(0,"Distance"+unitStr, fieldSize[1]) +
                          PrintText.StrFormat(0,"Elevation"+unitStr, fieldSize[2]) + "\n" ;

        String[][] data = new String[fillCutSteps][headers.length];
        int i;
        for (i=0;i<fillCutSteps; i++) {
            designElevation[i] = calcDesignElevation(i*ComputeStepSize) ;
            data[i][0] = CStr(i + 1) ;
            data[i][1] = CStr(Math.round(i*ComputeStepSize*1000f)/1000f) ;
            data[i][2] = CStr(Math.round(designElevation[i]*1000f)/1000f);

            for (int j=0; j<3; j++){
                elevationPrintStr += PrintText.StrFormat(0,data[i][j].toString(), fieldSize[j]) ;
            }
            elevationPrintStr += "\n" ;
        }   // for i
        JTable table = new JTable(data, headers) {
        // override isCellEditable method, , 11/13/06
           public boolean isCellEditable(int row, int column) {
               // all un-editable
               return false;
           }    // isCellEditable method
        } ;
        table.setPreferredScrollableViewportSize(new Dimension(300, 250));
        table.setColumnSelectionAllowed(true) ;
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        //Add the scroll pane to this panel.
        frmDesignedElevationTable.add(scrollPane);
        //Get the column model.
        javax.swing.table.TableColumnModel colModel = table.getColumnModel();
        //Get the column at index pColumn, and set its preferred width.
        colModel.getColumn(0).setPreferredWidth(24);   
        
            
        //Display the window.
        Dimension screen = getToolkit().getDefaultToolkit().getScreenSize();
        double top = 0.5*(screen.getWidth()-frmDesignedElevationTable.getWidth());
        double left = 0.5*(screen.getHeight()-frmDesignedElevationTable.getHeight());
        int x = new Double(top).intValue();
        int y = new Double(left).intValue();
        frmDesignedElevationTable.setLocation(x, y);

        frmDesignedElevationTable.pack();
        frmDesignedElevationTable.setVisible(true);
        frmDesignedElevationTable.show();
       
    }   // popDesignedElevationData ;
    
    // display mass diagram data profile, 
    // mass diagram is an integral/accumulation of cut/fill profile
    public void popMassData(){
        frmMassTable = new JFrame("View Mass Data") ;
        frmMassTable.setSize(300, 250);
        //Make sure we have nice window decorations.
  //      frmMassTable.setDefaultLookAndFeelDecorated(true);
        frmMassTable.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // file menu
        MenuBar menu_bar = new MenuBar() ;
        Menu menu_file = new Menu("File") ;
        MenuItem file_savereport = new MenuItem("Save Data") ;
        MenuItem file_printreport = new MenuItem("Print") ;
        MenuItem separator = new MenuItem("-") ;
        MenuItem file_close = new MenuItem("Close") ;
        menu_file.add(file_savereport) ;   // add menu items
        menu_file.add(file_printreport) ;   // add menu items
        menu_file.add(separator) ;   // add menu items
        menu_file.add(file_close) ;   // add menu items

        menu_bar.add(menu_file) ;     // add menu
        frmMassTable.setMenuBar(menu_bar) ;

         file_savereport.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        saveMassDiagram();  
                        sb.setStatusBarText(0, "Save Mass Data") ;
                    } // actionPerformed
                } // ActionListener
         ) ; // file save report
         file_printreport.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        mass_printData();  
                        sb.setStatusBarText(0, "Print Mass Data") ;
                    } // actionPerformed
                } // ActionListener
         ) ; // file save report
         file_close.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        frmMassTable.dispose();
                    } // actionPerformed
                } // ActionListener
         ) ; // file Close
             
        String unitStr="", unitStr3="" ; 
        if (myDB.myUnit==1) {
            unitStr = " (ft)" ;
            unitStr3 = " (yd^3)" ;
        } else {
            unitStr=" (m)" ;
            unitStr3=" (m^3)" ;
        }
        String[] headers = { "Data", "Distance"+unitStr, "Mass"+unitStr3 };
        int[] fieldSize = {6, 16, 16} ;
        massPrintStr = PrintText.StrFormat(0,"Data", fieldSize[0]) +
                          PrintText.StrFormat(0,"Distance"+unitStr, fieldSize[1]) +
                          PrintText.StrFormat(0,"Mass"+unitStr3, fieldSize[2]) + "\n" ;

        String[][] data = new String[fillCutSteps][headers.length];
        int i;
        for (i=0;i<fillCutSteps; i++) {
            data[i][0] = CStr(i + 1) ;
            data[i][1] = CStr(Math.round(i*ComputeStepSize*1000f)/1000f) ;
            data[i][2] = CStr(Math.round(accuMass[i]*1000f)/1000f);

            for (int j=0; j<3; j++){
                massPrintStr += PrintText.StrFormat(0,data[i][j].toString(), fieldSize[j]) ;
            }
            massPrintStr += "\n" ;
        }   // for i
        JTable table = new JTable(data, headers) {
        // override isCellEditable method, , 11/13/06
           public boolean isCellEditable(int row, int column) {
               // all un-editable
               return false;
           }    // isCellEditable method
        } ;
        table.setPreferredScrollableViewportSize(new Dimension(300, 250));
        table.setColumnSelectionAllowed(true) ;
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        //Add the scroll pane to this panel.
        frmMassTable.add(scrollPane);
        //Get the column model.
        javax.swing.table.TableColumnModel colModel = table.getColumnModel();
        //Get the column at index pColumn, and set its preferred width.
        colModel.getColumn(0).setPreferredWidth(24);   
        
            
        //Display the window.
        Dimension screen = getToolkit().getDefaultToolkit().getScreenSize();
        double top = 0.5*(screen.getWidth()-frmMassTable.getWidth());
        double left = 0.5*(screen.getHeight()-frmMassTable.getHeight());
        int x = new Double(top).intValue();
        int y = new Double(left).intValue();
        frmMassTable.setLocation(x, y);

        frmMassTable.pack();
        frmMassTable.setVisible(true);
        frmMassTable.show();
       
    }   // popMassData ;

    // display cut and fill data profile
    public void popCutAndFillData(){
        frmCutFillTable = new JFrame("View Cut and Fill Data") ;
        frmCutFillTable.setSize(300, 250);
        //Make sure we have nice window decorations.
  //      frmCutFillTable.setDefaultLookAndFeelDecorated(true);
        frmCutFillTable.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // file menu
        MenuBar menu_bar = new MenuBar() ;
        Menu menu_file = new Menu("File") ;
        MenuItem file_savereport = new MenuItem("Save Data") ;
        MenuItem file_printreport = new MenuItem("Print") ;
        MenuItem separator = new MenuItem("-") ;
        MenuItem file_close = new MenuItem("Close") ;
        menu_file.add(file_savereport) ;   // add menu items
        menu_file.add(file_printreport) ;   // add menu items
        menu_file.add(separator) ;   // add menu items
        menu_file.add(file_close) ;   // add menu items

        menu_bar.add(menu_file) ;     // add menu
        frmCutFillTable.setMenuBar(menu_bar) ;

         file_savereport.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        saveCutAndFill();  
                        sb.setStatusBarText(0, "Save Cut and Fill Data") ;
                    } // actionPerformed
                } // ActionListener
         ) ; // file save report
         file_printreport.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        cut_fill_printData();  
                        sb.setStatusBarText(0, "Print Cut and Fill Data") ;
                    } // actionPerformed
                } // ActionListener
         ) ; // file save report
         file_close.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        frmCutFillTable.dispose();
                    } // actionPerformed
                } // ActionListener
         ) ; // file Close
             
        String unitStr="" ; 
        if (myDB.myUnit==1) {
            unitStr = " (ft)" ;
        } else {
            unitStr=" (m)" ;
        }
        String[] headers = { "Data", "Distance"+unitStr, "Cut/Fill"+unitStr };
        int[] fieldSize = {6, 16, 16} ;
        fill_cutPrintStr = PrintText.StrFormat(0,"Data", fieldSize[0]) +
                          PrintText.StrFormat(0,"Distance"+unitStr, fieldSize[1]) +
                          PrintText.StrFormat(0,"Cut/Fill"+unitStr, fieldSize[2]) + "\n" ;

        String[][] data = new String[fillCutSteps][headers.length];
        int i;
        for (i=0;i<fillCutSteps; i++) {
            data[i][0] = CStr(i + 1) ;
            data[i][1] = CStr(Math.round(i*ComputeStepSize*1000f)/1000f) ;
            data[i][2] = CStr(Math.round(CutandFill[i]*1000f)/1000f);

            for (int j=0; j<3; j++){
                fill_cutPrintStr += PrintText.StrFormat(0,data[i][j].toString(), fieldSize[j]) ;
            }
            fill_cutPrintStr += "\n" ;
        }   // for i
        JTable table = new JTable(data, headers) {
        // override isCellEditable method, , 11/13/06
           public boolean isCellEditable(int row, int column) {
               // all un-editable
               return false;
           }    // isCellEditable method
        } ;
        table.setPreferredScrollableViewportSize(new Dimension(300, 250));
        table.setColumnSelectionAllowed(true) ;
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        //Add the scroll pane to this panel.
        frmCutFillTable.add(scrollPane);
        //Get the column model.
        javax.swing.table.TableColumnModel colModel = table.getColumnModel();
        //Get the column at index pColumn, and set its preferred width.
        colModel.getColumn(0).setPreferredWidth(24);   
        
            
        //Display the window.
        Dimension screen = getToolkit().getDefaultToolkit().getScreenSize();
        double top = 0.5*(screen.getWidth()-frmCutFillTable.getWidth());
        double left = 0.5*(screen.getHeight()-frmCutFillTable.getHeight());
        int x = new Double(top).intValue();
        int y = new Double(left).intValue();
        frmCutFillTable.setLocation(x, y);

        frmCutFillTable.pack();
        frmCutFillTable.setVisible(true);
        frmCutFillTable.show();
       
    }   // popCutAndFillData ;

    public void station_printData(){  // print station data
        try
        {
            //PrintSimpleText printReport = new PrintSimpleText(reportStr) ;
            PrintText printReport = new PrintText() ;
            printReport.print(stationPrintStr) ;

        }
        catch (Exception e){
                //do nothing
            System.out.println("Print Station Data:"+e.toString());
            sb.setStatusBarText(1, "Print Station Data, "+e.toString()) ; //Error: 
        } // try
       
    }// station print data

    public void elevation_printData(){  // print designed elevation profile data
        try
        {
            //PrintSimpleText printReport = new PrintSimpleText(reportStr) ;
            PrintText printReport = new PrintText() ;
            printReport.print(elevationPrintStr) ;

        }
        catch (Exception e){
                //do nothing
            System.out.println("Print Designed Elevation Data:"+e.toString());
            sb.setStatusBarText(1, "Print Designed Elevation Data, "+e.toString()) ; //Error: 
        } // try
       
    }// designed elevation profile print data
    
    public void mass_printData(){  // print mass diagram data
        try
        {
            //PrintSimpleText printReport = new PrintSimpleText(reportStr) ;
            PrintText printReport = new PrintText() ;
            printReport.print(massPrintStr) ;

        }
        catch (Exception e){
                //do nothing
            System.out.println("Print Mass Data:"+e.toString());
            sb.setStatusBarText(1, "Print Mass Data, "+e.toString()) ; //Error: 
        } // try
       
    }// mass diagram print data

    public void cut_fill_printData(){  // print cut and fill data
        try
        {
            //PrintSimpleText printReport = new PrintSimpleText(reportStr) ;
            PrintText printReport = new PrintText() ;
            printReport.print(fill_cutPrintStr) ;

        }
        catch (Exception e){
                //do nothing
            System.out.println("Print Cut and Fill Data:"+e.toString());
            sb.setStatusBarText(1, "Print Cut and Fill Data, "+e.toString()) ; //Error: 
        } // try
       
    }// cut and fill print data
    
    public void station_saveData(){  // save Station data to file
        try
        {
            FileDialog fd=new FileDialog(new Frame(),"Save Station Data", FileDialog.SAVE);
            fd.setFile("*.txt");
            fd.show();
            String fullpath=fd.getDirectory()+fd.getFile();
            fd.dispose();
//System.out.println("filepath="+fullpath);
            if(fullpath!=null) {
                BufferedWriter out = new BufferedWriter(new FileWriter(fullpath));
                //String reportStr = generateReport();
                out.write(stationPrintStr);
                out.flush();
                out.close();
            }
        }
        catch (Exception e){
                //do nothing
            System.out.println("Save Landmark Data File:"+e.toString());
            sb.setStatusBarText(1, "Saving Landmark Data, "+e.toString()) ; //Error: 
        } // try
       
    }// station save data
   

}   // vDrawArea


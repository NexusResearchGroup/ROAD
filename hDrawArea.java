/*
 * hDrawArea.java
 * Horizontal design class.
 *
 * Created on March 21, 2006, 4:12 PM
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
import java.awt.print.*;
import java.net.URL ;
import java.io.*;
import java.io.FilenameFilter;
import javax.swing.*;
import javax.swing.table.* ;

public class hDrawArea extends DoubleBufferedPanel
    implements MouseListener,  MouseMotionListener
{
    toolbar tb;             // toolbar
    statusbar sb;           // status bar
    final int grid = 8;           // drawarea grid size
    final String NO_MAP_MSG = "No contour image loaded.\n\nPlease import contour map as background\nimage first!" ;
    
    vDrawArea vDesign = new vDrawArea();
    Applet myApplet ;       // applet pointer from parent, Geometry_Design
    MenuItem ptr_edit_undo  ;
    MenuItem ptr_edit_redo  ;
    MenuItem ptr_edit_delete  ;

    // variables here
    public SHARED myDB = new SHARED() ;
    int toolbarIndex = 0 ;
    mPoint e0, e1 ;	//= DBNull
    boolean line_started = false ;
    boolean curve_started = false ;
    boolean modification_started = false ;

    public Image image = null ;
    public int imageW = 0, imageH=0 ;
    //Graphics g ;
    String contourImageFilepath ;
    mPoint translate = new mPoint(0, 0);
    mPoint scaledxlate = new mPoint(0, 0);
    mPoint translate_delta = new mPoint(0, 0);
    mPoint scaledxlate_delta = new mPoint(0, 0);
    boolean mouseHoldDown = false ;
    float draw_scale  = 1.0f ;
    int dataSelIndex = -1 ;
    
    // Horizontal geometry DB
    public int hRoadDataCount = 0;  // number of segments (line/curve)
    int segLogIndex  = -1;
    int[] segLogBuffer = new int[16] ;   // undo, redo log
    int markLogIndex = -1 ;
    int[] markLogBuffer = new int[16] ;  // undo, redo log
    int endMarkSize  = 2 ;   // square end mark, actual size (red) = 2*endMarkSize square

    // other variables
    int myAlpha = 255 ; // declare a Alpha variable
    //Dim hAlignMarkerPen, elevationMarkerPen, currentPen As Pen
    mPoint modificationInfo ;
    String design_filename = "" ;
    StationInfo sInfo ;         // landmark station info
    float calcMinRadius ;       // calculated minimum radius (Rv)
    int idSegment ;
    private String landmarkPrintStr ;   // save & print landmark data
    private String tangentPrintStr ;    // save and print tangent (PC, PT) data
    private JTable stationTable = new JTable();

    // window frame =================
    myWindow frmAbout ;
    myWindow frame_msgbox, frame_clearLandmarks ;
    myWindow frame_curveSetting, updateRadius ;
    myWindow frame_deleteSegment, frame_msgboxClearAll, frmElevationMarker ;
    myWindow frame_editCurveSetting ;
    myWindow frmEditElevationMarker ; 
    myWindow frmInsertElevationMarker ;     // 2/28/07
    myWindow frame_deleteTangent ;
    myWindow frame_saveVDesign ;    //11/13/06 added
    
    public myWindow frame_settingsDesign, frame_settingsContour;
    public myFrame frmVerticalAlign = new myFrame() ;
    JFrame frmLandmarkTable = new JFrame("View Landmark Data") ;
    JFrame frmTangentTable = new JFrame("View PC, PT Data") ;
    
    PrintUtilities hd_pu, vd_pu ;
    
    // Java GUI
    TextField txtEle = new TextField("0");
    Checkbox line;          // station,landmark option
    Checkbox curve;         // station,landmark option
    Checkbox tangent;       // station,landmark option
    TextField txtRadius;    // curve radius setting
    TextField txtEditRadius;    // edit curve radius setting
    TextField txtImgResol;    // edit contour image resolution setting
    TextField txtMapScale;    // edit contour map scale setting
    // design dettings
    TextField txtSpeed;         // design speed
    TextField txtMaxcut;         // max cut
    TextField txtMaxfill;         // max fill
    TextField txtMaxgrade;         // max grade
    TextField txtMingrade;          // min grade
    TextField txtReactiontime;         // reaction time
    TextField txtDecel;         // veh decel
    TextField txtFricoef;         // friction coefficient
    TextField txtSFricoef;         // side friction coefficient
    TextField txtVCurLen;         // max vertical curve length
    TextField txtHCurRadius;         // max horizontal curve radius
    TextField txtMaxsuperE;         // max super elevation
    
    Choice listRoadwidth ;
    TextField txtLanewidth;         // lane width
    Label lblRoadColor ;
    Choice listRoadColor ;
    TextField txtShoulderwidth;         // shoulder width
    TextField txtMarkersize;         // landmark size
    Label lblMarkerColor ;
    Choice listMarkerColor;           // end mark color
    Choice listUnit ;               // my unit
    
    Label lblUnit1, lblUnit2, lblUnit3, lblUnit4;
    Label lblUnit5, lblUnit6, lblUnit7, lblUnit8;
    Label mX, mY, parentID ;    // popElevationMarkForm
    
    //PageFormat printPageFormat = new PageFormat() ;
    Runnable runThread0 = null ;    // stop on red light
    public Thread tSetValign ;
    public boolean setValign_flag = false ; // accessed from toolbar class
    private boolean deleteTangent_flag = false ;
    private boolean popCurveSettings_flag = false ;
    private boolean popMsgBox_flag = false ;
    private boolean viewRoadOnly_flag = false ;
    private String msgBox_title = "" ;
    private String msgBox_message = "" ;
    private String item_clicked_str = "" ;      // used for right mouse delete
    private myIcon iconQ = new myIcon("question_mark") ;
    
    //==================================================================
    // class initialization
    hDrawArea()
    {
    }
    
    hDrawArea(toolbar t, statusbar s, MenuItem mUndo, MenuItem mRedo, MenuItem mDel)
    {
	tb = t;
        sb = s;
        ptr_edit_undo = mUndo ;
        ptr_edit_redo = mRedo ;
        ptr_edit_delete = mDel ;
	setBackground(Color.white);
	t.parent = this;
        s.parent = this;
        
        // =======================================================================
        // bring vertical design to top display thread
        // =====================================================================
        runThread0 = new Runnable() {
            public void run() {
                while (true) {
                    if (popMsgBox_flag){
                        popMessageBox1(msgBox_title, msgBox_message);
                        popMsgBox_flag = false ;
                    } else if (setValign_flag){
                        newstatus(10, " Vertical Curve Design");
                        setValign_flag = false ;
                    } else if (deleteTangent_flag) {
                       popDeleteTangent("Delete Tangent Data","Do you want to delete tangent data pair?");
                       deleteTangent_flag = false ;
                    } else if (popCurveSettings_flag) {
                        popCurveSettings(); 
                        popCurveSettings_flag = false ;
                    } else {
                        tSetValign.yield();
                        try {Thread.sleep(100) ;}
                        catch (InterruptedException ie) {} ;
                    }
                }
             }   // void run
        } ; // runThread 0
        tSetValign = new Thread(runThread0, "VerticalAlign") ;
        tSetValign.start() ;
    }

    // object initialization
    public void init(int flag) {
        if (this.getMouseListeners().length==0) {
            // add mouse listener if not already included
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        frmAbout = new myWindow();
        myDB.imageScale = (float)myDB.ContourImageResolution / (float)myDB.ContourScale;  //  // pixel/ft
        if (flag==0) {
            myDB.hAlignMarkCount = 0;
            myDB.elevationMarkCount = 0;
            myDB.vConstructMarkCount = 0;
            myDB.currentElevationMarker = new mPointF(0f, 0f);
        }
        myDB.curveRadius = myDB.minHCurveRadius ;   // set to min
        //animationFlag = false;
        
        int i ;
        for (i=0;i<segLogBuffer.length;i++) {
            segLogBuffer[i] = -1;
            markLogBuffer[i] = -1;
        }
        push2SegLogBuffer(hRoadDataCount); 
        push2MarkLogBuffer(myDB.elevationMarkCount); 
        
        sb.setStatusBarText(3, new Float(Math.round(draw_scale*10f)/10f).toString()) ;
        processImage();
        
    }
    
    // process image file
    public void processImage() {
            PixelGrabber pg=new PixelGrabber(image,0,0,-1,-1,true);
            try{
                if(pg.grabPixels()){
                    imageW=pg.getWidth();
                    imageH=pg.getHeight();
//                    int[] op=(int[]) pg.getPixels();
//                    int[] np=(int[]) new int[w*w];
//                    g.drawImage(image,0,0,this);
                }
            } catch(InterruptedException ie){
                sb.setStatusBarText(1, "Error: "+ie.toString()) ;
            }
    }
    // methods
    // added 11/17/06 to process angle (deg) after generated by ATAN2
    // when vectors span over +/- PI boundary
    private double processAngle(double angle) {
       double angle_ret=0 ;
        if (angle > 180) {
            angle_ret = angle - 360;
        } else if (angle < -180) {
            angle_ret = angle + 360 ;
        } else {
            angle_ret = angle ;
        }
        return angle_ret ;
    }
    // paint method to draw the panel area
    public void paint(Graphics gr) 
    {
        Graphics2D g = (Graphics2D)gr ;
        int w=imageW;
        int h=imageH;

        if(image!=null){
            if (draw_scale == 1) {
                g.drawImage(image, translate.X, translate.Y, w, h, this);
            } else {
                // scaled repaint
                g.drawImage(image, scaledxlate.X, scaledxlate.Y, CInt(w * draw_scale), CInt(h * draw_scale), this);
            }
            
            // line / curve construction
            switch (toolbarIndex) {
                case 3:  // move
                    if (mouseHoldDown==true) {
                        //g.Clear(PictureBox1.BackColor)
                        if (draw_scale == 1 ) {
                            g.drawImage(image, translate.X + translate_delta.X, translate.Y + translate_delta.Y, w, h, this);
                        } else {
                            g.drawImage(image, scaledxlate.X + scaledxlate_delta.X, scaledxlate.Y + scaledxlate_delta.Y, 
                                CInt(w * draw_scale), CInt(h * draw_scale), this);
                        }
                    }
                    break;
                case 4: // // line
                    if (line_started && e0!=null && e1!=null) { 
                        g.setColor(Color.red) ;
                        g.setStroke(new BasicStroke(2));
                        g.draw( new Rectangle( e0.X - 2, e0.Y - 2, 4, 4));
                        g.draw(new Rectangle( e1.X - 2, e1.Y - 2, 4, 4));
                        //currentPen = New Pen(Color.FromArgb(myAlpha, myPenColor), myRoadLaneSizes) //Set up the pen
                        g.setColor(myDB.myPenColor) ;
                        g.setStroke(new BasicStroke(CInt(myDB.myRoadLaneSizes)));
                        g.drawLine(e0.X, e0.Y, e1.X, e1.Y);
                        /*System.out.println("e0.x="+e0.X);
                        System.out.println("e0.y="+e0.Y);
                        System.out.println("e1.x="+e1.X);
                        System.out.println("e1.y="+e1.Y);
                        */
                    } 
                    break;
                case 5: // // curve
                    if (curve_started==true && e1!=null ){
                        //currentPen = New Pen(Color.FromArgb(myAlpha, myPenColor), myRoadLaneSizes) //Set up the pen
                        g.setColor(myDB.myPenColor) ;
                        g.setStroke(new BasicStroke(CInt(myDB.myRoadLaneSizes)));
                        int pixelRadius ;
                        pixelRadius = CInt(myDB.curveRadius * myDB.imageScale * draw_scale) ; // trandform to draw scale
                        g.drawOval( e1.X - pixelRadius, e1.Y - pixelRadius, pixelRadius * 2, pixelRadius * 2);
                        //System.out.println("e0.x="+e0.X);
                        //System.out.println("e0.y="+e0.Y);
                        //System.out.println("r="+pixelRadius);
                        
                    }
                    break;
            }   // end switch
            // ==============================
            int i ;
            mPoint p1, p2 ;
            // 11/16/06 modified
            if (viewRoadOnly_flag) {
                // view road only
                if (myDB.elevationMarkCount >= 2 ) {
                    // 2 or more elevation data exists
                    for (i=1; i<myDB.elevationMarkCount; i++) {
                        p1 = drawTransform(myDB.elevationMarks[i-1].getLocation());//   // start point
                        p2 = drawTransform(myDB.elevationMarks[i].getLocation());//   // end point
                        int curveID, pixelRadius ;
                        float myRadius ;
                        mPoint pc ;
                        double start_angle, end_angle, angle_len ;
                        //g.setColor(myDB.hRoadData[0].getPenColor()) ;
                        g.setColor(myDB.myPenColor) ;
                        g.setStroke(new BasicStroke(CInt(myDB.myRoadLaneSizes*1.5))); // 2/9/07
                        //System.out.println("type="+myDB.elevationMarks[i-1].getSegmentType()) ;
                        switch (myDB.elevationMarks[i-1].getSegmentType()) {
                            case 1: // line
                                g.drawLine( p1.X, p1.Y, p2.X, p2.Y);
                                break ;
                            case 2: // Curve
                                curveID = myDB.elevationMarks[i-1].getParentIndex() ;
                                myRadius = myDB.hRoadData[curveID].getRadius() * draw_scale;
                                
                                pixelRadius = CInt(myRadius * myDB.imageScale);
                                pc = drawTransform(myDB.hRoadData[curveID].getPoint1()) ;   // curve center
                                start_angle = vectorAngle(pc, p1) ;
                                end_angle = vectorAngle(pc, p2) ;
                                angle_len = end_angle - start_angle ;
                                //System.out.println("start, end, len b4="+start_angle+","+end_angle+","+angle_len) ;
                                angle_len = processAngle(angle_len) ; 
                                //System.out.println("len after="+angle_len) ;
                                //System.out.println("ID, radius="+curveID + ","+pixelRadius) ;
                                //System.out.println("vec1 len="+vectorLen(vector(pc, p1))) ;
                                //System.out.println("vec2 len="+vectorLen(vector(pc, p2))) ;
                                
                                g.drawArc(pc.X - pixelRadius, pc.Y - pixelRadius, pixelRadius * 2, pixelRadius * 2, 
                                    CInt(start_angle), CInt(angle_len));
                                
                                break ;
                            case 3: // tangent
                                if (myDB.elevationMarks[i].getSegmentType()==1) {
                                    // line
                                    g.drawLine( p1.X, p1.Y, p2.X, p2.Y);
                                } else {
                                    curveID = myDB.elevationMarks[i-1].getParentIndex() ;
                                    //System.out.println("landmark index="+(i+1)) ;
                                    //System.out.println("curve ID="+curveID) ;
                                    myRadius = myDB.hRoadData[curveID].getRadius() * draw_scale;
                                    pixelRadius = CInt(myRadius * myDB.imageScale);
                                    pc = drawTransform(myDB.hRoadData[curveID].getPoint1()) ;
                                    start_angle = vectorAngle(pc, p1) ;
                                    end_angle = vectorAngle(pc, p2) ;
                                    angle_len = end_angle - start_angle ;
                                    angle_len = processAngle(angle_len) ;
                                    g.drawArc(pc.X - pixelRadius, pc.Y - pixelRadius, pixelRadius * 2, pixelRadius * 2, 
                                        CInt(start_angle), CInt(angle_len));
                                        // curve
                                }   // if
                                break ;
                        }   // switch
                    }   // for i
                // end view road only
                } else {
                    popMessageBox("View Road Only","Please place at least 2 elevation landmarks!");
                    viewRoadOnly_flag = false ;
                }
            } else {
                // view design including construct line/curve
                if (hRoadDataCount > 0) {
                    for (i=0;i<hRoadDataCount;i++){
                        if (!myDB.hRoadData[i].isDeleted()) {
                            // segment is not deleted
                            // repaint data
                            if (myDB.hRoadData[i].getRadius() > 0f) {
                                // curve
                                float myRadius ;
                                p1 = drawTransform(myDB.hRoadData[i].getPoint1());
                                g.setColor(Color.red) ;
                                g.setStroke(new BasicStroke(2));
                                g.draw(new Rectangle( CInt(p1.X - endMarkSize ),
                                            CInt(p1.Y - endMarkSize ), 
                                            CInt(2 * endMarkSize ), 
                                            CInt(2 * endMarkSize ))) ;// // center
                                myRadius = myDB.hRoadData[i].getRadius() * draw_scale;
                                //myCurPen = New Pen(Color.FromArgb(myAlpha, hRoadData(i).getPenColor()), hRoadData(i).getPenWidth) //Set up the pen
                                g.setColor(myDB.hRoadData[i].getPenColor()) ;
                                g.setStroke(new BasicStroke(CInt(myDB.myRoadLaneSizes)));
                                int pixelRadius;
                                pixelRadius = CInt(myRadius * myDB.imageScale);
                                g.drawOval(p1.X - pixelRadius, p1.Y - pixelRadius, pixelRadius * 2, pixelRadius * 2);
                            } else {
                                // line
                                p1 = drawTransform(myDB.hRoadData[i].getPoint1());//   // start point
                                p2 = drawTransform(myDB.hRoadData[i].getPoint2());//   // end point
                                g.setColor(Color.red) ;
                                g.setStroke(new BasicStroke(2));
                                g.draw(new Rectangle( CInt(p1.X - endMarkSize ), 
                                        CInt(p1.Y - endMarkSize ), 
                                        CInt(2 * endMarkSize ), 
                                        CInt(2 * endMarkSize )));
                                g.draw(new Rectangle( CInt(p2.X - endMarkSize ), 
                                        CInt(p2.Y - endMarkSize ), 
                                        CInt(2 * endMarkSize ), 
                                        CInt(2 * endMarkSize )));
                                //myCurPen = New Pen(Color.FromArgb(myAlpha, hRoadData(i).getPenColor()), hRoadData(i).getPenWidth); ////Set up the pen
                                g.setColor(myDB.hRoadData[i].getPenColor()) ;
                                g.setStroke(new BasicStroke(CInt(myDB.myRoadLaneSizes)));
                                g.drawLine( p1.X, p1.Y, p2.X, p2.Y);
                            }   // if line or curve
                        }   // if not deleted
                    }   // for
                }   //hRoadDataCount
            }   // if viewRoadOnly_flag
            
            // hAlignMarks
            if (myDB.hAlignMarkCount > 0) { 
                for (i=0;i<myDB.hAlignMarkCount;i++){
                    p1 = drawTransform(myDB.hAlignMarks[i].getLocation());  //   // tangent point
                    g.setColor(Color.red) ;
                    g.setStroke(new BasicStroke(2));
                    g.drawOval( p1.X - 2, p1.Y - 2, 4, 4);
                }
            }

            // elevation markers
            //elevationMarkerPen = New Pen(Color.FromArgb(myAlpha, elevationMarkerColor), elevationMarkerSize) //Set up elevation marker pen
            g.setColor(myDB.elevationMarkerColor) ;
            g.setStroke(new BasicStroke(myDB.elevationMarkerSize));
            if (myDB.elevationMarkCount > 0 ) {
                for (i=0;i<myDB.elevationMarkCount;i++){
                    p1 = drawTransform(myDB.elevationMarks[i].getLocation()) ;//  // marker point
                    g.drawOval( p1.X - 2, p1.Y - 2, 4, 4);
                }
            }

            // animation
/*
            If animationFlag Then
                // animation ON
                // draw a starting mark only
                If myUnit = 1 Then
                    // US
                    p1 = drawTransform(New PointF(animatedVehPos.X / FT2M * imageScale, animatedVehPos.Z / FT2M * imageScale))
                ElseIf myUnit = 2 Then
                    p1 = drawTransform(New PointF(animatedVehPos.X * imageScale, animatedVehPos.Z * imageScale))
                End If
                Dim pur_pen4 As Pen = New Pen(Color.Purple, 4)
                g.DrawEllipse(pur_pen4, p1.X - 5, p1.Y - 5, 10, 10)

            End If
*/        
 
        }   // image != null?
        else {
            Rectangle r = bounds();
            if(grid>0)
            {
                g.setColor(new Color(224,224,224)); // sub grid lines

                for(int i=grid;i<r.height;i+=grid)
                    g.drawLine(0,i,r.width,i);
                for(int i=grid;i<r.width;i+=grid)
                    g.drawLine(i,0,i,r.height);

                g.setColor(new Color(184,184,184)); // major grid lines

                for(int i=grid*10;i<r.height;i+=grid*10)
                    g.drawLine(0,i,r.width,i);
                for(int i=grid*10;i<r.width;i+=grid*10)
                    g.drawLine(i,0,i,r.height);
            }   // draw grid
        } // image=null?
    }   // end of paint

    // mouse key down method
    public boolean keyDown(Event e,int k)
    {
	return(true);
    }

    // update toolbar index
    public void newstatus(int index, String str)
    {
        sb.setStatusBarText(0, str) ;
        toolbarIndex = index ;
        if (toolbarIndex==0) {
            ptr_edit_delete.setEnabled(true) ;
        } else {
            ptr_edit_delete.setEnabled(false) ;
        }
	//for(grobj j = glist.ghead;j!=null;j=j.next)
        //{    j.select = 0;
        //}
        if (toolbarIndex==4 | toolbarIndex==5 | toolbarIndex==8) {
            // line curve & marker
            ptr_edit_undo.setEnabled(true) ;
            ptr_edit_redo.setEnabled(true) ;
        } else {
            ptr_edit_undo.setEnabled(false) ;
            ptr_edit_redo.setEnabled(false) ;           
        }
        switch (toolbarIndex) {
            case 0: // pointer
                break ;
            case 1: // zoomin
                changeDrawScale(0.1f);
                repaint();
                break ;
            case 2: // zoom out
                changeDrawScale(-0.1f);
                repaint();
                break ;
            case 3: // move
                break ;
            case 4: // line
                if (image==null){
                    popMessageBox("No Contour Map", NO_MAP_MSG);
                    //frame_msgbox.toFront() ;
                }
                viewRoadOnly_flag = false ; // 11/16/06
                break ;
            case 5: // curve
                if (frame_curveSetting==null){
                    //popCurveSettings();
                    popCurveSettings_flag = true ;
                } else {    // not null
                    if (frame_curveSetting.isShowing()==false){
                        //popCurveSettings();
                        popCurveSettings_flag = true ;
                    } else {
                        frame_curveSetting.show();
                    }
                    frame_curveSetting.toFront();
                }
                viewRoadOnly_flag = false ; // 11/16/06
                repaint();
                
                break ;
            case 6: // edit end point
                if (image==null){
                    popMessageBox("No Contour Map", NO_MAP_MSG);
                    //frame_msgbox.toFront() ;
                }
                viewRoadOnly_flag = false ; // 11/16/06
                break ;
            case 7: // horizontal curve alignment
                if (image==null){
                    popMessageBox("No Contour Map", NO_MAP_MSG);
                    //frame_msgbox.toFront() ;
                } else {
                    tool_curvehAlignMarks();
                }
                viewRoadOnly_flag = false ; // 11/16/06
                break ;
            case 8: // place station, marker
                if (image==null){
                    popMessageBox("No Contour Map", NO_MAP_MSG);
                    //frame_msgbox.toFront() ;
                }
                viewRoadOnly_flag = false ; // 11/16/06
                break ;
            case 9: // refresh, marker insert
                //repaint();
                // insert landmark
                if (image==null){ 
                    popMessageBox("No Contour Map", NO_MAP_MSG);
                    //frame_msgbox.toFront() ;
                } 
                viewRoadOnly_flag = false ; 
                
                break ;
            case 10: // vertical curve design
                if (image==null){
                    popMessageBox("No Contour Map", NO_MAP_MSG);
                    //frame_msgbox.toFront() ;
                } else if (myDB.elevationMarkCount < 2 ) {
                    popMessageBox("Vertical Curve Design","Please place at least 2 elevation landmarks first!");
                    //frame_msgbox.toFront() ;
                    
                //} else if (design_filename.Length <= 0) { 
                    // design filename does not exist
                    
                    //result = MessageBox.Show("Save horizontal geometry design?", "Save Design", MessageBoxButtons.OKCancel, MessageBoxIcon.Question, MessageBoxDefaultButton.Button1)
                    //If result = DialogResult.OK Then
                    //    file_save_Click(Nothing, Nothing)
                    //End If
                    
                    //startVerticalDesign();
                } else {
                    
                    //startVerticalDesign();
                    String status = checkLandmarks() ;  // added 3/1/07
                    if (status.length()>0) { 
                        popMessageBox("Landmark Data Error", "Error at landmark station "+status+
                        ".\nPlease include tangent point when making \ntransition between line and curve segments.\n"+
                        "Use View->Station Landmarks to review data.");
                    } else {
                    
                        if (frmVerticalAlign==null){
                        //    javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        //        public void run() {
                                    popVerticalAlign("Vertical Curve Design"); 
                        //        }
                        //    });
                        } else {    // not null
                            if (frmVerticalAlign.isShowing()==false){
                                popVerticalAlign("Vertical Curve Design");
                            } else {
                                frmVerticalAlign.show();
                            }
                        }   // if (frmVerticalAlign==null)

                        //javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        //    public void run() {
                                //try{
                                //    Thread.sleep(500);
                                //} catch(InterruptedException e){
                                    //System.out.println("Sleep Interrupted");
                                //}
                                frmVerticalAlign.toFront();

                        //    }
                        //});
                    }   // if
                } // if elevationMarkCount < 2
                break ;
        }
	//repaint();
    }
    
    public void changeDrawScale(float scale) {
        draw_scale += scale ;
        if (draw_scale > 5.0f) {
            draw_scale = 5.0f;
        }
        else if (draw_scale < 0.1f) { 
            draw_scale = 0.1f ;
        }
        sb.setStatusBarText(3, new Float(Math.round(draw_scale*10f)/10f).toString()) ;
        imageResize() ;
    }
    
    public void imageResize() {
        Rectangle r = bounds();
        scaledxlate.X = CInt(0.5f * r.width * (1 - draw_scale) + draw_scale * translate.X);
        scaledxlate.Y = CInt(0.5f * r.height * (1 - draw_scale) + draw_scale * translate.Y);
        repaint();
        //PictureBox1.Invalidate()
    }

    //public boolean mouseDown(Event e, int x, int y)
    public boolean mouseDown(int x, int y)
    {
        sb.setStatusBarText(2, Integer.toString(x)+","+Integer.toString(y)) ;
    
        switch (toolbarIndex) {
            case 3:  // move
                e0 = new mPoint(x, y) ;
                mouseHoldDown = true ;
                break ;
            case 4: //   line
                line_started = true;
                e0 = new mPoint(x, y) ;
                //System.out.println("line-down");
                break ;
            case 5: //  // curve
                // construct curve
               // if e.M MouseButtons.Left {
                    curve_started = true;
                    e0 = new mPoint(x, y) ;
               // }
                break ;
            case 6: //  // modify line/curve
                // check if click a point or a line
                modificationInfo = searchSegmentDB(transform(new mPoint(x, y))) ;
                //System.out.println("X,Y = " + modificationInfo.X + "," + modificationInfo.Y) ;
                
                if (modificationInfo.X >= 0 && modificationInfo.Y >= 0) {
                    // closest segment terminal found
                    modification_started = true;
                }
                break ;
        }   // end switch
        return(true);
    }
    
    public mPoint searchSegmentDB(mPointF ptf ) {
        // transform pt from screen pixel to actual unit
        int i ;
        float dist;
        mPoint data = new mPoint(-1, -1);
        for (i=0 ; i<hRoadDataCount; i++){
            // check point 1 or center of radius if a circle
            dist = distanceOf(myDB.hRoadData[i].getPoint1(), ptf);
            //System.out.println("end pt1="+dist) ;
            if (dist <= endMarkSize*2f/draw_scale ) {  //Math.sqrt(2), 10/11/06
                data.X = i;
                data.Y = 1;
                break;
            }
            // check poitn 2 
            dist = distanceOf(myDB.hRoadData[i].getPoint2(), ptf);
            //System.out.println("end pt2="+dist) ;
            if (dist <= endMarkSize*2f/draw_scale) { //Math.sqrt(2), 10/11/06
                data.X = i;
                data.Y = 2;
                break;
            }
        }//end for
        return data;
}
    // transform mouse click position on the screen to pixel location on the digital map
    public mPointF transform(mPoint input) {
        mPointF ptf = new mPointF(0f,0f);
        if (draw_scale == 1f) {
            ptf.X = (input.X - translate.X);
            ptf.Y = (input.Y - translate.Y);
        } else {
            ptf.X = (input.X - scaledxlate.X) / draw_scale;
            ptf.Y = (input.Y - scaledxlate.Y) / draw_scale;
        }
        return ptf;
    }
    
    // transform location saved onthe DB to relative position on screen
    public mPoint drawTransform(mPointF input) {
        mPoint ptf = new mPoint(0,0);
        if (draw_scale == 1) {
            ptf.X = CInt(input.X + translate.X + translate_delta.X);
            ptf.Y = CInt(input.Y + translate.Y + translate_delta.Y);
        } else {
            ptf.X = CInt(input.X * draw_scale + scaledxlate.X + scaledxlate_delta.X);
            ptf.Y = CInt(input.Y * draw_scale + scaledxlate.Y + scaledxlate_delta.Y);
        }

        return ptf;
    }
    public int popSegLogBuffer() {
        // pop the current # of data from log buffer
        if (segLogIndex > 0) {
            segLogIndex -= 1;
            return segLogBuffer[segLogIndex];
        } else {
            return -99;
        }
    }
        
    public int popMarkLogBuffer(){
        // pop the current # of landmark data from log buffer
        if (markLogIndex > 0) {
            markLogIndex -= 1;
            return markLogBuffer[markLogIndex];
        } else {
            return -99;
        }
    }
    public void push2SegLogBuffer(int _myhRoadDataCount){
        // save # of data into log buffer
        if (segLogIndex == segLogBuffer.length - 1 ) {
            // buffer fulled
            // shift forward by 1
            int i ;
            for (i=0; i<segLogIndex; i++) {
                segLogBuffer[i] = segLogBuffer[i + 1];
            }
            segLogBuffer[segLogIndex] = _myhRoadDataCount;
        } else {
            segLogIndex += 1;
            segLogBuffer[segLogIndex] = _myhRoadDataCount;
        }
    }
    
    public void push2MarkLogBuffer(int _myLandmarkCount ) {

        // save # of data into log buffer
        if (markLogIndex == markLogBuffer.length - 1) { 
            // buffer fulled
            // shift forward by 1
            int i;
            for (i=0; i<markLogIndex;i++){
                markLogBuffer[i] = markLogBuffer[i + 1];
            }
            markLogBuffer[markLogIndex] = _myLandmarkCount;
        } else {
            markLogIndex += 1;
            markLogBuffer[markLogIndex] = _myLandmarkCount;
        }
    }
    
    //public boolean mouseDrag(Event e, int x, int y)
    public boolean mouseLeftDrag(int x, int y)
    {
        sb.setStatusBarText(2, Integer.toString(x)+","+Integer.toString(y)) ;
        //System.out.println("toolbar index=" + toolbarIndex) ;
        switch (toolbarIndex){
            case 3: // move
                if (image != null ){
                    if (e0!=null) { // Is Nothing And mouseHoldDown Then
                        if (draw_scale == 1) {
                            translate_delta.X = x - e0.X;
                            translate_delta.Y = y - e0.Y;
                        } else {
                            translate_delta.X = CInt((x - e0.X) / draw_scale);
                            translate_delta.Y = CInt((y - e0.Y) / draw_scale);
                            scaledxlate_delta.X = (x - e0.X);
                            scaledxlate_delta.Y = (y - e0.Y);
                        }

                        e1 = new mPoint(x,y);
                        repaint();
                    }
                }  // if image
                else {    // g is not defined
                    popMessageBox("No Contour Map", NO_MAP_MSG);
                    toolbarIndex = 0;
                } // else
                break;
            case 4 :     // line 
                if (image != null ){
                    e1 = new mPoint(x,y);
                    repaint();
                }
                else {    // g is not defined
                    popMessageBox("No Contour Map", NO_MAP_MSG);
                    toolbarIndex = 0;
                } // else
                break;
            case 5 :     //  curve
                if (image != null ){
                    e1 = new mPoint(x,y);
                    repaint();
                }
                else {    // g is not defined
                    popMessageBox("No Contour Map", NO_MAP_MSG);
                    toolbarIndex = 0;
                } // else
                break;
            case 6:     // modify
                
                if (image != null ){
                    //System.out.println("modification_started=" + modification_started) ;
                    if (modification_started) { // modify end control point 

                        int dataIndex = modificationInfo.X;
                        int pointId = modificationInfo.Y;
                        //System.out.println("x, y = " + dataIndex + ", " + pointId) ;
                        
                        myDB.hRoadData[dataIndex].modifyPoint(pointId, transform(new mPoint(x,y)));
                        //System.out.println("idx="+dataIndex+", id="+pointId);
                        repaint();
                    }
                    e1 = new mPoint(x,y);
                }
                else {    // g is not defined
                    popMessageBox("No Contour Map", NO_MAP_MSG);
                    toolbarIndex = 0;
                } // else
               
                break;
                
        } // end switch
        return (true);
    }
    
    //public boolean mouseUp(Event e, int x, int y)
    public boolean mouseLeftUp(int x, int y)
    {
        int dataIndex  = -1;
        int tangentIndex = -1 ;
        mPointF marker = transform(new mPoint(x,y));

        sb.setStatusBarText(2, Integer.toString(x)+","+Integer.toString(y)) ;
        switch (toolbarIndex){
            case 0: // // pointer, select
                checkItemSelect(transform(new mPoint(x,y)));
                
                repaint();
                break;

            case 3: //  // move
                translate_delta = new mPoint(0, 0);
                scaledxlate_delta = new mPoint(0, 0);
                if  (e1 != null ) {
                    mouseHoldDown = false;
                    if (draw_scale == 1) { 
                        translate.X += e1.X - e0.X;
                        translate.Y += e1.Y - e0.Y;
                    } else {
                        translate.X += CInt((e1.X - e0.X) / draw_scale);
                        translate.Y += CInt((e1.Y - e0.Y) / draw_scale);
                        scaledxlate.X += (e1.X - e0.X);
                        scaledxlate.Y += (e1.Y - e0.Y);
                    }
                    
                    e0 = null;
                    e1 = null;
                    repaint();
                }
                break;
            case 4: //  // line
                if (e1 !=null && e0 != null) { 
                    line_started = false;
                    
                    myDB.hRoadData[hRoadDataCount] = new Data2D() ;
                    myDB.hRoadData[hRoadDataCount].saveData(transform(e0), transform(e1), myDB.myPenColor, myDB.myRoadLaneSizes);
                    // debug
                    //debugWindow.Text &= "P1=" & hRoadData(hRoadDataCount).getPoint1.X & ", " & hRoadData(hRoadDataCount).getPoint1.Y
                    //debugWindow.Text &= "P2=" & hRoadData(hRoadDataCount).getPoint2.X & ", " & hRoadData(hRoadDataCount).getPoint2.Y & vbCrLf
                    hRoadDataCount += 1;

                    // save # of data in log buffer
                    push2SegLogBuffer(hRoadDataCount);

                    
                    e0 = null;
                    e1 = null;
                    repaint();
                }
                break;
            case 5: //  // curve
                if ( e1 !=null )  {
                    curve_started = false;

                    myDB.hRoadData[hRoadDataCount] = new Data2D();
                    myDB.hRoadData[hRoadDataCount].saveData(transform(e1), myDB.curveRadius, myDB.myPenColor, myDB.myRoadLaneSizes);
                    hRoadDataCount += 1;
                    // save # of data in log buffer
                    push2SegLogBuffer(hRoadDataCount);

                    e0 = null;
                    e1 = null;
                    repaint();
                }
                break;
            case 6:	//  // Modify
                modification_started = false;
                break;
            case 8: //  Set elevation marker
                dataIndex  = -1;
                tangentIndex = -1 ;
                marker = transform(new mPoint(x,y));
                tangentIndex = checkTangentLandmarks(marker);
                if (tangentIndex < 0) { 
                    dataIndex = checkMarkLocation(marker);
                } else {
                    dataIndex = tangentIndex;
                }
                //System.out.println("dataIndex="+dataIndex+", tangentIndex="+tangentIndex);
                
                if (dataIndex < 0) { 
                    popMessageBox( "Elevation Marker", "Please place marker on line/curve segments");
                } else {
                    // comment out 3/4/06, using database point in checkMarkLocation()
                    //currentElevationMarker = marker

                    // pop screen to enter evelation & save marker data
                    if (frmElevationMarker != null) {
                        if (frmElevationMarker.isShowing()) {
                            frmElevationMarker.dispose();
                        }
                    }
                    sInfo = new StationInfo();
                    sInfo.title="Station (" + (myDB.elevationMarkCount + 1) + ")" ;
                    sInfo.CheckBox_edit = false;
                    sInfo.parentId = dataIndex;
                    sInfo.location = myDB.currentElevationMarker ;
                    sInfo.optionInit();
                    if (tangentIndex >= 0){
                        sInfo.tangent_option = true ;
                        sInfo.initial_state = 3 ;   // tangent
                    } else if (myDB.hRoadData[dataIndex].getRadius() > 0) {
                        sInfo.curve_option = true ;
                        sInfo.initial_state = 2 ;   // curve
                    } else {
                        sInfo.line_option = true ;
                        sInfo.initial_state = 1 ;   // line
                    }
                    popElevationMarkerForm();
                }
                break;
            // ======================== added 2/28/07 ===============================
            case 9: //  Insert elevation marker
                dataIndex  = -1;
                tangentIndex = -1 ;
                marker = transform(new mPoint(x,y));
                tangentIndex = checkTangentLandmarks(marker);
                if (tangentIndex < 0) { 
                    dataIndex = checkMarkLocation(marker);
                } else {
                    dataIndex = tangentIndex;
                }
                //System.out.println("dataIndex="+dataIndex+", tangentIndex="+tangentIndex);
                
                if (dataIndex < 0) { 
                    popMessageBox( "Insert Elevation Marker", "Please place marker on line/curve segments");
                } else {
                    // comment out 3/4/06, using database point in checkMarkLocation()
                    //currentElevationMarker = marker

                    // pop screen to enter evelation & save marker data
                    if (frmInsertElevationMarker != null) {
                        if (frmInsertElevationMarker.isShowing()) {
                            frmInsertElevationMarker.dispose();
                        }
                    }
                    sInfo = new StationInfo();
                    
                    sInfo.CheckBox_edit = false;
                    sInfo.parentId = dataIndex;
                    sInfo.location = myDB.currentElevationMarker ;
                    sInfo.optionInit();
                    if (tangentIndex >= 0){
                        sInfo.tangent_option = true ;
                        sInfo.initial_state = 3 ;   // tangent
                    } else if (myDB.hRoadData[dataIndex].getRadius() > 0) {
                        sInfo.curve_option = true ;
                        sInfo.initial_state = 2 ;   // curve
                    } else {
                        sInfo.line_option = true ;
                        sInfo.initial_state = 1 ;   // line
                    }
                    sInfo.insert = findMarkerInsertIndex() ; 
                    sInfo.title="Insert Station (" + (sInfo.insert + 1) + ")" ; 
                    
                    popInsertElevationMarker();
                }
                break;
        }   // switch
        return (true) ;
    }
    
    // search the landmark DB and find where the index of inserted marker located
    private int findMarkerInsertIndex() {
        int myIndex = -1 ;
        mPointF vec1, vec2 ;
        for (int i=0; i<myDB.elevationMarkCount-1; i++) {
            vec1 = vector(myDB.currentElevationMarker, myDB.elevationMarks[i].getLocation());
            vec2 = vector(myDB.currentElevationMarker, myDB.elevationMarks[i+1].getLocation());
            if (vectorDOT(vec1, vec2)<0) {
                myIndex = i+1 ;
                break ;
            }   // end if
        }   // end for
        return myIndex ;
    }
    
    /** Pop up a window to display message */    
    public void popElevationMarkerForm() {
        if (image!=null){
            // open a frame
            frmElevationMarker = new myWindow(sInfo.title) ;
            frmElevationMarker.setLocation(150,40) ;
            frmElevationMarker.setSize(300,200) ;
            frmElevationMarker.validate() ;
            frmElevationMarker.setVisible(true) ;
            frmElevationMarker.setResizable(false) ;

            ActionListener frmElevationMarker_ok_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    //System.out.println("txtEle.text=" + txtEle.getText()) ;
                    if (txtEle.getText().length()>0) {  // 11/9/06 added
                        sInfo.elevation = new Float(txtEle.getText()).floatValue(); 
                        byte segment_type ;
                        if ( line.getState() == true) {
                            segment_type = 1 ;
                            sInfo.line_option=true ;
                            sInfo.curve_option=false ;
                            sInfo.tangent_option=false ;
                            // =========== 11/9/06 added
                            if (sInfo.initial_state != segment_type) {
                                // from curve or tangent to line
                                mPointF vec1, vec2 ;
                                float L1, L2 ;
                                int i = sInfo.parentId ;
                                vec1 = vector(myDB.hRoadData[i].getPoint1(), myDB.hRoadData[i].getPoint2());
                                vec2 = vector(myDB.hRoadData[i].getPoint1(), myDB.currentElevationMarker);
                                L1 = vectorLen(vec1);
                                L2 = vectorLen(vec2);
                                myDB.currentElevationMarker = new mPointF(myDB.hRoadData[i].getPoint1().X + vec1.X * L2 / L1, 
                                    myDB.hRoadData[i].getPoint1().Y + vec1.Y * L2 / L1);
                            }   // if (sInfo.initial_state

                        } else if (curve.getState() == true ) {
                            segment_type = 2;
                            sInfo.line_option=false ;
                            sInfo.curve_option=true ;
                            sInfo.tangent_option=false ;
                            // =========== 11/9/06 added
                            if (sInfo.initial_state != segment_type) {
                                // from line or tangent to curve
                                // update new point on curve
                                mPointF vec2 ;
                                float L1, L2 ;
                                int i = sInfo.parentId ;
                                vec2 = vector(myDB.hRoadData[i].getPoint1(), myDB.currentElevationMarker);
                                L1 = myDB.hRoadData[i].getRadius() * myDB.imageScale;
                                L2 = vectorLen(vec2);
                                myDB.currentElevationMarker = new mPointF(myDB.hRoadData[i].getPoint1().X + vec2.X * L1 / L2, 
                                    myDB.hRoadData[i].getPoint1().Y + vec2.Y * L1 / L2);
                            }   // if (sInfo.initial_state
                        } else {
                            // tangent point
                            segment_type = 3;
                            sInfo.line_option=false ;
                            sInfo.curve_option=false ;
                            sInfo.tangent_option=true ;
                        }
                        
                        if (sInfo.CheckBox_edit == true ) {
                            // edit existing landmark
                            int index ;
                            index = sInfo.dataIndex ;
                            myDB.elevationMarks[index].setMarker(myDB.currentElevationMarker, sInfo.elevation, new Integer(sInfo.parentId).byteValue(), segment_type);
                        } else {
                            myDB.elevationMarks[myDB.elevationMarkCount] = new MarkerDB();
                            // create new landmark
                            myDB.elevationMarks[myDB.elevationMarkCount].setMarker(myDB.currentElevationMarker, sInfo.elevation, new Integer(sInfo.parentId).byteValue(), segment_type);
                            myDB.elevationMarkCount += 1;
                            // save # of data in log buffer
                            push2MarkLogBuffer(myDB.elevationMarkCount);
                        }
                        frmElevationMarker.dispose() ;
                        repaint();
                    } else {
                        // txtEle is empty
                        popMessageBox("Elevation Data", "Please specify station elevation!");
                    }
                }   // action performed
            } ;
            ActionListener frmElevationMarker_cancel_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    frmElevationMarker.dispose() ;
                }
            } ;
            
            ItemListener frmElevationMarker_line_listener = new ItemListener() {
                public void itemStateChanged(ItemEvent ie) {
                    float x1, y1 ;
                    String str="";
                    str = mX.getText() ;
                    x1 = CFloat(str.substring(str.indexOf(":")+1)) ;
                    str = mY.getText() ;
                    y1 = CFloat(str.substring(str.indexOf(":")+1)) ;
                    getLineMarkLocation(new mPointF(x1, y1));
                }
            } ;
            ItemListener frmElevationMarker_curve_listener = new ItemListener() {
                public void itemStateChanged(ItemEvent ie) {
                    float x1, y1 ;
                    String str="";
                    str = mX.getText() ;
                    x1 = CFloat(str.substring(str.indexOf(":")+1)) ;
                    str = mY.getText() ;
                    y1 = CFloat(str.substring(str.indexOf(":")+1)) ;
                    getCurveMarkLocation(new mPointF(x1, y1));
                }
            } ;

            frmElevationMarker.setLayout(new GridBagLayout()) ;
            // Create a constrains object, and specify default values
            GridBagConstraints c = new GridBagConstraints() ;
            c.fill = GridBagConstraints.BOTH ; // component grows in both directions
            c.weightx = 1.0 ; c.weighty = 1.0 ;

            c.gridx = 0 ; c.gridy = 0; c.gridwidth = 1 ; c.gridheight = 1 ;
            c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
            String unitStr = "";
            if (myDB.myUnit==1) {
                unitStr = "(ft)";
            } else if (myDB.myUnit==2) {
                unitStr = "(m)";
            }  
            Label lblEle = new Label("Elevation " + unitStr) ;
            //frmElevationMarker.setBackground(new Color(200, 200, 200)) ;
            frmElevationMarker.add(lblEle,c) ;
            c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
            txtEle.setForeground(Color.BLACK) ;
            frmElevationMarker.add(txtEle,c) ;
            
            c.gridx = 1 ; c.gridy = 0; c.gridwidth = 1 ;
            Button btn_ok = new Button(" OK ") ;
            frmElevationMarker.add(btn_ok, c) ;
            btn_ok.addActionListener(frmElevationMarker_ok_listener) ;
            c.gridx = 1 ; c.gridy = 1;
            Button btn_cancel = new Button(" Cancel ") ;
            frmElevationMarker.add(btn_cancel, c) ;
            btn_cancel.addActionListener(frmElevationMarker_cancel_listener) ;

            Panel radioButtonPanel = new Panel();
            radioButtonPanel.setLayout(new GridLayout(3, 1));
            CheckboxGroup checks = new CheckboxGroup();
            line=new Checkbox("Line Segment", sInfo.line_option, checks);
            line.addItemListener(frmElevationMarker_line_listener) ;
            curve=new Checkbox("Curve Segment", sInfo.curve_option, checks);
            curve.addItemListener(frmElevationMarker_curve_listener) ;
            tangent=new Checkbox("Tangent Point", sInfo.tangent_option, checks);
            radioButtonPanel.add(line);
            radioButtonPanel.add(curve);
            radioButtonPanel.add(tangent);
            c.gridx = 0 ; c.gridy = 2; c.gridwidth = 1 ; c.gridheight = 3;
            frmElevationMarker.add(radioButtonPanel, c) ;
            c.gridx = 1 ; c.gridy = 2; c.gridwidth = 1 ; c.gridheight = 1;
            String sx=new Float(sInfo.location.getX()).toString();
            String sy=new Float(sInfo.location.getY()).toString();
            mX = new Label("X:"+sx) ; 
            frmElevationMarker.add(mX, c) ;
            c.gridx = 1 ; c.gridy = 3; c.gridwidth = 1 ; c.gridheight = 1;
            mY = new Label("Y:"+sy);
            frmElevationMarker.add(mY, c) ;
            c.gridx = 1 ; c.gridy = 4; c.gridwidth = 1 ; c.gridheight = 1;
            parentID = new Label(sInfo.parentId+"") ;
            frmElevationMarker.add(parentID, c) ;
            
            //ButtonGroup bgroup = new ButtonGroup();
            //bgroup.add(txtRadius);
            //bgroup.add(lblEle);
            //bgroup.add(maybeButton);

            frmElevationMarker.invalidate();
            frmElevationMarker.show() ;
            frmElevationMarker.toFront() ;
        }
        else {
            popMessageBox("No Contour Map", NO_MAP_MSG);
        }
            
    } // popElevationMarkerForm

    /** Pop up a window to display message */    
    public void popInsertElevationMarker() {
        if (image!=null){
            // open a frame
            frmInsertElevationMarker = new myWindow(sInfo.title) ;
            frmInsertElevationMarker.setLocation(150,40) ;
            frmInsertElevationMarker.setSize(300,200) ;
            frmInsertElevationMarker.validate() ;
            frmInsertElevationMarker.setVisible(true) ;
            frmInsertElevationMarker.setResizable(false) ;

            ActionListener frmInsertElevationMarker_ok_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    //System.out.println("txtEle.text=" + txtEle.getText()) ;
                    if (txtEle.getText().length()>0) {  // 11/9/06 added
                        sInfo.elevation = new Float(txtEle.getText()).floatValue(); 
                        byte segment_type ;
                        if ( line.getState() == true) {
                            segment_type = 1 ;
                            sInfo.line_option=true ;
                            sInfo.curve_option=false ;
                            sInfo.tangent_option=false ;
                            // =========== 11/9/06 added
                            if (sInfo.initial_state != segment_type) {
                                // from curve or tangent to line
                                mPointF vec1, vec2 ;
                                float L1, L2 ;
                                int i = sInfo.parentId ;
                                vec1 = vector(myDB.hRoadData[i].getPoint1(), myDB.hRoadData[i].getPoint2());
                                vec2 = vector(myDB.hRoadData[i].getPoint1(), myDB.currentElevationMarker);
                                L1 = vectorLen(vec1);
                                L2 = vectorLen(vec2);
                                myDB.currentElevationMarker = new mPointF(myDB.hRoadData[i].getPoint1().X + vec1.X * L2 / L1, 
                                    myDB.hRoadData[i].getPoint1().Y + vec1.Y * L2 / L1);
                            }   // if (sInfo.initial_state

                        } else if (curve.getState() == true ) {
                            segment_type = 2;
                            sInfo.line_option=false ;
                            sInfo.curve_option=true ;
                            sInfo.tangent_option=false ;
                            // =========== 11/9/06 added
                            if (sInfo.initial_state != segment_type) {
                                // from line or tangent to curve
                                // update new point on curve
                                mPointF vec2 ;
                                float L1, L2 ;
                                int i = sInfo.parentId ;
                                vec2 = vector(myDB.hRoadData[i].getPoint1(), myDB.currentElevationMarker);
                                L1 = myDB.hRoadData[i].getRadius() * myDB.imageScale;
                                L2 = vectorLen(vec2);
                                myDB.currentElevationMarker = new mPointF(myDB.hRoadData[i].getPoint1().X + vec2.X * L1 / L2, 
                                    myDB.hRoadData[i].getPoint1().Y + vec2.Y * L1 / L2);
                            }   // if (sInfo.initial_state
                        } else {
                            // tangent point
                            segment_type = 3;
                            sInfo.line_option=false ;
                            sInfo.curve_option=false ;
                            sInfo.tangent_option=true ;
                        }
                        
                        myDB.elevationMarks[myDB.elevationMarkCount] = new MarkerDB();
                        
                        // shift landmarkd by 1 starting from sInfo.insert index
                        float x, y ;
                        mPointF ptf ;
                        for (int i=myDB.elevationMarkCount; i>sInfo.insert; i--) {
                            ptf = myDB.elevationMarks[i-1].getLocation() ;
                            x = ptf.X ;
                            y = ptf.Y ;
                            myDB.elevationMarks[i].setLocation(x, y) ;
                            myDB.elevationMarks[i].setElevation(myDB.elevationMarks[i-1].getElevation()) ;
                            myDB.elevationMarks[i].setSegmentType(myDB.elevationMarks[i-1].getSegmentType()) ;
                            myDB.elevationMarks[i].setDistance(myDB.elevationMarks[i-1].getDistance()) ;
                            myDB.elevationMarks[i].setParentIndex(myDB.elevationMarks[i-1].getParentIndex()) ;
                            myDB.elevationMarks[i].setGrade(myDB.elevationMarks[i-1].getGrade()) ;
                            myDB.elevationMarks[i].PVTnC_Overlap = myDB.elevationMarks[i-1].PVTnC_Overlap ;
                        }
                        // insert new landmark
                        myDB.elevationMarks[sInfo.insert].setMarker(myDB.currentElevationMarker, sInfo.elevation, new Integer(sInfo.parentId).byteValue(), segment_type);
                        myDB.elevationMarkCount += 1;
                        
                        // save # of data in log buffer, no undo option
                        //push2MarkLogBuffer(myDB.elevationMarkCount);
                        
                        frmInsertElevationMarker.dispose() ;
                        repaint();
                    } else {
                        // txtEle is empty
                        popMessageBox("Elevation Data", "Please specify station elevation!");
                    }
                }   // action performed
            } ;
            ActionListener frmInsertElevationMarker_cancel_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    frmInsertElevationMarker.dispose() ;
                }
            } ;
            
            ItemListener frmInsertElevationMarker_line_listener = new ItemListener() {
                public void itemStateChanged(ItemEvent ie) {
                    float x1, y1 ;
                    String str="";
                    str = mX.getText() ;
                    x1 = CFloat(str.substring(str.indexOf(":")+1)) ;
                    str = mY.getText() ;
                    y1 = CFloat(str.substring(str.indexOf(":")+1)) ;
                    getLineMarkLocation(new mPointF(x1, y1));
                }
            } ;
            ItemListener frmInsertElevationMarker_curve_listener = new ItemListener() {
                public void itemStateChanged(ItemEvent ie) {
                    float x1, y1 ;
                    String str="";
                    str = mX.getText() ;
                    x1 = CFloat(str.substring(str.indexOf(":")+1)) ;
                    str = mY.getText() ;
                    y1 = CFloat(str.substring(str.indexOf(":")+1)) ;
                    getCurveMarkLocation(new mPointF(x1, y1));
                }
            } ;

            frmInsertElevationMarker.setLayout(new GridBagLayout()) ;
            // Create a constrains object, and specify default values
            GridBagConstraints c = new GridBagConstraints() ;
            c.fill = GridBagConstraints.BOTH ; // component grows in both directions
            c.weightx = 1.0 ; c.weighty = 1.0 ;

            c.gridx = 0 ; c.gridy = 0; c.gridwidth = 1 ; c.gridheight = 1 ;
            c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
            String unitStr = "";
            if (myDB.myUnit==1) {
                unitStr = "(ft)";
            } else if (myDB.myUnit==2) {
                unitStr = "(m)";
            }  
            Label lblEle = new Label("Elevation " + unitStr) ;
            //frmInsertElevationMarker.setBackground(new Color(200, 200, 200)) ;
            frmInsertElevationMarker.add(lblEle,c) ;
            c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
            //txtEle.setForeground(Color.BLACK) ;
            txtEle.setForeground(Color.green) ;
            frmInsertElevationMarker.add(txtEle,c) ;
            
            c.gridx = 1 ; c.gridy = 0; c.gridwidth = 1 ;
            Button btn_ok = new Button(" Insert ") ;
            frmInsertElevationMarker.add(btn_ok, c) ;
            btn_ok.addActionListener(frmInsertElevationMarker_ok_listener) ;
            c.gridx = 1 ; c.gridy = 1;
            Button btn_cancel = new Button(" Cancel ") ;
            frmInsertElevationMarker.add(btn_cancel, c) ;
            btn_cancel.addActionListener(frmInsertElevationMarker_cancel_listener) ;

            Panel radioButtonPanel = new Panel();
            radioButtonPanel.setLayout(new GridLayout(3, 1));
            CheckboxGroup checks = new CheckboxGroup();
            line=new Checkbox("Line Segment", sInfo.line_option, checks);
            line.addItemListener(frmInsertElevationMarker_line_listener) ;
            curve=new Checkbox("Curve Segment", sInfo.curve_option, checks);
            curve.addItemListener(frmInsertElevationMarker_curve_listener) ;
            tangent=new Checkbox("Tangent Point", sInfo.tangent_option, checks);
            radioButtonPanel.add(line);
            radioButtonPanel.add(curve);
            radioButtonPanel.add(tangent);
            c.gridx = 0 ; c.gridy = 2; c.gridwidth = 1 ; c.gridheight = 3;
            frmInsertElevationMarker.add(radioButtonPanel, c) ;
            c.gridx = 1 ; c.gridy = 2; c.gridwidth = 1 ; c.gridheight = 1;
            String sx=new Float(sInfo.location.getX()).toString();
            String sy=new Float(sInfo.location.getY()).toString();
            mX = new Label("X:"+sx) ; 
            frmInsertElevationMarker.add(mX, c) ;
            c.gridx = 1 ; c.gridy = 3; c.gridwidth = 1 ; c.gridheight = 1;
            mY = new Label("Y:"+sy);
            frmInsertElevationMarker.add(mY, c) ;
            c.gridx = 1 ; c.gridy = 4; c.gridwidth = 1 ; c.gridheight = 1;
            parentID = new Label(sInfo.parentId+"") ;
            frmInsertElevationMarker.add(parentID, c) ;
            
            //ButtonGroup bgroup = new ButtonGroup();
            //bgroup.add(txtRadius);
            //bgroup.add(lblEle);
            //bgroup.add(maybeButton);

            frmInsertElevationMarker.invalidate();
            frmInsertElevationMarker.show() ;
            frmInsertElevationMarker.toFront() ;
        }
        else {
            popMessageBox("No Contour Map", NO_MAP_MSG);
        }
            
    } // popInsertElevationMarker

    public void print(){
        // print current frame
        //PrintUtilities.printComponent(this) ;    //, printPageFormat); 
        //PrintUtilities pu = new PrintUtilities(this) ;
        
        hd_pu = new PrintUtilities(this) ;
        hd_pu.print();
    }   // print
    
    public void printPageSetup(){
        // print current frame
//        PrintUtilities.printPageSetup() ;   
//        PrintUtilities pu = new PrintUtilities(this) ;
        hd_pu = new PrintUtilities(this) ;
        hd_pu.printPageSetup();
    }   // printPageSetup
    
    /** Pop up a window to display message */    
    public void popEditElevationMarker() {

            // open a frame
            frmEditElevationMarker = new myWindow(sInfo.title) ;
            frmEditElevationMarker.setLocation(350,40) ;
            frmEditElevationMarker.setSize(300,200) ;
            frmEditElevationMarker.validate() ;
            frmEditElevationMarker.setVisible(true) ;
            frmEditElevationMarker.setResizable(false) ;

            ActionListener frmEditElevationMarker_ok_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    if (txtEle.getText().length()>0) {
                        sInfo.elevation = new Float(txtEle.getText()).floatValue(); 
                        byte segment_type ;
                        if ( line.getState() == true) {
                            segment_type = 1 ;
                        } else if (curve.getState() == true ) {
                            segment_type = 2;
                        } else {
                            segment_type = 3;
                        }
                        if (sInfo.CheckBox_edit == true ) {
                            // edit existing landmark
                            int index ;
                            index = sInfo.dataIndex ;
                            myDB.elevationMarks[index].setMarker(myDB.currentElevationMarker, sInfo.elevation, new Integer(sInfo.parentId).byteValue(), segment_type);
                        } 
                        /*else {
                            myDB.elevationMarks[myDB.elevationMarkCount] = new MarkerDB();
                            // create new landmark
                            myDB.elevationMarks[myDB.elevationMarkCount].setMarker(myDB.currentElevationMarker, sInfo.elevation, new Integer(sInfo.parentId).byteValue(), segment_type);
                            myDB.elevationMarkCount += 1;
                            // save # of data in log buffer
                            push2MarkLogBuffer(myDB.elevationMarkCount);
                        }
                        */
                        frmEditElevationMarker.dispose() ;
                        repaint();
                    } else {
                        // txtEle elevation field is empty
                        popMessageBox("Elevation Data", "Please specify station elevation!");
                    }
                }   // action performed
            } ;
            ActionListener frmEditElevationMarker_delete_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    // delete selected landmark
                    // shift landmarkd by 1 starting from sInfo.insert index
                    int index = sInfo.dataIndex ;
                    float x, y ;
                    mPointF ptf ;
                    for (int i=index; i<myDB.elevationMarkCount-1; i++) {
                        ptf = myDB.elevationMarks[i+1].getLocation() ;
                        x = ptf.X ;
                        y = ptf.Y ;
                        myDB.elevationMarks[i].setLocation(x, y) ;
                        myDB.elevationMarks[i].setElevation(myDB.elevationMarks[i+1].getElevation()) ;
                        myDB.elevationMarks[i].setSegmentType(myDB.elevationMarks[i+1].getSegmentType()) ;
                        myDB.elevationMarks[i].setDistance(myDB.elevationMarks[i+1].getDistance()) ;
                        myDB.elevationMarks[i].setParentIndex(myDB.elevationMarks[i+1].getParentIndex()) ;
                        myDB.elevationMarks[i].setGrade(myDB.elevationMarks[i+1].getGrade()) ;
                        myDB.elevationMarks[i].PVTnC_Overlap = myDB.elevationMarks[i+1].PVTnC_Overlap ;
                    }
                    myDB.elevationMarkCount -= 1;
                    // save # of data in log buffer
                    //push2MarkLogBuffer(myDB.elevationMarkCount);
                    
                    frmEditElevationMarker.dispose() ;
                    repaint();
                }
            } ;

            frmEditElevationMarker.setLayout(new GridBagLayout()) ;
            // Create a constrains object, and specify default values
            GridBagConstraints c = new GridBagConstraints() ;
            c.fill = GridBagConstraints.BOTH ; // component grows in both directions
            c.weightx = 1.0 ; c.weighty = 1.0 ;

            c.gridx = 0 ; c.gridy = 0; c.gridwidth = 1 ; c.gridheight = 1 ;
            c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
            String unitStr = "";
            if (myDB.myUnit==1) {
                unitStr = "(ft)";
            } else if (myDB.myUnit==2) {
                unitStr = "(m)";
            } 
            Label lblEle = new Label("Elevation " + unitStr) ;
            //frmEditElevationMarker.setBackground(new Color(200, 200, 200)) ;
            frmEditElevationMarker.add(lblEle,c) ;
            c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
            txtEle.setForeground(Color.RED) ;
            txtEle.setText(CStr(sInfo.elevation));
            frmEditElevationMarker.add(txtEle,c) ;
            
            c.gridx = 1 ; c.gridy = 0; c.gridwidth = 1 ;
            Button btn_ok = new Button(" OK ") ;
            frmEditElevationMarker.add(btn_ok, c) ;
            btn_ok.addActionListener(frmEditElevationMarker_ok_listener) ;
            c.gridx = 1 ; c.gridy = 1;
            Button btn_delete = new Button(" Delete ") ;
            frmEditElevationMarker.add(btn_delete, c) ;
            btn_delete.addActionListener(frmEditElevationMarker_delete_listener) ;

            Panel radioButtonPanel = new Panel();
            radioButtonPanel.setLayout(new GridLayout(3, 1));
            CheckboxGroup checks = new CheckboxGroup();
            line=new Checkbox("Line Segment", sInfo.line_option, checks);
            curve=new Checkbox("Curve Segment", sInfo.curve_option, checks);
            tangent=new Checkbox("Tangent Point", sInfo.tangent_option, checks);
            radioButtonPanel.add(line);
            radioButtonPanel.add(curve);
            radioButtonPanel.add(tangent);
            c.gridx = 0 ; c.gridy = 2; c.gridwidth = 1 ; c.gridheight = 3;
            frmEditElevationMarker.add(radioButtonPanel, c) ;
            c.gridx = 1 ; c.gridy = 2; c.gridwidth = 1 ; c.gridheight = 1;
            String sx=new Float(sInfo.location.getX()).toString();
            String sy=new Float(sInfo.location.getY()).toString();
            frmEditElevationMarker.add(new Label("X:"+sx), c) ;
            c.gridx = 1 ; c.gridy = 3; c.gridwidth = 1 ; c.gridheight = 1;
            frmEditElevationMarker.add(new Label("Y:"+sy), c) ;
            c.gridx = 1 ; c.gridy = 4; c.gridwidth = 1 ; c.gridheight = 1;
            frmEditElevationMarker.add(new Label(sInfo.parentId+""), c) ;
            
            //ButtonGroup bgroup = new ButtonGroup();
            //bgroup.add(txtRadius);
            //bgroup.add(lblEle);
            //bgroup.add(maybeButton);

            frmEditElevationMarker.invalidate();
            frmEditElevationMarker.show() ;
            frmEditElevationMarker.toFront() ;
            
    } // popEditElevationMarker


 
    public void mouseClicked(java.awt.event.MouseEvent mouseEvent) {
        if ((mouseEvent.getModifiers() & InputEvent.BUTTON3_MASK)
            == InputEvent.BUTTON3_MASK) {
            mPoint e = new mPoint(mouseEvent.getX(),mouseEvent.getY());
            mPointF marker ;
            
            // right Mouse clicked
            switch (toolbarIndex){
                case 5:	// curve
                    idSegment = -1 ;
                    mPoint radMod = searchSegmentDB(transform(e));
                    if ((radMod.X >= 0) && (radMod.Y >= 0)) { 
                        // found a segment
                        if (myDB.hRoadData[radMod.X].getRadius() > 0f) { 
                            // curve center selected
                            // modify curve radius
                            if (frame_editCurveSetting != null) { 
                                if (frame_editCurveSetting.isShowing()) {
                                    frame_editCurveSetting.dispose();
                                }
                            }
                            idSegment = radMod.X;
                            //frame_curveSetting.dispose();
                            popEditCurveSettings();
                            e0 = null;
                            e1 = null;
                            //frmEditCurveSetting.segIndex.Text = radMod.X.ToString()
                            //frmEditCurveSetting.txtRadius.Text = hRoadData(radMod.X).getRadius().ToString
                            //frmEditCurveSetting.Show()
                        }
                    }
                    break;
                case 6: // modify tangent point
                    if (myDB.hAlignMarkCount>1) {
                        dataSelIndex = -1;
                        marker = transform(e);
                        dataSelIndex = checkhAlignTangent(marker);
                        //System.out.println("click tanget index="+dataSelIndex) ;
                        if (dataSelIndex >= 0 ) {
                            // pop window to delete tangent pair ?
                            deleteTangent_flag = true ;
                            //popDeleteTangent("Delete Tangent Data","Do you want to delete tangent data pair?");
                        }
                    }
                    break;
                case 8: case 9: // edit station marker
                    int dataIndex = -1;
                    //mPointF marker ;
                    marker = transform(e);
                    dataIndex = checkElevationLandmarks(marker);
                    if (dataIndex >= 0) {
                        // comment out 3/4/06, use existing ele landmark point instead of clicked point
                        //currentElevationMarker = marker

                        // pop screen to enter evelation & save marker data
                        if (frmEditElevationMarker != null) { 
                            if (frmEditElevationMarker.isShowing()) {
                                frmEditElevationMarker.dispose();
                            }
                        }
                        int dVal = dataIndex+1 ;
                        sInfo = new StationInfo();
                        sInfo.title = "Edit Station (" + dVal + ")";
                        sInfo.elevation = myDB.elevationMarks[dataIndex].getElevation() ;
                        sInfo.CheckBox_edit = true;
                        sInfo.dataIndex = dataIndex ;
                        sInfo.location = new mPointF(mouseEvent.getX(),mouseEvent.getY());
                        sInfo.parentId  = myDB.elevationMarks[dataIndex].getParentIndex();
                        sInfo.optionInit(); 
                        switch(myDB.elevationMarks[dataIndex].getSegmentType() ) {
                            case 1:
                                sInfo.line_option = true;
                                break;
                            case 2:
                                sInfo.curve_option = true;
                                break;
                            case 3:
                                sInfo.tangent_option=true;
                                break;
                        } 
                        popEditElevationMarker(); // display elevation landmark edit screen
                    }   // if dataIndex>0
                    break;
                default:   // all else
                    // added 12/21/2007, chenfu
                    checkItemSelect(transform(e));
                    //System.out.println("right_clicked str = "+item_clicked_str) ;
                    if (item_clicked_str.length()>1) {
                        parseClickedStr() ;
                        popDeleteSegment("Edit - Delete","Are you sure to delete selected segment(s)?");
                    }
                    break ;
            }   // switch
        }   // if right mouse
    }   // end function

    // used to isolate right click selected item(s)
    private void parseClickedStr() {
        int end ;
        String myStr = item_clicked_str ;
        // reset all setect items
        edit_unselectAll() ;
        while (myStr.length()>0) {
            end = myStr.indexOf(",", 0) ;
            int idx = CInt(myStr.substring(0, end)) ;
            myStr = myStr.substring(end+1) ;
            myDB.hRoadData[idx].setItemSelect(true); 
        }
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
    
    /** Pop up a window to display curve setting */    
    public void popCurveSettings() {
        if (image!=null){
            // open a frame
            frame_curveSetting = new myWindow("Curve Settings") ;
            frame_curveSetting.setSize(200,120) ;
            //frame_curveSetting.setCenter() ;
            frame_curveSetting.validate() ;
            frame_curveSetting.setVisible(true) ;
            frame_curveSetting.setResizable(false);
            frame_curveSetting.setLocation(250,2) ;

            KeyAdapter frame_curveSetting_radius_listener = new KeyAdapter() {
                public void keyTyped(KeyEvent ke) {
                }
                public void keyPressed(KeyEvent ke) {
                }        
                public void keyReleased(KeyEvent ke) {
                    String str = txtRadius.getText();
                    float val = new Float(str).floatValue();
                    myDB.curveRadius = val;
                    //System.out.println(str + ", val=" + new Float(val).toString());
                }        
            } ;
            // check min radius
            ActionListener frame_curveSetting_chk_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    float Rv, spd, val ;
                    if (myDB.myUnit==1){
                        spd = (myDB.speedLimit * 1.467f);
                        calcMinRadius = spd*spd / 32.2f / (myDB.sideFrictionCoef + myDB.maxSuperelevation);
                        // or using MPH speed, AASHTO 2004, pp.146 Eq 3-10
                        //calcMinRadius = (speedLimit ^ 2) / (15 * (sideFrictionCoef + maxSuperelevation))
                    } else if (myDB.myUnit==2) {
                        spd = myDB.speedLimit;
                        // or using MPH speed, AASHTO 2004, pp.146 Eq 3-10
                        calcMinRadius = (spd*spd) / (127f * (myDB.sideFrictionCoef + myDB.maxSuperelevation));
                    }
                    val = new Float(txtRadius.getText()).floatValue();
                    String str_Rv = new Float(calcMinRadius).toString();
                    if (calcMinRadius > val) { 
                        popUpdateCurveRadius();
                    } else {
                        popMessageBox("Check Minimum Radius","Design radius greater than \nminimum radius " + str_Rv + ". OK!");
                    }

                    //frame_curveSetting.dispose() ;
                }
            } ;
            String unitStr="";
            if (myDB.myUnit == 1) {
                unitStr = "(ft)" ;
            } else if (myDB.myUnit == 2) { 
                unitStr = "(m)" ;
            }
    //System.out.println("myUnit="+myDB.myUnit);

            frame_curveSetting.setLayout(new GridBagLayout()) ;
            // Create a constrains object, and specify default values
            GridBagConstraints c = new GridBagConstraints() ;
            c.fill = GridBagConstraints.BOTH ; // component grows in both directions
            c.weightx = 1.0 ; c.weighty = 1.0 ;

            c.gridx = 0 ; c.gridy = 0; c.gridwidth = 1 ; c.gridheight = 1 ;
            c.insets = new Insets(5,5,0,5) ; // 5-pixel margins on all sides
            Label lblRadius = new Label("Radius ");
            txtRadius= new TextField(new Float(myDB.minHCurveRadius).toString()) ;
            //txtRadius.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
            //txtRadius.setForeground(new Color(0,0,218)) ;
            //frame_curveSetting.setBackground(new Color(200, 200, 200)) ;
            frame_curveSetting.add(lblRadius, c) ;
            c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
            c.insets = new Insets(1,5,0,5) ;
            frame_curveSetting.add(txtRadius,c) ;
            c.gridx = 1 ; c.gridy = 1; c.gridwidth = 1 ;
            frame_curveSetting.add(new Label(unitStr),c) ;
            c.gridx = 0 ; c.gridy = 2; c.gridwidth = 2 ;
            c.insets = new Insets(5,5,5,5) ;
            Button btn_chk = new Button("Check Minimum Radius") ;
            frame_curveSetting.add(btn_chk,c) ;

            txtRadius.addKeyListener(frame_curveSetting_radius_listener);
            btn_chk.addActionListener(frame_curveSetting_chk_listener) ;

            frame_curveSetting.invalidate();
            //frame_curveSetting.toFront() ;
            frame_curveSetting.show() ;
        }
        else {
            popMessageBox("No Contour Map", NO_MAP_MSG);
        }
        
    } // popCurveSettings

   /** Pop up a window to display contour image setting */    
    public void popSettingsContour() {
        // open a frame
        frame_settingsContour = new myWindow("Contour Image Settings") ;
        //frame_settingsContour.setLocation(350,60) ;
        frame_settingsContour.setSize(200,150) ;
        frame_settingsContour.setCenter() ;
        frame_settingsContour.validate() ;
        frame_settingsContour.setVisible(true) ;
        frame_settingsContour.setResizable(false);
        KeyAdapter frame_settingsContour_listener = new KeyAdapter() {
            public void keyTyped(KeyEvent ke) {
            }
            public void keyPressed(KeyEvent ke) {
            }        
            public void keyReleased(KeyEvent ke) {
            }        
        } ;
        // save changes
        ActionListener frame_settingsContour_ok_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                float val;
                val = new Float(txtImgResol.getText()).floatValue();
                myDB.ContourImageResolution = val ;
                val = new Float(txtMapScale.getText()).floatValue();
                myDB.ContourScale = val ;
                myDB.imageScale = (float)myDB.ContourImageResolution / (float)myDB.ContourScale;  //  // pixel/ft

                frame_settingsContour.dispose() ;
                repaint() ;
            }
        } ;
        ActionListener frame_settingsContour_cancel_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_settingsContour.dispose() ;
                repaint() ;
            }
        } ;
        String unitStr="", unitStr1="";
        if (myDB.myUnit == 1) {
            unitStr = "pixel/in" ;
            unitStr1 = "ft/in" ;
        } else if (myDB.myUnit == 2) { 
            unitStr = "pixel/cm" ;
            unitStr1 = "m/cm" ;
        }

        frame_settingsContour.setLayout(new GridBagLayout()) ;
        // Create a constrains object, and specify default values
        GridBagConstraints c = new GridBagConstraints() ;
        c.fill = GridBagConstraints.BOTH ; // component grows in both directions
        c.weightx = 1.0 ; c.weighty = 1.0 ;

        c.gridx = 0 ; c.gridy = 0; c.gridwidth = 1 ; c.gridheight = 1 ;
        c.insets = new Insets(5,5,0,5) ; // 5-pixel margins on all sides
        Label lblimg = new Label("Image Rosolution");
        txtImgResol= new TextField(new Float(myDB.ContourImageResolution).toString()) ;
        //txtImgResol.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
        //txtImgResol.setForeground(new Color(0,0,218)) ;
        //frame_settingsContour.setBackground(new Color(200, 200, 200)) ;
        frame_settingsContour.add(lblimg, c) ;
        c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
        c.insets = new Insets(1,5,0,5) ;
        frame_settingsContour.add(txtImgResol,c) ;
        c.gridx = 1 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_settingsContour.add(new Label(unitStr),c) ;

        c.gridx = 0 ; c.gridy = 2; c.gridwidth = 1 ; c.gridheight = 1 ;
        c.insets = new Insets(5,5,0,5) ; // 5-pixel margins on all sides
        Label lblmap = new Label("Map Scale");
        txtMapScale= new TextField(new Float(myDB.ContourScale).toString()) ;
        frame_settingsContour.add(lblmap, c) ; 
        c.gridx = 0 ; c.gridy = 3; c.gridwidth = 1 ;
        c.insets = new Insets(1,5,0,5) ;
        frame_settingsContour.add(txtMapScale,c) ;
        c.gridx = 1 ; c.gridy = 3; c.gridwidth = 1 ;
        frame_settingsContour.add(new Label(unitStr1),c) ;

        c.gridx = 0 ; c.gridy = 4; c.gridwidth = 1 ;
        c.insets = new Insets(5,5,5,5) ;
        Button btn_ok = new Button(" OK ") ;
        frame_settingsContour.add(btn_ok,c) ;
        c.gridx = 1 ; c.gridy = 4; c.gridwidth = 1 ;
        Button btn_cancel = new Button(" Cancel ") ;
        frame_settingsContour.add(btn_cancel,c) ;
        
        btn_ok.addActionListener(frame_settingsContour_ok_listener) ;
        btn_cancel.addActionListener(frame_settingsContour_cancel_listener) ;
        
        frame_settingsContour.invalidate();
        frame_settingsContour.show() ;
        frame_settingsContour.toFront() ;
        
    } // popSettingsContour

   /** Pop up a window to display road design setting */    
    public void popSettingsDesign() {
        // open a frame
        frame_settingsDesign = new myWindow("Design Settings") ;
        //frame_settingsDesign.setLocation(330,80) ;
        frame_settingsDesign.setSize(470,410) ;
        frame_settingsDesign.setCenter() ;
        frame_settingsDesign.validate() ;
        frame_settingsDesign.setVisible(true) ;
        frame_settingsDesign.setResizable(false);
        // save changes
        ActionListener frame_settingsDesign_ok_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                myDB.speedLimit=new Float(txtSpeed.getText()).floatValue();
                myDB.maxCut=new Float(txtMaxcut.getText()).floatValue();
                myDB.maxFill=new Float(txtMaxfill.getText()).floatValue();
                myDB.gradeLimit=new Float(txtMaxgrade.getText()).floatValue()/100f;
                myDB.minGrade=new Float(txtMingrade.getText()).floatValue()/100f;
                
                myDB.reactionTime=new Float(txtReactiontime.getText()).floatValue();
                myDB.vehDecel=new Float(txtDecel.getText()).floatValue();
                myDB.frictionCoef=new Float(txtFricoef.getText()).floatValue();
                myDB.sideFrictionCoef=new Float(txtSFricoef.getText()).floatValue();
                myDB.minVCurveLen=new Float(txtVCurLen.getText()).floatValue();
                myDB.minHCurveRadius=new Float(txtHCurRadius.getText()).floatValue();
                myDB.maxSuperelevation=new Float(txtMaxsuperE.getText()).floatValue()/100f;

                myDB.myRoadLaneSizes=new Float(listRoadwidth.getItem(listRoadwidth.getSelectedIndex())).floatValue();
                myDB.myLaneWidth=new Float(txtLanewidth.getText()).floatValue();
                myDB.myShoulderWidth=new Float(txtShoulderwidth.getText()).floatValue();
                myDB.myPenColor=lblRoadColor.getBackground();

                myDB.elevationMarkerSize=new Float(txtMarkersize.getText()).floatValue();
                myDB.elevationMarkerColor=lblMarkerColor.getBackground();
                myDB.myUnit=new Integer(listUnit.getSelectedIndex()+1).intValue();
                frame_settingsDesign.dispose() ;
                repaint() ;
            }
        } ;
        ActionListener frame_settingsDesign_cancel_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_settingsDesign.dispose() ;
                repaint() ;
            }
        } ;
        /*
        ActionListener frame_settingsDesign_roadColor_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                
            }
        } ;
        ActionListener frame_settingsDesign_markerColor_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                
            }
        } ;
        */
        ItemListener frame_settingsDesign_roadColor_listener = new ItemListener() {
            public void itemStateChanged(ItemEvent ie) {
                int index = listRoadColor.getSelectedIndex();
                switch (index-1) {
                    case 0: // black
                        lblRoadColor.setBackground(Color.BLACK);
                        break ;
                    case 1: // blue
                        lblRoadColor.setBackground(Color.BLUE);
                        break ;
                    case 2: // cyan
                        lblRoadColor.setBackground(Color.CYAN);
                        break ;
                    case 3: // darkgray
                        lblRoadColor.setBackground(Color.DARK_GRAY);
                        break ;
                    case 4: // gray
                        lblRoadColor.setBackground(Color.GRAY);
                        break ;
                    case 5: // green
                        lblRoadColor.setBackground(Color.GREEN);
                        break ;
                    case 6: // light gray
                        lblRoadColor.setBackground(Color.LIGHT_GRAY);
                        break ;
                    case 7: // magenta
                        lblRoadColor.setBackground(Color.MAGENTA);
                        break ;
                    case 8: // orange
                        lblRoadColor.setBackground(Color.ORANGE);
                        break ;
                    case 9: // pink
                        lblRoadColor.setBackground(Color.PINK);
                        break ;
                    case 10: // red
                        lblRoadColor.setBackground(Color.RED);
                        break ;
                    case 11: // white
                        lblRoadColor.setBackground(Color.WHITE);
                        break ;
                    case 12: // yellow
                        lblRoadColor.setBackground(Color.YELLOW);
                        break ;
                }// switch
            }
        } ;
        ItemListener frame_settingsDesign_markerColor_listener = new ItemListener() {
            public void itemStateChanged(ItemEvent ie) {
                int index = listMarkerColor.getSelectedIndex();
                switch (index-1) {
                    case 0: // black
                        lblMarkerColor.setBackground(Color.BLACK);
                        break ;
                    case 1: // blue
                        lblMarkerColor.setBackground(Color.BLUE);
                        break ;
                    case 2: // cyan
                        lblMarkerColor.setBackground(Color.CYAN);
                        break ;
                    case 3: // darkgray
                        lblMarkerColor.setBackground(Color.DARK_GRAY);
                        break ;
                    case 4: // gray
                        lblMarkerColor.setBackground(Color.GRAY);
                        break ;
                    case 5: // green
                        lblMarkerColor.setBackground(Color.GREEN);
                        break ;
                    case 6: // light gray
                        lblMarkerColor.setBackground(Color.LIGHT_GRAY);
                        break ;
                    case 7: // magenta
                        lblMarkerColor.setBackground(Color.MAGENTA);
                        break ;
                    case 8: // orange
                        lblMarkerColor.setBackground(Color.ORANGE);
                        break ;
                    case 9: // pink
                        lblMarkerColor.setBackground(Color.PINK);
                        break ;
                    case 10: // red
                        lblMarkerColor.setBackground(Color.RED);
                        break ;
                    case 11: // white
                        lblMarkerColor.setBackground(Color.WHITE);
                        break ;
                    case 12: // yellow
                        lblMarkerColor.setBackground(Color.YELLOW);
                        break ;
                }// switch
            }
        } ;
        ItemListener frame_settingsDesign_unit_listener = new ItemListener() {
            public void itemStateChanged(ItemEvent ie) {
                int i;
                int index = listUnit.getSelectedIndex();
                float oldImageScale, scaleErr ;
                switch (index) {
                    case 0: // US unit
                        // US unit
                        lblUnit1.setText("MPH");    // speed
                        lblUnit2.setText("ft");     // max cut
                        lblUnit3.setText("ft");     // max fill
                        lblUnit4.setText("ft/s/s"); // decel
                        lblUnit5.setText("ft");     // min vertical curve len
                        lblUnit6.setText("ft");     // min horizontal curve radius
                        lblUnit7.setText("ft");     // lane width
                        lblUnit8.setText("ft");     // shoulder width

                        txtMaxcut.setText( CStr(CFloat(txtMaxcut.getText()) / myDB.FT2M));
                        txtMaxfill.setText(CStr(CFloat(txtMaxfill.getText()) / myDB.FT2M));
                        txtVCurLen.setText(CStr(CFloat(txtVCurLen.getText()) / myDB.FT2M));
                        txtHCurRadius.setText(CStr(CFloat(txtHCurRadius.getText()) / myDB.FT2M));
                        txtDecel.setText(CStr(CFloat(txtDecel.getText()) / myDB.FT2M));
                        txtSpeed.setText(CStr(CFloat(txtSpeed.getText()) / myDB.MPH2Kmh));
                        txtLanewidth.setText(CStr(CFloat(txtLanewidth.getText()) / myDB.FT2M));
                        txtShoulderwidth.setText(CStr(CFloat(txtShoulderwidth.getText()) / myDB.FT2M));
                        
                        // 4/30/06 added change from metric to US
                        myDB.ContourImageResolution = CInt(myDB.ContourImageResolution * 2.54f);
                        myDB.ContourScale = CInt(myDB.ContourScale / 0.12f);
                        oldImageScale = myDB.imageScale ;
                        myDB.imageScale = (float)myDB.ContourImageResolution / (float)myDB.ContourScale;
                        scaleErr = oldImageScale * myDB.FT2M / myDB.imageScale ;
                        // update radius
                        //int i ;
                        if (hRoadDataCount > 0) { 
                            float radius ;
                            for (i=0; i<hRoadDataCount; i++) {
                                radius = myDB.hRoadData[i].getRadius();
                                if (radius > 0) { 
                                    myDB.hRoadData[i].setRadius(radius / myDB.FT2M * scaleErr);
                                }   //End If
                            }   //Next // i
                        }   //End If
                        // update station elevation data
                        if (myDB.elevationMarkCount > 0) {  // Then
                            float elevation, distance ;
                            for (i=0; i<myDB.elevationMarkCount; i++) {
                                elevation = myDB.elevationMarks[i].getElevation();
                                distance = myDB.elevationMarks[i].getDistance();
                                myDB.elevationMarks[i].setElevation(elevation / myDB.FT2M);
                                myDB.elevationMarks[i].setDistance(distance / myDB.FT2M * scaleErr);
                            }   //Next    ' i
                        }   //End If
                        
                        break;
                    case 1: // metric
                        // metric unit
                        lblUnit1.setText("Km/h");    // speed
                        lblUnit2.setText("m");     // max cut
                        lblUnit3.setText("m");     // max fill
                        lblUnit4.setText("m/s/s"); // decel
                        lblUnit5.setText("m");     // min vertical curve len
                        lblUnit6.setText("m");     // min horizontal curve radius
                        lblUnit7.setText("m");     // lane width
                        lblUnit8.setText("m");     // shoulder width

                        txtMaxcut.setText( CStr(CFloat(txtMaxcut.getText()) * myDB.FT2M));
                        txtMaxfill.setText(CStr(CFloat(txtMaxfill.getText()) * myDB.FT2M));
                        txtVCurLen.setText(CStr(CFloat(txtVCurLen.getText()) * myDB.FT2M));
                        txtHCurRadius.setText(CStr(CFloat(txtHCurRadius.getText()) * myDB.FT2M));
                        txtDecel.setText(CStr(CFloat(txtDecel.getText()) * myDB.FT2M));
                        txtSpeed.setText(CStr(CFloat(txtSpeed.getText()) * myDB.MPH2Kmh));
                        txtLanewidth.setText(CStr(CFloat(txtLanewidth.getText()) * myDB.FT2M));
                        txtShoulderwidth.setText(CStr(CFloat(txtShoulderwidth.getText()) * myDB.FT2M));
                        
                        // 4/30/06 added change from US to metric
                        myDB.ContourImageResolution = CInt(myDB.ContourImageResolution / 2.54f);
                        myDB.ContourScale = CInt(myDB.ContourScale * 0.12f);
                        oldImageScale = myDB.imageScale ;
                        myDB.imageScale = (float)myDB.ContourImageResolution / (float)myDB.ContourScale;
                        scaleErr = oldImageScale / myDB.FT2M / myDB.imageScale ;
                        // update radius
                        //int i ;
                        if (hRoadDataCount > 0) { 
                            float radius ;
                            for (i=0; i<hRoadDataCount; i++) {
                                radius = myDB.hRoadData[i].getRadius();
                                if (radius > 0) { 
                                    myDB.hRoadData[i].setRadius(radius * myDB.FT2M * scaleErr);
                                }   //End If
                            }   //Next // i
                        }   //End If
                        // update station elevation data
                        if (myDB.elevationMarkCount > 0) {  // Then
                            float elevation, distance ;
                            for (i=0; i<myDB.elevationMarkCount; i++) {
                                elevation = myDB.elevationMarks[i].getElevation();
                                distance = myDB.elevationMarks[i].getDistance();
                                myDB.elevationMarks[i].setElevation(elevation * myDB.FT2M);
                                myDB.elevationMarks[i].setDistance(distance * myDB.FT2M * scaleErr);
                            }   //Next    ' i
                        }   //End If

                        break;
                }   // switch
            }   // itemStateChanged
        } ; // frame_settingsDesign_unit_listener

        String unitStr="", unitStr1="";
        if (myDB.myUnit == 1) {
            unitStr = "MPH" ;
            unitStr1 = "ft" ;
        } else if (myDB.myUnit == 2) { 
            unitStr = "Km/h" ;
            unitStr1 = "m" ;
        }

        frame_settingsDesign.setLayout(new GridBagLayout()) ;
        // Create a constrains object, and specify default values
        GridBagConstraints c = new GridBagConstraints() ;
        c.fill = GridBagConstraints.BOTH ; // component grows in both directions
        c.weightx = 1.0 ; c.weighty = 1.0 ;
        // row 0 
        c.gridx = 0 ; c.gridy = 0; c.gridwidth = 1 ; c.gridheight = 1 ;
        c.insets = new Insets(0,5,0,5) ; // 5-pixel margins on all sides
        Label lblgen = new Label("General");
        lblgen.setForeground(Color.BLUE);
        Font myFont = new Font("Times New Roman", Font.BOLD, 14);
        lblgen.setFont(myFont);
        frame_settingsDesign.add(lblgen,c);
        // row 1 Left ===================
        c.gridx = 0 ; c.gridy = 1;  c.insets = new Insets(0,5,5,5) ;
        frame_settingsDesign.add( new Label("Speed Limit"),c);
        c.gridx = 1 ; c.insets = new Insets(0,0,5,5) ;
        txtSpeed= new TextField(new Float(myDB.speedLimit).toString()) ;
        frame_settingsDesign.add(txtSpeed,c) ;
        c.gridx = 2 ; 
        lblUnit1 = new Label(unitStr);
        frame_settingsDesign.add(lblUnit1,c);
        // row 1 Right
        c.gridx = 3 ;
        frame_settingsDesign.add( new Label("Reaction Time"),c);
        c.gridx = 4 ; 
        txtReactiontime= new TextField(new Float(myDB.reactionTime).toString()) ;
        frame_settingsDesign.add(txtReactiontime,c) ;
        c.gridx = 5 ; 
        frame_settingsDesign.add(new Label("sec"),c);
        
        // row 2 Left ===================
        c.gridx = 0 ; c.gridy = 2; 
        c.insets = new Insets(0,5,5,5) ; // 5-pixel margins on all sides
        frame_settingsDesign.add( new Label("Max Cut"),c);
        c.gridx = 1 ; c.insets = new Insets(0,0,5,5) ;
        txtMaxcut= new TextField(new Float(myDB.maxCut).toString()) ;
        frame_settingsDesign.add(txtMaxcut,c) ;
        c.gridx = 2 ; 
        lblUnit2 = new Label(unitStr1);
        frame_settingsDesign.add(lblUnit2,c);
        // row 2 Right
        c.gridx = 3 ;
        frame_settingsDesign.add( new Label("Deceleration"),c);
        c.gridx = 4 ; 
        txtDecel= new TextField(new Float(myDB.vehDecel).toString()) ;
        frame_settingsDesign.add(txtDecel,c) ;
        c.gridx = 5 ; 
        lblUnit4 = new Label(unitStr1+"/s/s");
        frame_settingsDesign.add(lblUnit4,c);

        // row 3 Left ===================
        c.gridx = 0 ; c.gridy = 3; 
        c.insets = new Insets(0,5,5,5) ; // 5-pixel margins on all sides
        frame_settingsDesign.add( new Label("Max Fill"),c);
        c.gridx = 1 ; c.insets = new Insets(0,0,5,5) ;
        txtMaxfill= new TextField(new Float(myDB.maxFill).toString()) ;
        frame_settingsDesign.add(txtMaxfill,c) ;
        c.gridx = 2 ; 
        lblUnit3 = new Label(unitStr1);
        frame_settingsDesign.add(lblUnit3,c);
        // row 3 Right
        c.gridx = 3 ;
        frame_settingsDesign.add( new Label("Friction Coef."),c);
        c.gridx = 4 ; 
        txtFricoef= new TextField(new Float(myDB.frictionCoef).toString()) ;
        frame_settingsDesign.add(txtFricoef,c) ;
        c.gridx = 5 ; 
        frame_settingsDesign.add(new Label(" "),c);

        // row 4 Left ===================
        c.gridx = 0 ; c.gridy = 4; 
        c.insets = new Insets(0,5,5,5) ; // 5-pixel margins on all sides
        frame_settingsDesign.add( new Label("Max Grade (%)"),c);
        c.gridx = 1 ;  c.insets = new Insets(0,0,5,5) ;
        txtMaxgrade= new TextField(new Float(myDB.gradeLimit*100f).toString()) ;
        frame_settingsDesign.add(txtMaxgrade,c) ;
        //c.gridx = 2 ; 
        //frame_settingsDesign.add(new Label("%"),c);
        // row 4 Right
        c.gridx = 3 ;
        frame_settingsDesign.add( new Label("Side Friction Coef."),c);
        c.gridx = 4 ; 
        txtSFricoef= new TextField(new Float(myDB.sideFrictionCoef).toString()) ;
        frame_settingsDesign.add(txtSFricoef,c) ;
        c.gridx = 5 ; 
        frame_settingsDesign.add(new Label(" "),c);

        // row 5  ===================================
        c.gridx = 0 ; c.gridy = 5; 
        c.insets = new Insets(0,5,5,5) ; // 5-pixel margins on all sides
        frame_settingsDesign.add( new Label("Min Grade (%)"),c);
        c.gridx = 1 ;  c.insets = new Insets(0,0,5,5) ;
        txtMingrade= new TextField(new Float(myDB.minGrade*100f).toString()) ;
        frame_settingsDesign.add(txtMingrade,c) ;

        c.gridx = 2 ; c.gridy = 5; c.gridwidth=2;
        c.insets = new Insets(0,5,5,5) ; // 5-pixel margins on all sides
        frame_settingsDesign.add( new Label("Minimum Vertical Curve Length"),c);
        c.insets = new Insets(0,0,5,5) ;
        c.gridx = 4 ; c.gridwidth=1;
        txtVCurLen= new TextField(new Float(myDB.minVCurveLen).toString()) ;
        frame_settingsDesign.add(txtVCurLen,c) ;
        c.gridx = 5 ; 
        lblUnit5 = new Label(unitStr1);
        frame_settingsDesign.add(lblUnit5,c);

        // row 6  ================================
        c.gridx = 2 ; c.gridy = 6; c.gridwidth=2;
        c.insets = new Insets(0,5,5,5) ; // 5-pixel margins on all sides
        frame_settingsDesign.add( new Label("Minimum Horizontal Curve Radius"),c);
        c.insets = new Insets(0,0,5,5) ;
        c.gridx = 4 ; c.gridwidth=1;
        txtHCurRadius= new TextField(new Float(myDB.minHCurveRadius).toString()) ;
        frame_settingsDesign.add(txtHCurRadius,c) ;
        c.gridx = 5 ; 
        lblUnit6 = new Label(unitStr1);
        frame_settingsDesign.add(lblUnit6,c);

        // row 7  ===================
        c.gridx = 2 ; c.gridy = 7; c.gridwidth=2;
        c.insets = new Insets(0,5,5,5) ; // 5-pixel margins on all sides
        frame_settingsDesign.add( new Label("Maximum Superelevation"),c);
        c.insets = new Insets(0,0,5,5) ;
        c.gridx = 4 ; c.gridwidth=1;
        txtMaxsuperE= new TextField(new Float(myDB.maxSuperelevation*100).toString()) ;
        frame_settingsDesign.add(txtMaxsuperE,c) ;
        c.gridx = 5 ; 
        frame_settingsDesign.add(new Label("%"),c);

        // row 8 
        c.gridx = 0 ; c.gridy = 8; c.gridwidth = 1 ; c.gridheight = 1 ;
        c.insets = new Insets(0,5,5,5) ; // 5-pixel margins on all sides
        Label lblroad = new Label("Road Design");
        lblroad.setForeground(Color.BLUE);
        lblroad.setFont(myFont);
        frame_settingsDesign.add(lblroad,c);
        // row 9 Left ===================
        c.gridx = 0 ; c.gridy = 9;
        c.insets = new Insets(0,5,5,5) ;
        frame_settingsDesign.add( new Label("Road Width"),c);
        c.gridx = 1 ; c.insets = new Insets(0,0,5,5) ;
        listRoadwidth= new Choice();
        listRoadwidth.add("2");
        listRoadwidth.add("4");
        listRoadwidth.add("6");
        frame_settingsDesign.add(listRoadwidth,c) ;
        listRoadwidth.select(CInt(myDB.myRoadLaneSizes/2)-1) ;
        c.gridx = 2 ; 
        frame_settingsDesign.add(new Label("Lanes"),c);
        // row 9 Right
        c.gridx = 3 ;
        frame_settingsDesign.add( new Label("Lane Width"),c);
        c.gridx = 4 ; 
        txtLanewidth= new TextField(new Float(myDB.myLaneWidth).toString()) ;
        frame_settingsDesign.add(txtLanewidth,c) ;
        c.gridx = 5 ; 
        lblUnit7 = new Label(unitStr1);
        frame_settingsDesign.add(lblUnit7,c);
        
        // row 10 Left ===================
        c.gridx = 0 ; c.gridy = 10; 
        c.insets = new Insets(0,5,5,5) ;
        frame_settingsDesign.add( new Label("Road Color"),c);
        c.gridx = 1 ;c.insets = new Insets(0,0,5,5) ;
        lblRoadColor=new Label();
        lblRoadColor.setBackground(myDB.myPenColor);
        frame_settingsDesign.add(lblRoadColor,c) ;
        
        c.gridx = 2 ; c.gridwidth = 1;
        listRoadColor = new Choice();
        listRoadColor.add("Edit");
        listRoadColor.add("Black");
        listRoadColor.add("Blue");
        listRoadColor.add("Cyan");
        listRoadColor.add("Dark Gray");
        listRoadColor.add("Gray");
        listRoadColor.add("Green");
        listRoadColor.add("Light Gray");
        listRoadColor.add("Magenta");
        listRoadColor.add("Orange");
        listRoadColor.add("Pink");
        listRoadColor.add("Red");
        listRoadColor.add("White");
        listRoadColor.add("Yellow");
        
        frame_settingsDesign.add(listRoadColor,c) ;
      //  c.gridx = 2 ; 
      //  Button btnRoadColorEdit = new Button("Edit");
      //  frame_settingsDesign.add(btnRoadColorEdit,c);
        // row 10 Right
        c.gridx = 3 ; c.gridwidth = 1;
        frame_settingsDesign.add( new Label("Shoulder Width"),c);
        c.gridx = 4 ; 
        txtShoulderwidth= new TextField(new Float(myDB.myShoulderWidth).toString()) ;
        frame_settingsDesign.add(txtShoulderwidth,c) ;
        c.gridx = 5 ; 
        lblUnit8 = new Label(unitStr1);
        frame_settingsDesign.add(lblUnit8,c);

        // row 11
        c.gridx = 0 ; c.gridy = 11; c.gridwidth = 2 ; c.gridheight = 1 ;
        c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
        Label lblmarker = new Label("Landmark / Station");
        lblmarker.setForeground(Color.BLUE);
        lblmarker.setFont(myFont);
        frame_settingsDesign.add(lblmarker,c);
        c.gridx = 3 ; c.gridwidth = 1 ; c.insets = new Insets(0,0,5,5) ;
        Label lblunit = new Label("Unit");
        lblunit.setForeground(Color.BLUE);
        lblunit.setFont(myFont);
        frame_settingsDesign.add(lblunit,c);
        // row 12 Left ===================
        c.gridx = 0 ; c.gridy = 12;
        c.insets = new Insets(0,5,5,5) ;
        frame_settingsDesign.add( new Label("Marker Size"),c);
        c.gridx = 1 ; c.insets = new Insets(0,0,5,5) ;
        txtMarkersize = new TextField(new Float(myDB.elevationMarkerSize).toString());
        frame_settingsDesign.add(txtMarkersize,c) ;
        c.gridx = 2 ; 
        frame_settingsDesign.add(new Label("Pixels"),c);
        c.gridx = 3 ; 
        listUnit= new Choice();
        listUnit.add("US Customary");
        listUnit.add("Metric");
        listUnit.select(myDB.myUnit-1) ;
        frame_settingsDesign.add(listUnit,c);
        // row 13 Left ===================
        c.gridx = 0 ; c.gridy = 13;
        c.insets = new Insets(0,5,10,5) ;
        frame_settingsDesign.add( new Label("Marker Color"),c);
        c.gridx=1 ;c.insets = new Insets(0,0,10,5) ;
        lblMarkerColor = new Label();
        lblMarkerColor.setBackground(myDB.elevationMarkerColor);
        frame_settingsDesign.add(lblMarkerColor,c) ;

        c.gridx = 2 ; c.gridwidth = 1;
        listMarkerColor = new Choice();
        listMarkerColor.add("Edit");
        listMarkerColor.add("Black");
        listMarkerColor.add("Blue");
        listMarkerColor.add("Cyan");
        listMarkerColor.add("Dark Gray");
        listMarkerColor.add("Gray");
        listMarkerColor.add("Green");
        listMarkerColor.add("Light Gray");
        listMarkerColor.add("Magenta");
        listMarkerColor.add("Orange");
        listMarkerColor.add("Pink");
        listMarkerColor.add("Red");
        listMarkerColor.add("White");
        listMarkerColor.add("Yellow");
        
        frame_settingsDesign.add(listMarkerColor,c) ;
       // c.gridx = 2 ; 
       // Button btnMarkerColorEdit = new Button("Edit");
       // frame_settingsDesign.add(btnMarkerColorEdit,c);

        // ======================================================

        c.gridx = 4 ; c.gridy = 12; c.gridwidth = 2 ;
        c.insets = new Insets(0,5,5,5) ;
        Button btn_ok = new Button(" OK ") ;
        frame_settingsDesign.add(btn_ok,c) ;
        c.insets = new Insets(0,0,5,5) ;
        c.gridx = 4 ; c.gridy = 13; c.gridwidth = 2 ;c.insets = new Insets(0,5,10,5) ;
        Button btn_cancel = new Button(" Cancel ") ;
        frame_settingsDesign.add(btn_cancel,c) ;
        
        btn_ok.addActionListener(frame_settingsDesign_ok_listener) ;
        btn_cancel.addActionListener(frame_settingsDesign_cancel_listener) ;
      //  btnRoadColorEdit.addActionListener(frame_settingsDesign_roadColor_listener) ;
      //  btnMarkerColorEdit.addActionListener(frame_settingsDesign_markerColor_listener) ;
        listUnit.addItemListener(frame_settingsDesign_unit_listener) ;
        listRoadColor.addItemListener(frame_settingsDesign_roadColor_listener) ;
        listMarkerColor.addItemListener(frame_settingsDesign_markerColor_listener) ;
        
        frame_settingsDesign.invalidate();
        frame_settingsDesign.show() ;
        frame_settingsDesign.toFront() ;
    } // popSettingsDesign

   /** Pop up a window to edit current curve setting */    
    public void popEditCurveSettings() {
        // open a frame
        frame_editCurveSetting = new myWindow("Edit Curve") ;
        frame_editCurveSetting.setLocation(450,2) ;
        frame_editCurveSetting.setSize(200,120) ;
        //frame_editCurveSetting.setCenter() ;
        frame_editCurveSetting.validate() ;
        frame_editCurveSetting.setVisible(true) ;
        frame_editCurveSetting.setResizable(false);
        
        KeyAdapter frame_editCurveSetting_radius_listener = new KeyAdapter() {
            public void keyTyped(KeyEvent ke) {
            }
            public void keyPressed(KeyEvent ke) {
            }        
            public void keyReleased(KeyEvent ke) {
               // String str = txtEditRadius.getText();
               // float val = new Float(str).floatValue();
               // myDB.curveRadius = val;
                //System.out.println(str + ", val=" + new Float(val).toString());
            }        
        } ;
        // OK
        ActionListener frame_editCurveSetting_ok_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                float val ;
                val = new Float(txtEditRadius.getText()).floatValue();
                myDB.hRoadData[idSegment].setRadius(val);
                frame_editCurveSetting.dispose() ;
                //System.out.println("Radius="+ new Float(val).toString());
                repaint();
            }
        } ;
        // cancel
        ActionListener frame_editCurveSetting_cancel_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_editCurveSetting.dispose() ;
                //System.out.println("Radius="+ new Float(val).toString());
                repaint();
            }
        } ;
        String unitStr="";
        if (myDB.myUnit == 1) {
            unitStr = "(ft)" ;
        } else if (myDB.myUnit == 2) { 
            unitStr = "(m)" ;
        }
//System.out.println("myUnit="+myDB.myUnit);

        frame_editCurveSetting.setLayout(new GridBagLayout()) ;
        // Create a constrains object, and specify default values
        GridBagConstraints c = new GridBagConstraints() ;
        c.fill = GridBagConstraints.BOTH ; // component grows in both directions
        c.weightx = 1.0 ; c.weighty = 1.0 ;

        c.gridx = 0 ; c.gridy = 0; c.gridwidth = 1 ; c.gridheight = 1 ;
        c.insets = new Insets(5,5,0,5) ; // 5-pixel margins on all sides
        Label lblRadius = new Label("Radius ");
        txtEditRadius= new TextField(new Float(myDB.hRoadData[idSegment].getRadius()).toString()) ;
        //txtEditRadius.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
        //txtEditRadius.setForeground(new Color(0,0,218)) ;
        //frame_editCurveSetting.setBackground(new Color(200, 200, 200)) ;
        frame_editCurveSetting.add(lblRadius, c) ;
        c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
        c.insets = new Insets(1,5,0,5) ;
        frame_editCurveSetting.add(txtEditRadius,c) ;
        txtEditRadius.addKeyListener(frame_editCurveSetting_radius_listener);
        c.gridx = 1 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_editCurveSetting.add(new Label(unitStr),c) ;
        c.gridx = 0 ; c.gridy = 2; c.gridwidth = 1 ;
        c.insets = new Insets(5,5,5,5) ;
        Button btn_ok = new Button("Ok") ;
        frame_editCurveSetting.add(btn_ok,c) ;
        c.gridx = 1 ; c.gridy = 2; c.gridwidth = 1 ;
        Button btn_cancel = new Button("Cancel") ;
        frame_editCurveSetting.add(btn_cancel,c) ;
        
        btn_ok.addActionListener(frame_editCurveSetting_ok_listener) ;
        btn_cancel.addActionListener(frame_editCurveSetting_cancel_listener) ;
        frame_editCurveSetting.invalidate();
        frame_editCurveSetting.show() ;
        frame_editCurveSetting.toFront() ;
    } // popEditCurveSettings

    /** Pop up a window to display message */   
    public void popMessageBox(String caption, String message) {
        msgBox_title = caption ;
        msgBox_message = message ;
        popMsgBox_flag = true ;
    }
    private void popMessageBox1(String caption, String message) {
        // open a frame
        frame_msgbox = new myWindow(caption) ;
        //frame_msgbox.setLocation(400,50) ;
        frame_msgbox.setSize(310,150) ;
        frame_msgbox.setCenter() ;
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
        //frame_msgbox.invalidate();
        frame_msgbox.show() ;
        frame_msgbox.toFront() ;
    } // popMessageBox

    /** Pop up a window to display message */    
    public void popClearLandmarks(String caption, String message) {
        if (image!=null){
            // open a frame
            frame_clearLandmarks = new myWindow(caption) ;
            //frame_clearLandmarks.setLocation(350,150) ;
            frame_clearLandmarks.setSize(350,120) ;
            frame_clearLandmarks.setCenter() ;
            frame_clearLandmarks.validate() ;
            frame_clearLandmarks.setVisible(true) ;
            

            ActionListener frame_msgbox_yes_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    myDB.elevationMarkCount = 0;
                    push2MarkLogBuffer(myDB.elevationMarkCount);
                    frame_clearLandmarks.dispose() ;
                    repaint();
                }
            } ;
            ActionListener frame_msgbox_no_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    frame_clearLandmarks.dispose() ;
                }
            } ;
                        
            // =============
            frame_clearLandmarks.setLayout(new GridBagLayout()) ;
            // Create a constrains object, and specify default values
            GridBagConstraints c = new GridBagConstraints() ;
            c.fill = GridBagConstraints.BOTH ; // component grows in both directions
            c.weightx = 1.0 ; c.weighty = 1.0 ;

            c.gridx = 0 ; c.gridy = 0; c.gridwidth = 2 ; c.gridheight = 1 ;
            c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
            iconQ.setBackground(new Color(200, 200, 200)) ;
            frame_clearLandmarks.add(iconQ,c) ;
            
            c.gridx = 2 ; c.gridy = 0; c.gridwidth = 4 ; c.gridheight = 1 ;
            Label myMsg = new Label(message) ;
            //myMsg.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
            //myMsg.setForeground(new Color(0,0,218)) ;
            frame_clearLandmarks.setBackground(new Color(200, 200, 200)) ;
            frame_clearLandmarks.add(myMsg,c) ;

            c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
            frame_clearLandmarks.add(new Label(" "),c) ;
            c.gridx = 1 ; c.gridy = 1; c.gridwidth = 1 ;
            frame_clearLandmarks.add(new Label(" "),c) ;
            c.gridx = 2 ; c.gridy = 1; c.gridwidth = 1 ;
            frame_clearLandmarks.add(new Label(" "),c) ;
            c.gridx = 3 ; c.gridy = 1; c.gridwidth = 1 ;
            frame_clearLandmarks.add(new Label(" "),c) ;
            c.gridx = 4 ; c.gridy = 1; c.gridwidth = 1 ;
            Button btn_ok = new Button(" Yes ") ;
            frame_clearLandmarks.add(btn_ok, c) ;
            btn_ok.addActionListener(frame_msgbox_yes_listener) ;
            c.gridx = 5 ; c.gridy = 1;
            Button btn_no = new Button(" No ") ;
            frame_clearLandmarks.add(btn_no, c) ;
            btn_no.addActionListener(frame_msgbox_no_listener) ;

            frame_clearLandmarks.invalidate();
            frame_clearLandmarks.show() ;
            frame_clearLandmarks.toFront() ;
            frame_clearLandmarks.setResizable(false) ;
        }
        else {
            popMessageBox("No Contour Map", NO_MAP_MSG);
        }
            
    } // popClearLandMark
    
    /** Pop up a window to display message */    
    public void popDeleteSegment(String caption, String message) {
        if (image!=null){
            // open a frame
            frame_deleteSegment = new myWindow(caption) ;
            //frame_deleteSegment.setLocation(350,150) ;
            frame_deleteSegment.setSize(350,120) ;
            frame_deleteSegment.setCenter() ;
            frame_deleteSegment.validate() ;
            frame_deleteSegment.setVisible(true) ;
            

            ActionListener frame_msgbox_yes_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    int i, j ;
                    for (i=0; i<hRoadDataCount; i++) {
                        // check if segment selected
                        if (myDB.hRoadData[i].isSelected()) {
                            // remove
                            myDB.hRoadData[i].delete();
                            // removed associated tangent points, 12/21/07, chenfu
                            for (j = 0; j< myDB.hAlignMarkCount; j++) {
                                if ((int)myDB.hAlignMarks[j].getParentIndex()==i) {
                                    // remove tangent index j
                                    removeTangentPair(j) ;
                                    j=myDB.hAlignMarkCount;
                                    break ;
                                }
                                
                            }   // for j
                            
                        }   // if selected
                    }   //    for i
                    frame_deleteSegment.dispose() ;
                    repaint();
                }
            } ;
            
            ActionListener frame_msgbox_no_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    frame_deleteSegment.dispose() ;
                }
            } ;

            frame_deleteSegment.setLayout(new GridBagLayout()) ;
            // Create a constrains object, and specify default values
            GridBagConstraints c = new GridBagConstraints() ;
            c.fill = GridBagConstraints.BOTH ; // component grows in both directions
            c.weightx = 1.0 ; c.weighty = 1.0 ;

            c.gridx = 0 ; c.gridy = 0; c.gridwidth = 2 ; c.gridheight = 1 ;
            c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
            frame_deleteSegment.add(iconQ,c) ;
            c.gridx = 2 ; c.gridy = 0; c.gridwidth = 4 ; c.gridheight = 1 ;
            Label myMsg = new Label(message) ;
            //myMsg.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
            //myMsg.setForeground(new Color(0,0,218)) ;
            frame_deleteSegment.setBackground(new Color(200, 200, 200)) ;
            frame_deleteSegment.add(myMsg,c) ;
            
            c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
            frame_deleteSegment.add(new Label(" "),c) ;
            c.gridx = 1 ; c.gridy = 1; c.gridwidth = 1 ;
            frame_deleteSegment.add(new Label(" "),c) ;
            c.gridx = 2 ; c.gridy = 1; c.gridwidth = 1 ;
            frame_deleteSegment.add(new Label(" "),c) ;
            c.gridx = 3 ; c.gridy = 1; c.gridwidth = 1 ;
            frame_deleteSegment.add(new Label(" "),c) ;
            c.gridx = 4 ; c.gridy = 1; c.gridwidth = 1 ;
            Button btn_ok = new Button(" Yes ") ;
            frame_deleteSegment.add(btn_ok, c) ;
            btn_ok.addActionListener(frame_msgbox_yes_listener) ;
            c.gridx = 5 ; c.gridy = 1;
            Button btn_no = new Button(" No ") ;
            frame_deleteSegment.add(btn_no, c) ;
            btn_no.addActionListener(frame_msgbox_no_listener) ;

            frame_deleteSegment.invalidate();
            frame_deleteSegment.show() ;
            frame_deleteSegment.toFront() ;
            frame_deleteSegment.setResizable(false) ;
        }
        else {
            popMessageBox("No Contour Map", NO_MAP_MSG);
        }
            
    } // popDeleteSegments

    /** Pop up a window to display message */    
    public void popDeleteTangent(String caption, String message) {
       
        // open a frame
        frame_deleteTangent = new myWindow(caption) ;
        //frame_deleteTangent.setLocation(300,100) ;
        frame_deleteTangent.setSize(350,120) ;
        frame_deleteTangent.setCenter() ;
        frame_deleteTangent.validate() ;
        frame_deleteTangent.setVisible(true) ;

        ActionListener frame_deleteTangent_yes_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                // delete tangent data pair
                removeTangentPair(dataSelIndex) ;
/*
                int j ;
                if (Math.IEEEremainder(dataSelIndex, 2) == 0) { 
                    // delete i & i+1
                    for (j = dataSelIndex + 2 ; j< myDB.hAlignMarkCount ; j++) {
                        myDB.hAlignMarks[j - 2] = myDB.hAlignMarks[j];
                    }
                } else {
                    // delete i & i-1 
                    for (j = dataSelIndex + 1; j< myDB.hAlignMarkCount; j++) {
                        myDB.hAlignMarks[j - 2] = myDB.hAlignMarks[j];
                    }
                }
                myDB.hAlignMarkCount -= 2 ;
*/
                frame_deleteTangent.dispose() ;
                repaint();
            }
        } ;
        ActionListener frame_deleteTangent_no_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_deleteTangent.dispose() ;
            }
        } ;

        frame_deleteTangent.setLayout(new GridBagLayout()) ;
        // Create a constrains object, and specify default values
        GridBagConstraints c = new GridBagConstraints() ;
        c.fill = GridBagConstraints.BOTH ; // component grows in both directions
        c.weightx = 1.0 ; c.weighty = 1.0 ;

        c.gridx = 0 ; c.gridy = 0; c.gridwidth = 2 ; c.gridheight = 1 ;
        c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
        frame_deleteTangent.add(iconQ, c) ;
        
        c.gridx = 2 ; c.gridy = 0; c.gridwidth = 4 ; c.gridheight = 1 ;
        Label myMsg = new Label(message) ;
        //myMsg.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
        //myMsg.setForeground(new Color(0,0,218)) ;
        frame_deleteTangent.setBackground(new Color(200, 200, 200)) ;
        frame_deleteTangent.add(myMsg,c) ;
        
        c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_deleteTangent.add(new Label(" "),c) ;
        c.gridx = 1 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_deleteTangent.add(new Label(" "),c) ;
        c.gridx = 2 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_deleteTangent.add(new Label(" "),c) ;
        c.gridx = 3 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_deleteTangent.add(new Label(" "),c) ;
        c.gridx = 4 ; c.gridy = 1; c.gridwidth = 1 ;
        Button btn_ok = new Button(" Yes ") ;
        frame_deleteTangent.add(btn_ok, c) ;
        btn_ok.addActionListener(frame_deleteTangent_yes_listener) ;
        c.gridx = 5 ; c.gridy = 1;
        Button btn_no = new Button(" No ") ;
        frame_deleteTangent.add(btn_no, c) ;
        btn_no.addActionListener(frame_deleteTangent_no_listener) ;

        frame_deleteTangent.invalidate();
        frame_deleteTangent.show() ;
        frame_deleteTangent.setResizable(false) ;

    } // popDeleteTangent
    
    // remove Tangent pair
    private void removeTangentPair(int dataSelIndex) {
        // delete tangent data pair
        int j ;
        if (Math.IEEEremainder(dataSelIndex, 2) == 0) { 
            // delete i & i+1
            for (j = dataSelIndex + 2 ; j< myDB.hAlignMarkCount ; j++) {
                myDB.hAlignMarks[j - 2] = myDB.hAlignMarks[j];
            }
        } else {
            // delete i & i-1 
            for (j = dataSelIndex + 1; j< myDB.hAlignMarkCount; j++) {
                myDB.hAlignMarks[j - 2] = myDB.hAlignMarks[j];
            }
        }
        myDB.hAlignMarkCount -= 2 ;        
    }
    
    /** Pop up a window to display & check using minimum curve radius */    
    public void popUpdateCurveRadius() {
        String str_Rv = new Float(calcMinRadius).toString();
        String spdStr = new Float(myDB.speedLimit).toString();
        String superEleStr = new Float(myDB.maxSuperelevation * 100f).toString();
        String unitStr = "";
        if (myDB.myUnit==1) {
            unitStr = " (MPH) ";
        } else if (myDB.myUnit==2) {
            unitStr = " (Km/h) ";
        } 
        String message = "Minimum Radius " + str_Rv + " required for maximum \nspeed " + 
            spdStr + unitStr + "and superelevation " + superEleStr + "%. \nUse minimum radius?" ;
        // open a frame
        updateRadius = new myWindow("Check Minimum Radius") ;
        updateRadius.setLocation(300,10) ;
        updateRadius.setSize(350,150) ;
        updateRadius.validate() ;
        updateRadius.setVisible(true) ;

        ActionListener updateRadius_yes_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                myDB.curveRadius = calcMinRadius;
                String str_Rv = new Float(calcMinRadius).toString();
                txtRadius.setText(str_Rv);
                
                updateRadius.dispose() ;
                repaint();
            }
        } ;
        ActionListener updateRadius_no_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                updateRadius.dispose() ;
            }
        } ;

        updateRadius.setLayout(new GridBagLayout()) ;
        // Create a constrains object, and specify default values
        GridBagConstraints c = new GridBagConstraints() ;
        c.fill = GridBagConstraints.BOTH ; // component grows in both directions
        c.weightx = 1.0 ; c.weighty = 1.0 ;

        c.gridx = 0 ; c.gridy = 0; c.gridwidth = 2 ; c.gridheight = 1 ;
        c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
        TextArea myMsg = new TextArea(message,4,40) ;
        //myMsg.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
        //myMsg.setForeground(new Color(0,0,218)) ;
        //updateRadius.setBackground(new Color(200, 200, 200)) ;
        updateRadius.add(myMsg,c) ;
        c.insets = new Insets(0,5,5,5) ;
        c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
        Button btn_ok = new Button(" Yes ") ;
        updateRadius.add(btn_ok, c) ;
        btn_ok.addActionListener(updateRadius_yes_listener) ;
        c.gridx = 1 ; c.gridy = 1;
        Button btn_no = new Button(" No ") ;
        updateRadius.add(btn_no, c) ;
        btn_no.addActionListener(updateRadius_no_listener) ;

        updateRadius.invalidate();
        updateRadius.show() ;
        updateRadius.toFront() ;
        updateRadius.setResizable(false) ;
            
    } // popUpdateCurveRadius
    
 /** Pop up a window to display message */    
    public void popClearAll(String caption, String message) {
        if (image!=null){
            // open a frame
            frame_msgboxClearAll = new myWindow(caption) ;
            //frame_msgboxClearAll.setLocation(400,200) ;
            frame_msgboxClearAll.setSize(300,120) ;
            frame_msgboxClearAll.setCenter() ;
            frame_msgboxClearAll.validate() ;
            frame_msgboxClearAll.setVisible(true) ;

            ActionListener frame_msgbox_yes_listener = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    /*
                     int i=0 ;
                    toolbarIndex=0 ;                    
                    // clear all DBs
                    for (i=0; i<myDB.hAlignMarkCount; i++) {
                        myDB.hAlignMarks[i].RESET();
                    }
                    for (i=0; i<myDB.elevationMarkCount; i++) {
                        myDB.elevationMarks[i].RESET();
                    }
                    for (i=0; i<hRoadDataCount;i++) {
                        myDB.hRoadData[i].RESET();
                    }
                    */
                    hRoadDataCount = 0;
                    myDB.hAlignMarkCount = 0;
                    myDB.elevationMarkCount = 0;
                    translate = new mPoint(0, 0);
                    push2SegLogBuffer(hRoadDataCount);
                    push2MarkLogBuffer(myDB.elevationMarkCount);
                    frame_msgboxClearAll.dispose() ;
                    repaint();
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
            frame_msgboxClearAll.add(iconQ, c) ;
            c.gridx = 2 ; c.gridy = 0; c.gridwidth = 4 ; c.gridheight = 1 ;
            Label myMsg = new Label(message) ;
            //myMsg.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
            //myMsg.setForeground(new Color(0,0,218)) ;
            frame_msgboxClearAll.setBackground(new Color(200, 200, 200)) ;
            frame_msgboxClearAll.add(myMsg,c) ;
            
            c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
            frame_msgboxClearAll.add(new Label(" "), c) ;
            c.gridx = 1 ; c.gridy = 1; c.gridwidth = 1 ;
            frame_msgboxClearAll.add(new Label(" "), c) ;
            c.gridx = 2 ; c.gridy = 1; c.gridwidth = 1 ;
            frame_msgboxClearAll.add(new Label(" "), c) ;
            c.gridx = 3 ; c.gridy = 1; c.gridwidth = 1 ;
            frame_msgboxClearAll.add(new Label(" "), c) ;
            c.gridx = 4 ; c.gridy = 1; c.gridwidth = 1 ;
            Button btn_ok = new Button(" OK ") ;
            frame_msgboxClearAll.add(btn_ok, c) ;
            btn_ok.addActionListener(frame_msgbox_yes_listener) ;
            c.gridx = 5 ; c.gridy = 1;
            Button btn_no = new Button(" No ") ;
            frame_msgboxClearAll.add(btn_no, c) ;
            btn_no.addActionListener(frame_msgbox_no_listener) ;

            frame_msgboxClearAll.invalidate();
            frame_msgboxClearAll.show() ;
            frame_msgboxClearAll.toFront() ;
            frame_msgboxClearAll.setResizable(false) ;
        }
        else {
            popMessageBox("No Contour Map", NO_MAP_MSG);
        }
            
    } // popClearAll
    
    public float calcDist2Line(mPointF p, mPointF p0, mPointF p1){
        float dx, dy ;
        dx = p1.X - p0.X ;
        dy = p1.Y - p0.Y ;
        double dist = Math.abs((dx*p.Y-dy*p.X+p0.X*p1.Y-p1.X*p0.Y)/Math.sqrt(dx*dx+dy*dy)) ;
        return (new Double(dist).floatValue()) ;
    }
    
    public void checkItemSelect(mPointF ptf ){
        // transform pt from screen pixel to actual unit
        int i ;
        float dist, cosine ;
        item_clicked_str = "" ;
        for (i = 0 ; i< hRoadDataCount; i++) {
            if (myDB.hRoadData[i].getRadius() < 0) {
                // line segment
                // end point 1
                dist = distanceOf(myDB.hRoadData[i].getPoint1(), ptf);
                //System.out.println("dist1=" + dist) ;
                if (dist <= endMarkSize * Math.sqrt(2) /draw_scale) {
                    myDB.hRoadData[i].selectItem();
                    item_clicked_str += i+"," ;
                } else {
                    // check end point 2
                    dist = distanceOf(myDB.hRoadData[i].getPoint2(), ptf);
                    //System.out.println("dist2=" + dist) ;
                    if (dist <= endMarkSize * Math.sqrt(2) /draw_scale) {
                        myDB.hRoadData[i].selectItem();
                        item_clicked_str += i+"," ;
                    } else {
                        // not selecting end points
                        cosine = getCosTheta(vector(myDB.hRoadData[i].getPoint2(), ptf), 
                            vector(myDB.hRoadData[i].getPoint1(), ptf));
                        //System.out.println("cosine=" + cosine) ;
                        // added 11/1/06
                        float dist2line = calcDist2Line(ptf, myDB.hRoadData[i].getPoint1(),
                            myDB.hRoadData[i].getPoint2()) ;
                        //System.out.println("dist 2 line=" + dist2line) ;
                        if ((cosine <= -0.99f) && 
                            (dist2line <= endMarkSize * Math.sqrt(2) /draw_scale)) { // ~180 degree, item highlighted
                            myDB.hRoadData[i].selectItem();
                            item_clicked_str += i+"," ;
                        }
                    }
                }
                //System.out.println("Check item selected = "+myDB.hRoadData[i].isSelected()) ;
            } else {
                // curve segment
                dist = distanceOf(myDB.hRoadData[i].getPoint1(), ptf);
                //System.out.println("dist="+new Float(dist).toString());
                if ((Math.abs(dist - myDB.hRoadData[i].getRadius() * myDB.imageScale)) <= myDB.hRoadData[i].getPenWidth() * Math.sqrt(2)/draw_scale) {
                    // item highlighted
                    myDB.hRoadData[i].selectItem();
                    item_clicked_str += i+"," ;
                }
            }
        }   // for
        //System.out.println("Check item selected") ;
    }   // checkItemSelect
    
    // vector p1->p2
    public mPointF vector(mPointF p1, mPointF p2 ) 
    {
        mPointF _vec ;
        _vec = new mPointF(p2.X - p1.X, p2.Y - p1.Y);
        return _vec;
    }
    public mPoint vector(mPoint p1, mPoint p2 ) 
    {
        mPoint _vec ;
        _vec = new mPoint(p2.X - p1.X, p2.Y - p1.Y);
        return _vec;
    }
    public float vectorDOT(mPointF v1, mPointF v2 ) {
        return v1.X*v2.X+v1.Y*v2.Y ;
    }
    // return vector angle in deg, 0deg at 3 oclock dir, + for CCW dir
    public double vectorAngle(mPoint p1, mPoint p2 ) 
    {
        double angle ;
        mPoint _vec ;
        _vec = new mPoint(p2.X - p1.X, p2.Y - p1.Y);
        angle = Math.atan2(-_vec.Y, _vec.X)*180.0/Math.PI ;    // degree
        return angle ; 
    }
    
    public int checkMarkLocation(mPointF ptf ) {
        // transform pt from screen pixel to actual unit
        int i, myIndex ;
        float dist, cosine ;
        boolean acceptElevation = false;
        myIndex=-1 ;
        float minDist=9999f ;
        for (i=0; i<hRoadDataCount; i++) {
            if (myDB.hRoadData[i].getRadius() < 0) { 
                // line segment
                dist = distanceOf(myDB.hRoadData[i].getPoint1(), ptf);
                if (dist <= endMarkSize * Math.sqrt(2)/draw_scale) { 
                    acceptElevation = true;
                    myDB.currentElevationMarker = myDB.hRoadData[i].getPoint1();
                } else {
                    dist = distanceOf(myDB.hRoadData[i].getPoint2(), ptf);
                    if (dist <= endMarkSize * Math.sqrt(2)/draw_scale) { 
                        acceptElevation = true;
                        myDB.currentElevationMarker = myDB.hRoadData[i].getPoint2();
                    } else {
                        // not selecting end points
                        mPointF vec1, vec2 ;
                        float L1, L2 ;
                        vec1 = vector(myDB.hRoadData[i].getPoint1(), myDB.hRoadData[i].getPoint2());
                        vec2 = vector(myDB.hRoadData[i].getPoint1(), ptf);
                        L1 = vectorLen(vec1);
                        L2 = vectorLen(vec2);
                        cosine = getCosTheta(vec1, vec2);
                        //System.out.println("cosine="+cosine) ;
                        //System.out.println("L1, L2"+L1 + ", "+ L2) ;
                        float dist2line = calcDist2Line(ptf, myDB.hRoadData[i].getPoint1(),
                            myDB.hRoadData[i].getPoint2()) ;
                        if (cosine >= 0.99f && L2 <= L1  && 
                            (dist2line <= endMarkSize * Math.sqrt(2) /draw_scale)) { // // 1 degree close to line segment
                            acceptElevation = true;
                            myDB.currentElevationMarker = new mPointF(myDB.hRoadData[i].getPoint1().X + vec1.X * L2 / L1, 
                                myDB.hRoadData[i].getPoint1().Y + vec1.Y * L2 / L1);
                        }
                    }
                }
            } else {
                // curve segment
                dist = distanceOf(myDB.hRoadData[i].getPoint1(), ptf);
                //System.out.println("curve dist="+dist) ;
                //System.out.println("diff="+Math.abs(dist - myDB.hRoadData[i].getRadius() * myDB.imageScale)) ;
                //System.out.println("limit="+myDB.hRoadData[i].getPenWidth() /draw_scale) ;
                if (Math.abs(dist - myDB.hRoadData[i].getRadius() * myDB.imageScale) <= myDB.hRoadData[i].getPenWidth() * Math.sqrt(2)/draw_scale) {
                    // click on curve
                    acceptElevation = true;
                    mPointF vec2 ;
                    float L1, L2 ;
                    vec2 = vector(myDB.hRoadData[i].getPoint1(), ptf);
                    L1 = myDB.hRoadData[i].getRadius() * myDB.imageScale;
                    L2 = vectorLen(vec2);
                    myDB.currentElevationMarker = new mPointF(myDB.hRoadData[i].getPoint1().X + vec2.X * L1 / L2, 
                                                    myDB.hRoadData[i].getPoint1().Y + vec2.Y * L1 / L2);
                }
            }
            if (acceptElevation == true) {
                if (minDist > dist) { //Then
                    minDist = dist;
                    myIndex = i;
                }
                acceptElevation = false;
                //break;
            }
        }
        //if (acceptElevation == false ) {
        //    return -1 ;  // marker does not land on any segment
        //} else {
        //    return i;
        //}
        return myIndex ;
    }   //checkMarkLocation
     
    // find nearest circle segment based on mouse click if exists
    public void getCurveMarkLocation(mPointF ptf ) {
        // transform pt from screen pixel to actual unit
        int i ;
        float dist, cosine, diff, min_diff ;
        boolean acceptElevation = false ;
        int min_diff_index = -1 ;
        min_diff = 9999f ;
        for (i=0; i<hRoadDataCount; i++) {
            if (myDB.hRoadData[i].getRadius() > 0) { 
                // curve segment
                dist = distanceOf(myDB.hRoadData[i].getPoint1(), ptf);
                diff = Math.abs(dist - myDB.hRoadData[i].getRadius() * myDB.imageScale) ;
                //System.out.println("i, diff="+i+", "+diff) ;
                if (diff <= myDB.hRoadData[i].getPenWidth() * Math.sqrt(2)/draw_scale) {
                    // click on curve
                    acceptElevation = true;
                    mPointF vec2 ;
                    float L1, L2 ;
                    vec2 = vector(myDB.hRoadData[i].getPoint1(), ptf);
                    L1 = myDB.hRoadData[i].getRadius() * myDB.imageScale;
                    L2 = vectorLen(vec2);
                    myDB.currentElevationMarker = new mPointF(myDB.hRoadData[i].getPoint1().X + vec2.X * L1 / L2, 
                                                    myDB.hRoadData[i].getPoint1().Y + vec2.Y * L1 / L2);
                    // found segment
                } else { // 10/17/06 added
                    if (diff < min_diff) {
                        min_diff = diff ;
                        min_diff_index = i ;
                    }
                }
            }   // curve
            if (acceptElevation == true) {
                break;
            }
        }
        if (acceptElevation == false ) {
            if (min_diff<=20f && min_diff_index>=0) {   // 10/17/06 added
                // take min dist diff less than 20 pixels if any exists
                sInfo.parentId = min_diff_index ;
                parentID.setText(CStr(min_diff_index));               
            } else {
                sInfo.parentId = -1 ;
                parentID.setText("None") ;  // marker does not land on any segment
            }
        } else {
            sInfo.parentId = i ;
            parentID.setText(CStr(i));
        }
    }   // getCurveMarkLocation
    
    // find nearest line segment based on mouse click, if any
    public void getLineMarkLocation(mPointF ptf ) {
        // transform pt from screen pixel to actual unit
        int i ;
        float dist, cosine ;
        boolean acceptElevation = false;
        for (i=0; i<hRoadDataCount; i++) {
            if (myDB.hRoadData[i].getRadius() < 0) { 
                // line segment
                dist = distanceOf(myDB.hRoadData[i].getPoint1(), ptf);
                if (dist <= endMarkSize * Math.sqrt(2)/draw_scale) { 
                    acceptElevation = true;
                    myDB.currentElevationMarker = myDB.hRoadData[i].getPoint1();
                } else {
                    dist = distanceOf(myDB.hRoadData[i].getPoint2(), ptf);
                    if (dist <= endMarkSize * Math.sqrt(2)/draw_scale) { 
                        acceptElevation = true;
                        myDB.currentElevationMarker = myDB.hRoadData[i].getPoint2();
                    } else {
                        // not selecting end points
                        mPointF vec1, vec2 ;
                        float L1, L2 ;
                        vec1 = vector(myDB.hRoadData[i].getPoint1(), myDB.hRoadData[i].getPoint2());
                        vec2 = vector(myDB.hRoadData[i].getPoint1(), ptf);
                        L1 = vectorLen(vec1);
                        L2 = vectorLen(vec2);
                        cosine = getCosTheta(vec1, vec2);
                        float dist2line = calcDist2Line(ptf, myDB.hRoadData[i].getPoint1(),
                            myDB.hRoadData[i].getPoint2()) ;
                        if (cosine >= 0.99f && L2 <= L1 &&
                            (dist2line <= endMarkSize * Math.sqrt(2) /draw_scale)) { // // 1 degree close to line segment
                            acceptElevation = true;
                            myDB.currentElevationMarker = new mPointF(myDB.hRoadData[i].getPoint1().X + vec1.X * L2 / L1, 
                                myDB.hRoadData[i].getPoint1().Y + vec1.Y * L2 / L1);
                        }   // if cosine
                    }   // if distance
                }   // if radius
            }   // line
            if (acceptElevation == true) {
                break;
            }
        }
        if (acceptElevation == false ) {
            sInfo.parentId = -1 ;
            parentID.setText("None") ;  // marker does not land on any segment
        } else {
            sInfo.parentId = i ;
            parentID.setText(CStr(i));
        }
    }   // getLineMarkLocation

    public int checkTangentLandmarks(mPointF ptf) {
        // transform pt from screen pixel to actual unit
        int i ;
        float dist ;
        boolean foundTangentPoint = false ;
        for (i=0;i<myDB.hAlignMarkCount;i++){
            // check tangent landmark database
            dist = distanceOf(myDB.hAlignMarks[i].getLocation(), ptf);
            if (dist <= endMarkSize * Math.sqrt(2) /draw_scale) {
                foundTangentPoint = true;
                myDB.currentElevationMarker = myDB.hAlignMarks[i].getLocation();
                break;
            }
        }
        if (foundTangentPoint == false ) {
            return -1 ;  // marker does not land on any tangent points
        } else {
            return myDB.hAlignMarks[i].getParentIndex();
        }
    }   // checkTangentMark
    
    public float getCosTheta(mPointF v1, mPointF v2) {
        double cos_theta, dot, v1_len, v2_len;
        dot = v1.X * v2.X + v1.Y * v2.Y;
        v1_len = Math.sqrt(v1.X*v1.X + v1.Y*v1.Y);
        v2_len = Math.sqrt(v2.X*v2.X + v2.Y*v2.Y);
        cos_theta = dot / (v1_len * v2_len);
        return new Double(cos_theta).floatValue();
    }
    public float vectorLen(mPointF vec) {
        double dist ;
        dist = Math.sqrt(vec.X*vec.X + vec.Y*vec.Y) ;
        return new Double(dist).floatValue();
    }
    
    public int vectorLen(mPoint vec) {
        double dist ;
        dist = Math.sqrt(vec.X*vec.X + vec.Y*vec.Y) ;
        return new Double(dist).intValue();
    }
    
    public int edit_undo() {
        if (toolbarIndex==4 || toolbarIndex==5) {
            int bufData ;
            bufData = popSegLogBuffer();
            if (bufData >= 0) {
                hRoadDataCount = bufData;
            }
        } else if (toolbarIndex==7) {
            int bufData ;
            bufData = popMarkLogBuffer();
            if (bufData >= 0 ) {
                myDB.elevationMarkCount = bufData;
            }
        }
        repaint();
        return (0);
    }
    public int edit_redo() {
        if (toolbarIndex==4 || toolbarIndex==5) {
            if (segLogIndex < segLogBuffer.length - 1) { 
                if (segLogBuffer[segLogIndex + 1] >= 0) {
                    // log info exists
                    segLogIndex += 1;
                    hRoadDataCount = segLogBuffer[segLogIndex];
                }
            }
        } else if (toolbarIndex==7) {
            if (markLogIndex < markLogBuffer.length - 1) { 
                if (markLogBuffer[markLogIndex + 1] > 0) { 
                    // log info exists
                    markLogIndex += 1;
                    myDB.elevationMarkCount = markLogBuffer[markLogIndex];
                }
            }
        }
        repaint();
        return(0);
    }
    public void edit_unselectAll() {
        int i ;
        for (i=0; i<hRoadDataCount;i++){
            myDB.hRoadData[i].unSelectItem();
        }
        repaint();
    }
    public void edit_selectAll() {
        int i ;
        for (i=0; i<hRoadDataCount;i++){
            myDB.hRoadData[i].setItemSelect(true); 
        }
        repaint();
    }
    // reset scales when loading new design
    public void view_RESET() {
        viewRoadOnly_flag = false ; // 11/22/06 added
        translate.X = 0;
        translate.Y = 0;
        scaledxlate.X = 0;
        scaledxlate.Y = 0;
        draw_scale = 1f;
        sb.setStatusBarText(3, new Float(Math.round(draw_scale*10f)/10f).toString()) ;
        repaint();
    }
    
    public void mouseEntered(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public void mouseExited(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public void mousePressed(java.awt.event.MouseEvent mouseEvent) {
        int x = mouseEvent.getX();
        int y = mouseEvent.getY();
        mouseDown(x,y);
    }
    
    public void mouseReleased(java.awt.event.MouseEvent mouseEvent) {
        int x = mouseEvent.getX();
        int y = mouseEvent.getY();
        if ((mouseEvent.getModifiers() & InputEvent.BUTTON1_MASK)
            == InputEvent.BUTTON1_MASK) {
            mouseLeftUp(x,y);    
        }
    }
    
    public void mouseDragged(java.awt.event.MouseEvent mouseEvent) {
        int x = mouseEvent.getX();
        int y = mouseEvent.getY();
        if ((mouseEvent.getModifiers() & InputEvent.BUTTON1_MASK)
            == InputEvent.BUTTON1_MASK) {
            mouseLeftDrag(x,y);
        }
    }
    
    public void mouseMoved(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public int checkhAlignTangent(mPointF ptf) {
        // transform pt from screen pixel to actual unit
        int i ;
        float dist ;
        boolean foundTangentPoint=false ;
        for (i=0;i<myDB.hAlignMarkCount;i++) {
            // check tangent landmark database
            dist = distanceOf(myDB.hAlignMarks[i].getLocation(), ptf);
            if (dist <= endMarkSize * Math.sqrt(2.0)/draw_scale) { 
                foundTangentPoint = true;
                break;
            }
        }
        if (foundTangentPoint == false) {
            return -1;   // marker does not land on any tangent points
        } else {
            return i ;  
        }
    }   // checkhAlignTangent
    
    public int checkElevationLandmarks(mPointF ptf) {
            // transform pt from screen pixel to actual unit
            int i ;
            float dist ;
            boolean foundLandmark=false ;
            for (i=0; i<myDB.elevationMarkCount;i++) {
                // check tangent landmark database
                dist = distanceOf(myDB.elevationMarks[i].getLocation(), ptf);
                if (dist <= myDB.elevationMarkerSize * Math.sqrt(2.0)/draw_scale) { 
                    foundLandmark = true;
                    myDB.currentElevationMarker = myDB.elevationMarks[i].getLocation();
                    break;
                }
            }
            if (foundLandmark == false ) {
                return -1 ;  // marker does not land on any elevation landmark points
            } else {
                return i;
            }
    } //  checkElevationLandmarks
    
    public void tool_curvehAlignMarks() {
        int i, selectedCurve=-1 ;
        int[] selectedLines = new int[2];
        int selLineIdx = 0;
        int selCurveIdx = 0;
        for (i=0; i<hRoadDataCount; i++) {
            if (myDB.hRoadData[i].isSelected()) {
                // item selected
                if (myDB.hRoadData[i].getRadius() < 0) { 
                    // line
                    selectedLines[selLineIdx] = i;
                    selLineIdx += 1;
                } else {
                    // curve
                    selectedCurve = i;
                    selCurveIdx += 1;
                }
            }
        } // for i

        // check if 2 lines & 1 curve are selected
        if ((selLineIdx == 2) && (selCurveIdx == 1)) { 
            mPointF pt1, pt2 ;  // 2 tangent points on the curve
            calculateCurveCenter(selectedLines[0], selectedLines[1], selectedCurve);

            pt1 = calculateTangentPoint(selectedLines[0], selectedCurve); 
            myDB.hRoadData[selectedCurve].saveTangentAngle((byte)1, pt1);
            pt2 = calculateTangentPoint(selectedLines[1], selectedCurve); 
            myDB.hRoadData[selectedCurve].saveTangentAngle((byte)2, pt2); 

            // save tangent points
            myDB.hAlignMarks[myDB.hAlignMarkCount] = new MarkerDB();
            myDB.hAlignMarks[myDB.hAlignMarkCount].setMarker(pt1, 0f, (byte)selectedCurve);
            myDB.hAlignMarkCount += 1;
            myDB.hAlignMarks[myDB.hAlignMarkCount] = new MarkerDB();
            myDB.hAlignMarks[myDB.hAlignMarkCount].setMarker(pt2, 0f, (byte)selectedCurve);
            myDB.hAlignMarkCount += 1;
            // unselect segments
            myDB.hRoadData[selectedLines[0]].unSelectItem();
            myDB.hRoadData[selectedLines[1]].unSelectItem();
            myDB.hRoadData[selectedCurve].unSelectItem();

        } else {
            popMessageBox("Horizontal Alignment","Please select 2 linear and 1 curve segments first!");
        }
        repaint();
        //PictureBox1.Invalidate()
    }   // tool_curvehAlignMarks
    
    public void tool_property() {
        int i, selectedItems=0 ;
        String propertyStr = "" ;
        String unitStr = "" ;
        if (myDB.myUnit==1 ) {
            unitStr = " (ft) " ;
        } else if (myDB.myUnit==2 ) {
            unitStr = " (m) " ;
        }
        for (i=0; i<hRoadDataCount; i++) {
            if (myDB.hRoadData[i].isSelected()) {
                // item selected
                if (myDB.hRoadData[i].getRadius() < 0) { 
                    // line
                    float len = distanceOf(myDB.hRoadData[i].getPoint1(), myDB.hRoadData[i].getPoint2()) ;
                    propertyStr += "("+CStr(i+1)+") Line, Length=" +
                        CStr(len/myDB.imageScale) + unitStr + "\n" ;
                } else {
                    // curve
                    propertyStr += "("+CStr(i+1)+") Circle, Radius=" + 
                        CStr(myDB.hRoadData[i].getRadius()) + unitStr + "\n" ;
                }   //System.out.println("imageScale = "+myDB.imageScale) ;
                selectedItems++ ;
            }
        } // for i

        // check if 2 lines & 1 curve are selected
        if (selectedItems>0) { 
            popMessageBox("Properties", propertyStr);
        } else {
            popMessageBox("Properties","Please select a line or curve segment first!");
        }
        repaint();
        
    }   // tool_property

    // check station segment type continuity, 11/16/06
    public void tool_checkStation() {
        
        String status = "" ;
        status = checkLandmarks() ;
        /*
        int i;
        int last_type=myDB.elevationMarks[0].getSegmentType() ;
        int cur_type=0 ;
        for (i=1; i<myDB.elevationMarkCount; i++) {
            cur_type = myDB.elevationMarks[i].getSegmentType() ;
            if (cur_type != 3 && last_type != cur_type && last_type != 3) {
                // not tangent, different type and not tangent previously
                if (status.length()>0) {
                    status = status + ", " + i ;
                } else {
                    status += i ;
                }
            }
            last_type = cur_type ;
            
        } // for i
        */
        if (status.length()==0) { 
            popMessageBox("Check Station Data", "Station data OK!");
        } else {
            popMessageBox("Check Station Data", "Station data error at station "+status+".");
        }

        repaint();
        
    }   // tool_checkStation
    
    private String checkLandmarks() {
        int i;
        String status = "" ;
        int last_type=myDB.elevationMarks[0].getSegmentType() ;
        int cur_type=0 ;
        for (i=1; i<myDB.elevationMarkCount; i++) {
            cur_type = myDB.elevationMarks[i].getSegmentType() ;
            if (cur_type != 3 && last_type != cur_type && last_type != 3) {
                // not tangent, different type and not tangent previously
                if (status.length()>0) {
                    status = status + ", " + i ;
                } else {
                    status += i ;
                }
            }
            last_type = cur_type ;
        } // for i
        return status ;
        
    }   // checkLandmarks
         
    public void calculateCurveCenter(int lineIndex1, int lineIndex2, int curveIndex) {
        mPointF p1, p2, p3, p4, pc ;
        float a1, a2, b1, b2, c1, c2, rad ;
        float L1, L2, den ;
        mPointF[] Center = new mPointF[4] ;
        p1 = myDB.hRoadData[lineIndex1].getPoint1();
        p2 = myDB.hRoadData[lineIndex1].getPoint2();
        p3 = myDB.hRoadData[lineIndex2].getPoint1();
        p4 = myDB.hRoadData[lineIndex2].getPoint2();
        pc = myDB.hRoadData[curveIndex].getPoint1();
        rad = myDB.hRoadData[curveIndex].getRadius() * myDB.imageScale;
        a1 = p2.Y - p1.Y;
        b1 = p1.X - p2.X;
        a2 = p4.Y - p3.Y;
        b2 = p3.X - p4.X;
        L1 = distanceOf(p1, p2);
        L2 = distanceOf(p3, p4);
        int i ;
        for (i=0; i<4; i++) {
            Center[i] = new mPointF();
        }
        den = (a1 * b2 - a2 * b1);
        c1 = p2.X * p1.Y - p1.X * p2.Y + L1 * rad;
        c2 = p4.X * p3.Y - p3.X * p4.Y + L2 * rad;
        // case 1
        Center[0].X = (c2 * b1 - c1 * b2) / den;
        Center[0].Y = -(c2 * a1 - c1 * a2) / den;

        c1 = p2.X * p1.Y - p1.X * p2.Y + L1 * rad;
        c2 = p4.X * p3.Y - p3.X * p4.Y - L2 * rad;
        // case 2
        Center[1].X = (c2 * b1 - c1 * b2) / den;
        Center[1].Y = -(c2 * a1 - c1 * a2) / den;

        c1 = p2.X * p1.Y - p1.X * p2.Y - L1 * rad;
        c2 = p4.X * p3.Y - p3.X * p4.Y + L2 * rad;
        // case 3
        Center[2].X = (c2 * b1 - c1 * b2) / den;
        Center[2].Y = -(c2 * a1 - c1 * a2) / den;

        c1 = p2.X * p1.Y - p1.X * p2.Y - L1 * rad;
        c2 = p4.X * p3.Y - p3.X * p4.Y - L2 * rad;
        // case 4
        Center[3].X = (c2 * b1 - c1 * b2) / den;
        Center[3].Y = -(c2 * a1 - c1 * a2) / den;

        int index = 0;
        float min_dist = 99999f;
        float dist ;
        // find the closest one
        for (i=0; i<4; i++) {
            dist = distanceOf(pc, Center[i]);
            if (dist < min_dist) {
                min_dist = dist;
                index = i;
            }
            //debugWindow.Text &= i & " (Xc, Yc)=" & Center(i).X & ", " & Center(i).Y & " dist=" & dist & vbCrLf
        }
        //debugWindow.Text &= i & " (PXo, PYo)=" & pc.X & ", " & pc.Y & vbCrLf

        if (min_dist < 40 && min_dist >= 0) {    // change from 20 to 40 , 2/12/2007
            myDB.hRoadData[curveIndex].updateCurveCenter(Center[index]);
        } else {
            popMessageBox("calculateCurveCenter", "Please move curve closer to line segments!");
        }

    } //calculateCurveCenter
    
    // calc & return tangent point of a line & a circle
    public mPointF calculateTangentPoint(int lineIndex, int curveIndex) {
        float dx, dy, xt, yt, xy, len2 ;
        mPointF p1, p2, pc ;

        p1 = myDB.hRoadData[lineIndex].getPoint1();
        p2 = myDB.hRoadData[lineIndex].getPoint2();
        pc = myDB.hRoadData[curveIndex].getPoint1();
        xy = p2.Y * p1.X - p1.Y * p2.X;
        dx = p2.X - p1.X;
        dy = p2.Y - p1.Y; 
        len2 = (dx*dx + dy*dy);
        xt = (dy * xy + (pc.Y * dy + pc.X * dx) * dx) / len2;
        yt = (-dx * xy + (pc.Y * dy + pc.X * dx) * dy) / len2;
        return new mPointF(xt, yt);
    }   // calculateTangentPoint

    // Show Vertical Curve Design Panel
    public void popVerticalAlign(String _title) {
        if (frmVerticalAlign.isShowing()==false) 
        {
            frmVerticalAlign = new myFrame(_title) ;//"Simulation Statistics") ;
            frmVerticalAlign.setSize(620, 560) ;
            //frmVerticalAlign.setLocation(150,20) ;
            frmVerticalAlign.setCenter() ;
            frmVerticalAlign.validate() ;
            frmVerticalAlign.setVisible(true) ;
            frmVerticalAlign.setResizable(false);
            

            // file menu
            MenuBar menu_bar = new MenuBar() ;
            Menu menu_file = new Menu("File") ;
            MenuItem file_open = new MenuItem("Load Vertical Curve") ;
            MenuItem file_save = new MenuItem("Save Vertical Curve") ;
            MenuItem separator = new MenuItem("-") ;
       //     MenuItem file_savereport = new MenuItem("Save Report") ;
       //     MenuItem file_pagesetup = new MenuItem("Page Setup") ;
            MenuItem file_print = new MenuItem("Print") ;
            MenuItem file_close = new MenuItem("Close") ;
            // file menu items
            menu_file.add(file_open) ;   // add menu items
            menu_file.add(file_save) ;   // add menu items
            menu_file.addSeparator() ;
       //     menu_file.add(file_savereport) ;   // add menu items
       //     menu_file.addSeparator() ;
       //     menu_file.add(file_pagesetup) ;   // add menu items
            menu_file.add(file_print) ;   // add menu items
            menu_file.addSeparator() ;
            menu_file.add(file_close) ;   // add menu items

            // edit menu
            Menu menu_edit = new Menu("Edit") ;
            MenuItem edit_undo = new MenuItem("Undo") ;
            MenuItem edit_redo = new MenuItem("Redo") ;
            MenuItem edit_cleardesign = new MenuItem("Clear Design") ;
            MenuItem edit_clearcurve = new MenuItem("Clear Curves") ;
            menu_edit.add(edit_undo) ;
            menu_edit.add(edit_redo) ;
            menu_edit.addSeparator();
            menu_edit.add(edit_cleardesign) ;
            menu_edit.add(edit_clearcurve) ;

            // view menu
            Menu menu_view = new Menu("View") ;
            MenuItem view_elevation = new MenuItem("Elevation Profile") ;
            MenuItem view_fillcut = new MenuItem("Fill-Cut Profile") ;
            MenuItem view_massdiagram = new MenuItem("Mass Diagram") ;
            MenuItem view_stations = new MenuItem("Station Data") ;
            MenuItem view_report = new MenuItem("Design Report") ;
            MenuItem view_animation = new MenuItem("3D Animation") ;
            
            menu_view.add(view_elevation) ;
            menu_view.add(view_fillcut) ;
            menu_view.add(view_massdiagram) ;
            menu_view.addSeparator();
            menu_view.add(view_stations) ;
            menu_view.add(view_report) ;
            menu_view.add(view_animation) ;
            
            // tool menu
            Menu menu_tool = new Menu("Tool") ;
            MenuItem tool_gradeON = new MenuItem("Grade Construction ON") ;
            MenuItem tool_gradeOFF = new MenuItem("Grade Construction OFF") ;
            MenuItem tool_vAlign = new MenuItem("Generate Vertical Curves") ;
            MenuItem tool_curveEdit = new MenuItem("Edit Curve Length") ;
            menu_tool.add(tool_gradeON) ;
            menu_tool.add(tool_gradeOFF) ;
            menu_tool.addSeparator() ;
            menu_tool.add(tool_vAlign) ;
            menu_tool.add(tool_curveEdit) ; // 10/9/06 edit vertical curve length
            
            // help menu
            Menu menu_help = new Menu("Help") ;
            MenuItem help_manual = new MenuItem("User's Manual PDF") ;
            MenuItem help_cortona = new MenuItem("Cortona VRML Client") ;
            MenuItem help_about = new MenuItem("About ROAD") ;
            //MenuItem help_aboutJavaHelp = new MenuItem("About JavaHelp") ;
            MenuItem help_web_contents = new MenuItem("Web Contents") ; 
            //MenuItem help_javahelp = new MenuItem("User's Guide") ;

            menu_help.add(help_web_contents) ;
            //menu_help.add(help_javahelp) ;
            
            menu_help.add(help_manual) ;
            menu_help.add(separator) ;
            menu_help.add(help_about) ;
            //menu_help.add(help_aboutJavaHelp) ;
            menu_help.add(separator) ;
            menu_help.add(help_cortona) ;
            
            // ===========================================
            menu_bar.add(menu_file) ;     // add menu
            menu_bar.add(menu_edit) ;     // add menu
            menu_bar.add(menu_view) ;     // add menu
            menu_bar.add(menu_tool) ;     // add menu
            menu_bar.add(menu_help) ;     // add menu
            frmVerticalAlign.setMenuBar(menu_bar) ;

            toolbarV tbv = new toolbarV();
            statusbarV sbv = new statusbarV() ;
            Panel cm = new Panel();
            Panel ccv = new Panel();
            frmVerticalAlign.setLayout(new BorderLayout(0,0));

            //Scrollbar ss = new Scrollbar(Scrollbar.HORIZONTAL);
            ccv.setLayout(new BorderLayout(0,0));
            
            vDesign = new vDrawArea(tbv, sbv); 
            vDesign.myApplet = this.myApplet ;
            vDesign.myDB = myDB ;
            vDesign.hRoadDataCount = hRoadDataCount; 
            vDesign.init(); // initialization
            
            ccv.add("Center",vDesign); 

            cm.setBackground(Color.black);
            cm.setLayout(new BorderLayout(1,1));
            cm.add("North",tbv);
            cm.add("Center",ccv);
            cm.add("South",sbv);

            frmVerticalAlign.add("West", new border(2, Color.black));
            frmVerticalAlign.add("East", new border(2, Color.black));
            frmVerticalAlign.add("North", new border(2, Color.black));
            frmVerticalAlign.add("South", new border(2, Color.black));
            frmVerticalAlign.add("Center",cm);
            frmVerticalAlign.invalidate() ;
            //frmVerticalAlign.show() ;
            /*
             help_javahelp.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        try
                        {
                            new HelpDoc() ;
                        }
                        catch (Exception e){
                                //do nothing
                            popMessageBox("Help - User's Guide", "Error:"+e.toString()) ;
                        } // try
                    } // actionPerformed
                } // ActionListener
            ) ; // help_javahelp
            */
            help_web_contents.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        try
                        {
                            AppletContext a = myApplet.getAppletContext();
                            URL u = new URL(SHARED.CONTENTS_PATH);  
                            a.showDocument(u,"_blank");
                            //_blank to open page in new window		
                        }
                        catch (Exception e){
                                //do nothing
                            popMessageBox("Help - Web Content", "Error:"+e.toString()) ;
                        } // try
                    } // actionPerformed
                } // ActionListener
            ) ; // help_web_contents

            help_manual.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                         try
                        {
                            AppletContext a = myApplet.getAppletContext(); 
                            URL u = new URL(myDB.MANUAL_PATH); 
                            a.showDocument(u,"_blank");
                            //_blank to open page in new window		
                        }
                        catch (Exception e){
                                //do nothing
                            sb.setStatusBarText(1, "Error: Manual file "+e.toString()) ;
                        }   // try
                    }   // actionPerformed
                }   // ActionListener
            ) ; // help_manual
            
            help_cortona.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) { 
                         try
                        {
                            AppletContext a = myApplet.getAppletContext(); 
                            URL u = new URL("http://www.parallelgraphics.com/developer/products/cortona/help/"); 
                            a.showDocument(u,"_blank");
                            //_blank to open page in new window		
                        }
                        catch (Exception e){
                            //do nothing
                            sb.setStatusBarText(1, "Error: Cortona help file "+e.toString()) ;
                        }   // try
                    }   // actionPerformed
                }   // ActionListener
            ) ; // help_cortona
            
            help_about.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        popAbout();
                   }    // actionPerformed
                }  // ActionListener
            ) ; // help_about
            /*
            help_aboutJavaHelp.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        popMessageBox("JavaHelp", 
                        "Note: You need to have JavaHelp package\n" +  
                        "(jh.jar, jhall.jar, jhbasic.jar, jsearch.jar)\n" +
                        "installed in your ..\\Java\\jre\\lib\\ext\\ directory\n" + 
                        "in order to view the Users' Guide. JavaHelp is\n" + 
                        "available at http://java.sun.com/products/javahelp/.") ;
                        
                    } // actionPerformed
                } // ActionListener
            ) ; // help_aboutJavaHelp
            */
            file_open.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.file_open(); 
                            vDesign.setStatusBarText(0, "Load vertical curve") ;
                        }   // actionPerformed
                    }   // ActionListener
             ) ;    // file open
             file_save.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.file_save(); 
                            vDesign.setStatusBarText(0, "Save vertical curve") ;
                        }   // actionPerformed
                    }   // ActionListener
             ) ;    // file save
             /*
             file_savereport.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.file_saveReport();  
                            vDesign.setStatusBarText(0, "Save Report") ;
                        } // actionPerformed
                    } // ActionListener
             ) ; // file save report
              **/
/*             file_pagesetup.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            
                            vd_pu = new PrintUtilities(vDesign) ;
                            vd_pu.printPageSetup();
                        } // actionPerformed
                    } // ActionListener
             ) ; // file Print Page Setup
 */
             file_print.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            //hDesign.print();
                            //PrintUtilities.printComponent(vDesign);
                            //PrintUtilities pu = new PrintUtilities(vDesign) ;
                            vd_pu = new PrintUtilities(vDesign) ;
                            vd_pu.print();
                        } // actionPerformed
                    } // ActionListener
             ) ; // file Print 
             file_close.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            
                            popSaveVDesignB4Close();
                        } // actionPerformed
                    } // ActionListener
             ) ; // file Close

             edit_undo.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.edit_undo();
                        } // actionPerformed
                    } // ActionListener
             ) ; // edit_undo
             edit_redo.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.edit_redo();
                        } // actionPerformed
                    } // ActionListener
             ) ; // edit_redo
             edit_cleardesign.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.popClearAllDesign("Edit - Clear Design","Are you sure to clear vertical curve design?");
                        } // actionPerformed
                    } // ActionListener
             ) ; // edit_clearLandmarks
             edit_clearcurve.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.popClearVCurves("Edit - Clear Curves","Are you sure to clear vertical curves?");
                        } // actionPerformed
                    } // ActionListener
             ) ; // edit_clearCurves
             
             view_elevation.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.viewElevation();
                            vDesign.sb.setStatusBarText(0, "View elevation profile") ; //Status:
                        } // actionPerformed
                    } // ActionListener
             ) ; // view elevation profile
             view_fillcut.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.viewFillCut();
                            vDesign.sb.setStatusBarText(0, "View fill-cut profile") ; //Status:
                        } // actionPerformed
                    } // ActionListener
             ) ; // view fill cut profile
             view_massdiagram.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.view_MassDiagram(); 
                            vDesign.sb.setStatusBarText(0, "View mass diagram") ; //Status:
                        } // actionPerformed
                    } // ActionListener
             ) ; // view elevation profile
             view_stations.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.popStationData();
                            vDesign.sb.setStatusBarText(0, "View station data") ; //Status:
                        } // actionPerformed
                    } // ActionListener
             ) ; // view stations
             view_report.addActionListener( 
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.popReport();
                            vDesign.sb.setStatusBarText(0, "Generate report") ; //Status: 

                        } // actionPerformed
                    } // ActionListener
             ) ; // view report
             view_animation.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.popAnimation3D();
                            vDesign.sb.setStatusBarText(0, "3D animation") ; //Status:
                        } // actionPerformed
                    } // ActionListener
             ) ; // view 3D animation

             tool_gradeON.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.tool_gradeON();
                        } // actionPerformed
                    } // ActionListener
             ) ; // tool grade ON
             tool_gradeOFF.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.tool_gradeOFF();
                        } // actionPerformed
                    } // ActionListener
             ) ; // tool Grade OFF
             tool_vAlign.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.vertAlign();
                        } // actionPerformed
                    } // ActionListener
             ) ; // tool vertical cuve alignment
             tool_curveEdit.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            vDesign.popVertCurveLenEdit();
                        } // actionPerformed
                    } // ActionListener
             ) ; // tool vertical cuve length modification
             
            //=============================
            frmVerticalAlign.invalidate() ;
            frmVerticalAlign.setVisible(true) ;
            frmVerticalAlign.show() ;
        }
        else {  // frmVerticalAlign already displayed
            frmVerticalAlign.show() ;
        }
        frmVerticalAlign.toFront();

    }   // popVerticalAlign
    
    public void popAbout(){
        if (frmAbout.isShowing()==false) {
            frmAbout = new myWindow("About ROAD") ;
            frmAbout.setSize(300, 140) ;
            frmAbout.setResizable(false);
            //frmAbout.setLocation(100,100) ;
            frmAbout.setCenter() ;

            frmAbout.setLayout(new BorderLayout(0,0));
            Panel textboxp = new Panel();
            textboxp.setLayout(new BorderLayout(0,0));
            textboxp.add("Center",new aboutTextbox()); 

            Panel about = new Panel();
            about.setBackground(Color.white);
            about.setLayout(new BorderLayout(1,1));
            about.add("Center",textboxp);
            frmAbout.add(about);
            frmAbout.invalidate() ;
            frmAbout.setVisible(true) ;
            frmAbout.show();
        }
        else {
            frmAbout.show();
        }
       
    }
    
    // view horizontal road design only, w/o construction lines/circles
    public void viewRoadOnly() {
        viewRoadOnly_flag = true ;
        repaint() ;
    }
    public void viewRoadDesign() {
        viewRoadOnly_flag = false ;
        repaint() ;
    }
    
    public void popLandmarkData(){
        frmLandmarkTable = new JFrame("View Landmark Data") ;
        frmLandmarkTable.setSize(350, 200);
        //Make sure we have nice window decorations.
  //      frmLandmarkTable.setDefaultLookAndFeelDecorated(true);
        frmLandmarkTable.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // file menu
        MenuBar menu_bar = new MenuBar() ;
        Menu menu_file = new Menu("File") ;
        MenuItem file_saveStation = new MenuItem("Save Data to File") ;
        MenuItem file_printreport = new MenuItem("Print") ;
        MenuItem separator = new MenuItem("-") ;
        MenuItem file_close = new MenuItem("Close") ;
        menu_file.add(file_saveStation) ;   // add menu items
        menu_file.add(file_printreport) ;   // add menu items
        menu_file.add(separator) ;   // add menu items
        menu_file.add(file_close) ;   // add menu items
        
        Menu menu_data = new Menu("Data") ;
        MenuItem data_saveElevation = new MenuItem("Update Elevation") ;
        menu_data.add(data_saveElevation) ; // save elevation data

        menu_bar.add(menu_file) ;     // add menu
        menu_bar.add(menu_data) ;     // add menu
        frmLandmarkTable.setMenuBar(menu_bar) ;

         file_saveStation.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        landmark_saveData();  
                        sb.setStatusBarText(0, "Save Landmark Data") ;
                    } // actionPerformed
                } // ActionListener
         ) ; // file save report
         data_saveElevation.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        TableModel model = stationTable.getModel() ;
                        //System.out.println(" column ="+model.getColumnCount()) ;
                        //System.out.println(" rows="+model.getRowCount()) ;
                        
                        for (int i=0; i<myDB.elevationMarkCount; i++) {
                            String valStr = (String)model.getValueAt(i, 3) ;
                            //System.out.println(i+" data="+valStr) ;
                            myDB.elevationMarks[i].setElevation(new Float(valStr).floatValue()) ;
                        }
                        sb.setStatusBarText(0, "Update Elevation Data") ;
                    } // actionPerformed
                } // ActionListener
         ) ; //  save elevation    
         file_printreport.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        landmark_printData();  
                        sb.setStatusBarText(0, "Print Landmark Data") ;
                    } // actionPerformed
                } // ActionListener
         ) ; // file save report
         file_close.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        frmLandmarkTable.dispose();
                    } // actionPerformed
                } // ActionListener
         ) ; // file Close
             
        String[] headers = { "ID", "POS X", "POS Y", "Elevation", "Type" };
        int[] fieldSize = {6,10,10,12,8} ;
        landmarkPrintStr = PrintText.StrFormat(0,"ID", fieldSize[0]) +
                           PrintText.StrFormat(0,"POS X", fieldSize[1]) +
                           PrintText.StrFormat(0,"POS Y", fieldSize[2]) +
                           PrintText.StrFormat(0,"Elavation", fieldSize[3]) +
                           PrintText.StrFormat(0,"Type", fieldSize[4]) + "\n" ;
        String[][] data = new String[myDB.elevationMarkCount][headers.length];
        int i;
        float myScale = (float)myDB.ContourImageResolution / (float)myDB.ContourScale;
        for (i=0;i<myDB.elevationMarkCount; i++) {
            data[i][0] = CStr(i + 1) ;
            data[i][1] = CStr(Math.round(myDB.elevationMarks[i].getLocation().X/myScale*1000f)/1000f) ;
            data[i][2] = CStr(Math.round(myDB.elevationMarks[i].getLocation().Y/myScale*1000f)/1000f) ;
            data[i][3] = CStr(Math.round(myDB.elevationMarks[i].getElevation()*1000f)/1000f);
            switch (myDB.elevationMarks[i].getSegmentType()) {
                case 1:
                    data[i][4] = "Line";
                    break;
                case 2:
                    data[i][4] = "Curve";
                    break;
                case 3:
                    data[i][4] = "Tangent";
                    break;
                default:
                    data[i][4] = "None";
                    break;
            }   //End Select
            for (int j=0; j<5; j++){
                landmarkPrintStr += PrintText.StrFormat(0, data[i][j].toString(), fieldSize[j]) ;
            }
            landmarkPrintStr += "\n" ;
        }   // for i
        stationTable = new JTable(data, headers) {
        // override isCellEditable method, 11/13/06
           public boolean isCellEditable(int row, int column) {
               if (column == 3) {
                   return true ;
               } else {
                   return false ;
               }    
           }    // isCellEditable method
        } ;
        stationTable.setPreferredScrollableViewportSize(new Dimension(350, 200));
        stationTable.setColumnSelectionAllowed(true) ;
        
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(stationTable);

        //Add the scroll pane to this panel.
        frmLandmarkTable.add(scrollPane);
        //Get the column model.
        TableColumnModel colModel = stationTable.getColumnModel();
        //Get the column at index pColumn, and set its preferred width.
        colModel.getColumn(0).setPreferredWidth(24);   
        
        //Display the window.
        Dimension screen = getToolkit().getDefaultToolkit().getScreenSize();
        double top = 0.5*(screen.getWidth()-frmLandmarkTable.getWidth());
        double left = 0.5*(screen.getHeight()-frmLandmarkTable.getHeight());
        int x = new Double(top).intValue();
        int y = new Double(left).intValue();
        frmLandmarkTable.setLocation(x, y);

        frmLandmarkTable.pack();
        frmLandmarkTable.setVisible(true);
        frmLandmarkTable.show();
       
    }   // popLandmarkData ;
    
    public void popTangentData(){
        frmTangentTable = new JFrame("View PC, PT Data") ;
        frmTangentTable.setSize(350, 200);
        //Make sure we have nice window decorations.
  //      frmTangentTable.setDefaultLookAndFeelDecorated(true);
        frmTangentTable.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // file menu
        MenuBar menu_bar = new MenuBar() ;
        Menu menu_file = new Menu("File") ;
        MenuItem file_saveTangent = new MenuItem("Save Data") ;
        MenuItem file_printreport = new MenuItem("Print") ;
        MenuItem separator = new MenuItem("-") ;
        MenuItem file_close = new MenuItem("Close") ;
        menu_file.add(file_saveTangent) ;   // add menu items
        menu_file.add(file_printreport) ;   // add menu items
        menu_file.add(separator) ;   // add menu items
        menu_file.add(file_close) ;   // add menu items

        menu_bar.add(menu_file) ;     // add menu
        frmTangentTable.setMenuBar(menu_bar) ;

         file_saveTangent.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        tangent_saveData();  
                        sb.setStatusBarText(0, "Save PC, PT Data") ;
                    } // actionPerformed
                } // ActionListener
         ) ; // file save report
         file_printreport.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        tangent_printData();  
                        sb.setStatusBarText(0, "Print PC, PT Data") ;
                    } // actionPerformed
                } // ActionListener
         ) ; // file save report
         file_close.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        frmTangentTable.dispose();
                    } // actionPerformed
                } // ActionListener
         ) ; // file Close
             
        String[] headers = { "ID", "POS X", "POS Y", "Curve ID", "Type" };
        int[] fieldSize = {6,10,10,12,8} ;
        tangentPrintStr = PrintText.StrFormat(0,"ID", fieldSize[0]) +
                           PrintText.StrFormat(0,"POS X", fieldSize[1]) +
                           PrintText.StrFormat(0,"POS Y", fieldSize[2]) +
                           PrintText.StrFormat(0,"Curve ID", fieldSize[3]) +
                           PrintText.StrFormat(0,"Type", fieldSize[4]) + "\n" ;
        String[][] data = new String[myDB.hAlignMarkCount][headers.length];
        int i;
        float myScale = (float)myDB.ContourImageResolution / (float)myDB.ContourScale;
        for (i=0;i<myDB.hAlignMarkCount; i++) {
            data[i][0] = CStr(i + 1) ;
            data[i][1] = CStr(Math.round(myDB.hAlignMarks[i].getLocation().X/myScale*1000f)/1000f) ;
            data[i][2] = CStr(Math.round(myDB.hAlignMarks[i].getLocation().Y/myScale*1000f)/1000f) ;
            data[i][3] = CStr(Math.round(myDB.hAlignMarks[i].getParentIndex())+1);
            int div = i / 2 ;
            int remainder = i - div*2 ;
            switch (remainder) {
                case 0: 
                    data[i][4] = "PC";
                    break;
                case 1:
                    data[i][4] = "PT";
                    break;
                default:
                    data[i][4] = "None";
                    break;
            }   //End Select
            for (int j=0; j<5; j++){
                tangentPrintStr += PrintText.StrFormat(0, data[i][j].toString(), fieldSize[j]) ;
            }
            tangentPrintStr += "\n" ;
        }   // for i
        JTable table = new JTable(data, headers) {
        // override isCellEditable method, , 11/13/06
           public boolean isCellEditable(int row, int column) {
               // all un-editable
               return false;
           }    // isCellEditable method
        } ;

        table.setPreferredScrollableViewportSize(new Dimension(350, 200));
        table.setColumnSelectionAllowed(true) ;
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
        //table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION) ;
        //table.setRowSelectionAllowed(false);
        //table.setCellSelectionEnabled(true) ;
        //table.setColumnSelectionAllowed(false) ;
        
        //Add the scroll pane to this panel.
        frmTangentTable.add(scrollPane);
        //Get the column model.
        javax.swing.table.TableColumnModel colModel = table.getColumnModel();
        //Get the column at index pColumn, and set its preferred width.
        colModel.getColumn(0).setPreferredWidth(24);   
        
            
        //Display the window.
        Dimension screen = getToolkit().getDefaultToolkit().getScreenSize();
        double top = 0.5*(screen.getWidth()-frmTangentTable.getWidth());
        double left = 0.5*(screen.getHeight()-frmTangentTable.getHeight());
        int x = new Double(top).intValue();
        int y = new Double(left).intValue();
        frmTangentTable.setLocation(x, y);

        frmTangentTable.pack();
        frmTangentTable.setVisible(true);
        frmTangentTable.show();
       
    }   // pop Tangent (PC PT) Data ;
    
    public void landmark_printData(){  // print report file
        try
        {
            //PrintSimpleText printReport = new PrintSimpleText(reportStr) ;
            PrintText printReport = new PrintText() ;
            printReport.print(landmarkPrintStr) ;

        }
        catch (Exception e){
                //do nothing
            System.out.println("Print Landmark Data:"+e.toString());
            sb.setStatusBarText(1, "Error: Print Landmark Data, "+e.toString()) ;
        } // try
       
    }// landmark print data
    
    public void tangent_printData(){  // print report file
        try
        {
            //PrintSimpleText printReport = new PrintSimpleText(reportStr) ;
            PrintText printReport = new PrintText() ;
            printReport.print(tangentPrintStr) ;

        }
        catch (Exception e){
                //do nothing
            System.out.println("Print PC, PT Data:"+e.toString());
            sb.setStatusBarText(1, "Error: Print PC, PT Data, "+e.toString()) ;
        } // try
       
    }// tangent print data
    
    public void landmark_saveData(){  // save report file
        try
        {
            FileDialog fd=new FileDialog(new Frame(),"Save Landmark Data", FileDialog.SAVE);
            fd.setFile("*.txt");
            fd.show();
            String fullpath=fd.getDirectory()+fd.getFile();
            fd.dispose();
//System.out.println("filepath="+fullpath);
            if(fullpath!=null) {
                BufferedWriter out = new BufferedWriter(new FileWriter(fullpath));
                //String reportStr = generateReport();
                out.write(landmarkPrintStr);
                out.flush();
                out.close();
            }
        }
        catch (Exception e){
                //do nothing
            System.out.println("Save Landmark Data File:"+e.toString());
            sb.setStatusBarText(1, "Error: Saving Landmark Data, "+e.toString()) ;
        } // try
       
    }// landmark save data

    
    public void tangent_saveData(){  // save report file
        try
        {
            FileDialog fd=new FileDialog(new Frame(),"Save PC, PT Data", FileDialog.SAVE);
            fd.setFile("*.txt");
            fd.show();
            String fullpath=fd.getDirectory()+fd.getFile();
            fd.dispose();
//System.out.println("filepath="+fullpath);
            if(fullpath!=null) {
                BufferedWriter out = new BufferedWriter(new FileWriter(fullpath));
                //String reportStr = generateReport();
                out.write(tangentPrintStr);
                out.flush();
                out.close();
            }
        }
        catch (Exception e){
                //do nothing
            System.out.println("Save PC, PT Data File:"+e.toString());
            sb.setStatusBarText(1, "Error: Saving PC, PT Data, "+e.toString()) ;
        } // try
       
    }// tangent save data

    public void popSaveVDesignB4Close() {       
        // open a frame
        frame_saveVDesign = new myWindow("Save Vertical Design File") ;
        //frame_saveVDesign.setLocation(350,150) ;
        frame_saveVDesign.setSize(350,120) ;
        frame_saveVDesign.setCenter() ;
        frame_saveVDesign.validate() ;
        frame_saveVDesign.setVisible(true) ;

        ActionListener frame_msgbox_yes_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                
            vDesign.file_save(); 
            vDesign.setStatusBarText(0, "Save vertical curve") ;
            frame_saveVDesign.dispose() ;
            //frmVerticalAlign.dispose();
            frmVerticalAlign.windowClosing(null) ;  // 11/17/06 added
                
                //repaint();
            }
        } ;
        ActionListener frame_msgbox_no_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_saveVDesign.dispose() ;
                //frmVerticalAlign.dispose();
                frmVerticalAlign.windowClosing(null) ;  // 11/17/06 added
            }
        } ;
        ActionListener frame_msgbox_cancel_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_saveVDesign.dispose() ;
            }
        } ;

        frame_saveVDesign.setLayout(new GridBagLayout()) ;
        // Create a constrains object, and specify default values
        GridBagConstraints c = new GridBagConstraints() ;
        c.fill = GridBagConstraints.BOTH ; // component grows in both directions
        c.weightx = 1.0 ; c.weighty = 1.0 ;

        c.gridx = 0 ; c.gridy = 0; c.gridwidth = 2 ; c.gridheight = 1 ;
        c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
        frame_saveVDesign.add(iconQ,c) ;
        c.gridx = 2 ; c.gridy = 0; c.gridwidth = 4 ; c.gridheight = 1 ;
        Label myMsg = new Label("Do you want to save current vertical design?") ;
        //myMsg.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
        //myMsg.setForeground(new Color(0,0,218)) ;
        frame_saveVDesign.setBackground(new Color(200, 200, 200)) ;
        frame_saveVDesign.add(myMsg,c) ;

        c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_saveVDesign.add(new Label(" "),c) ;
        c.gridx = 1 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_saveVDesign.add(new Label(" "),c) ;
        c.gridx = 2 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_saveVDesign.add(new Label(" "),c) ;
        c.gridx = 3 ; c.gridy = 1; c.gridwidth = 1 ;
        Button btn_ok = new Button(" Yes ") ;
        frame_saveVDesign.add(btn_ok, c) ;
        btn_ok.addActionListener(frame_msgbox_yes_listener) ;
        c.gridx = 4 ; c.gridy = 1;
        Button btn_no = new Button(" No ") ;
        frame_saveVDesign.add(btn_no, c) ;
        btn_no.addActionListener(frame_msgbox_no_listener) ;
        c.gridx = 5 ; c.gridy = 1; c.gridwidth = 1 ;
        Button btn_cancel = new Button("Cancel") ;
        frame_saveVDesign.add(btn_cancel,c) ;
        btn_cancel.addActionListener(frame_msgbox_cancel_listener) ;

        frame_saveVDesign.invalidate();
        frame_saveVDesign.show() ;
        frame_saveVDesign.toFront() ;
        frame_saveVDesign.setResizable(false) ;

    } // popSaveVDesignFileB4Close

}   // hDrawArea class

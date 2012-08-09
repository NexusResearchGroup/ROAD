/*
 * Geometry_Design.java
 *
 * Roadway Geometry Design main screen.
 * 
 * Created on March 17, 2006, 12:08 PM
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
import java.awt.event.* ;        // doesn't automatically load with java.awt.*
import java.applet.*;
import java.net.URL ; 
import java.awt.image.*;
import java.io.*;
import java.io.FilenameFilter;
import com.sun.image.codec.jpeg.*;
//import javax.help.* ;
import javax.swing.*;

public class Geometry_Design extends Applet
{
    String URLParam ;
    // Java GUI
    myWindow frmGeometryDesign ;        // pp screen for horizontal geometry design
    myWindow frmAbout ;                 // help about screen
    myWindow frame_saveDesign ;         // 11/13/06 added
    hDrawArea hDesign = new hDrawArea();
    // 4/21/06 modified
    MenuItem edit_undo  ;               // menu item handled by hDesign methods
    MenuItem edit_redo  ;
    MenuItem edit_delete  ;
    private myIcon iconQ = new myIcon("question_mark") ;
    
    // class initialization
    public void init()
    {
        frmAbout = new myWindow() ;
        frmGeometryDesign = new myWindow() ;
        
        Button btnOK =  new Button("Click Here to Start Roadway Design") ;
        setLayout(new BorderLayout(0,0));
        Panel textboxp = new Panel();
        textboxp.setLayout(new BorderLayout(0,0));
        textboxp.add("Center",new aboutTextbox()); 
        
        Panel startup = new Panel();
        startup.setBackground(Color.white);
	startup.setLayout(new BorderLayout(1,1));
	startup.add("Center",textboxp);
	startup.add("South",btnOK);

        // handle event on view timing plan button
        ActionListener btnOKListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                popGeometryDesign("Horizontal Geometry Design") ;
            }
        } ;
        btnOK.addActionListener(btnOKListener) ;

	add("West", new border(2, Color.black));
	add("East", new border(2, Color.black));
	add("North", new border(2, Color.black));
	add("South", new border(2, Color.black));
        add("Center",startup);

    }
    // Show Horizontal Geometric Design Window
    public void popGeometryDesign(String _title) {
        if (frmGeometryDesign.isShowing()==false) 
        {
            frmGeometryDesign = new myWindow(_title) ;
            frmGeometryDesign.setSize(800, 600) ;
            
            frmGeometryDesign.setCenter();
            frmGeometryDesign.validate() ;
            frmGeometryDesign.setVisible(true) ;
            //frmGeometryDesign.show() ;

            // file menu
            MenuBar menu_bar = new MenuBar() ;
            Menu menu_file = new Menu("File") ;
            MenuItem file_open = new MenuItem("Open Design") ;
            MenuItem file_save = new MenuItem("Save Design") ;
            MenuItem separator = new MenuItem("-") ;
            MenuItem file_close = new MenuItem("Close Design") ;
            MenuItem file_import = new MenuItem("Import Contour") ;
      //      MenuItem file_pagesetup = new MenuItem("Page Setup") ; //page setup functino is pretty much cover under print
            MenuItem file_print = new MenuItem("Print") ;
            MenuItem file_exit = new MenuItem("Exit") ;
            // file menu items
            menu_file.add(file_open) ;   // add menu items
            menu_file.add(file_save) ;   // add menu items
            menu_file.addSeparator() ;
            menu_file.add(file_close) ;   // add menu items
            menu_file.addSeparator() ;
            menu_file.add(file_import) ;
            menu_file.addSeparator() ;
      //      menu_file.add(file_pagesetup) ;
            menu_file.add(file_print) ;
            menu_file.addSeparator() ;
            menu_file.add(file_exit) ;
            // edit menu
            Menu menu_edit = new Menu("Edit") ;
            edit_undo = new MenuItem("Undo") ;
            edit_redo = new MenuItem("Redo") ;
            edit_delete = new MenuItem("Delete") ;
            MenuItem edit_clearLandmarks = new MenuItem("Clear Landmarks") ;
            MenuItem edit_clearAll = new MenuItem("Clear All") ;
            MenuItem edit_unselectAll = new MenuItem("Unselect All") ;
            MenuItem edit_selectAll = new MenuItem("Select All") ;
            menu_edit.add(edit_undo) ;
            menu_edit.add(edit_redo) ;
            menu_edit.add(edit_delete) ;
            menu_edit.addSeparator();
            menu_edit.add(edit_clearLandmarks) ;
            menu_edit.add(edit_clearAll) ;
            menu_edit.addSeparator();
            menu_edit.add(edit_selectAll) ;
            menu_edit.add(edit_unselectAll) ;
            // view menu
            Menu menu_view = new Menu("View") ;
            MenuItem view_reset = new MenuItem("Reset (1:1)", new MenuShortcut(KeyEvent.VK_R)) ;
            MenuItem view_zoomin = new MenuItem("Zoom In 10%", new MenuShortcut(KeyEvent.VK_UP)) ;
            MenuItem view_zoomout = new MenuItem("Zoom Out 10%", new MenuShortcut(KeyEvent.VK_DOWN)) ;
            MenuItem view_zoomin5 = new MenuItem("Zoom In 50%", new MenuShortcut(KeyEvent.VK_F5)) ;
            MenuItem view_zoomout5 = new MenuItem("Zoom Out 50%", new MenuShortcut(KeyEvent.VK_F6)) ;
            MenuItem view_landmarks = new MenuItem("Station Landmarks");
            MenuItem view_tangents = new MenuItem("PC, PT Data");
            MenuItem view_roadOnly = new MenuItem("Road Only", new MenuShortcut(KeyEvent.VK_Z));
            MenuItem view_road = new MenuItem("Road Design", new MenuShortcut(KeyEvent.VK_A));
            //view_landmarks.setEnabled(false);
            
            menu_view.add(view_reset) ;
            menu_view.add(view_zoomin) ;
            menu_view.add(view_zoomout) ;
            menu_view.add(view_zoomin5) ;
            menu_view.add(view_zoomout5) ;
            menu_view.addSeparator();
            menu_view.add(view_landmarks) ;
            menu_view.add(view_tangents) ;
            menu_view.addSeparator();
            menu_view.add(view_road) ;
            menu_view.add(view_roadOnly) ;
            
            // settings menu
            Menu menu_settings = new Menu("Settings") ;
            MenuItem settings_design = new MenuItem("Road Design") ;
            MenuItem settings_contour = new MenuItem("Contour Image") ;
            menu_settings.add(settings_design) ;
            menu_settings.add(settings_contour) ;
            // option menu
            Menu menu_tool = new Menu("Tool") ;
            MenuItem tool_line = new MenuItem("Create Line") ;
            MenuItem tool_curve = new MenuItem("Create Curve") ;
            MenuItem tool_modify = new MenuItem("Modify End Point") ;
            MenuItem tool_station = new MenuItem("Place Landmark") ;
            MenuItem tool_insert = new MenuItem("Insert Landmark") ;
            MenuItem tool_halign = new MenuItem("Align Curve") ;
            MenuItem tool_property = new MenuItem("Show Properties") ;
            MenuItem tool_checkStation = new MenuItem("Check Station Data") ;
            menu_tool.add(tool_line) ;
            menu_tool.add(tool_curve) ;
            menu_tool.add(tool_modify) ;
            menu_tool.add(tool_station) ;
            menu_tool.add(tool_insert) ;
            menu_tool.addSeparator() ;
            menu_tool.add(tool_halign) ; 
            menu_tool.add(tool_checkStation) ;
            menu_tool.add(tool_property) ;
            // help menu
            Menu menu_help = new Menu("Help") ;
            MenuItem help_web_contents = new MenuItem("Web Contents") ; 
            MenuItem help_manual = new MenuItem("User's Manual PDF") ;
            MenuItem help_about = new MenuItem("About ROAD") ;
            //MenuItem help_javahelp = new MenuItem("User's Guide") ; 
            //MenuItem help_aboutJavaHelp = new MenuItem("About JavaHelp") ;
            
            menu_help.add(help_web_contents) ;
            //menu_help.add(help_javahelp) ;
            menu_help.add(help_manual) ;
            menu_help.addSeparator() ;
            menu_help.add(help_about) ;
            //menu_help.add(help_aboutJavaHelp) ; // 11/26/06 added

            /*
            // Find the HelpSet file and create the HelpSet object:
            String helpHS = "javahelp/road.hs" ;    //"http://128.101.111.90/Road/javahelp/road.hs" ;
            HelpSet hs = null ;
            HelpBroker hb = null ;
            ClassLoader cl = this.getClass().getClassLoader();
            try {
                URL hsURL = HelpSet.findHelpSet(cl, helpHS);
                hs = new HelpSet(null, hsURL);
            } catch (Exception ee) {
                // Say what the exception really is
                System.out.println( "HelpSet " + ee.getMessage()) ;
                System.out.println("HelpSet "+ helpHS +" not found") ;
            }
            // Create a HelpBroker object:
            hb = hs.createHelpBroker();

            help_javahelp.addActionListener(new CSH.DisplayHelpFromSource( hb ));
            */

            // ===========================================
            menu_bar.add(menu_file) ;     // add menu
            menu_bar.add(menu_edit) ;     // add menu
            menu_bar.add(menu_view) ;     // add menu
            menu_bar.add(menu_settings) ; // add menu
            menu_bar.add(menu_tool) ;   // add menu
            menu_bar.add(menu_help) ;     // add menu
            frmGeometryDesign.setMenuBar(menu_bar) ;

            toolbar tb = new toolbar();
            statusbar sb = new statusbar() ;
            Panel cm = new Panel();
            Panel cc = new Panel();
            frmGeometryDesign.setLayout(new BorderLayout(0,0));

            //Scrollbar ss = new Scrollbar(Scrollbar.HORIZONTAL);
            cc.setLayout(new BorderLayout(0,0));
            //cc.add("South",new Scrollbar(Scrollbar.HORIZONTAL));
            //cc.add("East",new Scrollbar(Scrollbar.VERTICAL));
            hDesign = new hDrawArea(tb, sb, edit_undo, edit_redo, edit_delete); 
            hDesign.setBackground(new Color(247,247,247)) ;
            hDesign.myApplet = this;
            cc.add("Center",hDesign); 

            cm.setBackground(Color.black);
            cm.setLayout(new BorderLayout(1,1));
            cm.add("North",tb);
            cm.add("Center",cc);
            cm.add("South",sb);

            frmGeometryDesign.add("West", new border(2, Color.black));
            frmGeometryDesign.add("East", new border(2, Color.black));
            frmGeometryDesign.add("North", new border(2, Color.black));
            frmGeometryDesign.add("South", new border(2, Color.black));
            frmGeometryDesign.add("Center",cm);
            frmGeometryDesign.invalidate() ;
            frmGeometryDesign.show() ;
            
            /*
             help_javahelp.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        try
                        {
                            new HelpDoc() ; // open javaHelp document
                        }
                        catch (Exception e){
                                //do nothing
                            hDesign.popMessageBox("Help - User's Guide", "Error:"+e.toString()) ;
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
                            AppletContext a = getAppletContext();
                            URL u = new URL(SHARED.CONTENTS_PATH);  
                            a.showDocument(u,"_blank");
                            //_blank to open page in new window		
                        }
                        catch (Exception e){
                                //do nothing
                            hDesign.popMessageBox("Help - Web Content", "Error:"+e.toString()) ;
                        } // try
                    } // actionPerformed
                } // ActionListener
            ) ; // help_web_contents
            
            help_manual.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        try
                        {
                            AppletContext a = getAppletContext();
                            URL u = new URL(SHARED.MANUAL_PATH);  
                            a.showDocument(u,"_blank");
                            //_blank to open page in new window		
                        }
                        catch (Exception e){
                                //do nothing
                        } // try
                    } // actionPerformed
                } // ActionListener
            ) ; // help_manual
            /*
            help_aboutJavaHelp.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        hDesign.popMessageBox("JavaHelp", 
                        "Note: You need to have JavaHelp package\n" +  
                        "(jh.jar, jhall.jar, jhbasic.jar, jsearch.jar)\n" +
                        "installed in your ..\\Java\\jre\\lib\\ext\\ directory\n" + 
                        "in order to view the Users' Guide. JavaHelp is\n" + 
                        "available at http://java.sun.com/products/javahelp/.") ;
                        
                    } // actionPerformed
                } // ActionListener
            ) ; // help_aboutJavaHelp
            */
            help_about.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent aev) {
                        if (frmAbout.isShowing()==false) {
                            frmAbout = new myWindow("About ROAD") ;
                            frmAbout.setSize(300, 140) ;
                            frmAbout.setResizable(false);
                            //frmAbout.setLocation(100,100) ;
                            frmAbout.show() ;
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
                        }
                        else {
                            frmAbout.show();
                        }
                        
                   } // actionPerformed
                } // ActionListener
            ) ; // help_about
            file_import.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            
                            try
                            {
                                FileDialog fd=new FileDialog(new Frame(),"Import Contour Image", FileDialog.LOAD);
                                fd.show();
                                String dir = fd.getDirectory() ;
                                String filename = fd.getFile() ;
                                String fullpath = dir + filename;
                                fd.dispose();

                                //System.out.println("file-import path: "+fullpath) ;
                                if(dir != null && filename != null) {
                                    hDesign.contourImageFilepath =fullpath;
                                    hDesign.image = getToolkit().getImage(fullpath);
                                    hDesign.init(0);
                                    hDesign.view_RESET();
                                }
                                //hDesign.repaint();
                            }
                            catch (Exception e){
                                System.out.println("file-import: "+e.toString()) ;
                                    //do nothing
                            } // try
                        } // actionPerformed
                    } // ActionListener
             ) ; // file import
            file_open.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            FileInputStream fis=null;
                            DataInputStream br=null;
                            hDesign.line_started = false;
                            hDesign.curve_started = false;
                            hDesign.modification_started = false;
                            hDesign.mouseHoldDown = false;
                            String dir = "", filename = "" ;

                            try
                            {
                                FileDialog fd=new FileDialog(new Frame(),"Open Design", FileDialog.LOAD);
                                fd.setFile("*.rdw");
                                fd.show();
                                dir = fd.getDirectory() ;
                                filename = fd.getFile() ;
                                String fullpath = dir + filename ;
                                fd.dispose();

                                if(filename != null && dir != null) {
                                    //System.out.println("Open filename="+fullpath);
                                    //reset draw settings
                                    hDesign.view_RESET();
                                    fis = new FileInputStream(fullpath);
                                    br = new DataInputStream( new BufferedInputStream(fis,512)); 
                                    // 1 - read settings
                                    int r, g, b ;
                                    r = br.readInt();
                                    g = br.readInt();
                                    b = br.readInt();
                                    hDesign.myDB.myPenColor = new Color(r, g, b);
                                    hDesign.myDB.myRoadLaneSizes = br.readFloat();
                                    r = br.readInt();
                                    g = br.readInt();
                                    b = br.readInt();
                                    hDesign.myDB.elevationMarkerColor = new Color(r, g, b);
                                    hDesign.myDB.elevationMarkerSize = br.readFloat();
                                    // 2 - read image file
                                    hDesign.contourImageFilepath = br.readUTF();
                                    boolean exists = (new File(hDesign.contourImageFilepath)).exists();
                                    if (!exists) {
                                        // file path/name not found, try local directory
                                        String dirname, imagefilename ;
                                        
                                        int mylidx = fullpath.lastIndexOf("\\") ;
                                   //     System.out.println("bk=" +mylidx+ "\\");
                                        dirname = fullpath.substring(0, mylidx + 1);
                                        mylidx = hDesign.contourImageFilepath.lastIndexOf("\\");
                                        imagefilename = hDesign.contourImageFilepath.substring(mylidx + 1);
                                        hDesign.contourImageFilepath = dirname + imagefilename;
                                    }
                                    //System.out.println("open filename="+hDesign.contourImageFilepath);

                                    hDesign.image = getToolkit().getImage(hDesign.contourImageFilepath);
                                    hDesign.translate = new mPoint(0, 0);
                                    // 3 - read image scale & resolution
                                    hDesign.myDB.ContourImageResolution = br.readFloat();   // 10/9/06
                                    hDesign.myDB.ContourScale = br.readFloat(); // 10/9/06
                                    hDesign.myDB.imageScale = (float)hDesign.myDB.ContourImageResolution / (float)hDesign.myDB.ContourScale;
                                    // 4 - read road data
                                    hDesign.hRoadDataCount = br.readInt();
                                    hDesign.push2SegLogBuffer(hDesign.hRoadDataCount);
                                    //Dim i As Integer
                                    float x, y ;
                                    int i;
                                    for (i=0;i<hDesign.hRoadDataCount;i++){
                                        if (hDesign.myDB.hRoadData[i] == null ) { 
                                            hDesign.myDB.hRoadData[i] = new Data2D();
                                        }
                                        hDesign.myDB.hRoadData[i].resetDeleteFlag() ;   // 11/8/06
                                        x = br.readFloat();
                                        y = br.readFloat();
                                        hDesign.myDB.hRoadData[i].setPoint1(x, y); 
                                        x = br.readFloat();
                                        y = br.readFloat();
                                        hDesign.myDB.hRoadData[i].setPoint2(x, y);
                                        hDesign.myDB.hRoadData[i].setRadius(br.readFloat());
                                        hDesign.myDB.hRoadData[i].selectItemSet(br.readBoolean());
                                        hDesign.myDB.hRoadData[i].setPenWidth(br.readFloat());
                                        r = br.readInt();
                                        g = br.readInt();
                                        b = br.readInt();
                                        hDesign.myDB.hRoadData[i].setPenColor(new Color(r, g, b));
                                    }
                                    // 5 - get saved elevation landmark data
                                    hDesign.myDB.elevationMarkCount = br.readInt();
                                    hDesign.push2MarkLogBuffer(hDesign.myDB.elevationMarkCount);
                                    for (i=0; i<hDesign.myDB.elevationMarkCount; i++) {
                                        if (hDesign.myDB.elevationMarks[i] == null) { 
                                            hDesign.myDB.elevationMarks[i] = new MarkerDB();
                                        }
                                        x = br.readFloat();
                                        y = br.readFloat();
                                        hDesign.myDB.elevationMarks[i].setLocation(x, y);
                                        hDesign.myDB.elevationMarks[i].setElevation(br.readFloat());
                                        hDesign.myDB.elevationMarks[i].setParentIndex(br.readByte());
                                        hDesign.myDB.elevationMarks[i].setSegmentType(br.readByte());
                            //System.out.println("SegmentType ="+hDesign.myDB.elevationMarks[i].getSegmentType());
                                    }
                                    // 6 - get horizontal alignment landmarks
                                    hDesign.myDB.hAlignMarkCount = br.readInt();
                         //System.out.println("hAlignMarkCount ="+hDesign.myDB.hAlignMarkCount);
                                    for (i=0;i<hDesign.myDB.hAlignMarkCount;i++){
                                        if (hDesign.myDB.hAlignMarks[i] == null) { 
                                            hDesign.myDB.hAlignMarks[i] = new MarkerDB();
                                        }
                                        x = br.readFloat();
                                        y = br.readFloat();
                                        hDesign.myDB.hAlignMarks[i].setLocation(x, y);
                                        hDesign.myDB.hAlignMarks[i].setElevation(br.readFloat());
                                        hDesign.myDB.hAlignMarks[i].setParentIndex(br.readByte());
                                        hDesign.myDB.hAlignMarks[i].setSegmentType(br.readByte());
                                    }

                                    // 7 - read control parameters
                                    hDesign.myDB.myUnit = br.readInt();                           // unit 1-US, 2-Metric
                                    hDesign.myDB.gradeLimit = br.readFloat();                     // max grade limit default 6%
                                    hDesign.myDB.minGrade = br.readFloat();                     // min grade limit default 0.5%
                                    hDesign.myDB.speedLimit = br.readFloat();                     // speed limit MPH default 40MPH
                                    hDesign.myDB.maxCut = br.readFloat();                         // maximum cut, default 10 ft
                                    hDesign.myDB.maxFill = br.readFloat();                        // maximum fill, default 10 ft
                                    hDesign.myDB.vehDecel = br.readFloat();                       // vehicle deceleration rate, 11.2 ft/s/s
                                    hDesign.myDB.reactionTime = br.readFloat() ;                  // drive reaction time 2.5 sec
                                    hDesign.myDB.frictionCoef = br.readFloat();                   // friction coefficient
                                    hDesign.myDB.sideFrictionCoef = br.readFloat();
                                    hDesign.myDB.minVCurveLen = br.readFloat();                   // min vertical curve length
                                    hDesign.myDB.minHCurveRadius = br.readFloat();               // minimum horizontal curve radius
                                    hDesign.myDB.maxSuperelevation = br.readFloat();             // max superelevation
                                    hDesign.myDB.myLaneWidth = br.readFloat();
                                    hDesign.myDB.myShoulderWidth = br.readFloat();

                                    br.close();
                                    fis.close();
                                
                                }
                                //hDesign.processImage(); 
                                hDesign.init(1);
                                hDesign.repaint();
                            }
                            catch (Exception e){
                                    //do nothing
                                System.out.println("Open Design File:"+e.toString());
                                System.out.println("File directory = "+dir+", filename = "+filename);
                            } // try
                        } // actionPerformed
                    } // ActionListener
             ) ; // file open
             file_save.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            // 11/13/06
                            saveDesignFile() ;
                            
                        } // actionPerformed
                    } // ActionListener
             ) ; // file save
             file_close.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            // pop screen to prompt save current design, if exists
                            popSaveDesignB4Close() ;
                            
                        } // actionPerformed
                    } // ActionListener
             ) ; // file Close
/*            file_pagesetup.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.printPageSetup();
                            
                        } // actionPerformed
                    } // ActionListener
             ) ; // file Print Page Setup
*/
             file_print.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.print();

                            //PrintUtilities.printComponent(hDesign);
                        } // actionPerformed
                    } // ActionListener
             ) ; // file Print
             file_exit.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            // pop screen to prompt save current design, if exists
                            popSaveDesignB4Exit() ;
                            
                            //frmGeometryDesign.dispose();
                        } // actionPerformed
                    } // ActionListener
             ) ; // file Exit
             edit_undo.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.edit_undo();
                        } // actionPerformed
                    } // ActionListener
             ) ; // edit_undo
             edit_redo.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.edit_redo();
                        } // actionPerformed
                    } // ActionListener
             ) ; // edit_redo
             edit_delete.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.popDeleteSegment("Edit - Delete","Are you sure to delete selected segment(s)?");
                        } // actionPerformed
                    } // ActionListener
             ) ; // edit_delete
             edit_clearLandmarks.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.popClearLandmarks("Edit - Clear All Landmarks","Are you sure to clear all station landmarks?");
                        } // actionPerformed
                    } // ActionListener
             ) ; // edit_clearLandmarks
             edit_clearAll.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.popClearAll("Edit - Clear All Design","Are you sure to clear all design?");
                        } // actionPerformed
                    } // ActionListener
             ) ; // edit_clearAll
             edit_selectAll.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.edit_selectAll();
                        } // actionPerformed
                    } // ActionListener
             ) ; // edit_unselect All
             edit_unselectAll.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.edit_unselectAll() ;
                        } // actionPerformed
                    } // ActionListener
             ) ; // edit_unselect All
             view_reset.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.view_RESET();
                        } // actionPerformed
                    } // ActionListener
             ) ; // view reset
             view_landmarks.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.popLandmarkData();
                        } // actionPerformed
                    } // ActionListener
             ) ; // view landmarks
             view_tangents.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.popTangentData(); 
                        } // actionPerformed
                    } // ActionListener
             ) ; // view PC PT data
             view_road.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.viewRoadDesign();  
                        } // actionPerformed
                    } // ActionListener
             ) ; // view horizontal road design only
             view_roadOnly.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.viewRoadOnly();  
                        } // actionPerformed
                    } // ActionListener
             ) ; // view horizontal road design only
             
             view_zoomin.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.changeDrawScale(0.1f);
                        } // actionPerformed
                    } // ActionListener
             ) ; // view zoom in
             view_zoomout.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.changeDrawScale(-0.1f);
                        } // actionPerformed
                    } // ActionListener
             ) ; // view zoom out
             view_zoomin5.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.changeDrawScale(0.5f);
                        } // actionPerformed
                    } // ActionListener
             ) ; // view zoom in 50%
             view_zoomout5.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.changeDrawScale(-0.5f);
                        } // actionPerformed
                    } // ActionListener
             ) ; // view zoom out 50%
             tool_line.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.newstatus(4, " Line Tool") ;
                            hDesign.repaint() ;
                        } // actionPerformed
                    } // ActionListener
             ) ; // tool_line
             tool_curve.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.newstatus(5, " Curve Tool") ;
                            hDesign.repaint() ;
                        } // actionPerformed
                    } // ActionListener
             ) ; // tool_curve
             tool_modify.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.newstatus(6, " Modify End Point") ;
                            hDesign.repaint() ;
                        } // actionPerformed
                    } // ActionListener
             ) ; // tool_modify 
             tool_station.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.newstatus(8, " Set Station/Landmark") ;
                            hDesign.repaint() ;
                        } // actionPerformed
                    } // ActionListener
             ) ; // tool_station
             tool_insert.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.newstatus(9, " Insert Station/Landmark") ;
                            hDesign.repaint() ;
                        } // actionPerformed
                    } // ActionListener
             ) ; // tool_insert
             tool_halign.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.tool_curvehAlignMarks();
                        } // actionPerformed
                    } // ActionListener
             ) ; // tool_halign
             tool_property.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.tool_property() ;
                        } // actionPerformed
                    } // ActionListener
             ) ; // tool_property
             tool_checkStation.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            hDesign.tool_checkStation() ; 
                        } // actionPerformed
                    } // ActionListener
             ) ; // toolcheckStation
             
             settings_design.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            if (hDesign.frame_settingsDesign==null){
                                hDesign.popSettingsDesign(); 
                            } else {
                                if (hDesign.frame_settingsDesign.isShowing()==false){
                                    hDesign.popSettingsDesign();
                                } else {
                                    hDesign.frame_settingsDesign.show();
                                }
                            }
                            
                        } // actionPerformed
                    } // ActionListener
             ) ; // settings_design
             settings_contour.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent aev) {
                            if (hDesign.frame_settingsContour==null) {
                                hDesign.popSettingsContour();
                            }   else {
                                if (hDesign.frame_settingsContour.isShowing()==false){
                                    hDesign.popSettingsContour();
                                } else {
                                    hDesign.frame_settingsContour.show();
                                }
                            }
                        } // actionPerformed
                    } // ActionListener
             ) ; // settings_contour
             
            //=============================
            frmGeometryDesign.invalidate() ;
            frmGeometryDesign.setVisible(true) ;
            frmGeometryDesign.show() ;
        }
        else {  // frmGeometryDesign already displayed
            frmGeometryDesign.show() ;
        }   // end if frmGeometryDesign is Showing
        
    } // popGeometryDesign
    
    // save horizonatl design file
    public void saveDesignFile() {
        FileOutputStream fos=null;
        DataOutputStream w=null;

        try
        {
            FileDialog fd=new FileDialog(new Frame(),"Save Design", FileDialog.SAVE);
            fd.setFile("*.rdw");
 /*            fd.setFilenameFilter(new FilenameFilter(){
                public boolean accept(File dir, String name){
                  return (name.endsWith(".rdp")) ;  // || name.endsWith(".gif"));
                  }
            });
  */
            fd.show();
            String fullpath=fd.getDirectory()+fd.getFile();
            fd.dispose();
//System.out.println("filepath="+fullpath);
            if(fullpath!=null) {
                if (fullpath.indexOf(".rdw")<0) {
                    fullpath += ".rdw" ;
                }
                fos = new FileOutputStream(fullpath);
                w = new DataOutputStream( new BufferedOutputStream(fos,512)); 
                // 1 - save settings
                w.writeInt(hDesign.myDB.myPenColor.getRed());
                w.writeInt(hDesign.myDB.myPenColor.getGreen());
                w.writeInt(hDesign.myDB.myPenColor.getBlue());
                w.writeFloat(hDesign.myDB.myRoadLaneSizes);
                w.writeInt(hDesign.myDB.elevationMarkerColor.getRed());
                w.writeInt(hDesign.myDB.elevationMarkerColor.getGreen());
                w.writeInt(hDesign.myDB.elevationMarkerColor.getBlue());
                w.writeFloat(hDesign.myDB.elevationMarkerSize);
                w.flush();
                // 2 - save image file path/name
                w.writeUTF(hDesign.contourImageFilepath) ;
                // 3 - save image resolution & scale
                w.writeFloat(hDesign.myDB.ContourImageResolution);
                w.writeFloat(hDesign.myDB.ContourScale);
                // 4 - save hRoadData DB
                int i, actualDataSize ;
                // 11/8/06
                // save only undeleted items
                actualDataSize = hDesign.hRoadDataCount ;
                for (i=0;i<hDesign.hRoadDataCount;i++) {
                    if (hDesign.myDB.hRoadData[i].isDeleted()) {
                        actualDataSize-- ;
                    }   // if item deleted
                }   // i
                w.writeInt(actualDataSize);
                w.flush();
                int saved_count = 0 ;
                int[] lookupRef = new int[actualDataSize] ;
                for (i=0;i<hDesign.hRoadDataCount;i++) {
                    if (!hDesign.myDB.hRoadData[i].isDeleted()) {   // 11/8/06
                        // item not deleted
                        w.writeFloat(hDesign.myDB.hRoadData[i].getPoint1().X);
                        w.writeFloat(hDesign.myDB.hRoadData[i].getPoint1().Y);
                        w.writeFloat(hDesign.myDB.hRoadData[i].getPoint2().X);
                        w.writeFloat(hDesign.myDB.hRoadData[i].getPoint2().Y);
                        w.writeFloat(hDesign.myDB.hRoadData[i].getRadius());
                        w.writeBoolean(hDesign.myDB.hRoadData[i].isSelected());
                        w.writeFloat(hDesign.myDB.hRoadData[i].getPenWidth());
                        w.writeInt(hDesign.myDB.hRoadData[i].getPenColor().getRed());
                        w.writeInt(hDesign.myDB.hRoadData[i].getPenColor().getGreen());
                        w.writeInt(hDesign.myDB.hRoadData[i].getPenColor().getBlue());
                        w.flush();
                        lookupRef[saved_count] = i ;    // lookup reference table after remove deleted segment
                        saved_count++ ;
                    }
                }
                // 5 - save landmarks DB
                w.writeInt(hDesign.myDB.elevationMarkCount);
                byte oldParentID ;
                for (i=0;i<hDesign.myDB.elevationMarkCount;i++){
                    w.writeFloat(hDesign.myDB.elevationMarks[i].getLocation().X);
                    w.writeFloat(hDesign.myDB.elevationMarks[i].getLocation().Y);
                    w.writeFloat(hDesign.myDB.elevationMarks[i].getElevation());
                    oldParentID = hDesign.myDB.elevationMarks[i].getParentIndex() ; // 3/1/07
                    w.writeByte(lookupParentIndex(oldParentID, lookupRef));         // 3/1/07
                    w.writeByte(hDesign.myDB.elevationMarks[i].getSegmentType());
                    w.flush();
                }
                // 6 - save horizontal alignment landmarks
                w.writeInt(hDesign.myDB.hAlignMarkCount);
                for (i=0; i<hDesign.myDB.hAlignMarkCount;i++) {
                    w.writeFloat(hDesign.myDB.hAlignMarks[i].getLocation().X);
                    w.writeFloat(hDesign.myDB.hAlignMarks[i].getLocation().Y);
                    w.writeFloat(hDesign.myDB.hAlignMarks[i].getElevation());
                    oldParentID = hDesign.myDB.hAlignMarks[i].getParentIndex() ;    // 3/1/07
                    w.writeByte(lookupParentIndex(oldParentID, lookupRef));         // 3/1/07
                    w.writeByte(hDesign.myDB.hAlignMarks[i].getSegmentType());
                    w.flush();
                }
                // 7 - save control parameters
                w.writeInt(hDesign.myDB.myUnit);                           // unit 1-US, 2-Metric
                w.writeFloat(hDesign.myDB.gradeLimit) ;                      // grade limit default 6%
                w.writeFloat(hDesign.myDB.minGrade) ;                      // min grade limit default 0.5%
                w.writeFloat(hDesign.myDB.speedLimit) ;                      // speed limit MPH default 40MPH
                w.writeFloat(hDesign.myDB.maxCut) ;                          // maximum cut, default 10 ft
                w.writeFloat(hDesign.myDB.maxFill) ;                         // maximum fill, default 10 ft
                w.writeFloat(hDesign.myDB.vehDecel) ;                        // vehicle deceleration rate, 11.2 ft/s/s
                w.writeFloat(hDesign.myDB.reactionTime) ;                    // drive reaction time 2.5 sec
                w.writeFloat(hDesign.myDB.frictionCoef)  ;                   // friction coefficient
                w.writeFloat(hDesign.myDB.sideFrictionCoef);
                w.writeFloat(hDesign.myDB.minVCurveLen)   ;                  // min vertical curve length
                w.writeFloat(hDesign.myDB.minHCurveRadius) ;                 // min horizontal curve radius
                w.writeFloat(hDesign.myDB.maxSuperelevation) ;               // max superelevation
                w.writeFloat(hDesign.myDB.myLaneWidth)   ;                   // lane width 12 ft
                w.writeFloat(hDesign.myDB.myShoulderWidth) ;                 // shoulder width 6 ft
                w.flush();
                w.close();
            }
            fos.close();
        }
        catch (Exception e){
                //do nothing
            System.out.println("Save Design File:"+e.toString());
        } // try
        
    }
   
    // 3/1/07 added
    private byte lookupParentIndex(byte old_index, int[] lookupRef) {
        int i ;
        byte value=(byte)255 ;
        for (i=0; i<lookupRef.length; i++) {
            if (lookupRef[i]==old_index) {
                value = (byte) i ;
                break ;
            }
        }
        if (value==255) {
            System.out.println("Error in looupParentIndex: parentIndex not found!") ;
        }
        return value ;
    }
    
    // prompt to save horizontal design file before closing window
    public void popSaveDesignB4Close() {       
        // open a frame
        frame_saveDesign = new myWindow("Save Horizontal Design File") ;
        //frame_saveDesign.setLocation(350,150) ;
        frame_saveDesign.setSize(350,120) ;
        frame_saveDesign.setCenter() ;
        frame_saveDesign.validate() ;
        frame_saveDesign.setVisible(true) ;


        ActionListener frame_msgbox_yes_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                saveDesignFile() ;

                frame_saveDesign.dispose() ;
                
                resetHDesign() ;
                //repaint();
            }
        } ;
        ActionListener frame_msgbox_no_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_saveDesign.dispose() ;
                resetHDesign() ;
            }
        } ;
        ActionListener frame_msgbox_cancel_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_saveDesign.dispose() ;
            }
        } ;

        frame_saveDesign.setLayout(new GridBagLayout()) ;
        // Create a constrains object, and specify default values
        GridBagConstraints c = new GridBagConstraints() ;
        c.fill = GridBagConstraints.BOTH ; // component grows in both directions
        c.weightx = 1.0 ; c.weighty = 1.0 ;

        c.gridx = 0 ; c.gridy = 0; c.gridwidth = 2 ; c.gridheight = 1 ;
        c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
        frame_saveDesign.add(iconQ,c) ;
        c.gridx = 2 ; c.gridy = 0; c.gridwidth = 4 ; c.gridheight = 1 ;
        Label myMsg = new Label("Do you want to save current horizontal design?") ;
        //myMsg.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
        //myMsg.setForeground(new Color(0,0,218)) ;
        frame_saveDesign.setBackground(new Color(200, 200, 200)) ;
        frame_saveDesign.add(myMsg,c) ;

        c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_saveDesign.add(new Label(" "),c) ;
        c.gridx = 1 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_saveDesign.add(new Label(" "),c) ;
        c.gridx = 2 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_saveDesign.add(new Label(" "),c) ;
        c.gridx = 3 ; c.gridy = 1; c.gridwidth = 1 ;
        Button btn_ok = new Button(" Yes ") ;
        frame_saveDesign.add(btn_ok, c) ;
        btn_ok.addActionListener(frame_msgbox_yes_listener) ;
        c.gridx = 4 ; c.gridy = 1;
        Button btn_no = new Button(" No ") ;
        frame_saveDesign.add(btn_no, c) ;
        btn_no.addActionListener(frame_msgbox_no_listener) ;
        c.gridx = 5 ; c.gridy = 1; c.gridwidth = 1 ;
        Button btn_cancel = new Button("Cancel") ;
        frame_saveDesign.add(btn_cancel,c) ;
        btn_cancel.addActionListener(frame_msgbox_cancel_listener) ;

        frame_saveDesign.invalidate();
        frame_saveDesign.show() ;
        frame_saveDesign.toFront() ;
        frame_saveDesign.setResizable(false) ;

    } // popSaveDesignFileB4Close
    
    // prompt to save horizontal design before exit application
    public void popSaveDesignB4Exit() {       
        // open a frame
        frame_saveDesign = new myWindow("Save Horizontal Design File") ;
        //frame_saveDesign.setLocation(350,150) ;
        frame_saveDesign.setSize(350,120) ;
        frame_saveDesign.setCenter() ;
        frame_saveDesign.validate() ;
        frame_saveDesign.setVisible(true) ;


        ActionListener frame_msgbox_yes_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                saveDesignFile() ;
                
                frame_saveDesign.dispose() ;
                frmGeometryDesign.dispose();
                //repaint();
            }
        } ;
        ActionListener frame_msgbox_no_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_saveDesign.dispose() ;
                frmGeometryDesign.dispose();
            }
        } ;
        ActionListener frame_msgbox_cancel_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                frame_saveDesign.dispose() ;
            }
        } ;
        frame_saveDesign.setLayout(new GridBagLayout()) ;
        // Create a constrains object, and specify default values
        GridBagConstraints c = new GridBagConstraints() ;
        c.fill = GridBagConstraints.BOTH ; // component grows in both directions
        c.weightx = 1.0 ; c.weighty = 1.0 ;

        c.gridx = 0 ; c.gridy = 0; c.gridwidth = 2 ; c.gridheight = 1 ;
        c.insets = new Insets(5,5,5,5) ; // 5-pixel margins on all sides
        frame_saveDesign.add(iconQ,c) ;
        c.gridx = 2 ; c.gridy = 0; c.gridwidth = 4 ; c.gridheight = 1 ;
        Label myMsg = new Label("Do you want to save current horizontal design?") ;
        //myMsg.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
        //myMsg.setForeground(new Color(0,0,218)) ;
        frame_saveDesign.setBackground(new Color(200, 200, 200)) ;
        frame_saveDesign.add(myMsg,c) ;

        c.gridx = 0 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_saveDesign.add(new Label(" "),c) ;
        c.gridx = 1 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_saveDesign.add(new Label(" "),c) ;
        c.gridx = 2 ; c.gridy = 1; c.gridwidth = 1 ;
        frame_saveDesign.add(new Label(" "),c) ; 
        c.gridx = 3 ; c.gridy = 1; c.gridwidth = 1 ;
        Button btn_ok = new Button(" Yes ") ;
        frame_saveDesign.add(btn_ok, c) ;
        btn_ok.addActionListener(frame_msgbox_yes_listener) ;
        c.gridx = 4 ; c.gridy = 1;
        Button btn_no = new Button(" No ") ;
        frame_saveDesign.add(btn_no, c) ;
        btn_no.addActionListener(frame_msgbox_no_listener) ;
        c.gridx = 5 ; c.gridy = 1; c.gridwidth = 1 ;
        Button btn_cancel = new Button("Cancel") ;
        frame_saveDesign.add(btn_cancel,c) ;
        btn_cancel.addActionListener(frame_msgbox_cancel_listener) ;

        frame_saveDesign.invalidate();
        frame_saveDesign.show() ;
        frame_saveDesign.toFront() ;
        frame_saveDesign.setResizable(false) ;

    } // popSaveDesignFileB4Exit

    // rest horizontal design database & settings
    public void resetHDesign() {
        int i=0 ;
        hDesign.image=null;
        hDesign.toolbarIndex=0 ;
        // clear all DBs
        for (i=0; i<hDesign.myDB.hAlignMarkCount; i++) {
            hDesign.myDB.hAlignMarks[i].RESET();
        }
        for (i=0; i<hDesign.myDB.elevationMarkCount; i++) {
            hDesign.myDB.elevationMarks[i].RESET();
        }
        for (i=0; i<hDesign.hRoadDataCount;i++) {
            hDesign.myDB.hRoadData[i].RESET();
        }

        hDesign.myDB.hAlignMarkCount = 0;
        hDesign.myDB.elevationMarkCount = 0;
        hDesign.hRoadDataCount = 0;
        hDesign.translate = new mPoint(0, 0);
        hDesign.repaint();        
    }   // reset hDesign
    
    /**
    * find the helpset file and create a HelpSet object
    */
    /*
    public HelpSet getHelpSet(String helpsetfile) {
          HelpSet hs = null;
          ClassLoader cl = this.getClass().getClassLoader();
          try {
            URL hsURL = HelpSet.findHelpSet(cl, helpsetfile);
            hs = new HelpSet(null, hsURL);
          } catch(Exception ee) {
            System.out.println("HelpSet: "+ee.getMessage());
            System.out.println("HelpSet: "+ helpsetfile + " not found");
          }
          return hs;
    }
    */
}   // Geometry_Design

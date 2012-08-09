/*
 * myFrame.java
 *
 * Created on December 16, 2003, 11:09 AM
 * Description: A window frame with WindowListener
 *
 * Note: compile in Java 1.1
 *
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

//package network2D ;

import java.awt.* ;
import java.awt.event.* ;        // doesn't automatically load with java.awt.*
import java.io.*;

public class myFrame extends Frame implements WindowListener {
    public boolean isClosed=false;
    /** Creates a new instance of myFrame */
    public myFrame() {
        this.addWindowListener(this) ;
    }
    
    public myFrame(String title) {
        this.addWindowListener(this) ;
        this.setTitle(title) ;
    }

    /** Invoked when the Window is set to be the active Window. Only a Frame or
     * a Dialog can be the active Window. The native windowing system may
     * denote the active Window or its children with special decorations, such
     * as a highlighted title bar. The active Window is always either the
     * focused Window, or the first Frame or Dialog that is an owner of the
     * focused Window.
     *
     */
    public void windowActivated(WindowEvent e) {
    }
    
    /** Invoked when a window has been closed as the result
     * of calling dispose on the window.
     *
     */
    public void windowClosed(WindowEvent e) {
        isClosed=true;
    }
    
    /** Invoked when the user attempts to close the window
     * from the window's system menu.  If the program does not
     * explicitly hide or dispose the window while processing
     * this event, the window close operation will be cancelled.
     *
     */
    public void windowClosing(WindowEvent e) {
        //System.out.println("closing") ;
        // delete vrml file if exists
        String osinfo = System.getProperty("os.name");
        String osarch = System.getProperty("os.arch");
    //System.out.println(osinfo+","+osarch);
        String vrmlfile = "" ;
        String htmlfile = "" ;
        if (osinfo.indexOf("Windows")>=0) {
            // delete html & vrml files in root dir if exist
            vrmlfile = "c:\\vrml_db" ;
            if ((new File(vrmlfile)).exists()) {
                // VRML file or directory exists
                boolean success = (new File(vrmlfile)).delete();
            }       
            htmlfile = "c:\\roaddesign.html" ;
            if ((new File(htmlfile)).exists()) {
                // HTML file or directory exists
                boolean success = (new File(htmlfile)).delete();
            }       
            
            String username = System.getProperty("user.name");
            vrmlfile = "c:\\Documents and Settings\\"+username+"\\Desktop\\vrml_db" ;
            htmlfile = "c:\\Documents and Settings\\"+username+"\\Desktop\\roaddesign.html" ;
        } else { //if (osinfo.indexOf("Linux")>=0){
            vrmlfile = "vrml_db" ;
            htmlfile = "roaddesign.html" ;
        }
        
        boolean exists = (new File(vrmlfile)).exists();
        if (exists) {
            // VRML file or directory exists
            boolean success = (new File(vrmlfile)).delete();
        }       
        exists = (new File(htmlfile)).exists();
        if (exists) {
            // HTML file or directory exists
            boolean success = (new File(htmlfile)).delete();
        }       
        
        this.dispose() ;
    }
    
    /** Invoked when a Window is no longer the active Window. Only a Frame or a
     * Dialog can be the active Window. The native windowing system may denote
     * the active Window or its children with special decorations, such as a
     * highlighted title bar. The active Window is always either the focused
     * Window, or the first Frame or Dialog that is an owner of the focused
     * Window.
     *
     */
    public void windowDeactivated(WindowEvent e) {
    }
    
    /** Invoked when a window is changed from a minimized
     * to a normal state.
     *
     */
    public void windowDeiconified(WindowEvent e) {
    }
    
    /** Invoked when a window is changed from a normal to a
     * minimized state. For many platforms, a minimized window
     * is displayed as the icon specified in the window's
     * iconImage property.
     * @see java.awt.Frame#setIconImage
     *
     */
    public void windowIconified(WindowEvent e) {
    }
    
    /** Invoked the first time a window is made visible.
     *
     */
    public void windowOpened(WindowEvent e) {
        isClosed=false;
        //try {
        //Thread.sleep(1000); // wait for a little while
        //} catch (InterruptedException ie){}
        this.toFront();
        String osinfo = System.getProperty("os.name");
        String osarch = System.getProperty("os.arch");
    //System.out.println(osinfo+","+osarch);
        String filename="" ;
        if (osinfo.indexOf("Windows")>=0) {
        //    filename = "c:\\roaddesign.html" ;
            // desktop
            String username = System.getProperty("user.name");
            filename =  "C:\\Documents and Settings\\"+username+"\\Desktop\\roaddesign.html" ;
        } else {    //if (osinfo.indexOf("Linux")>=0){
            filename = "roaddesign.html" ;
        }
        
        boolean exists = (new File(filename)).exists();
        if (!exists) {
            // File or directory does not exist
            // create HTML file for VRML browser
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(filename));
                out.write(getHTML());
                out.flush();
                out.close();
            } catch (IOException ioe) {
                String err_msg = ioe.toString() ;
                
                // 11/17/06 added
                if (err_msg.indexOf("FileNotFound")>0) {
                    // user desktop folder is not available
                    filename = "c:\\roaddesign.html" ;
                    // try save in root dir
                    try {
                        BufferedWriter out = new BufferedWriter(new FileWriter(filename));
                        out.write(getHTML());
                        out.flush();
                        out.close();
                        System.out.println("Save roaddesign.html in root directory!");
                    } catch (IOException ioe1) {
                        System.out.println("MyFrame:WindowOpened:"+filename+","+ioe1.toString()) ;
                    }   // try again
                    
                }   // if file not found
            }   // catch
            
        }   // if file not exists
    }   // windowopened
    
    public void setCenter(){
        Dimension screen = getToolkit().getDefaultToolkit().getScreenSize();
        double top = 0.5*(screen.getWidth()-this.getWidth());
        double left = 0.5*(screen.getHeight()-this.getHeight());
        int x = new Double(top).intValue();
        int y = new Double(left).intValue();
        this.setLocation(x, y);
        this.toFront();
    }
    public String getHTML(){
        String html_str="";
        try
        {
            InputStream in = this.getClass().getResourceAsStream("roaddesign.txt");
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader reader = new BufferedReader(isr);
            String line;
            while ((line = reader.readLine()) != null) {
            //    System.out.println(line);
                html_str += line ;
            }
            reader.close();
            isr.close() ;
            in.close();
        }
        catch (Exception e){
                //do nothing
            System.out.println(e.toString());
        } // try   
        return html_str ;
    }   // getHtml

}

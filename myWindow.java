/*
 * myWindow.java
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

public class myWindow extends Frame implements WindowListener {
    public boolean isClosed=false;
    /** Creates a new instance of myWindow */
    public myWindow() {
        this.addWindowListener(this) ;
    }
    
    public myWindow(String title) {
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
    }
    
    public void setCenter(){
        Dimension screen = getToolkit().getDefaultToolkit().getScreenSize();
        double top = 0.5*(screen.getWidth()-this.getWidth());
        double left = 0.5*(screen.getHeight()-this.getHeight());
        int x = new Double(top).intValue();
        int y = new Double(left).intValue();
        this.setLocation(x, y);
        this.toFront();
    }
}

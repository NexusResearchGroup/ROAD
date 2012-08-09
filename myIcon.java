/*
 * myIcon.java
 *
 * Created on November 3, 2006, 3:19 PM
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
import java.net.URL;
import java.io.*;

public class myIcon extends Panel {
    private Image img = null ;
    
    /** Creates a new instance of myIcon */
    public myIcon(String filename) {
        URL url = null;
        url = getClass().getResource(filename+".png");
        if (url==null) {
            url = getClass().getResource(filename+".PNG");
        }
        img = Toolkit.getDefaultToolkit().getImage(url);
        this.setBackground(new Color(200, 200, 200)) ;
    }
    
    public void paint(Graphics g) 
    {
        g.drawImage(img, 5, 5, 37, 37, this) ;
    }
    
    public Dimension preferredSize()
    {
	return(new Dimension(37,37));
    }
    
    public boolean mouseDown(Event e, int x, int y)
    {
        return true ;
    }
}

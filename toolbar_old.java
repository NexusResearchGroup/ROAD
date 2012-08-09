/*
 * toolbar.java
 *
 * Created on March 16, 2006, 8:25 PM
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

public class toolbar_old extends Panel
{
    int n;
    int status = -1;
    Color cstat = new Color(254,254,254);
    hDrawArea parent;

    toolbar_old()
    {
	setBackground(Color.lightGray);
    }

    public void paint(Graphics g) 
    {
	Rectangle r = bounds();


	for(int i=0;i<10;i++)
	{
	    if(i==status)
	    {
		g.setColor(new Color(192,192,192));
		g.fillRect(i*33, 0,32,32);
	    }
	    g.setColor(Color.black);
	    g.drawLine(i*33+32, 0,i*33+32, 32);
            URL url = null;
            switch (i) {
            case 0:
                url = getClass().getResource("Arrow.png");
                break ;
            case 1:
                url = getClass().getResource("ZoomIn.png");
                break ;
            case 2:
                url = getClass().getResource("ZoomOut.png");
                break ;
            case 3:
                url = getClass().getResource("Move.png");
                break ;
            case 4:
                url = getClass().getResource("Line.png");
                break ;
            case 5:
                url = getClass().getResource("Curve.png");
                break ;
            case 6:
                url = getClass().getResource("modify.png");
                break ;
            case 7:
                url = getClass().getResource("markerH.png");
                break ;
            case 8:
                url = getClass().getResource("Refresh1.png");
                break ;
            case 9:
                url = getClass().getResource("vertical_align1.png");
                break ;
             }
            Image img = Toolkit.getDefaultToolkit().getImage(url);
            g.drawImage(img, i*33+4,4,this) ;
	}   // i
        
    }

    public Dimension preferredSize()
    {
	return(new Dimension(32,32));
    }

    public boolean mouseDown(Event e, int x, int y)
    {
	if(x<10*33)
	{
	    int oldstatus = status;
	    status = x/33;
	    if(status<0) status = 0;
	    if(status>9) status = 9;
	    //if(oldstatus!=status)
	    //{
                String str = "" ;
                switch (status) {
                    case 0:
                        str = " Select Segment" ;
                        break ;
                    case 1:
                        str = " Zoom In" ;
                        break ;
                    case 2:
                        str = " Zoom Out" ;
                        break ;
                    case 3:
                        str = " Move" ;
                        break ;
                    case 4:
                        str = " Line Tool" ;
                        break ;
                    case 5:
                        str = " Curve Tool" ;
                        break ;
                    case 6:
                        str = " Modify End Point" ;
                        break ;
                    case 7:
                        str = " Set Station/Landmark" ;
                        break ;
                    case 8:
                        str = " Refresh" ;
                        break ;
                    case 9:
                        str = " Vertical Alignment" ;
                        break ;
                } // switch
		parent.newstatus(status, str);
		repaint();
	    //}
	}
	return(true);
    }
}

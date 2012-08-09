/*
 * toolbarV.java
 * Vertical curve design toolbar
 *
 * Created on March 23, 2006, 8:25 PM
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

public class toolbarV extends Panel
{
    final int NUM_ICONS=9;        // Number of tool items
    final int imgSize=40;       // default image size
    int status = -1;
    vDrawArea parent;
    boolean constructEnabled = true;    // vertical curve construction flag
    Image img[] = new Image[NUM_ICONS] ;

    // class construction
    toolbarV()
    {
	setBackground(Color.lightGray);
        for (int i=0; i<NUM_ICONS; i++) {
            URL url = null;
            switch (i) {
            case 0:
                if (constructEnabled) {
                    url = getImageResource("construction");
                } else {
                    url = getImageResource("constructdisabled");
                }
                break ;
            case 1:
                url = getImageResource("constructoff");
                break ;
            case 2:
                url = getImageResource("calcPVI");
                break ;
            case 3:
                url = getImageResource("modCurve");
                break ;
            case 4:
                url = getImageResource("elevation");
                break ;
            case 5:
                url = getImageResource("fillncut");
                break ;
            case 6:
                url = getImageResource("massdiagram");
                break ;
            case 7:
                url = getImageResource("report");
                break ;
            case 8:
                url = getImageResource("animation");
                break ;
             }
            img[i] = Toolkit.getDefaultToolkit().getImage(url);
        }
    }

    public void paint(Graphics g) 
    {
	Rectangle r = bounds();
        int i ;
	for(i=0;i<NUM_ICONS;i++)
	{
	    if(i==status)
	    {
		g.setColor(new Color(192,192,192));
		g.fillRect(i*(imgSize+1), 0,imgSize,imgSize);
	    }
	    g.setColor(Color.black);
	    g.drawLine(i*(imgSize+1)+imgSize, 0,i*(imgSize+1)+imgSize, imgSize);

            g.drawImage(img[i], i*(imgSize+1)+4,4,this) ;
            if (i!=status) {
                // button boundary depressed
                g.setColor(Color.white);
                g.drawLine(i*(imgSize+1)+3, 3,(i+1)*(imgSize+1)-4, 3);
                g.drawLine(i*(imgSize+1)+3, 3,i*(imgSize+1)+3, (imgSize-4));
                g.setColor(Color.black);
                g.drawLine(i*(imgSize+1)+3, (imgSize-4),(i+1)*(imgSize+1)-4, (imgSize-4));
                g.drawLine((i+1)*(imgSize+1)-4, 3,(i+1)*(imgSize+1)-4, (imgSize-4));
            } else {
                // button pressed
                // button boundary depressed
                g.setColor(Color.black);
                g.drawLine(i*(imgSize+1)+3, 3,(i+1)*(imgSize+1)-4, 3);
                g.drawLine(i*(imgSize+1)+3, 3,i*(imgSize+1)+3, (imgSize-4));
                g.setColor(Color.white);
                g.drawLine(i*(imgSize+1)+3, (imgSize-4),(i+1)*(imgSize+1)-4, (imgSize-4));
                g.drawLine((i+1)*(imgSize+1)-4, 3,(i+1)*(imgSize+1)-4, (imgSize-4));
           }
	}   // i
        g.setColor(Color.black);
        g.drawString("ITS Institute, University of Minnesota", i*(imgSize+1)+20, 25) ;
        
    }
    
    public URL getImageResource(String img) {
        URL url = null;
        //try {
            url = getClass().getResource(img+".png");
            if (url==null) {
                url = getClass().getResource(img+".PNG");
            }
        //} catch (IOException ioe) {
        //    System.out.println(url.toString());
        //}
        return url ;
    }
    
    public Dimension preferredSize()
    {
	return(new Dimension(imgSize,imgSize));
    }

    public void setConstructEnabled(boolean state) {
        constructEnabled = state ;
        repaint();
    }
    
    public boolean mouseDown(Event e, int x, int y)
    {
	if(x<NUM_ICONS*(imgSize+1))
	{
	    int oldstatus = status;
	    status = x/(imgSize+1);
	    if(status<0) status = 0;
	    if(status>NUM_ICONS) status = NUM_ICONS;
	    //if(oldstatus!=status)
	    //{
                String str = "" ;
                switch (status) {
                    case 0:
                        str = " Grade Construction ON" ;
                        break ;
                    case 1:
                        str = " Grade Construction OFF" ;
                        break ;
                    case 2:
                        str = " Generate Vertical Curves" ;
                        break ;
                    case 3:
                        str = " Modify Curve Length" ;
                        break ;
                    case 4:
                        str = " View Elevation Profile" ;
                        break ;
                    case 5:
                        str = " View Fill/Cut Profile" ;
                        break ;
                    case 6:
                        str = " View Mass Diagram" ;
                        break ;
                    case 7:
                        str = "";     //" Generate Report" ; defined in runThread0, vDrawArea
                        break ;
                    case 8:
                        str = " Create 3D Animation Model" ;
                        break ;
                  
                } // switch
                if (status==3) {
                    parent.setCurvelenEdit_flag = true ;  
                } else if (status==7) {
                    parent.setReport_flag = true ; 
                } else {
                    parent.newstatus(status, str);
                }
		repaint();
	    //}
	}
	return(true);
    }
}

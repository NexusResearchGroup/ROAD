/*
 * toolbar.java
 * Horizontal curve design toolbar.
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
import java.io.*;
//import javax.swing.*;

public class toolbar extends Panel
{
    int status = -1;        //. toolbar selecction
    hDrawArea parent;
    final int NUM_ICONS = 11 ;
    Image img[] = new Image[NUM_ICONS] ;
//    JToolTip myToolTip = new JToolTip();
    
    // class construction
    toolbar()
    {
	setBackground(Color.lightGray);
        for (int i=0; i<NUM_ICONS; i++) {
            URL url = null;
            switch (i) {
            case 0:
                //url = getClass().getResource("Arrow.png");
                url = getImageResource("Arrow");
                break ;
            case 1:
                //url = getClass().getResource("ZoomIn.png");
                url = getImageResource("ZoomIn");
                break ;
            case 2:
                //url = getClass().getResource("ZoomOut.png");
                url = getImageResource("ZoomOut");
                break ;
            case 3:
                //url = getClass().getResource("Move.png");
                url = getImageResource("Move");
                break ;
            case 4:
                //url = getClass().getResource("Line.png");
                url = getImageResource("Line");
                break ;
            case 5:
                //url = getClass().getResource("Curve.png");
                url = getImageResource("Curve");
                break ;
            case 6:
                //url = getClass().getResource("modify.png");
                url = getImageResource("modify");
                break ;
            case 7:
                //url = getClass().getResource("markerH.png");
                url = getImageResource("hAlign1");
                break ;
            case 8:
                //url = getClass().getResource("markerH.png");
                url = getImageResource("markerH");
                break ;
            case 9:
                //url = getClass().getResource("Refresh1.png");
                //url = getImageResource("Refresh1");
                url = getImageResource("markerInsert");  
                break ;
            case 10:
                //url = getClass().getResource("vertical_align1.png");
                url = getImageResource("vertical_align1");
                break ;
             }
            img[i] = Toolkit.getDefaultToolkit().getImage(url);
        }
    }

    public void paint(Graphics g) 
    {
	Rectangle r = bounds();


	for(int i=0;i<NUM_ICONS;i++)
	{
	    if(i==status)
	    {
		g.setColor(new Color(192,192,192));
		g.fillRect(i*33, 0,32,32);
	    }
	    //g.setColor(Color.black);
	    //g.drawLine(i*33+32, 0,i*33+32, 32);
            
            g.drawImage(img[i], i*33+4,4,this) ;
            
            if (i!=status) {
                // button boundary depressed
                g.setColor(Color.white);
                g.drawLine(i*33+1, 1,(i+1)*33-2, 1);
                g.drawLine(i*33+1, 1,i*33+1, 30);
                g.setColor(Color.black);
                g.drawLine(i*33+2, 30,(i+1)*33-2, 30);
                g.drawLine((i+1)*33-2, 2,(i+1)*33-2, 30);
            } else {
                // button pressed
                // button boundary depressed
                g.setColor(Color.black);
                g.drawLine(i*33+1, 1,(i+1)*33-2, 1);
                g.drawLine(i*33+1, 1,i*33+1, 30);
                g.setColor(Color.white);
                g.drawLine(i*33+2, 30,(i+1)*33-2, 30);
                g.drawLine((i+1)*33-2, 2,(i+1)*33-2, 30);
            }
	}   // i
        g.setColor(Color.black);
        g.drawString("MTO/CE/ITS Institute, University of Minnesota", 383, 20) ;
    }   // paint

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
    }   //getImageResource
    
    public Dimension preferredSize()
    {
	return(new Dimension(32,32));
    }

    public boolean mouseDown(Event e, int x, int y)
    {
	if(x<NUM_ICONS*33)
	{
	    int oldstatus = status;
	    status = x/33;
	    if(status<0) status = 0;
	    if(status>NUM_ICONS) status = NUM_ICONS;
	    //if(oldstatus!=status)
	    //{
                String str = "" ;
                switch (status) {
                    case 0:
                        str = " Select Segment" ;
                        //myToolTip.setToolTipText("Pointer tool");
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
                        str = " Align Horizontal Curve" ;
                        break ;
                    case 8:
                        str = " Set Station/Landmark" ;
                        break ;
                    case 9:
                        //str = " Refresh" ;
                        str = " Insert Station/Landmark" ;
                        break ;
                    case 10:
                        str = " Vertical Alignment" ;
                        break ;
                } // switch
                if (status==10) {
                    parent.setValign_flag = true ; 
                } else {
                    parent.newstatus(status, str);
                }
		repaint();
	    //}
	}
	return(true);
    }   //mouseDown
}

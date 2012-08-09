// draw borders

import java.awt.*;
import java.applet.*;

public class blacksquare extends Panel
{
    //int n;

    blacksquare()
    {
    }

    public void paint(Graphics g) 
    {
	Rectangle r;

	r = bounds();
	g.setColor(Color.blue);
	g.fillRect(0,0,r.width,r.height);
        //System.out.print("width=" + r.width + ", height=" + r.height) ;
    }

    public Dimension preferredSize()
    {
	return(new Dimension(2,2));
    }
}

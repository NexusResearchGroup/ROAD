// draw borders

import java.awt.*;

public class border extends Panel
{
    int n;
    Color _color ;

    border(int size, Color c)
    {
        n=size;
        _color=c;
    }

    public void paint(Graphics g) 
    {
	Rectangle r;

	r = bounds();
	g.setColor(_color);
	g.fillRect(0,0,r.width,r.height);
        //System.out.print("width=" + r.width + ", height=" + r.height) ;
    }

    public Dimension preferredSize()
    {
	return(new Dimension(n,n));
    }
}

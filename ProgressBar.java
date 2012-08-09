/*
 * ProgressBar.java
 *
 * Created on March 30, 2006, 10:30 AM
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

public class ProgressBar extends Frame
{
    private              int Count;
    private              int Max;
    private static final int FrameBottom = 24;

    public ProgressBar (String Title, int TotalItems)
    {
        super(Title);

        Count = 0;
        Max   = TotalItems;

        // Allowing this to be resized causes more trouble than it is worth
        // and the goal is for this to load and launch quickly!
        setResizable(false);

        setLayout(null);
        addNotify();
        resize (insets().left + insets().right + 380,
                insets().top + insets().bottom + FrameBottom);
    }

    public synchronized void show()
    {
        //move(50, 50);
        setCenter();
        super.show();
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
    
    // Update the count and then update the progress indicator.  If we have
    // updated the progress indicator once for each item, dispose of the
    // progress indicator.System.out.println("url="+u.toString());
    public void updateProgress ()
    {
        Count++;

        if (Count == Max) {
            //dispose();
            Count=0 ;
        }
        
        Dimension myDimension  = size();
        int       ProgressWidth   = (myDimension.width * Count)/ Max;

        // Fill the bar the appropriate percent full.
        Graphics g = this.getGraphics();
        g.setColor (new Color(0, 128, 0));
        g.fillRect (0, 0, ProgressWidth, myDimension.height);
        g.setColor (Color.white);
        g.fillRect (ProgressWidth,0,myDimension.width-ProgressWidth, myDimension.height);

    }


    // Paint the progress indicator.
    public void paint (Graphics g)
    {
    }

    public boolean handleEvent(Event event)
    {
        if (event.id == Event.WINDOW_DESTROY)
        {
            dispose();
            return true;
        }

        return super.handleEvent(event);
    }
}
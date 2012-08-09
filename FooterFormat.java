/*
 * FooterFormat.java
 *
 * Created on April 18, 2006, 11:24 AM
 */

/**
 *
 * @author  Chen-Fu Liao
 */
import java.awt.*;
import java.awt.font.*;
import java.awt.print.*;
import java.text.*;
import java.util.*;
public class FooterFormat extends PageFormat implements Printable {

    /**
     * The font we use for the footer.
     */
    private static final Font mFooterFont = new Font("Serif", Font.ITALIC, 10);
    /**
     * The amount of space at the bottom of the imageable area that we
     * reserve for the footer.
     */
    private static final float mFooterHeight = (float) (0.25 * 72);
    /**
     * The format for the date string shown in the footer.
     */
    private static final DateFormat mDateFormat = new SimpleDateFormat();
    /**
     * A formatted string describing when this instance was
     * created.
     */
    private String mDateStr = mDateFormat.format(new Date()).toString();
    /**
     * Tell the caller that the imageable area of the paper is shorter
     * than it actually is. We use the extra room at the bottom of the
     * page for our footer text.
     */
    public double getImageableHeight() {


        double imageableHeight = super.getImageableHeight() - mFooterHeight;
        if (imageableHeight < 0) imageableHeight = 0;
        return imageableHeight;

    }
    /**
     * Draws the footer text which has the following format:
     * <date>
     */
    public int print(Graphics g, PageFormat format, int pageIndex) {

        /* Make a copy of the passed in Graphics instance so
         * that we do not upset the caller's current Graphics
         * settings such as the current color and font.
         */
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setPaint(Color.black);
        g2d.setFont(mFooterFont);
        LineMetrics metrics = mFooterFont.getLineMetrics(mDateStr, g2d.getFontRenderContext());
        /* We will draw the footer at the bottom of the imageable
         * area. We subtract off the font's descent so that the bottoms
         * of descenders remain visable.
         */
        float y = (float) (super.getImageableY() + super.getImageableHeight()- metrics.getDescent() - metrics.getLeading());
        // Cast to an int because of printing bug in drawString(String, float, float)!
        g2d.drawString(mDateStr, (int) super.getImageableX(), (int)y);
        g2d.dispose();
        return Printable.PAGE_EXISTS;


    }

}



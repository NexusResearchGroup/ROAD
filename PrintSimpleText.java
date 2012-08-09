/*
 * PrintSimpleText.java
 *
 * Created on August 1, 2006, 2:27 PM
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
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.awt.Graphics2D ;
import java.text.*;

public class PrintSimpleText implements Printable {
    private AttributedString mStyledText ;
    
    /** Creates a new instance of PrintSimpleText */
    public PrintSimpleText(String myText) {
        mStyledText = new AttributedString(myText);
        /* Get the representation of the current printer and
        * the current print job.
        */
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        /* Build a book containing pairs of page painters (Printables)
        * and PageFormats. This example has a single page containing
        * text.
        */
        Book book = new Book();
        book.append(this, new PageFormat());
        /* Set the object to be printed (the Book) into the PrinterJob.
        * Doing this before bringing up the print dialog allows the
        * print dialog to correctly display the page range to be printed
        * and to dissallow any print settings not appropriate for the
        * pages to be printed.
        */
        printerJob.setPageable(book);
        /* Show the print dialog to the user. This is an optional step
        * and need not be done if the application wants to perform
        * 'quiet' printing. If the user cancels the print dialog then false
        * is returned. If true is returned we go ahead and print.
        */
        boolean doPrint = printerJob.printDialog();
        if (doPrint) {
            try {
                    printerJob.print();
            } catch (PrinterException exception) {
                    System.err.println("Printing error: " + exception);
            }
        }   // if
    }
    
    public int print(java.awt.Graphics g, java.awt.print.PageFormat format, int pageIndex) throws java.awt.print.PrinterException {
        Graphics2D g2d = (Graphics2D) g; 
        // Move the origin from the corner of the Paper to the corner of the imageable area.
        g2d.translate(format.getImageableX(), format.getImageableY()); 
        
        // Set the text color.
        g2d.setPaint(Color.black);
        
        //g2d.drawString(mStyledText.toString(), 0,0) ;
        
        // Use a LineBreakMeasurer instance to break our text into lines that fit the imageable area of the page.
        
        Point2D.Float pen = new Point2D.Float();
        AttributedCharacterIterator charIterator = mStyledText.getIterator();
        LineBreakMeasurer measurer = new LineBreakMeasurer(charIterator, g2d.getFontRenderContext());
        float wrappingWidth = (float) format.getImageableWidth(); 
        while (measurer.getPosition() < charIterator.getEndIndex()) {
            TextLayout layout = measurer.nextLayout(wrappingWidth);
            pen.y += layout.getAscent();
            float dx = layout.isLeftToRight()? 0 : (wrappingWidth - layout.getAdvance());
            layout.draw(g2d, pen.x + dx, pen.y);
            pen.y += layout.getDescent() + layout.getLeading() +1;
        }
        return Printable.PAGE_EXISTS;
    }
    
}

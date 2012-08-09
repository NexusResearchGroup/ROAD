import java.awt.*;
import javax.swing.*;
import java.awt.print.*;
import java.awt.font.*;
import java.text.*;
import java.util.*;
/** A simple utility class that lets you very simply print
 *  an arbitrary component. Just pass the component to the
 *  PrintUtilities.printComponent. The component you want to
 *  print doesn't need a print method and doesn't have to
 *  implement any interface or do anything special at all.
 *  <P>
 *  If you are going to be printing many times, it is marginally more 
 *  efficient to first do the following:
 *  <PRE>
 *    PrintUtilities printHelper = new PrintUtilities(theComponent);
 *  </PRE>
 *  then later do printHelper.print(). But this is a very tiny
 *  difference, so in most cases just do the simpler
 *  PrintUtilities.printComponent(componentToBePrinted).
 *
 *  7/99 Marty Hall, http://www.apl.jhu.edu/~hall/java/
 *  May be freely used or adapted.
 */

public class PrintUtilities implements Printable {
  private Component componentToBePrinted;
  private PageFormat myPageFormat = new PageFormat();
  private PrinterJob printJob = PrinterJob.getPrinterJob();

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


        double imageableHeight = this.getImageableHeight() - mFooterHeight;
        if (imageableHeight < 0) imageableHeight = 0;
        return imageableHeight;

    }

  public static void printComponent(Component c) { //, PageFormat format) {
  //    this.myPageFormat = format ;
      new PrintUtilities(c).print();
  }
  
  public void printPageSetup() { 
  //    this.myPageFormat = format ;
      myPageFormat = printJob.pageDialog(myPageFormat) ;
      
  }
  
  public PrintUtilities(Component componentToBePrinted) {
    this.componentToBePrinted = componentToBePrinted;
  }
  
  public void print() {
//    PrinterJob printJob = PrinterJob.getPrinterJob();
    printJob.setPrintable(this);
    
    //Book bk = new Book();
    //bk.append(this, myPageFormat);
    //printJob.setPageable(bk) ;
    
    if (printJob.printDialog())
      try {
        printJob.print();
      } catch(PrinterException pe) {
        System.out.println("Error printing: " + pe);
      }
  }

  public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
    if (pageIndex > 0) {
        return(NO_SUCH_PAGE);
    } else {
      Graphics2D g2d = (Graphics2D)g;
      g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
/*
      g2d.setPaint(Color.black);
        g2d.setFont(mFooterFont);
        LineMetrics metrics = mFooterFont.getLineMetrics(mDateStr, g2d.getFontRenderContext());
        // We will draw the footer at the bottom of the imageable
        // area. We subtract off the font's descent so that the bottoms
        // of descenders remain visable.
        //
        float y = (float) (pageFormat.getImageableY() + pageFormat.getImageableHeight()- metrics.getDescent() - metrics.getLeading());
        // Cast to an int because of printing bug in drawString(String, float, float)!
        g2d.drawString(mDateStr, (int) pageFormat.getImageableX(), (int)y);
        System.out.println(mDateStr + ", " + y + ", " + pageFormat.getImageableX());
*/
      disableDoubleBuffering(componentToBePrinted);
      componentToBePrinted.paint(g2d);
      enableDoubleBuffering(componentToBePrinted);
      return(PAGE_EXISTS);
    }
  }

  /** The speed and quality of printing suffers dramatically if
   *  any of the containers have double buffering turned on.
   *  So this turns if off globally.
   *  @see enableDoubleBuffering
   */
  public static void disableDoubleBuffering(Component c) {
    RepaintManager currentManager = RepaintManager.currentManager(c);
    currentManager.setDoubleBufferingEnabled(false);
  }

  /** Re-enables double buffering globally. */
  
  public static void enableDoubleBuffering(Component c) {
    RepaintManager currentManager = RepaintManager.currentManager(c);
    currentManager.setDoubleBufferingEnabled(true);
  }
}

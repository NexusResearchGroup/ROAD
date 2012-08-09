//package com.genedavissoftware.printing.backend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

//import com.genedavissoftware.printing.*;

/**
 * This class is intended to handle printing requests from children of 
 * GDSPrinting. The process is to set the text as html (setNewText()) 
 * and then call print().
 * 
 * Internally the html is reformated as the user chooses bigger or smaller
 * buttons in the preview window.
 */
public class PrintPreview extends JFrame implements Printable {
	public static PrintPreview pp = null;
	
	static int fontSize = 0;
	static boolean doubleBuffered = true;

	Panel pnMainPanel;
	Button btBiggerJB;
	Button btSmallerJB;
	Button btCancelJB;
	
	JScrollPane editorScrollPane;
	JEditorPane editorPane;
	Button btOkayJB;
	
	////////////////////////////////////////
	// need generic component for printing
	/**What ever component you print, it is placed here to await printing.*/
	static Component comp;
	
	//This is the raw data to print if using the internal JEditorPane for the
	//component to print.
	static String html = "";

	//print methods...

	/**
	 * This is one of the print methods called by the classes that the programmer is
	 * using to interface with the PrintPreview.<br>
	 * <br>
	 */
	public static void print(String text) throws GDSPrintException {
		PrintPreview.pp = new PrintPreview();
		
		//TODO Set the text to be printed
		comp = PrintPreview.pp.createComponent();

		//The text should come in as HTML, whether in a body, or
		//just text intermingled with HTML tags.
                fontSize = -1 ;
		PrintPreview.pp.setNewText(text);
		
		PrintPreview.pp.showPreviewWindow();
                PrintPreview.pp.setCenter() ;
                
		
        SwingUtilities.invokeLater(new Runnable() {
    		public void run() {
    			PrintPreview.pp.setVisible( true );
    		}
        });
	}
	
        public void setCenter() {
                
            Dimension screen = getToolkit().getDefaultToolkit().getScreenSize();
            double top = 0.5*(screen.getWidth()-this.getWidth());
            double left = 0.5*(screen.getHeight()-this.getHeight());
            int x = new Double(top).intValue();
            int y = new Double(left).intValue();

            PrintPreview.pp.setLocation(x, y);            
        }
	
	/**
	 * This is one of the print methods called by the classes that the programmer is
	 * using to interface with the PrintPreview.<br>
	 * <br>
	 * Eventually this Component will need to implement some interface to allow
	 * for increasing and decreasing of text size in a manner the programmer intends.
	 * For now, however, I'm just going to deactivate the "Bigger" and "Smaller"
	 * buttons.<br>
	 */
	public static void print(Component comp) throws GDSPrintException {
		pp = new PrintPreview();
		
		//TODO Set the Component to be printed
		
        SwingUtilities.invokeLater(new Runnable() {
    		public void run() {
    			pp.setVisible( true );
    		}
        });
	}
	

	//constructor

	/**
	 * Default constructor.<br>
	 * <br>
	 * Self contained printable editor pane for previewing text and 
	 * images that will be displayed for printing.
	 */
	public PrintPreview() {
	   super( "Print Preview" );
	}

	//populate 'comp'

	/**
	 * Creates a default component to print. The default is a JEditorPane.<br>
	 */
	public JEditorPane createComponent() {
	
            ///////////////////////////////////
            //   Time to create the display pane
            editorPane = new JEditorPane();
            //editorPane.setContentType("text/plain");
            editorPane.setContentType("text/html");
            editorPane.setEditable(false);		


            return editorPane;
	}
	

	// set up the preview JFrame

	/**
	 * Creates the window for the component to be previewed in. The 'comp'
	 * variable should be filled first, before the method is called.
	 */
	public void showPreviewWindow() {
		   
               setSize(new Dimension(700, 550));
               setResizable(false);

               pnMainPanel = new Panel();
               GridBagLayout gbMainPanel = new GridBagLayout();
               GridBagConstraints gbcMainPanel = new GridBagConstraints();
               pnMainPanel.setLayout( gbMainPanel );

               btSmallerJB = new Button( "Zoom Out"  );
               btSmallerJB.addActionListener(new SmallerAL());
               gbcMainPanel.gridx = 0;
               gbcMainPanel.gridy = 0;
               gbcMainPanel.gridwidth = 4;
               gbcMainPanel.gridheight = 2;
               gbcMainPanel.fill = GridBagConstraints.NONE;
               gbcMainPanel.weightx = 0;
               gbcMainPanel.weighty = 1;
               gbcMainPanel.anchor = GridBagConstraints.NORTHWEST;
               gbcMainPanel.insets = new Insets( 5,5,5,0 );
               gbMainPanel.setConstraints( btSmallerJB, gbcMainPanel );
               pnMainPanel.add( btSmallerJB );

               btBiggerJB = new Button( "Zoom In"  );
               btBiggerJB.addActionListener(new BiggerAL());
               gbcMainPanel.gridx = 4;
               gbcMainPanel.gridy = 0;
               gbcMainPanel.gridwidth = 4;
               gbcMainPanel.gridheight = 2;
               gbcMainPanel.fill = GridBagConstraints.NONE;
               gbcMainPanel.weightx = 0;
               gbcMainPanel.weighty = 1;
               gbcMainPanel.anchor = GridBagConstraints.NORTHWEST;
               gbcMainPanel.insets = new Insets( 5,5,5,0 );
               gbMainPanel.setConstraints( btBiggerJB, gbcMainPanel );
               pnMainPanel.add( btBiggerJB );


               btOkayJB = new Button( " Print ..." );
               btOkayJB.addActionListener(new OkayAL());
               gbcMainPanel.gridx = 27;
               gbcMainPanel.gridy = 28;
               gbcMainPanel.gridwidth = 7;
               gbcMainPanel.gridheight = 2;
               gbcMainPanel.fill = GridBagConstraints.NONE;
               gbcMainPanel.weightx = 1;
               gbcMainPanel.weighty = 1;
               gbcMainPanel.anchor = GridBagConstraints.SOUTHEAST;
               gbcMainPanel.insets = new Insets( 5,0,5,5 );
               gbMainPanel.setConstraints( btOkayJB, gbcMainPanel );
               pnMainPanel.add( btOkayJB );


               btCancelJB = new Button( "Cancel"  );
               btCancelJB.addActionListener(new CancelAL());
               gbcMainPanel.gridx = 34;
               gbcMainPanel.gridy = 28;
               gbcMainPanel.gridwidth = 4;
               gbcMainPanel.gridheight = 2;
               gbcMainPanel.fill = GridBagConstraints.NONE;
               gbcMainPanel.weightx = 0;
               gbcMainPanel.weighty = 1;
               gbcMainPanel.anchor = GridBagConstraints.SOUTHEAST;
               gbcMainPanel.insets = new Insets( 5,0,5,5 );
               gbMainPanel.setConstraints( btCancelJB, gbcMainPanel );
               pnMainPanel.add( btCancelJB );



               /////////////////////////////////////////////
               //   Put the editor pane in a scroll pane.

               // using the comp variable. This may have been supplied by 
               // the users of the API.
               editorScrollPane = new JScrollPane(comp);
               editorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
               editorScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
               editorScrollPane.setPreferredSize(new Dimension(500, 400));



               JScrollPane scpPanel1 = new JScrollPane( editorScrollPane );
               gbcMainPanel.gridx = 0;
               gbcMainPanel.gridy = 2;
               gbcMainPanel.gridwidth = 38;
               gbcMainPanel.gridheight = 26;
               gbcMainPanel.fill = GridBagConstraints.BOTH;
               gbcMainPanel.weightx = 2;
               gbcMainPanel.weighty = 2;
               gbcMainPanel.anchor = GridBagConstraints.CENTER;
               gbcMainPanel.insets = new Insets( 0,5,0,5 );
               gbMainPanel.setConstraints( scpPanel1, gbcMainPanel );
               pnMainPanel.add( scpPanel1 );

               setDefaultCloseOperation( DISPOSE_ON_CLOSE );

               setContentPane( pnMainPanel );
               pack();
	}

	
	////////////////////////////////////
	//  Utility methods
	
	public void setNewText(String html) {
		PrintPreview.html = html;
		
		JEditorPane jep = (JEditorPane) comp;
		jep.setText( resizeHtml() );
	}
	
	
	/*
	 * Update the size of the text in the html.
	 * WARNING: Here there be regex.
	 */
	public String resizeHtml() {//FIXME this needs to have more generic search and replace.
		String retval = PrintPreview.html;
		
		String header = "";
		String body = "";
		
		//System.out.println("font size = "+fontSize);
		
		if ((fontSize+1) < 0) header = "<font size=\"" + (fontSize+1) + "\">";//'-' sign shows up by itself, don't want an extra '+' sign
		else header = "<font size=\"+" + (fontSize+1) + "\">";
		
		if (fontSize < 0) body = "<font size=\"" + (fontSize) + "\">";//'-' sign shows up by itself, don't want an extra '+' sign
		else body = "<font size=\"+"+ fontSize +"\">";

		//FIXME replace all headers and bodies. This is real basic and won't be good enough when raw html is supported.
		Pattern pathead = Pattern.compile("<font size=\"\\+1\">");
		Matcher mathead = pathead.matcher(retval);
		retval = mathead.replaceAll(header);

		Pattern patbod = Pattern.compile("<font size=\"\\+0\">");
		Matcher matbod = patbod.matcher(retval);
		retval = matbod.replaceAll( (body) );


		//System.out.println("retval-->"+retval+"<--");
		return retval;
	}
	
	
	////////////////////////////////////////
	//  Printable implementation
	
	/**
	 * Printable's implementation
	 */
	public int print(Graphics g, PageFormat pf, int pageIndex) {
		//assume the page exists until proven otherwise
		int retval = Printable.PAGE_EXISTS;
		
		
		//Decide which pages exist, and how big they are.
		Dimension dim = comp.getSize();
		double height = dim.getHeight();
		double pageHeight = pf.getImageableHeight();
		int pageCount = (int) Math.ceil(height / pageHeight);
		
		
		//The first page is numbered '0'
		if ((pageIndex+1) > pageCount){
			retval = Printable.NO_SUCH_PAGE;
		} else {
			disableDoubleBuffering();//see if we can give printing a speed bump
			
			//setting up the Graphics object for printing
			g.translate((int)(pf.getImageableX()), (int)(pf.getImageableY()));
			g.translate(0, (int)(-pageIndex * pageHeight));
			
	    		//populate the Graphics object from HelloPrint's paint() method
			editorPane.paint(g);
			
			resetDoubleBuffering();//put things back the way we found them
		}
		
		return retval;
	}
	

	/**
	 * Disabling double buffering speeds printing
	 */
	public void disableDoubleBuffering() {
		RepaintManager cm = RepaintManager.currentManager(this);
		doubleBuffered = cm.isDoubleBufferingEnabled();
		if (doubleBuffered) cm.setDoubleBufferingEnabled(false);
	}
	
	/**
	 * Turn back on double buffering if it was ever on.
	 */
	public void resetDoubleBuffering() {
		RepaintManager cm = RepaintManager.currentManager(this);
		cm.setDoubleBufferingEnabled(doubleBuffered);
	}
	
	
	
	///////////////////////////////////////
	//  Event Listeners for the buttons
	
	class BiggerAL implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			if (fontSize < 2) fontSize++;
			setNewText(PrintPreview.html);
		}
	}
	
	class SmallerAL implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			if (fontSize > -3) fontSize--;
			setNewText(PrintPreview.html);
		}
	}
	
	class OkayAL implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
	        //invokeLater() is used as a workaround for a java
	        //gui bug.
	        SwingUtilities.invokeLater(new Runnable() {
        		public void run() {
        			try {
           			
           			//get a PrintJob
      				PrinterJob pjob = PrinterJob.getPrinterJob();
        				//set a HelloPrint as the target to print
      				pjob.setPrintable(pp, pjob.defaultPage());
      				//get the print dialog, continue if canel
      				//is not clicked
        				if (pjob.printDialog()) {
        					//print the target (HelloPrint)
        					pjob.print();
        				}
        			} catch (Exception e) {
    	        			e.printStackTrace();
        			}
        		}
	        });
	        
	        pp.setVisible(false);
		}
	}
	
	class CancelAL implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
	        SwingUtilities.invokeLater(new Runnable() {
	    		public void run() {
	    			pp.setVisible(false);
	    		}
	        });
		}
	}
} 

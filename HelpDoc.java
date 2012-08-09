/*
 * HelpDoc.java
 *
 * Created on November 21, 2006, 2:22 PM
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

import java.net.*;
import javax.help.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class HelpDoc {
   myWindow f;
   myWindow frame_msgbox ;
    
    /** Creates a new instance of HelpDoc */
    public HelpDoc() {
        f = new myWindow("User's Guide");
        JButton btn = new JButton("OK");
        JButton btn_cancel = new JButton("Cancel");
        JButton btn_help = new JButton("Help");

        try {
            // 1. create HelpSet and HelpBroker objects
            HelpSet hs = getHelpSet("javahelp/road.hs"); 
            HelpBroker hb = hs.createHelpBroker();

            // 2. assign help to components, Context Sensitive Help (CSH)
            CSH.setHelpIDString(btn, "Introduction");   // startup page
            // 3. handle events
            btn.addActionListener(new CSH.DisplayHelpFromSource(hb));
        } catch (Exception e){
            popMessageBox("JavaHelp", e.getMessage()) ;
        }
        
        ActionListener al = new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    f.dispose() ;
                } // actionPerformed
            } ; // ActionListener        btn.addActionListener(
        btn.addActionListener(al) ;
        btn_cancel.addActionListener(al) ; // btn cancel
        btn_help.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent aev) {
                    popMessageBox("JavaHelp", 
                    "Note: You need to have JavaHelp package\n" +  
                    "(jh.jar, jhall.jar, jhbasic.jar, jsearch.jar)\n" +
                    "installed in your ..\\Java\\jre\\lib\\ext\\ directory\n" + 
                    "in order to view the Users' Guide. JavaHelp is\n" + 
                    "available at http://java.sun.com/products/javahelp/.") ;
                } // actionPerformed
            }  // ActionListener        btn.addActionListener(
        ) ;
        
        f.setSize(220,90);
        f.setCenter() ;
        f.setLayout(new GridBagLayout()) ;
        // Create a constrains object, and specify default values
        GridBagConstraints c = new GridBagConstraints() ;
        c.fill = GridBagConstraints.BOTH ; // component grows in both directions
        c.weightx = 1.0 ; c.weighty = 1.0 ;

        c.gridx = 0 ; c.gridy = 0; c.gridwidth = 3 ; c.gridheight = 2 ;
        c.insets = new Insets(2,2,0,2) ; // 5-pixel margins on all sides

        // attach menubar to frame, set its size, and make it visible
        f.add(new JLabel(" Launch JavaHelp?"), c) ;
        c.gridx = 0 ; c.gridy = 2; c.gridwidth = 1 ; c.gridheight = 1 ;
        c.insets = new Insets(2,2,2,2) ; // 5-pixel margins on all sides
        f.add(btn, c) ;
        c.insets = new Insets(2,2,2,2) ; // 5-pixel margins on all sides
        c.gridx = 1 ;
        f.add(btn_cancel, c) ;
        c.gridx = 2 ;
        f.add(btn_help, c) ;
        f.setVisible(true);
        f.setResizable(false) ;
        
    }

    /**
    * find the helpset file and create a HelpSet object
    */
    public HelpSet getHelpSet(String helpsetfile) {
        HelpSet hs = null;
        ClassLoader cl = this.getClass().getClassLoader();
        try {
            URL hsURL = HelpSet.findHelpSet(cl, helpsetfile);
            hs = new HelpSet(null, hsURL);
        } catch(Exception ee) {
            System.out.println("HelpSet: "+ee.getMessage());
            System.out.println("HelpSet: "+ helpsetfile + " not found");
        }
        return hs;
    }    
    
    private void popMessageBox(String caption, String message) {
        // open a frame
        frame_msgbox = new myWindow(caption) ;
        //frame_msgbox.setLocation(400,50) ;
        frame_msgbox.setSize(310,150) ;
        frame_msgbox.setCenter() ;
        frame_msgbox.validate() ;
        frame_msgbox.setVisible(true) ;
        frame_msgbox.setResizable(false);
        //frame_msgbox.show() ;
/*
        ActionListener frame_msgbox_ok_listener = new ActionListener() {
            public void actionPerformed(ActionEvent aev) {
                
                frame_msgbox.dispose() ;
            }
        } ;
*/
        frame_msgbox.setLayout(new BorderLayout(1,1)) ;
        TextArea myTitle = new TextArea(message, 3, 60) ;
        myTitle.setFont(new Font("SansSerif", Font.PLAIN , 12)) ;
        myTitle.setForeground(new Color(0,0,218)) ;
        frame_msgbox.setBackground(new Color(200, 200, 200)) ;
        frame_msgbox.add("Center",myTitle) ;
        
        //Button btn_ok = new Button(" OK ") ;
        //frame_msgbox.add("South",btn_ok) ;
        //btn_ok.addActionListener(frame_msgbox_ok_listener) ;
        //frame_msgbox.invalidate();
        frame_msgbox.show() ;
        frame_msgbox.toFront() ;
    } // popMessageBox

}

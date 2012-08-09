/*
 * PrintText.java
 *
 * Created on August 1, 2006, 4:05 PM
 */

/**
 *
 * @author  Chen-Fu Liao
 */
import java.util.regex.*;

public class PrintText {
    
    /** Creates a new instance of PrintText */
    public PrintText() {
    }
	/**
	 * Print the string.
	 */
    public void print(String s) throws GDSPrintException {
            //first prep the string for the printing
            String toPrint = prepareString(s);
            //now send the prep'ed string to the print preview
            PrintPreview.print(toPrint); 
    }


    /**
     * Internally all text is converted to HTML
     */
    private String prepareString(String s) {
            String retval = s;

            //replace all < and > with &lt; and &gt; symbols
            Pattern pat1 = Pattern.compile("<");
            Matcher mat1 = pat1.matcher(retval);
            retval = mat1.replaceAll("&lt;");

            Pattern pat2 = Pattern.compile(">");
            Matcher mat2 = pat1.matcher(retval);
            retval = mat2.replaceAll("&gt;");

            //Add <font size="0"> to the beginning and </font> to the ending

            return ("<body bgcolor=\"#f0f0f0\"><pre><font size=\"+0\">" 
            + "<font color=\"blue\">" + retval + "</font></font></pre></body>");
    }
    
    public static String StrFormat(int flag, String str, int length) {
        int len = str.length() ;
        String ret_str ;
        if (len>length) {
            ret_str = str ;
        } else {
            String space = "" ;
            for (int i=0; i<length-len; i++) {
                space += " " ;
            }
            if (flag==0) {
                // left align
                ret_str = str+space ;
            } else {
                // right align
                ret_str = space+str ;
            }
        }   // if len > length ?
        return ret_str ;
    }   // StrFormat
    
}

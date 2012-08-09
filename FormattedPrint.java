 /*
  
  Copyright (c) 2005, Terrance Gene Davis
  All rights reserved.
  
  Redistribution and use in source and binary forms, with or without 
  modification, are permitted provided that the following conditions 
  are met:
  
  *   Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
  *   Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
  *   Neither the name of the Gene Davis Software nor the names of its
      contributors may be used to endorse or promote products derived from
      this software without specific prior written permission.
  
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.

*/

//package com.genedavissoftware.printing;

import java.net.*;

//import com.genedavissoftware.printing.backend.PrintPreview;

/**
 * Build a formatted document and print it. types of input possible are
 * plain text, bold text, italicized text, section titles, document titles
 * tables of text and images.<br>
 * <br>
 * The image and table options still need some work, the rest of this class should
 * be functional.<br>
 * <br>
 * There is the issue of what to do with multiple spaces. The internal HTML makes
 * extra spaces go away. What should be done? have an addSpace() method? I haven't
 * decided and will gladly take input.<br>
 */
public class FormattedPrint extends GDSPrinting {
	
	String doc = "";
	
	
	/**Testing/example for using FormattedPrint*/
	public static void main(String args[]) {
		FormattedPrint formatted = new FormattedPrint();
		
		//Making a sample document
		formatted.addDocumentTitle("Our Lovely Title");
		formatted.addImage( ClassLoader.getSystemResource("images/woman.gif") );
		formatted.addSectionTitle("Our First Section");
		formatted.addPlainText("Just sample text Just sample text Just sample text Just sample text Just sample text Just sample text Just sample text Just sample text Just sample text Just sample text Just sample text Just sample text Just sample text Just sample text Just sample text ");
		formatted.addLineBreak();
		formatted.addPlainText("Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text ");
		formatted.addItalicsText("Just some more italicized sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text ");
		formatted.addBoldText("Just some more italicized sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text ");
		formatted.addPlainText("Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text Just some more sample text ");
		
		formatted.addLineBreak();
		formatted.addLineBreak();
		formatted.addSectionTitle("Our First Section");
		
		String[] testA = {"this","is","a","ringer","of","a","test","!!!!!"};
		formatted.addTable(2,4,testA);
		formatted.addLineBreak();
		formatted.addLineBreak();
		formatted.addPlainText("Thus concludes our nifty test.");
		
		
		
		try { formatted.print(); } catch (Exception e) {}
	}
	
	
	/**
	 * Print the currently maintained internal document.
	 */
	public void print() throws GDSPrintException  {
		PrintPreview.print(doc);
	}
	
	/**
	 * Empty the internal document in preparation for filling with 
	 * new text.
	 */
	public void clear() {
		doc = "";
	}
	
	
	
	
	
	
	/**
	 * Add a line break to the document. Only titles (section and Document)
	 * do this automatically.
	 */
	public void addLineBreak() {
		doc += "<br>";
	}
	
	/**
	 * Add plain text to the document.
	 */
	public void addPlainText(String text) {
		doc += "<font size=\"+0\">" + text + "</font>";
	}
	
	/**
	 * Adds italicized text. Some JVMs have a bug that will prevent 
	 * the italicized text from being italicized.
	 */
	public void addItalicsText(String text) {
		doc += "<font size=\"+0\"><i>" + text + "</i></font>";
		}
	
	/**
	 * Add bold text to the document.
	 */
	public void addBoldText(String text) {
		doc += "<font size=\"+0\"><b>" + text + "</b></font>";
		}
	
	/**
	 * Add a section title to the document. These titles are smaller
	 * than the document titles.
	 */
	public void addSectionTitle(String text) {
		doc += "<font size=\"+1\"><b>" + text + "</b></font><br>";
		}
	
	/**
	 * Add a Title sized String to the document.
	 */
	public void addDocumentTitle(String text) {
		doc += "<h1>" + text + "</h1>";
		}
	
	/**
	 * Add in a table. The columns and rows of the table are based
	 * off the parameters that are passed in . The String[] array 
	 * fills the table from left to right and top to bottom. Any left
	 * over Strings are silently ignored. Any extra fields are
	 * filled with empty strings.
	 */
	public void addTable(int cols, int rows, String[] data) {
		
		doc += "<table border=\"0\">";
		  
		int count = 0;
		
		for (int y=0;y<rows;y++){
			doc += "<tr>";
			for (int x=0;x<cols;x++){
				doc += "<td>";
				if (data != null && count < data.length) doc += "<font size=\"+0\">"+data[count]+"<font size=\"+0\">";
				count++;
				doc += "</td>";
				}
			doc += "</tr>";
		}
		
		  
		  doc += "</table></font>";
	}
	
	/**
	 * The URL to the image that is to be added to the document.
	 * The method ClassLoader.getResource(String name) provides
	 * a handy method for generating this URL.<br>
	 * <br>
	 * For instance:<br>
	 * <br>
	 * ClassLoader.getSystemResource("images/some.jpg");<br>
	 * <br>
	 * Would grab the URL of the the image 'some.jpg' in the
	 * folder 'images' in the directory that your app was started 
	 * in.<br>
	 */
	public void addImage(URL image) {
		doc += "<img src=\""+image.toString()+"\" border=\"0\" align=\"right\">";
	}
}

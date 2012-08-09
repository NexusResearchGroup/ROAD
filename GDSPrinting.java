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

/**
 * The parent of all classes used for printing. It provides methods for
 * displaying footers, headers and page numbers on the top or bottom of 
 * the page.<br>
 * <br>
 * This will likely be the last class to be finished, because placement
 * on the page, and makeing the display look good in the preview will take
 * lots of work. I'm sure I'll finish it eventually
 */
public abstract class GDSPrinting {
	/**Align left*/
	public static final int LEFT = 1;
	/**Align center*/
	public static final int CENTER = 2;
	/**Align right*/
	public static final int RIGHT = 3;
	

	/**Align top*/
	public static final int TOP = 4;
	/**Align bottom*/
	public static final int BOTTOM = 5;

	
	
	
	/**
	 * Change the initial page number. The default is 1.
	 */
	public void setInitialPageNumber(int page) {}

	/**
	 * Choose whether to display the current page number. The 
	 * default is false.
	 */
	public void showPageNumber(boolean show) {}

	/**
	 * Change the horizontal alignment of the page number. Choose from:<br>
	 * <br>
	 * GDSPrinting.LEFT<br>
	 * GDSPrinting.CENTER<br>
	 * GDSPrinting.RIGHT<br>
	 * <br>
	 * The default is GDSPrinting.RIGHT.<br>
	 */
	public void setPageNumberHorizontalAlignment(int align) {}

	/**
	 * Change the vertical alignment of the page number. Choose from:<br>
	 * <br>
	 * GDSPrinting.TOP<br>
	 * GDSPrinting.BOTTOM<br>
	 * <br>
	 * The default is GDSPrinting.BOTTOM.<br>
	 */
	public void setPageNumberVerticalAlignment(int align) {}

	
	
	
	/**
	 * Set the text for the header. The default is an empty String.
	 */
	public void setHeader(String head) {} 

	/**
	 * Choose whether to display the current header. The 
	 * default is false.
	 */
	public void showHeader(boolean show) {}

	/**
	 * Change the horizontal alignment of the header. Choose from:<br>
	 * <br>
	 * GDSPrinting.LEFT<br>
	 * GDSPrinting.CENTER<br>
	 * GDSPrinting.RIGHT<br>
	 * <br>
	 * The default is GDSPrinting.RIGHT.<br>
	 */
	public void setHeaderAlignment(int align) {}

	
	
	
	/**
	 * Set the text for the footer. The default is an empty String.
	 */
	public void setFooter(String head) {}

	/**
	 * Choose whether to display the current footer. The 
	 * default is false.
	 */
	public void showFooter(boolean show) {}

	/**
	 * Change the horizontal alignment of the footer. Choose from:<br>
	 * <br>
	 * GDSPrinting.LEFT<br>
	 * GDSPrinting.CENTER<br>
	 * GDSPrinting.RIGHT<br>
	 * <br>
	 * The default is GDSPrinting.RIGHT.<br>
	 */
	public void setFooterAlignment(int align) {}
}

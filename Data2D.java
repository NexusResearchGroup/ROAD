/*
 * Data2D.java
 * Data2D class define the horizontal line/curve design segments.
 * radius > 0 indicate a curve segment, and
 * radius <0 indicate a line segment
 *
 * Created on March 17, 2006, 12:08 PM
 * Modified 11/8/06
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
import java.lang.Math.*;

public class Data2D {
    private mPointF point1 ;  // line segment start point or center of a curve
    private mPointF point2 ;  // line segment end point
    private float radius = -1 ;                // radius, -1  for line data
    private Color penColor ;
    private float penWidth ;
    private boolean selected ;      // flag to indicate if segment is selected
    private boolean deleted = false;       // flag to indicate if this segment is deleted, 4/21/06 added
    private Color highlightedColor ;    // complimentary color of penColor

    /** Creates a new instance of Data2D */
    public Data2D() {
        point1 = new mPointF(0f,0f) ;
        point2 = new mPointF(0f,0f) ;
        selected = false ;
        deleted = false ;
    }
    public void saveData(mPointF p1, mPointF p2, Color _penColor, float _penWidth) {
        point1 = p1 ;
        point2 = p2 ;
        radius = -1 ;
        penColor = _penColor ;
        penWidth = _penWidth ;
        selected = false ;
        highlightedColor = new Color(255 - penColor.getRed(), 255 - penColor.getGreen(), 
            255 - penColor.getBlue()) ;
    }
    public void saveData(mPoint p1 , mPoint p2 , Color _penColor , float _penWidth) {
        point1.X = p1.X ;
        point1.Y = p1.Y ;
        point2.X = p2.X ;
        point2.Y = p2.Y ;
        radius = -1 ;
        penColor = _penColor ;
        penWidth = _penWidth ;
        selected = false ;
        highlightedColor = new Color(255 - penColor.getRed(), 255 - penColor.getGreen(), 255 - penColor.getBlue()) ;
    }   
    public void saveData( mPointF p1, float r , Color _penColor , float _penWidth ) {
        point1 = p1 ;
        radius = r ;
        point2 = new mPointF(0f, 0f) ;
        penColor = _penColor ;
        penWidth = _penWidth ;
        selected = false ;
        highlightedColor = new Color(255 - penColor.getRed(), 255 - penColor.getGreen(), 255 - penColor.getBlue()) ;
    }
    
    public void updateCurveCenter( mPointF ptr) {
        point1 = ptr ;
    }
    public mPointF getPoint1() {
        return point1 ;
    }
    public mPointF getPoint2() {
        return point2 ;
    }
    public float getRadius() {
        return radius;
    }
    // return pen color
    public Color getPenColor() {
        if (selected) {
            return highlightedColor ;
        } else {
            return penColor;
        }

    }
    // return pen width
    public float getPenWidth() {
        return penWidth ;
    }
    //edit line item end points
    public void modifyPoint(int id , mPointF newPoint){
        switch (id) {
            case 1:  //' point1
                point1 = newPoint ;
                break;
            case 2:  //' point 2
                point2 = newPoint ;
                break;
        }

    }
    // return select flag
    public boolean isSelected() {
        return selected ;
    }
    // toggle selected item
    public void selectItem() {
        if (selected) {
            selected = false ;
        } else {
            selected = true ;
        }
    }
    // select item, set item select flag
    public void setItemSelect(boolean state) {
        selected = state ;
    }
    // unselect item
    public void unSelectItem() {
        selected = false ;
    }
    // select item, set item select flag
    public void selectItemSet(boolean  state ){
        selected = state ;
    }
    // reset item data class
    public void RESET() {
        point1 = new mPointF(0f, 0f);    // line segment start point or center of a curve
        point2 = new mPointF(0f, 0f);    // line segment end point
        radius = -1;                 // radius, -1  for line data
        penColor = Color.blue;
        penWidth = 2;
        selected = false;
        highlightedColor = Color.yellow;
    }
    // set line item start point
    public void setPoint1(float px , float py ){
        point1 = new mPointF(px, py);
    }
    // set line item end point
    public void setPoint2(float px, float py ){
        point2 = new mPointF(px, py);
    }
    //set curve item radius
    public void setRadius(float r ){
        radius = r ;
    }
    // set pen width
    public void setPenWidth(float pw ){
        penWidth = pw;
    }
    // set pen color
    public void setPenColor(Color pc ){
        penColor = pc;
        highlightedColor = new Color(255 - penColor.getRed(), 255 - penColor.getGreen(), 255 - penColor.getBlue());
    }
    // save line-curve tangent point to DB
    public void saveTangentAngle(byte flag , mPointF pt ){
        // calculate absolute angle of tangent point ref to center of circle
        float dx, dy ;
        float theta ;
        dx = pt.X - point1.X ;
        dy = pt.Y - point1.Y;
        theta = new Double(Math.atan2(dy, dx)).floatValue() ;  // [-pi, pi] in radian
        switch ( flag ){
            case 1:
                point2.X = theta;    //' store tangent angle to point2 x & y
                break;
            case 2:                  //' Note: point2 was not used for circle data
                point2.Y = theta;
                break;
        }
    }   // saveYangebtAngle
    // deleted selected item
    public void delete(){
        // outside draw boundary
        point1 = new mPointF(-9999, -9999);   // line segment start point or center of a curve
        point2 = new mPointF(-9999, -9999);  // line segment end point
        radius = -9999;                         // radius, -1  for line data
        selected = false;
        deleted = true;
    }
    // return item deleted flag
    public boolean isDeleted() {
        return deleted;
    }
    // reset item delete flag, undelete
    public void resetDeleteFlag() {
        deleted = false ;
    }
}

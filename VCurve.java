/*
 * VCurve.java
 * Vertical curve database.
 *
 * Created on March 17, 2006, 1:41 PM
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
public class VCurve {
    private float curveLen ; // linear curve length
    private float PVC ;   // point of the vertical curve (initial point)
    private float PVC_e ; // elevation at PVC
    private float PVC_dist ;  // distance of PVC from starting point
    private float PVT ;   // point of vertical tangent (final point of the vertical curve)
    private float PVT_e ; // elevation at PVT
    private float PVT_dist ;  // distance of PVT from starting point
    private float PVI ;   // point of vertical intersection
    private float PVI_e ; // elevation at PVI
    private float ele_minmax ;     // min/max elevation of the vertical curve
    private float dist_Eminmax ;   // location where the min/max elevation occurrs
    private float para_a ;
    private float para_b ;    // y=ax^2+bx+c, c=pvc_e
    private float Grade1 ;    // grade1
    private float Grade2 ;    // grade2
    
    private float minVCurveLen;
    /** Creates a new instance of vCurve */
    public VCurve() {
    }
    public VCurve(float minCurLen){
        minVCurveLen = minCurLen ;
    }
    public void setCurveLen(float val , float ssd ){
        // make sure the curve length is >= stopping sight distance
        if (val < ssd) {
            curveLen = ssd;
        } else {
            curveLen = val;
        }
        // round the curve to 10' ?
        // check min vertical curve limit
        if (curveLen < minVCurveLen) { 
            curveLen = minVCurveLen;
        }
    }

    // check min curve length for crest curve
    public float checkCrestLm(int unit, float _A, float _SSD, float _Len) {
        float val = 0f ;
        float constant = 0f ;
        if (unit==1) {  // US unit
            constant = 2158f ;
        } else if (unit==2) {
            // Metric
            constant = 658f ;
        }
        if (_SSD<_Len) {
            val = _SSD*_SSD*_A/constant ;
        } else {
            val = 2*_SSD-constant/_A ;
        }
        return val ;
    }
    
    // check min curve length for sag curve
    public float checkSagLm(int unit, float _A, float _SSD, float _Len) {
        float val = 0f ;
        float constant = 0f ;
        if (unit==1) {  // US unit
            constant = 400f ;
        } else if (unit==2) {
            // Metric
            constant = 120f ;
        }
        if (_SSD<_Len) {
            val = _SSD*_SSD*_A/(constant+3.5f*_SSD) ;
        } else {
            val = 2*_SSD-(constant+3.5f*_SSD)/_A ;
        }
        return val ;
    }

    public void setPVC(float val ){
        PVC = val;
    }
    public void setPVC_Elevation(float val ){
        PVC_e = val;
    }
    public void setPVC_Distance(float val ){
        PVC_dist = val;
    }
    public void setPVT(float val){
        PVT = val;
    }
    public void setPVT_Elevation(float val ){
        PVT_e = val;
    }
    public void setPVT_Distance(float val ){
        PVT_dist = val;
    }
    public void setPVI(float val ){
        PVI = val;
        PVC = PVI - 0.5f * curveLen;
        PVT = PVI + 0.5f * curveLen;
    }
    public void setPVI_e(float pvi_elevation ){
        PVI_e = pvi_elevation;
    }
    public void setPara_a(float val ){
        para_a = val;
    }
    public void setPara_b(float val ){
        para_b = val;
    }
    public void set_G1(float val ){
        Grade1 = val;
    }
    public void set_G2(float val ){
        Grade2 = val;
    }
    public void calcPVI(float pvi_elevation, float g1, float g2 ){
        Grade1 = g1;
        Grade2 = g2;
        PVI_e = pvi_elevation;
        PVC_e = PVI_e - 0.5f * curveLen * g1;
        PVT_e = PVI_e + 0.5f * curveLen * g2;
        // calculate min max elevations of the curve and their locations
        float dx ;
        dx = -g1 * curveLen / (g2 - g1);
        if (dx > curveLen) {
            dx = curveLen;
        } else if (dx < 0 ) {
            dx = 0;
        }
        para_a = 0.5f * (g2 - g1) / curveLen;
        para_b = g1;
        ele_minmax = PVC_e + para_b * dx + para_a * dx * dx;
        dist_Eminmax = dx + PVC;
    }
    
    public float getDX_Elevation(float dx ){
        return PVC_e + para_b * dx + para_a * dx * dx;
    }
    public void setMinMaxElevation(float val ) {
        ele_minmax = val;
    }
    public void setMinMaxEleDist(float val ){
        dist_Eminmax = val;
    }

    public float getCurveLen() {
        return curveLen;
    }
    public float getPVC() {
        return PVC;
    }
    public float getPVC_Elevation() {
        return PVC_e;
    }
    public float getPVC_Distance() {
        return PVC_dist;
    }
    public float getPVT() {
        return PVT;
    }
    public float getPVT_Elevation() {
        return PVT_e;
    }
    public float getPVT_Distance() {
        return PVT_dist;
    }
    public float getPVI() {
        return PVI;
    }
    public float getPVI_e() {
        return PVI_e;
    }
    public float getMinMaxElevation() {
        return ele_minmax;
    }
    public float getMinMaxEleDist() {
        return dist_Eminmax;
    }
    public float getPara_a() {
        return para_a;
    }
    public float getPara_b() {
        return para_b;
    }
    public float getPara_c() {
        return PVC_e;
    }
    public float get_G1() {
        return Grade1;
    }
    public float get_G2() {
        return Grade2;
    }
}

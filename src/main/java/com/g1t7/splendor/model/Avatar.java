package com.g1t7.splendor.model;

import java.io.Serializable;

/**
 * Avatar stub — Swing animation, not used in the web version.
 */
public class Avatar implements Serializable {

    private int frame;
    private int crd;
    private int[] ti;
    private int[] tj;
    private int[] tx;
    private int[] ty;
    private int[] tw;
    private int[] th;

    public Avatar() {
    }

    public void run() {
        // No-op: animation not applicable in web version
    }

    public int getFrame() {
        return frame;
    }

    public void setFrame(int frame) {
        this.frame = frame;
    }

    public int getCrd() {
        return crd;
    }

    public void setCrd(int crd) {
        this.crd = crd;
    }

    public int[] getTi() {
        return ti;
    }

    public void setTi(int[] ti) {
        this.ti = ti;
    }

    public int[] getTj() {
        return tj;
    }

    public void setTj(int[] tj) {
        this.tj = tj;
    }

    public int[] getTx() {
        return tx;
    }

    public void setTx(int[] tx) {
        this.tx = tx;
    }

    public int[] getTy() {
        return ty;
    }

    public void setTy(int[] ty) {
        this.ty = ty;
    }

    public int[] getTw() {
        return tw;
    }

    public void setTw(int[] tw) {
        this.tw = tw;
    }

    public int[] getTh() {
        return th;
    }

    public void setTh(int[] th) {
        this.th = th;
    }
}

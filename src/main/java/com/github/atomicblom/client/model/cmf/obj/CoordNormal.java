package com.github.atomicblom.client.model.cmf.obj;

/**
 * Created by codew on 16/05/2016.
 */
public class CoordNormal extends Token {
    private final float x;
    private final float y;
    private final float z;

    public CoordNormal(String x, String y, String z) {
        this.x = Float.parseFloat(x);
        this.y = Float.parseFloat(y);
        this.z = Float.parseFloat(z);
    }
}

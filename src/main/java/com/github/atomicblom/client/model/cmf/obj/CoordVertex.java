package com.github.atomicblom.client.model.cmf.obj;

/**
 * Created by codew on 16/05/2016.
 */
public class CoordVertex extends Token
{
    private final float x;
    private final float y;
    private final float z;
    private final float w;

    public CoordVertex(String x, String y, String z, String w) {
        this.x = Float.parseFloat(x);
        this.y = Float.parseFloat(y);
        this.z = Float.parseFloat(z);
        this.w = Float.parseFloat(w);
    }
}

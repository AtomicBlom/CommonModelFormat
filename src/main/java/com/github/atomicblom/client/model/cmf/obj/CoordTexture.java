package com.github.atomicblom.client.model.cmf.obj;

import java.io.Serializable;

/**
 * Created by codew on 16/05/2016.
 */
public class CoordTexture extends Token{
    private final float u;
    private final float v;
    private final float w;

    public CoordTexture(String u, String v, String w) {
        this.u = Float.parseFloat(u);
        this.v = Float.parseFloat(v);
        this.w = Float.parseFloat(w);
    }
}

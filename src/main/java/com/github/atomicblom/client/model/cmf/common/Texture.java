package com.github.atomicblom.client.model.cmf.common;

import javax.vecmath.Vector2f;

/**
 * Created by steblo on 23/03/2016.
 */
public class Texture {
    public static Texture White = new Texture("builtin/white", 0, 0, new Vector2f(0, 0), new Vector2f(1, 1), 0);
    private final String path;
    private final int flags;
    private final int blend;
    private final Vector2f pos;
    private final Vector2f scale;
    private final float rot;

    public Texture(String path, int flags, int blend, Vector2f pos, Vector2f scale, float rot) {
        this.path = path;
        this.flags = flags;
        this.blend = blend;
        this.pos = pos;
        this.scale = scale;
        this.rot = rot;
    }

    public String getPath() {
        return path;
    }

    public int getFlags() {
        return flags;
    }

    public int getBlend() {
        return blend;
    }

    public Vector2f getPos() {
        return pos;
    }

    public Vector2f getScale() {
        return scale;
    }

    public float getRot() {
        return rot;
    }

    @Override
    public String toString() {
        return String.format("Texture [path=%s, flags=%s, blend=%s, pos=%s, scale=%s, rot=%s]", path, flags, blend, pos, scale, rot);
    }
}

package com.github.atomicblom.client.model.cmf.common;

import javax.vecmath.Vector4f;
import java.util.List;

/**
 * Created by steblo on 23/03/2016.
 */
public class Brush {
    private final String name;
    private final Vector4f color;
    private final float shininess;
    private final int blend;
    private final int fx;
    private final List<Texture> textures;

    public Brush(String name, Vector4f color, float shininess, int blend, int fx, List<Texture> textures) {
        this.name = name;
        this.color = color;
        this.shininess = shininess;
        this.blend = blend;
        this.fx = fx;
        this.textures = textures;
    }

    public String getName() {
        return name;
    }

    public Vector4f getColor() {
        return color;
    }

    public float getShininess() {
        return shininess;
    }

    public int getBlend() {
        return blend;
    }

    public int getFx() {
        return fx;
    }

    public List<Texture> getTextures() {
        return textures;
    }

    @Override
    public String toString() {
        return String.format("Brush [name=%s, color=%s, shininess=%s, blend=%s, fx=%s, textures=%s]", name, color, shininess, blend, fx, textures);
    }
}

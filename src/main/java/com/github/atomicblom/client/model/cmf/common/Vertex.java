package com.github.atomicblom.client.model.cmf.common;

import com.google.common.base.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.Arrays;

public class Vertex {
    private static Logger LOGGER = LogManager.getLogger();

    private final Vector3f pos;
    private final Vector3f normal;
    private final Vector4f color;
    private final Vector4f[] texCoords;

    public Vertex(Vector3f pos, Vector3f normal, Vector4f color, Vector4f[] texCoords) {
        this.pos = pos;
        this.normal = normal;
        this.color = color;
        this.texCoords = texCoords;
    }

    public Vertex bake(Mesh mesh, Function<Node<?>, Matrix4f> animator) {
        // geometry
        float totalWeight = 0f;
        Matrix4f t = new Matrix4f();
        if (mesh.getWeightMap().get(this).isEmpty()) {
            t.set(animator.apply(mesh.getParent()));
        } else {
            for (JointWeight jointWeight : mesh.getWeightMap().get(this)) {
                totalWeight += jointWeight.getWeight();
                Matrix4f jointmatrix = animator.apply(jointWeight.getJointNode());
                jointmatrix.mul(jointWeight.getWeight());

                t.add(jointmatrix);
            }
            if (Math.abs(totalWeight) > 1e-4) t.mul(1f / totalWeight);
            else t.setIdentity();
        }

        // pos
        Vector4f pos = new Vector4f(this.pos), newPos = new Vector4f();
        pos.w = 1;
        t.transform(pos, newPos);
        Vector3f rPos = new Vector3f(newPos.x / newPos.w, newPos.y / newPos.w, newPos.z / newPos.w);

        // normal
        Vector3f rNormal = null;

        if (normal != null) {
            Matrix3f tm = new Matrix3f();
            t.getRotationScale(tm);
            tm.invert();
            tm.transpose();
            Vector3f normal = new Vector3f(this.normal);
            rNormal = new Vector3f();
            tm.transform(normal, rNormal);
            rNormal.normalize();
        }

        // texCoords TODO
        return new Vertex(rPos, rNormal, color, texCoords);
    }

    public Vector3f getPos() {
        return pos;
    }

    public Vector3f getNormal() {
        return normal;
    }

    public Vector4f getColor() {
        return color;
    }

    public Vector4f[] getTexCoords() {
        return texCoords;
    }

    @Override
    public String toString() {
        return String.format("Vertex [pos=%s, normal=%s, color=%s, texCoords=%s]", pos, normal, color, Arrays.toString(texCoords));
    }
}

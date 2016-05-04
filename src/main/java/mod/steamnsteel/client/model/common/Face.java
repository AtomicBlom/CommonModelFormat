package mod.steamnsteel.client.model.common;

import javax.vecmath.Vector3f;

/**
 * Created by steblo on 23/03/2016.
 */
public class Face {
    private final Vertex v1, v2, v3, v4;
    private final Brush brush;
    private final Vector3f normal;
    private final int vertexCount;

    public Face(Vertex v1, Vertex v2, Vertex v3, Brush brush) {
        this(v1, v2, v3, brush, getNormal(v1, v2, v3));
    }

    public Face(Vertex v1, Vertex v2, Vertex v3, Brush brush, Vector3f normal) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        v4 = null;
        this.brush = brush;
        this.normal = normal;
        vertexCount = 3;

    }

    public Face(Vertex v1, Vertex v2, Vertex v3, Vertex v4, Brush brush) {
        this(v1, v2, v3, v4, brush, getNormal(v1, v2, v3));
    }

    public Face(Vertex v1, Vertex v2, Vertex v3, Vertex v4, Brush brush, Vector3f normal) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
        this.brush = brush;
        this.normal = normal;
        vertexCount = 4;
    }

    public Vertex getV1() {
        return v1;
    }

    public Vertex getV2() {
        return v2;
    }

    public Vertex getV3() {
        return v3;
    }

    public Vertex getV4() {
        return v4;
    }

    public int getVertexCount() { return vertexCount; }

    public Brush getBrush() {
        return brush;
    }

    @Override
    public String toString() {
        return String.format("Face [v1=%s, v2=%s, v3=%s]", v1, v2, v3);
    }

    public Vector3f getNormal() {
        return normal;
    }

    public static Vector3f getNormal(Vertex v1, Vertex v2, Vertex v3) {
        Vector3f a = new Vector3f(v2.getPos());
        a.sub(v1.getPos());
        Vector3f b = new Vector3f(v3.getPos());
        b.sub(v1.getPos());
        Vector3f c = new Vector3f();
        c.cross(a, b);
        c.normalize();
        return c;
    }
}

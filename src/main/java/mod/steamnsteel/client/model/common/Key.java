package mod.steamnsteel.client.model.common;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * Created by steblo on 23/03/2016.
 */
public class Key {
    private final Vector3f pos;
    private final Vector3f scale;
    private final Quat4f rot;

    public Key(Vector3f pos, Vector3f scale, Quat4f rot) {
        this.pos = pos;
        this.scale = scale;
        this.rot = rot;
    }

    public Vector3f getPos() {
        return pos;
    }

    public Vector3f getScale() {
        return scale;
    }

    public Quat4f getRot() {
        return rot;
    }

    @Override
    public String toString() {
        return String.format("Key [pos=%s, scale=%s, rot=%s]", pos, scale, rot);
    }
}

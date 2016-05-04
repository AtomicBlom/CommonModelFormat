package mod.steamnsteel.client.model.common;

import com.google.common.collect.ImmutableTable;
import mod.steamnsteel.client.model.common.Key;
import mod.steamnsteel.client.model.common.Node;

/**
 * Created by steblo on 23/03/2016.
 */
public class Animation {
    private final int flags;
    private final int frames;
    private final float fps;
    private final ImmutableTable<Integer, Node<?>, Key> keys;

    public Animation(int flags, int frames, float fps, ImmutableTable<Integer, Node<?>, Key> keys) {
        this.flags = flags;
        this.frames = frames;
        this.fps = fps;
        this.keys = keys;
    }

    public int getFlags() {
        return flags;
    }

    public int getFrames() {
        return frames;
    }

    public float getFps() {
        return fps;
    }

    public ImmutableTable<Integer, Node<?>, Key> getKeys() {
        return keys;
    }

    @Override
    public String toString() {
        return String.format("Animation [flags=%s, frames=%s, fps=%s, keys=...]", flags, frames, fps);
    }
}

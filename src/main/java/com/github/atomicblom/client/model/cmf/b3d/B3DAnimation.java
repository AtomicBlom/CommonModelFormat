package com.github.atomicblom.client.model.cmf.b3d;

import com.github.atomicblom.client.model.cmf.common.IAnimation;
import com.github.atomicblom.client.model.cmf.common.Key;
import com.github.atomicblom.client.model.cmf.common.Node;
import com.google.common.collect.ImmutableTable;
import net.minecraftforge.common.model.TRSRTransformation;

/**
 * Created by steblo on 23/03/2016.
 */
public class B3DAnimation implements IAnimation
{
    private final int flags;
    private final int frames;
    private final float fps;
    private final ImmutableTable<Integer, Node<?>, Key> keys;

    public B3DAnimation(int flags, int frames, float fps, ImmutableTable<Integer, Node<?>, Key> keys) {
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

    /*public ImmutableTable<Integer, Node<?>, Key> getKeys() {
        return keys;
    }*/

    @Override
    public TRSRTransformation apply(float time, Node<?> node)
    {
        TRSRTransformation ret = TRSRTransformation.identity();
        int start = Math.max(1, (int) Math.round(Math.floor(time)));
        int end = Math.min(start + 1, (int) Math.round(Math.ceil(time)));
        float progress = time - (float) Math.floor(time);
        Key keyStart = keys.get(start, node);
        Key keyEnd = keys.get(end, node);
        TRSRTransformation startTr = keyStart == null ? null : new TRSRTransformation(keyStart.getPos(), keyStart.getRot(), keyStart.getScale(), null);
        TRSRTransformation endTr = keyEnd == null ? null : new TRSRTransformation(keyEnd.getPos(), keyEnd.getRot(), keyEnd.getScale(), null);
        if (keyStart == null)
        {
            if (keyEnd == null)
            {
                ret = ret.compose(node.getTransformation());
            }
            // TODO animated TRSR for speed?
            else
            {
                ret = ret.compose(endTr);
            }
        } else if (progress < 1e-5 || keyEnd == null)
        {
            ret = ret.compose(startTr);
        } else
        {
            ret = ret.compose(startTr.slerp(endTr, progress));
        }
        return ret;
    }

    @Override
    public String toString() {
        return String.format("B3DAnimation [flags=%s, frames=%s, fps=%s, keys=...]", flags, frames, fps);
    }
}

package com.github.atomicblom.client.model.cmf.common;

import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.model.animation.IJointClip;

public class NodeClip implements IJointClip
{
    private final Node<?> node;

    public NodeClip(Node<?> node)
    {
        this.node = node;
    }

    // joint-local
    @Override
    public TRSRTransformation apply(float time)
    {
        if(node.getAnimation() != null)
        {
            return node.getAnimation().apply(time, node);
        }
        return node.getTransformation();
    }
}

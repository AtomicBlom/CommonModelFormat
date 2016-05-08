package com.github.atomicblom.client.model.cmf.common;

import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.model.animation.IJointClip;

class NodeClip implements IJointClip
{
    private final Node<?> node;

    public NodeClip(Node<?> node)
    {
        this.node = node;
    }

    @Override
    public TRSRTransformation apply(float time)
    {
        TRSRTransformation ret = TRSRTransformation.identity();
        if (node.getAnimation() == null)
        {
            return ret.compose(node.getTransformation());
        }
        return node.getAnimation().apply(time, node);
    }
}

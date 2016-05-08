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

    // model-local, so need to walk the tree if the animation is null
    @Override
    public TRSRTransformation apply(float time)
    {
        return getTransform(time, node);
    }

    public static TRSRTransformation getTransform(float time, Node<?> node)
    {
        if(node.getAnimation() == null)
        {
            TRSRTransformation ret = TRSRTransformation.identity();
            if(node.getParent() != null)
            {
                //ret = ret.compose(getTransform(time, node.getParent()));
            }
            ret = ret.compose(node.getTransformation());
            return ret;
        }
        else
        {
            return node.getAnimation().apply(time, node);
        }
    }
}

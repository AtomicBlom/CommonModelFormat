package com.github.atomicblom.client.model.cmf.common;

import net.minecraftforge.common.model.TRSRTransformation;

/**
 * Created by rainwarrior on 5/7/16.
 */
public interface IAnimation
{
    // node-local
    TRSRTransformation apply(float time, Node<?> node);
}

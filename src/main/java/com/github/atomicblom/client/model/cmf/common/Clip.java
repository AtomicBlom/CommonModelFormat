package com.github.atomicblom.client.model.cmf.common;

import net.minecraftforge.common.animation.Event;
import net.minecraftforge.common.model.animation.IClip;
import net.minecraftforge.common.model.animation.IJoint;
import net.minecraftforge.common.model.animation.IJointClip;

import com.google.common.collect.ImmutableSet;
import net.minecraftforge.common.model.animation.JointClips.IdentityJointClip;

// FIXME: is this fast enough?
public enum Clip implements IClip
{
    INSTANCE;

    @Override
    public IJointClip apply(final IJoint joint)
    {
        if(!(joint instanceof NodeJoint))
        {
            return IdentityJointClip.INSTANCE;
        }
        return new NodeClip(((NodeJoint)joint).getNode());
    }

    @Override
    public Iterable<Event> pastEvents(float lastPollTime, float time)
    {
        return ImmutableSet.of();
    }

}

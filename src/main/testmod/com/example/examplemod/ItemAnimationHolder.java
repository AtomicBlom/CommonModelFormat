package com.example.examplemod;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.animation.TimeValues;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.model.animation.CapabilityAnimation;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;

/**
 * Created by codew on 4/05/2016.
 */
public class ItemAnimationHolder implements ICapabilityProvider
{
    private final TimeValues.VariableValue cycleLength = new TimeValues.VariableValue(4);

    private final IAnimationStateMachine asm = ExampleMod.proxy.load(new ResourceLocation(ExampleMod.MODID.toLowerCase(), "asms/block/engine.json"), ImmutableMap.<String, ITimeValue>of(
            "cycle_length", cycleLength
    ));

    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        return capability == CapabilityAnimation.ANIMATION_CAPABILITY;
    }

    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        if (capability == CapabilityAnimation.ANIMATION_CAPABILITY)
        {
            return CapabilityAnimation.ANIMATION_CAPABILITY.cast(asm);
        }
        return null;
    }
}

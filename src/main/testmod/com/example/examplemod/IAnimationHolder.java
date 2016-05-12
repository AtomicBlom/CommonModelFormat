package com.example.examplemod;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;

/**
 * Created by rainwarrior on 5/12/16.
 */
public interface IAnimationHolder extends ICapabilityProvider
{
    ResourceLocation getAsmLocation();

    ImmutableMap<String, ITimeValue> getParameters();

    void setAsm(IAnimationStateMachine asm);
}

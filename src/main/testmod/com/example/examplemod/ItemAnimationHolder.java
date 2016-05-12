package com.example.examplemod;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.animation.TimeValues;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.model.animation.CapabilityAnimation;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;

/**
 * Created by codew on 4/05/2016.
 */
public class ItemAnimationHolder implements IAnimationHolder
{
    private IAnimationStateMachine asm = null;
    private final TimeValues.VariableValue cycleLength = new TimeValues.VariableValue(4);
    private final ImmutableMap<String, ITimeValue> parameters = ImmutableMap.<String, ITimeValue>of(
        "cycle_length", cycleLength
    );
    private final ResourceLocation asmLocation;

    public ItemAnimationHolder(ResourceLocation itemName)
    {
        this.asmLocation = new ResourceLocation(itemName.getResourceDomain(), "asms/block/" + itemName.getResourcePath() + ".json");
        ExampleMod.proxy.register(this);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        return capability == CapabilityAnimation.ANIMATION_CAPABILITY;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        if (capability == CapabilityAnimation.ANIMATION_CAPABILITY)
        {
            return CapabilityAnimation.ANIMATION_CAPABILITY.cast(asm);
        }
        return null;
    }

    @Override
    public ResourceLocation getAsmLocation()
    {
        return asmLocation;
    }

    @Override
    public ImmutableMap<String, ITimeValue> getParameters()
    {
        return parameters;
    }

    @Override
    public void setAsm(IAnimationStateMachine asm)
    {
        this.asm = asm;
    }

    // TODO figure out when to unload this
}

package com.example.examplemod;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.animation.Event;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.model.animation.CapabilityAnimation;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;

public abstract class AnimationTileEntityBase extends TileEntity implements IAnimationHolder
{
    private IAnimationStateMachine asm;
    private final ResourceLocation asmLocation;

    public AnimationTileEntityBase(ResourceLocation asmLocation)
    {
        this.asmLocation = asmLocation;
    }

    public void handleEvents(float time, Iterable<Event> pastEvents)
    {
        for (Event event : pastEvents)
        {
            System.out.println("Event: " + event.event() + " " + event.offset() + " " + getPos() + " " + time);
        }
    }

    @Override
    public boolean hasFastRenderer()
    {
        return true;
    }

    public abstract void click(boolean isSneaking);

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing side)
    {
        if (capability == CapabilityAnimation.ANIMATION_CAPABILITY)
        {
            return true;
        }
        return super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side)
    {
        if (capability == CapabilityAnimation.ANIMATION_CAPABILITY)
        {
            return CapabilityAnimation.ANIMATION_CAPABILITY.cast(asm);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public ResourceLocation getAsmLocation()
    {
        return asmLocation;
    }

    protected IAnimationStateMachine getAsm()
    {
        return asm;
    }

    @Override
    public void setAsm(IAnimationStateMachine asm)
    {
        //String oldState = asm.currentState();
        this.asm = asm;
        // can't guarantee that this transition exists
        //newAsm.transition(oldState);
    }
}

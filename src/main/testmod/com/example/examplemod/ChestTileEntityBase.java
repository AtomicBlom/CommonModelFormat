package com.example.examplemod;

import com.google.common.collect.ImmutableMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.common.animation.Event;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.animation.TimeValues;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.model.animation.CapabilityAnimation;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;

import static com.example.examplemod.ExampleMod.MODID;
import static com.example.examplemod.ExampleMod.proxy;

public abstract class ChestTileEntityBase extends TileEntity
{
    private final IAnimationStateMachine asm;
    private final TimeValues.VariableValue cycleLength = new TimeValues.VariableValue(4);
    private final TimeValues.VariableValue clickTime = new TimeValues.VariableValue(Float.NEGATIVE_INFINITY);

    public ChestTileEntityBase(String asmName)
    {
        asm = proxy.load(new ResourceLocation(MODID.toLowerCase(), "asms/block/" + asmName + ".json"), ImmutableMap.<String, ITimeValue>of(
            "click_time", clickTime
        ));
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

    public void click(boolean sneaking)
    {
        if (asm != null)
        {
            if (sneaking)
            {
                cycleLength.setValue(6 - cycleLength.apply(0));
            }
            else if(asm.currentState().equals("closed"))
            {
                clickTime.setValue(Animation.getWorldTime(getWorld()));
                asm.transition("opening");
            }
            else if(asm.currentState().equals("open"))
            {
                clickTime.setValue(Animation.getWorldTime(getWorld()));
                asm.transition("closing");
            }
        }
    }

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
}

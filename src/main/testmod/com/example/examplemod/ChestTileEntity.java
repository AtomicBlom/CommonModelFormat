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

/**
 * Created by codew on 4/05/2016.
 */
public class ChestTileEntity extends TileEntity
{
    private final IAnimationStateMachine asm;
    private final TimeValues.VariableValue cycleLength = new TimeValues.VariableValue(4);
    private final TimeValues.VariableValue clickTime = new TimeValues.VariableValue(Float.NEGATIVE_INFINITY);
    //private final VariableValue offset = new VariableValue(0);

    public ChestTileEntity()
    {
        /*asm = proxy.load(new ResourceLocation(MODID.toLowerCase(), "asms/block/chest.json"), ImmutableMap.<String, ITimeValue>of(
            "click_time", clickTime
        ));*/
        asm = ExampleMod.proxy.load(new ResourceLocation(ExampleMod.MODID.toLowerCase(), "asms/block/engine.json"), ImmutableMap.<String, ITimeValue>of(
                "cycle_length", cycleLength,
                "click_time", clickTime
                //"offset", offset
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

    /*public IExtendedBlockState getState(IExtendedBlockState state) {
        return state.withProperty(B3DFrameProperty.instance, curState);
    }*/

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
            /*else if (asm.currentState().equals("default"))
            {
                float time = Animation.getWorldTime(getWorld(), Animation.getPartialTickTime());
                clickTime.setValue(time);
                //offset.setValue(time);
                //asm.transition("moving");
                asm.transition("starting");
            } else if (asm.currentState().equals("moving"))
            {
                clickTime.setValue(Animation.getWorldTime(getWorld(), Animation.getPartialTickTime()));
                asm.transition("stopping");
            }*/
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

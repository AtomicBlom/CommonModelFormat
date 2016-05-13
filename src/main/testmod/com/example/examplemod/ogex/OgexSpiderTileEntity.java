package com.example.examplemod.ogex;

import com.example.examplemod.AnimationTileEntityBase;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.Resources;
import com.google.common.collect.ImmutableMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.common.animation.Event;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.animation.TimeValues;

import static com.example.examplemod.ExampleMod.MODID;

/**
 * Created by codew on 5/05/2016.
 */
public class OgexSpiderTileEntity extends AnimationTileEntityBase
{
    private final TimeValues.VariableValue cycleLength = new TimeValues.VariableValue(4);
    private final TimeValues.VariableValue clickStart = new TimeValues.VariableValue(Float.NEGATIVE_INFINITY);
    private final TimeValues.VariableValue clickEnd = new TimeValues.VariableValue(Float.NEGATIVE_INFINITY);
    private final ImmutableMap<String, ITimeValue> parameters = ImmutableMap.<String, ITimeValue>of(
        "click_start", clickStart,
        "click_end", clickEnd
    );

    public OgexSpiderTileEntity()
    {
        super(new ResourceLocation(MODID, "asms/block/" + Resources.OgexBlocks.blockSpiderId.getResourcePath() + ".json"));
        ExampleMod.proxy.register(this);
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
        if (getAsm() != null)
        {
            if (sneaking && getAsm().currentState().equals("idle"))
            {
                clickStart.setValue(Animation.getWorldTime(getWorld()));
                getAsm().transition("attack_leap");
            } else if (getAsm().currentState().equals("idle"))
            {
                clickStart.setValue(Animation.getWorldTime(getWorld()));
                getAsm().transition("walk_start");
            } else if (getAsm().currentState().equals("walk_loop"))
            {
                clickEnd.setValue(Animation.getWorldTime(getWorld()));
                getAsm().transition("walk_loop_last");
            }
        }
    }

    @Override
    public ImmutableMap<String, ITimeValue> getParameters()
    {
        return parameters;
    }
}

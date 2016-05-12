package com.example.examplemod;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.common.animation.Event;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.animation.TimeValues;

import static com.example.examplemod.ExampleMod.MODID;

public abstract class ChestTileEntityBase extends AnimationTileEntityBase
{
    private final TimeValues.VariableValue cycleLength = new TimeValues.VariableValue(4);
    private final TimeValues.VariableValue clickTime = new TimeValues.VariableValue(Float.NEGATIVE_INFINITY);
    private final ImmutableMap<String, ITimeValue> parameters = ImmutableMap.<String, ITimeValue>of(
        "cycle_length", cycleLength,
        "click_time", new TimeValues.VariableValue(Float.NEGATIVE_INFINITY)
    );

    public ChestTileEntityBase(ResourceLocation blockName)
    {
        super(new ResourceLocation(blockName.getResourceDomain(), "asms/block/" + blockName.getResourcePath() + ".json"));
        ExampleMod.proxy.register(this);
    }

    @Override
    public ImmutableMap<String, ITimeValue> getParameters()
    {
        return parameters;
    }

    public void handleEvents(float time, Iterable<Event> pastEvents)
    {
        for (Event event : pastEvents)
        {
            System.out.println("Event: " + event.event() + " " + event.offset() + " " + getPos() + " " + time);
        }
    }

    public void click(boolean sneaking)
    {
        if (getAsm() != null)
        {
            if (sneaking)
            {
                cycleLength.setValue(6 - cycleLength.apply(0));
            }
            else if(getAsm().currentState().equals("closed"))
            {
                clickTime.setValue(Animation.getWorldTime(getWorld()));
                getAsm().transition("opening");
            }
            else if(getAsm().currentState().equals("open"))
            {
                clickTime.setValue(Animation.getWorldTime(getWorld()));
                getAsm().transition("closing");
            }
        }
    }
}

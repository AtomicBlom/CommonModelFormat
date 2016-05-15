package net.minecraftforge.commonmodelformat;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.model.animation.AnimationStateMachine;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class
ServerProxy extends CommonProxy
{
    public IAnimationStateMachine load(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters)
    {
        return null;
    }

    @Override
    public void init(FMLInitializationEvent event)
    {
    }

    @Override
    public void register(IAnimationHolder animationHolder)
    {
        animationHolder.setAsm(AnimationStateMachine.getMissing());
    }
}

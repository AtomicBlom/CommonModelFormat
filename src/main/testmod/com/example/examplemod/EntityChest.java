package com.example.examplemod;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.animation.Event;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.animation.TimeValues;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.model.animation.CapabilityAnimation;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;

/**
 * Created by codew on 4/05/2016.
 */
public class EntityChest extends EntityLiving
{
    private final IAnimationStateMachine asm;
    private final TimeValues.VariableValue cycleLength = new TimeValues.VariableValue(getHealth() / 5);

    public EntityChest(World world)
    {
        super(world);
        setSize(1, 1);
        asm = ExampleMod.proxy.load(new ResourceLocation(ExampleMod.MODID.toLowerCase(), "asms/block/engine.json"), ImmutableMap.<String, ITimeValue>of(
                "cycle_length", cycleLength
        ));
    }

    public void handleEvents(float time, Iterable<Event> pastEvents)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void onEntityUpdate()
    {
        super.onEntityUpdate();
        if (worldObj.isRemote && cycleLength != null)
        {
            cycleLength.setValue(getHealth() / 5);
        }
    }

    @Override
    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(60);
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

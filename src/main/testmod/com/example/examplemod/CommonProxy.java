package com.example.examplemod;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Created by codew on 4/05/2016.
 */
public abstract class CommonProxy
{
    public void preInit(FMLPreInitializationEvent event)
    {
        GameRegistry.register(new ChestBlock());
        GameRegistry.register(new ItemBlock(Block.REGISTRY.getObject(ExampleMod.blockId))
        {
            @Override
            public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt)
            {
                return new ItemAnimationHolder();
            }
        }.setRegistryName(ExampleMod.blockId));
        GameRegistry.registerTileEntity(ChestTileEntity.class, ExampleMod.MODID + ":" + "tile_" + ExampleMod.blockName);
    }

    public abstract IAnimationStateMachine load(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters);

}

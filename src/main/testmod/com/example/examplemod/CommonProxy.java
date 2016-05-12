package com.example.examplemod;

import com.example.examplemod.B3D.B3DChestBlock;
import com.example.examplemod.B3D.B3DChestTileEntity;
import com.example.examplemod.ogex.*;
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

public abstract class CommonProxy
{
    public void preInit(FMLPreInitializationEvent event)
    {
        GameRegistry.register(new B3DChestBlock());
        GameRegistry.register(new ItemBlock(Block.REGISTRY.getObject(Resources.B3DBlocks.blockChestId))
        {
            @Override
            public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt)
            {
                return new ItemAnimationHolder();
            }
        }.setRegistryName(Resources.B3DBlocks.blockChestId));
        GameRegistry.registerTileEntity(B3DChestTileEntity.class, ExampleMod.MODID + ":" + "tile_" + Resources.B3DBlocks.blockChestName);

        GameRegistry.register(new OgexChestBlock());
        GameRegistry.register(new ItemBlock(Block.REGISTRY.getObject(Resources.OgexBlocks.blockChestId))
        {
            @Override
            public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt)
            {
                return new ItemAnimationHolder();
            }
        }.setRegistryName(Resources.OgexBlocks.blockChestId));
        GameRegistry.registerTileEntity(OgexChestTileEntity.class, ExampleMod.MODID + ":" + "tile_" + Resources.OgexBlocks.blockChestName);

        GameRegistry.register(new OgexFanBlock());
        GameRegistry.register(new ItemBlock(Block.REGISTRY.getObject(Resources.OgexBlocks.blockFanId))
        {
            @Override
            public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt)
            {
                return new ItemAnimationHolder();
            }
        }.setRegistryName(Resources.OgexBlocks.blockFanId));
        GameRegistry.registerTileEntity(OgexFanTileEntity.class, ExampleMod.MODID + ":" + "tile_" + Resources.OgexBlocks.blockFanName);

        GameRegistry.register(new OgexSpiderBlock());
        GameRegistry.register(new ItemBlock(Block.REGISTRY.getObject(Resources.OgexBlocks.blockSpiderId))
        {
            @Override
            public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt)
            {
                return new ItemAnimationHolder();
            }
        }.setRegistryName(Resources.OgexBlocks.blockSpiderId));
        GameRegistry.registerTileEntity(OgexSpiderTileEntity.class, ExampleMod.MODID + ":" + "tile_" + Resources.OgexBlocks.blockSpiderName);
    }

    public abstract IAnimationStateMachine load(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters);

}

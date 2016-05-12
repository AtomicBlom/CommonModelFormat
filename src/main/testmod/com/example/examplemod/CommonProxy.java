package com.example.examplemod;

import com.example.examplemod.B3D.B3DChestBlock;
import com.example.examplemod.B3D.B3DChestTileEntity;
import com.example.examplemod.ogex.*;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public abstract class CommonProxy
{
    private void registerAnimBlock(Block block, Class<? extends TileEntity> te)
    {
        GameRegistry.register(block);
        GameRegistry.register(new ItemBlock(block)
        {
            @Override
            public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt)
            {
                return new ItemAnimationHolder(block.getRegistryName());
            }
        }.setRegistryName(block.getRegistryName()));
        GameRegistry.registerTileEntity(te, ExampleMod.MODID + ":" + "tile_" + block.getRegistryName().getResourcePath());
    }
    public void preInit(FMLPreInitializationEvent event)
    {
        registerAnimBlock(new B3DChestBlock(), B3DChestTileEntity.class);
        registerAnimBlock(new OgexChestBlock(), OgexChestTileEntity.class);
        registerAnimBlock(new OgexFanBlock(), OgexFanTileEntity.class);
        registerAnimBlock(new OgexSpiderBlock(), OgexChestTileEntity.class);
    }
    public abstract void init(FMLInitializationEvent event);

    public abstract void register(IAnimationHolder animationHolder);
}

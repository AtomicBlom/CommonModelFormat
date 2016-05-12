package com.example.examplemod;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ResourceLocation;

/**
 * Created by rainwarrior on 5/12/16.
 */
public abstract class ConvenienceBlockBase extends Block
{
    public ConvenienceBlockBase(ResourceLocation name, Material material)
    {
        super(material);
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        setUnlocalizedName(name.getResourceDomain() + "." + name);
        setRegistryName(name);
    }
}

package com.example.examplemod.ogex;

import com.example.examplemod.ChestBlockBase;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.Resources;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by codew on 5/05/2016.
 */
public class OgexFanBlock extends ChestBlockBase {
    public OgexFanBlock() {
        {
            setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
            setUnlocalizedName(ExampleMod.MODID + "." + Resources.OgexBlocks.blockFanName);
            setRegistryName(Resources.OgexBlocks.blockFanId);
        }
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return new OgexFanTileEntity();
    }
}

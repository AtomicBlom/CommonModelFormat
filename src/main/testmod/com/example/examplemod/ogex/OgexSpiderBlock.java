package com.example.examplemod.ogex;

import com.example.examplemod.ChestBlockBase;
import com.example.examplemod.ChestTileEntityBase;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.Resources;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Created by codew on 5/05/2016.
 */
public class OgexSpiderBlock extends ChestBlockBase {
    public OgexSpiderBlock() {
        {
            setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
            setUnlocalizedName(ExampleMod.MODID + "." + Resources.OgexBlocks.blockSpiderName);
            setRegistryName(Resources.OgexBlocks.blockSpiderId);
        }
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return new OgexSpiderTileEntity();
    }

    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        if (world.isRemote)
        {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof OgexSpiderTileEntity)
            {
                ((OgexSpiderTileEntity) te).click(player.isSneaking());
            }
        }
        return true;
    }
}

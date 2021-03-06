package net.minecraftforge.commonmodelformat.ogex;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.commonmodelformat.ChestBlockBase;
import net.minecraftforge.commonmodelformat.Resources;

public class OgexSpiderBlock extends ChestBlockBase
{
    public OgexSpiderBlock() {
        super(Resources.OgexBlocks.blockSpiderId);
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

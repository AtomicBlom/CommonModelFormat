package net.minecraftforge.commonmodelformat.B3D;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.commonmodelformat.ChestBlockBase;
import net.minecraftforge.commonmodelformat.Resources;

public class B3DChestBlock extends ChestBlockBase
{
    public B3DChestBlock() {
        super(Resources.B3DBlocks.blockChestId);
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return new B3DChestTileEntity();
    }
}

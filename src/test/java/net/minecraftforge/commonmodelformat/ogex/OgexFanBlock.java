package net.minecraftforge.commonmodelformat.ogex;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.commonmodelformat.ChestBlockBase;
import net.minecraftforge.commonmodelformat.Resources;

public class OgexFanBlock extends ChestBlockBase
{
    public OgexFanBlock() {
        super(Resources.OgexBlocks.blockFanId);
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return new OgexFanTileEntity();
    }
}

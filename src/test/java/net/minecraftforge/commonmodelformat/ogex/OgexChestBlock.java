package net.minecraftforge.commonmodelformat.ogex;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.commonmodelformat.ChestBlockBase;
import net.minecraftforge.commonmodelformat.Resources;

/**
 * Created by codew on 5/05/2016.
 */
public class OgexChestBlock extends ChestBlockBase
{
    public OgexChestBlock() {
        super(Resources.OgexBlocks.blockChestId);
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return new OgexChestTileEntity();
    }
}

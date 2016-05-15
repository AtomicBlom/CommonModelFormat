package net.minecraftforge.commonmodelformat.ogex;

import net.minecraftforge.commonmodelformat.ChestTileEntityBase;
import net.minecraftforge.commonmodelformat.Resources;

public class OgexChestTileEntity extends ChestTileEntityBase
{
    public OgexChestTileEntity()
    {
        super(Resources.OgexBlocks.blockChestId);
    }

    // when it's slimy snek
    @Override
    public boolean shouldRenderInPass(int pass)
    {
        return pass == 1;
    }
}

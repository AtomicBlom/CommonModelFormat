package com.example.examplemod.ogex;

import com.example.examplemod.ChestTileEntityBase;
import com.example.examplemod.Resources;

/**
 * Created by codew on 5/05/2016.
 */
public class OgexChestTileEntity extends ChestTileEntityBase {
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

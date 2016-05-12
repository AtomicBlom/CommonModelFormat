package com.example.examplemod.ogex;

import com.example.examplemod.ChestTileEntityBase;

/**
 * Created by codew on 5/05/2016.
 */
public class OgexChestTileEntity extends ChestTileEntityBase {
    public OgexChestTileEntity()
    {
        super("snek");
    }

    // when it's slimy snek
    @Override
    public boolean shouldRenderInPass(int pass)
    {
        return pass == 1;
    }
}

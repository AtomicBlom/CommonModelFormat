package com.example.examplemod.B3D;

import com.example.examplemod.ChestBlockBase;
import com.example.examplemod.Resources;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class B3DChestBlock extends ChestBlockBase {
    public B3DChestBlock() {
        super(Resources.B3DBlocks.blockChestId);
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return new B3DChestTileEntity();
    }
}

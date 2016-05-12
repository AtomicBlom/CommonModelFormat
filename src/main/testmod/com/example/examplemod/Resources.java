package com.example.examplemod;

import net.minecraft.util.ResourceLocation;

import static com.example.examplemod.ExampleMod.MODID;

public class Resources {
    public static class B3DBlocks {
        public static final String blockChestName = "blockb3dchest";
        public static ResourceLocation blockChestId = new ResourceLocation(MODID, blockChestName);
    }

    public static class OgexBlocks {
        public static final String blockChestName = "blockogexchest";
        public static ResourceLocation blockChestId = new ResourceLocation(MODID, blockChestName);
        public static final String blockFanName = "blockogexfan";
        public static ResourceLocation blockFanId = new ResourceLocation(MODID, blockFanName);
        public static final String blockSpiderName = "blockogexspider";
        public static ResourceLocation blockSpiderId = new ResourceLocation(MODID, blockSpiderName);
    }
}

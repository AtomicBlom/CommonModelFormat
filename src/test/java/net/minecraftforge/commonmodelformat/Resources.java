package net.minecraftforge.commonmodelformat;

import net.minecraft.util.ResourceLocation;

import static net.minecraftforge.commonmodelformat.CommonModelFormatExamples.MODID;

public class Resources {
    public static class B3DBlocks {
        public static final ResourceLocation blockChestId =  new ResourceLocation(MODID, "blockb3dchest");
    }

    public static class OgexBlocks {
        public static final ResourceLocation blockChestId =  new ResourceLocation(MODID, "blockogexchest");
        public static final ResourceLocation blockFanId =    new ResourceLocation(MODID, "blockogexfan");
        public static final ResourceLocation blockSpiderId = new ResourceLocation(MODID, "blockogexspider");
    }
}

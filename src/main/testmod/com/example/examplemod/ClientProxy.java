package com.example.examplemod;

import com.example.examplemod.B3D.B3DChestTileEntity;
import com.example.examplemod.ogex.OgexChestTileEntity;
import com.github.atomicblom.client.model.cmf.b3d.B3DLoader;
import com.github.atomicblom.client.model.cmf.opengex.OpenGEXLoader;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.animation.AnimationTESR;
import net.minecraftforge.common.animation.Event;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy
{
    @Override
    public void preInit(FMLPreInitializationEvent event)
    {
        super.preInit(event);
        B3DLoader.INSTANCE.addDomain(ExampleMod.MODID);
        OpenGEXLoader.INSTANCE.addDomain(ExampleMod.MODID);
        ModelLoaderRegistry.registerLoader(B3DLoader.INSTANCE);
        ModelLoaderRegistry.registerLoader(OpenGEXLoader.INSTANCE);
        ModelLoader.setCustomModelResourceLocation(
            Item.REGISTRY.getObject(Resources.B3DBlocks.blockChestId), 0,
            new ModelResourceLocation(Resources.B3DBlocks.blockChestId, "inventory"));
        ModelLoader.setCustomModelResourceLocation(
                Item.REGISTRY.getObject(Resources.OgexBlocks.blockChestId), 0,
                new ModelResourceLocation(Resources.OgexBlocks.blockChestId, "inventory"));
        ClientRegistry.bindTileEntitySpecialRenderer(B3DChestTileEntity.class, new AnimationTESR<B3DChestTileEntity>()
        {
            @Override
            public void handleEvents(B3DChestTileEntity chest, float time, Iterable<Event> pastEvents) {
                chest.handleEvents(time, pastEvents);
            }
        });

        ClientRegistry.bindTileEntitySpecialRenderer(OgexChestTileEntity.class, new AnimationTESR<OgexChestTileEntity>()
        {
            @Override
            public void handleEvents(OgexChestTileEntity chest, float time, Iterable<Event> pastEvents) {
                chest.handleEvents(time, pastEvents);
            }
        });
    }

    public IAnimationStateMachine load(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters)
    {
        return ModelLoaderRegistry.loadASM(location, parameters);
    }

}

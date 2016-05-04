package com.example.examplemod;

import com.github.atomicblom.client.model.cmf.b3d.B3DLoader;
import com.github.atomicblom.client.model.cmf.opengex.OpenGEXLoader;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.animation.AnimationModelBase;
import net.minecraftforge.client.model.animation.AnimationTESR;
import net.minecraftforge.client.model.pipeline.VertexLighterSmoothAo;
import net.minecraftforge.common.animation.Event;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;

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
        ModelLoader.setCustomModelResourceLocation(Item.REGISTRY.getObject(ExampleMod.blockId), 0, new ModelResourceLocation(ExampleMod.blockId, "inventory"));
        ClientRegistry.bindTileEntitySpecialRenderer(ChestTileEntity.class, new AnimationTESR<ChestTileEntity>()
        {
            @Override
            public void handleEvents(ChestTileEntity chest, float time, Iterable<Event> pastEvents)
            {
                chest.handleEvents(time, pastEvents);
            }
        });
        String entityName = ExampleMod.MODID + ":entity_chest";

        //EntityRegistry.registerGlobalEntityID(EntityChest.class, entityName, EntityRegistry.findGlobalUniqueEntityId());
        EntityRegistry.registerModEntity(EntityChest.class, entityName, 0, ExampleMod.instance, 64, 20, true, 0xFFAAAA00, 0xFFDDDD00);
        RenderingRegistry.registerEntityRenderingHandler(EntityChest.class, new IRenderFactory<EntityChest>()
        {
            @SuppressWarnings("deprecation")
            public Render<EntityChest> createRenderFor(RenderManager manager)
            {
                /*cmf = ModelLoaderRegistry.getModel(new ResourceLocation(ModelLoaderRegistryDebug.MODID, "block/chest.b3d"));
                if(cmf instanceof IRetexturableModel)
                {
                    cmf = ((IRetexturableModel)cmf).retexture(ImmutableMap.of("#chest", "entity/chest/normal"));
                }
                if(cmf instanceof IModelCustomData)
                {
                    cmf = ((IModelCustomData)cmf).process(ImmutableMap.of("mesh", "[\"Base\", \"Lid\"]"));
                }*/
                ResourceLocation location = new ModelResourceLocation(new ResourceLocation(ExampleMod.MODID, ExampleMod.blockName), "entity");
                return new RenderLiving<EntityChest>(manager, new AnimationModelBase<EntityChest>(location, new VertexLighterSmoothAo(Minecraft.getMinecraft().getBlockColors()))
                {
                    @Override
                    public void handleEvents(EntityChest chest, float time, Iterable<Event> pastEvents)
                    {
                        chest.handleEvents(time, pastEvents);
                    }
                }, 0.5f)
                {
                    protected ResourceLocation getEntityTexture(EntityChest entity)
                    {
                        return TextureMap.LOCATION_BLOCKS_TEXTURE;
                    }
                };
            }
        });
    }

    public IAnimationStateMachine load(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters)
    {
        return ModelLoaderRegistry.loadASM(location, parameters);
    }

}

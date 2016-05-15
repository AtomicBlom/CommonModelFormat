package net.minecraftforge.commonmodelformat;

import com.github.atomicblom.client.model.cmf.b3d.B3DLoader;
import com.github.atomicblom.client.model.cmf.opengex.OpenGEXLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.animation.AnimationTESR;
import net.minecraftforge.common.animation.Event;
import net.minecraftforge.commonmodelformat.B3D.B3DChestTileEntity;
import net.minecraftforge.commonmodelformat.ogex.OgexChestTileEntity;
import net.minecraftforge.commonmodelformat.ogex.OgexFanTileEntity;
import net.minecraftforge.commonmodelformat.ogex.OgexSpiderTileEntity;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class ClientProxy extends CommonProxy implements IResourceManagerReloadListener
{
    @Override
    public void preInit(FMLPreInitializationEvent event)
    {
        super.preInit(event);
        B3DLoader.INSTANCE.addDomain(CommonModelFormatExamples.MODID);
        OpenGEXLoader.INSTANCE.addDomain(CommonModelFormatExamples.MODID);
        ModelLoaderRegistry.registerLoader(B3DLoader.INSTANCE);
        ModelLoaderRegistry.registerLoader(OpenGEXLoader.INSTANCE);

        ModelLoader.setCustomModelResourceLocation(
            Item.REGISTRY.getObject(Resources.B3DBlocks.blockChestId), 0,
            new ModelResourceLocation(Resources.B3DBlocks.blockChestId, "inventory"));

        ModelLoader.setCustomModelResourceLocation(
                Item.REGISTRY.getObject(Resources.OgexBlocks.blockChestId), 0,
                new ModelResourceLocation(Resources.OgexBlocks.blockChestId, "inventory"));
        ModelLoader.setCustomModelResourceLocation(
                Item.REGISTRY.getObject(Resources.OgexBlocks.blockFanId), 0,
                new ModelResourceLocation(Resources.OgexBlocks.blockFanId, "inventory"));

        ModelLoader.setCustomModelResourceLocation(
                Item.REGISTRY.getObject(Resources.OgexBlocks.blockSpiderId), 0,
                new ModelResourceLocation(Resources.OgexBlocks.blockSpiderId, "inventory"));

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

        ClientRegistry.bindTileEntitySpecialRenderer(OgexFanTileEntity.class, new AnimationTESR<OgexFanTileEntity>()
        {
            @Override
            public void handleEvents(OgexFanTileEntity chest, float time, Iterable<Event> pastEvents) {
                chest.handleEvents(time, pastEvents);
            }
        });

        ClientRegistry.bindTileEntitySpecialRenderer(OgexSpiderTileEntity.class, new AnimationTESR<OgexSpiderTileEntity>()
        {
            @Override
            public void handleEvents(OgexSpiderTileEntity chest, float time, Iterable<Event> pastEvents) {
                chest.handleEvents(time, pastEvents);
            }
        });
    }

    private final Set<IAnimationHolder> asmHolders = Collections.newSetFromMap(new WeakHashMap<IAnimationHolder, Boolean>());

    @Override
    public void init(FMLInitializationEvent event)
    {
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(this);
    }

    @Override
    public void register(IAnimationHolder animationHolder)
    {
        asmHolders.add(animationHolder);
        animationHolder.setAsm(ModelLoaderRegistry.loadASM(animationHolder.getAsmLocation(), animationHolder.getParameters()));
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager)
    {
        for(IAnimationHolder te : asmHolders)
        {
            te.setAsm(ModelLoaderRegistry.loadASM(te.getAsmLocation(), te.getParameters()));
        }
    }
}

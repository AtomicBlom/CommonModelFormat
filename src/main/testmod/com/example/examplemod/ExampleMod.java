package com.example.examplemod;

import net.minecraft.block.properties.PropertyDirection;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;



@Mod(modid = ExampleMod.MODID, version = ExampleMod.VERSION)
public class ExampleMod
{
    public static final String MODID = "examplemod";
    public static final String VERSION = "0.0";

    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    @Instance(MODID)
    public static ExampleMod instance;

    @SidedProxy(clientSide = "com.example.examplemod.ClientProxy", serverSide = "com.example.examplemod.ServerProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) { proxy.preInit(event); }

    @EventHandler
    public void init(FMLInitializationEvent event) { proxy.init(event); }
}

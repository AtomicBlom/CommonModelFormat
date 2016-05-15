package net.minecraftforge.commonmodelformat;

import net.minecraft.block.properties.PropertyDirection;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;



@Mod(modid = CommonModelFormatExamples.MODID, version = CommonModelFormatExamples.VERSION)
public class CommonModelFormatExamples
{
    public static final String MODID = "forgecommonmodelformat";
    public static final String VERSION = "0.1";

    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    @Instance(MODID)
    public static CommonModelFormatExamples instance;

    @SidedProxy(clientSide = "net.minecraftforge.commonmodelformat.ClientProxy", serverSide = "net.minecraftforge.commonmodelformat.ServerProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) { proxy.preInit(event); }

    @EventHandler
    public void init(FMLInitializationEvent event) { proxy.init(event); }
}

package com.github.atomicblom.client.model.cmf.opengex;

import com.github.atomicblom.client.model.cmf.b3d.*;
import com.github.atomicblom.client.model.cmf.common.LoaderBase;
import com.github.atomicblom.client.model.cmf.obj.OBJLoader;
import com.google.common.collect.ImmutableSet;
import com.github.atomicblom.client.model.cmf.common.Model;
import com.github.atomicblom.client.model.cmf.common.Mesh;
import com.github.atomicblom.client.model.cmf.common.ModelWrapper;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry.LoaderException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * Loader for OpenGEX models.
 * To enable for your mod call INSTANCE.addDomain(modid).
 * If you need more control over accepted resources - extend the class, and register a new INSTANCE with ModelLoaderRegistry.
 */
public class OpenGEXLoader extends LoaderBase
{
    public static OpenGEXLoader INSTANCE = new OpenGEXLoader();
    private OpenGEXLoader() {}

    @Override
    protected boolean canProcessResourcePath(String resourcePath)
    {
        return resourcePath.endsWith(".ogex");
    }

    @Override
    protected Model parseModel(IResource resource) throws IOException
    {
        Parser parser = new Parser(resource.getInputStream());
        return parser.parse();
    }
}

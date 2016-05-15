package com.github.atomicblom.client.model.cmf.obj;

import com.github.atomicblom.client.model.cmf.common.Model;
import net.minecraft.client.resources.IResource;

/*
 * Loader for OBJ models.
 * To enable for your mod call instance.addDomain(modid).
 * If you need more control over accepted resources - extend the class, and register a new instance with ModelLoaderRegistry.
 */
public class OBJLoader extends LoaderBase
{
    public static OBJLoader INSTANCE = new OBJLoader();
    private OBJLoader() {}


    @Override
    protected boolean canProcessResourcePath(String resourcePath)
    {
        return resourcePath.endsWith(".obj");
    }

    @Override
    protected Model parseModel(IResource resource)
    {
        Parser parser = new Parser(resource.getInputStream());
        return parser.parse();
    }

}

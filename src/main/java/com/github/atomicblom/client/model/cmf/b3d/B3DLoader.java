package com.github.atomicblom.client.model.cmf.b3d;

import com.github.atomicblom.client.model.cmf.common.Model;
import com.github.atomicblom.client.model.cmf.common.LoaderBase;
import net.minecraft.client.resources.IResource;
import java.io.IOException;

/*
 * Loader for Blitz3D models.
 * To enable for your mod call instance.addDomain(modid).
 * If you need more control over accepted resources - extend the class, and register a new instance with ModelLoaderRegistry.
 */
public class B3DLoader extends LoaderBase
{
    public static B3DLoader INSTANCE = new B3DLoader();
    private B3DLoader() {}

    @Override
    protected boolean canProcessResourcePath(String resourcePath)
    {
        return resourcePath.endsWith(".b3d");
    }

    @Override
    protected Model parseModel(IResource resource) throws IOException
    {
        Parser parser = new Parser(resource.getInputStream());
        return parser.parse();
    }

}

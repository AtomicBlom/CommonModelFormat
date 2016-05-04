package com.github.atomicblom.client.model.cmf.opengex;

import com.google.common.collect.ImmutableSet;
import com.github.atomicblom.client.model.cmf.common.GenericModel;
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
 * Loader for Blitz3D models.
 * To enable for your mod call INSTANCE.addDomain(modid).
 * If you need more control over accepted resources - extend the class, and register a new INSTANCE with ModelLoaderRegistry.
 */
public enum OpenGEXLoader implements ICustomModelLoader
{
    INSTANCE;

    private IResourceManager manager;

    private final Set<String> enabledDomains = new HashSet<String>();
    private final Map<ResourceLocation, GenericModel> cache = new HashMap<ResourceLocation, GenericModel>();

    public void addDomain(String domain)
    {
        enabledDomains.add(domain.toLowerCase());
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager)
    {
        this.manager = manager;
        cache.clear();
    }

    @Override
    public boolean accepts(ResourceLocation modelLocation)
    {
        return enabledDomains.contains(modelLocation.getResourceDomain()) && modelLocation.getResourcePath().endsWith(".ogex");
    }

    @Override
    @SuppressWarnings("unchecked")
    public IModel loadModel(ResourceLocation modelLocation) throws Exception
    {
        ResourceLocation file = new ResourceLocation(modelLocation.getResourceDomain(), modelLocation.getResourcePath());
        if(!cache.containsKey(file))
        {
            try
            {
                IResource resource = null;
                try
                {
                    resource = manager.getResource(file);
                }
                catch(FileNotFoundException e)
                {
                    if(modelLocation.getResourcePath().startsWith("models/block/"))
                        resource = manager.getResource(new ResourceLocation(file.getResourceDomain(), "models/item/" + file.getResourcePath().substring("models/block/".length())));
                    else if(modelLocation.getResourcePath().startsWith("models/item/"))
                        resource = manager.getResource(new ResourceLocation(file.getResourceDomain(), "models/block/" + file.getResourcePath().substring("models/item/".length())));
                    else throw e;
                }
                Parser parser = new Parser(resource.getInputStream());
                GenericModel model = parser.parse();
                cache.put(file, model);
            }
            catch(IOException e)
            {
                cache.put(file, null);
                throw e;
            }
            catch (OpenGEXException ex) {
                throw new OpenGEXException("Error while loading OpenGEX resource " + file, ex);
            }
        }
        GenericModel model = cache.get(file);
        if(model == null) throw new LoaderException("Error loading cmf previously: " + file);
        if(!(model.getRoot().getKind() instanceof Mesh))
        {
            return new ModelWrapper(modelLocation, model, ImmutableSet.<String>of(), true, true, 1);
        }
        return new ModelWrapper(modelLocation, model, ImmutableSet.of(model.getRoot().getName()), true, true, 1);
    }

}

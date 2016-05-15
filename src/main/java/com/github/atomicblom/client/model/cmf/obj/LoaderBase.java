package com.github.atomicblom.client.model.cmf.obj;

import com.github.atomicblom.client.model.cmf.common.Mesh;
import com.github.atomicblom.client.model.cmf.common.Model;
import com.github.atomicblom.client.model.cmf.common.ModelWrapper;
import com.github.atomicblom.client.model.cmf.common.Node;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by codew on 15/05/2016.
 */
public abstract class LoaderBase implements ICustomModelLoader
{
    private final Set<String> enabledDomains = new HashSet<String>();
    private final Map<ResourceLocation, Model> cache = new HashMap<ResourceLocation, Model>();
    private IResourceManager manager;

    public void addDomain(String domain)
    {
        enabledDomains.add(domain.toLowerCase());
    }

    public void onResourceManagerReload(IResourceManager manager)
    {
        this.manager = manager;
        cache.clear();
    }

    public boolean accepts(ResourceLocation modelLocation)
    {
        return enabledDomains.contains(modelLocation.getResourceDomain()) && canProcessResourcePath(modelLocation.getResourcePath());
    }

    protected abstract boolean canProcessResourcePath(String resourcePath);

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
                Model model = parseModel(resource);
                cache.put(file, model);
            }
            catch(IOException e)
            {
                cache.put(file, null);
                throw e;
            }
        }
        Model model = cache.get(file);
        if(model == null) throw new ModelLoaderRegistry.LoaderException("Error loading cmf previously: " + file);
        if(!(model.getRoot().getKind() instanceof Mesh))
        {
            return new ModelWrapper(modelLocation, model, ImmutableSet.<String>of(), true, true, 1);
        }
        return new ModelWrapper(modelLocation, model, ImmutableSet.of(((Node<Mesh>)model.getRoot()).getName()), true, true, 1);
    }

    protected abstract Model parseModel(IResource resource);
}

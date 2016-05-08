package com.github.atomicblom.client.model.cmf.common;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.*;
import net.minecraftforge.client.model.animation.IAnimatedModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.animation.IClip;
import net.minecraftforge.fml.common.FMLLog;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ModelWrapper implements IRetexturableModel, IModelCustomData, IModelSimpleProperties, IAnimatedModel
{
    private final ResourceLocation modelLocation;
    private final Model model;
    private final ImmutableSet<String> meshes;
    private final ImmutableMap<String, ResourceLocation> textures;
    private final boolean smooth;
    private final boolean gui3d;
    private final int defaultKey;

    public ModelWrapper(ResourceLocation modelLocation, Model model, ImmutableSet<String> meshes, boolean smooth, boolean gui3d, int defaultKey)
    {
        this(modelLocation, model, meshes, smooth, gui3d, defaultKey, buildTextures(model.getTextures()));
    }

    public ModelWrapper(ResourceLocation modelLocation, Model model, ImmutableSet<String> meshes, boolean smooth, boolean gui3d, int defaultKey, ImmutableMap<String, ResourceLocation> textures)
    {
        this.modelLocation = modelLocation;
        this.model = model;
        this.meshes = meshes;
        this.textures = textures;
        this.smooth = smooth;
        this.gui3d = gui3d;
        this.defaultKey = defaultKey;
    }

    private static ImmutableMap<String, ResourceLocation> buildTextures(Collection<Texture> textures)
    {
        ImmutableMap.Builder<String, ResourceLocation> builder = ImmutableMap.builder();

        for (Texture t : textures)
        {
            String path = t.getPath();
            String location = getLocation(path);
            if (!location.startsWith("#")) location = "#" + location;
            builder.put(path, new ResourceLocation(location));
        }
        return builder.build();
    }

    private static String getLocation(String path)
    {
        if (path.endsWith(".png")) path = path.substring(0, path.length() - ".png".length());
        return path;
    }

    @Override
    public Collection<ResourceLocation> getDependencies()
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<ResourceLocation> getTextures()
    {
        return Collections2.filter(textures.values(), new Predicate<ResourceLocation>()
        {
            @Override
            public boolean apply(ResourceLocation loc)
            {
                return !loc.getResourcePath().startsWith("#");
            }
        });
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        ImmutableMap.Builder<String, TextureAtlasSprite> builder = ImmutableMap.builder();
        TextureAtlasSprite missing = bakedTextureGetter.apply(new ResourceLocation("missingno"));
        for (Map.Entry<String, ResourceLocation> e : textures.entrySet())
        {
            if (e.getValue().getResourcePath().startsWith("#"))
            {
                FMLLog.severe("unresolved texture '%s' for opengex cmf '%s'", e.getValue().getResourcePath(), modelLocation);
                builder.put(e.getKey(), missing);
            } else
            {
                builder.put(e.getKey(), bakedTextureGetter.apply(e.getValue()));
            }
        }
        builder.put("missingno", missing);
        return new BakedWrapper(model.getRoot(), state, smooth, gui3d, format, meshes, builder.build());
    }

    @Override
    public ModelWrapper retexture(ImmutableMap<String, String> textures)
    {
        ImmutableMap.Builder<String, ResourceLocation> builder = ImmutableMap.builder();
        for (Map.Entry<String, ResourceLocation> e : this.textures.entrySet())
        {
            String path = e.getKey();
            String loc = getLocation(path);
            if (textures.containsKey(loc))
            {
                String newLoc = textures.get(loc);
                if (newLoc == null) newLoc = getLocation(path);
                builder.put(e.getKey(), new ResourceLocation(newLoc));
            } else
            {
                builder.put(e);
            }
        }
        return new ModelWrapper(modelLocation, model, meshes, smooth, gui3d, defaultKey, builder.build());
    }

    @Override
    public ModelWrapper process(ImmutableMap<String, String> data)
    {
        if (data.containsKey("mesh"))
        {
            JsonElement e = new JsonParser().parse(data.get("mesh"));
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString())
            {
                return new ModelWrapper(modelLocation, model, ImmutableSet.of(e.getAsString()), smooth, gui3d, defaultKey, textures);
            } else if (e.isJsonArray())
            {
                ImmutableSet.Builder<String> builder = ImmutableSet.builder();
                for (JsonElement s : e.getAsJsonArray())
                {
                    if (s.isJsonPrimitive() && s.getAsJsonPrimitive().isString())
                    {
                        builder.add(s.getAsString());
                    } else
                    {
                        FMLLog.severe("unknown mesh definition '%s' in array for opengex cmf '%s'", s.toString(), modelLocation);
                        return this;
                    }
                }
                return new ModelWrapper(modelLocation, model, builder.build(), smooth, gui3d, defaultKey, textures);
            } else
            {
                FMLLog.severe("unknown mesh definition '%s' for opengex cmf '%s'", e.toString(), modelLocation);
                return this;
            }
        }
        if (data.containsKey("key"))
        {
            JsonElement e = new JsonParser().parse(data.get("key"));
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber())
            {
                return new ModelWrapper(modelLocation, model, meshes, smooth, gui3d, e.getAsNumber().intValue(), textures);
            } else
            {
                FMLLog.severe("unknown keyframe definition '%s' for opengex cmf '%s'", e.toString(), modelLocation);
                return this;
            }
        }
        return this;
    }

    @Override
    public Optional<IClip> getClip(String name)
    {
        if (name.equals("main"))
        {
            return Optional.<IClip>of(Clip.INSTANCE);
        }
        return Optional.absent();
    }

    @Override
    public IModelState getDefaultState()
    {
        return null;
    }

    @Override
    public ModelWrapper smoothLighting(boolean value)
    {
        if (value == smooth)
        {
            return this;
        }
        return new ModelWrapper(modelLocation, model, meshes, value, gui3d, defaultKey, textures);
    }

    @Override
    public ModelWrapper gui3d(boolean value)
    {
        if (value == gui3d)
        {
            return this;
        }
        return new ModelWrapper(modelLocation, model, meshes, smooth, value, defaultKey, textures);
    }

}

package com.github.atomicblom.client.model.cmf.common;

import java.util.Collection;

import com.google.common.collect.ImmutableMap;

public class Model
{
    private final Collection<Texture> textures;
    private final Collection<Brush> brushes;
    private final Node<?> root;
    private final ImmutableMap<String, Node<Mesh>> meshes;

    public Model(Collection<Texture> textures, Collection<Brush> brushes, Node<?> root, ImmutableMap<String, Node<Mesh>> meshes)
    {
        this.textures = textures;
        this.brushes = brushes;
        this.root = root;
        this.meshes = meshes;
    }

    // boilerplate below

    public Collection<Texture> getTextures()
    {
        return textures;
    }

    public Collection<Brush> getBrushes()
    {
        return brushes;
    }

    public Node<?> getRoot()
    {
        return root;
    }

    public ImmutableMap<String, Node<Mesh>> getMeshes()
    {
        return meshes;
    }

}

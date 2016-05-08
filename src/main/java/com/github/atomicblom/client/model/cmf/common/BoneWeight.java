package com.github.atomicblom.client.model.cmf.common;

public class BoneWeight
{
    private final Node<Bone> boneNode;
    private final Float weight;

    public BoneWeight(Node<Bone> boneNode, Float weight)
    {

        this.boneNode = boneNode;
        this.weight = weight;
    }

    public Node<Bone> getBoneNode()
    {
        return boneNode;
    }

    public Float getWeight()
    {
        return weight;
    }
}


package com.github.atomicblom.client.model.cmf.common;

import com.github.atomicblom.client.model.cmf.common.Bone;
import com.github.atomicblom.client.model.cmf.common.Node;

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

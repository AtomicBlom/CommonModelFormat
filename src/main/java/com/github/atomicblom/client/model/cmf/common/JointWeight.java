package com.github.atomicblom.client.model.cmf.common;

public class JointWeight
{
    private final Node<Joint> jointNode;
    private final Float weight;

    public JointWeight(Node<Joint> jointNode, Float weight)
    {

        this.jointNode = jointNode;
        this.weight = weight;
    }

    public Node<Joint> getJointNode()
    {
        return jointNode;
    }

    public Float getWeight()
    {
        return weight;
    }
}


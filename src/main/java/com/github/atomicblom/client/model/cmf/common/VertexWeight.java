package com.github.atomicblom.client.model.cmf.common;

public class VertexWeight
{
    private final Vertex vertex;
    private final Float weight;

    public VertexWeight(Vertex vertex, Float weight)
    {

        this.vertex = vertex;
        this.weight = weight;
    }

    public Vertex getVertex()
    {
        return vertex;
    }

    public Float getWeight()
    {
        return weight;
    }
}

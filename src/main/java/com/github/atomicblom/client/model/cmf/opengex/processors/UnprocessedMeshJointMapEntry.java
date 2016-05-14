package com.github.atomicblom.client.model.cmf.opengex.processors;

import com.github.atomicblom.client.model.cmf.common.Mesh;
import com.github.atomicblom.client.model.cmf.common.Vertex;
import com.google.common.collect.Multimap;
import java.util.Set;

class UnprocessedMeshJointMapEntry
{
    private final Mesh mesh;
    private final Multimap<Vertex, JointWeightPose> verticesToProcess;

    UnprocessedMeshJointMapEntry(Mesh mesh, Multimap<Vertex, JointWeightPose> verticesToProcess)
    {

        this.mesh = mesh;
        this.verticesToProcess = verticesToProcess;
    }

    Set<Vertex> getVertices()
    {
        return verticesToProcess.keySet();
    }

    Iterable<JointWeightPose> getJointWeightPose(Vertex vertex)
    {
        return verticesToProcess.get(vertex);
    }

    public Mesh getMesh()
    {
        return mesh;
    }
}

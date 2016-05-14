package com.github.atomicblom.client.model.cmf.opengex.processors;

import com.github.atomicblom.client.model.cmf.common.Brush;
import com.github.atomicblom.client.model.cmf.common.Face;
import com.github.atomicblom.client.model.cmf.common.Vertex;
import com.github.atomicblom.client.model.cmf.opengex.OpenGEXException;
import com.github.atomicblom.client.model.cmf.opengex.ogex.MeshType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Multimap;
import javax.vecmath.Vector3f;

class BrushSubMeshCombiner
{
    private final ImmutableList.Builder<Face> faces = ImmutableList.builder();
    private final Builder<Vertex, JointWeightPose> vertexWeights = ImmutableMultimap.builder();
    private final Brush brush;
    private final MeshType type;

    BrushSubMeshCombiner(Brush brush, MeshType type)
    {
        this.brush = brush;
        this.type = type;
    }

    void addFace(Vertex[] vertices, Vector3f normal)
    {
        final Face face;
        if (type == MeshType.Triangles)
        {
            face = new Face(vertices[0], vertices[1], vertices[2], brush, normal);
        } else if (type == MeshType.Quads)
        {
            face = new Face(vertices[0], vertices[1], vertices[2], vertices[3], brush, normal);
        } else
        {
            throw new OpenGEXException("Attempting to generate a cmf for an unsupported OpenGL Mesh Type: " + type);
        }
        faces.add(face);
    }

    void addJointWeightPoseToVertex(Vertex vertex, Iterable<JointWeightPose> jointWeightPoses)
    {
        for (final JointWeightPose jointWeightPose : jointWeightPoses)
        {
            vertexWeights.put(vertex, jointWeightPose);
        }
    }

    Multimap<Vertex, JointWeightPose> getVertexWeights()
    {
        return vertexWeights.build();
    }

    Iterable<Face> getFaces()
    {
        return faces.build();
    }

    public Brush getBrush()
    {
        return brush;
    }
}

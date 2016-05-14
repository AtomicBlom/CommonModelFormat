package com.github.atomicblom.client.model.cmf.opengex.processors;

import com.github.atomicblom.client.model.cmf.common.Brush;
import com.github.atomicblom.client.model.cmf.common.Mesh;
import com.github.atomicblom.client.model.cmf.opengex.OpenGEXException;
import com.github.atomicblom.client.model.cmf.opengex.ogex.*;
import com.google.common.collect.Lists;
import javax.vecmath.Matrix4f;
import java.util.List;

public class MeshProcessor
{
    private final SceneProcessor sceneProcessor;
    private final Matrix4f upMatrix;

    public MeshProcessor(SceneProcessor sceneProcessor, Matrix4f upMatrix)
    {
        this.sceneProcessor = sceneProcessor;
        this.upMatrix = upMatrix;
    }

    List<Mesh> createMeshes(OgexGeometryNode ogexGeometryNode) {
        final List<Mesh> meshes = Lists.newArrayList();

        final OgexMesh ogexMesh = ogexGeometryNode.getGeometry().getMesh();
        final MeshType type = ogexMesh.getType();
        if (type != MeshType.Quads && type != MeshType.Triangles)
        {
            throw new OpenGEXException("Attempting to generate a cmf for an unsupported OpenGL Mesh Type: " + type);
        }

        final List<Brush> brushList = getBrushesForGeometryNode(ogexGeometryNode);

        if (brushList.isEmpty()) {
            return meshes;
        }

        final SubMeshProcessor subMeshProcessor = new SubMeshProcessor(sceneProcessor, upMatrix, ogexMesh, brushList);

        for (final OgexIndexArray indexArray : ogexMesh.getIndexArrays())
        {
            subMeshProcessor.processPartialMesh(indexArray);
        }

        for (final BrushSubMeshCombiner brushSubMeshCombiner : subMeshProcessor.getCombinedBrushes())
        {

            final Mesh mesh = new Mesh(brushSubMeshCombiner.getBrush(), brushSubMeshCombiner.getFaces());

            sceneProcessor.associateMeshWithVertexWeights(mesh, brushSubMeshCombiner.getVertexWeights());

            meshes.add(mesh);
        }

        return meshes;
    }

    private List<Brush> getBrushesForGeometryNode(OgexGeometryNode ogexGeometryNode)
    {
        final List<Brush> brushList = Lists.newArrayList();
        final Iterable<OgexMaterial> materials = ogexGeometryNode.getMaterials();
        for (final OgexMaterial ogexMaterial : materials) {
            final Brush brush = sceneProcessor.getBrushForMaterial(ogexMaterial);
            brushList.add(brush);
        }
        return brushList;
    }
}

package com.github.atomicblom.client.model.cmf.opengex.conversion;

import com.github.atomicblom.client.model.cmf.common.Brush;
import com.github.atomicblom.client.model.cmf.common.Mesh;
import com.github.atomicblom.client.model.cmf.opengex.OpenGEXException;
import com.github.atomicblom.client.model.cmf.opengex.ogex.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import javax.vecmath.Matrix4f;
import java.util.Map;

public class MeshBuilderContext
{
    private static final org.apache.logging.log4j.Logger Logger = LogManager.getLogger();

    private final NodeBuilderContext nodeBuilderContext;
    private final ImmutableList<Brush> brushList;
    private final Map<Brush, SubMeshBuilderContext> listToBuild = Maps.newHashMap();
    private final OgexMesh ogexMesh;
    private final int[] boneIndexTransformation;

    MeshBuilderContext(NodeBuilderContext nodeBuilderContext, OgexGeometryNode ogexGeometryNode)
    {

        this.nodeBuilderContext = nodeBuilderContext;
        brushList = buildBrushList(nodeBuilderContext.getBrushSet(), ogexGeometryNode);
        ogexMesh = ogexGeometryNode.getGeometry().getMesh();

        final OgexSkin skin = ogexMesh.getSkin();
        boneIndexTransformation = buildBoneIndexTransformation(skin);
    }

    private static ImmutableList<Brush> buildBrushList(ImmutableMap<OgexMaterial, Brush> brushSet, OgexGeometryNode ogexGeometryNode)
    {
        final ImmutableList.Builder<Brush> nodeBrushes = ImmutableList.builder();

        final Iterable<OgexMaterial> materials = ogexGeometryNode.getMaterials();
        for (final OgexMaterial ogexMaterial : materials) {
            final Brush brush = brushSet.get(ogexMaterial);
            if (brush != null) {
                nodeBrushes.add(brush);
            }
        }
        return nodeBrushes.build();
    }

    private static int[] buildBoneIndexTransformation(OgexSkin skin)
    {
        final int[] boneIndexTransform;
        if (skin != null) {
            final IntArray boneCount = skin.getBoneCount();
            boneIndexTransform = new int[boneCount.length()];
            int currentBoneIndex = 0;
            final int[] boneCounts = boneCount.asIntArray();
            for (int i = 0; i < boneCounts.length; i++)
            {
                boneIndexTransform[i] = currentBoneIndex;
                currentBoneIndex += boneCounts[i];
            }
        } else {
            boneIndexTransform = new int[0];
        }
        return boneIndexTransform;
    }

    public ImmutableList<Mesh> finish()
    {
        ImmutableList.Builder<Mesh> meshes = ImmutableList.builder();

        for (final Map.Entry<Brush, SubMeshBuilderContext> brushPairEntry : listToBuild.entrySet())
        {
            final SubMeshBuilderContext subMeshBuilderContext = brushPairEntry.getValue();

            final Mesh mesh = new Mesh(brushPairEntry.getKey(), subMeshBuilderContext.getFaces().build());

            nodeBuilderContext.addUnprocessedMeshJoint(mesh, subMeshBuilderContext.getVertexWeights());

            meshes.add(mesh);
        }

        return meshes.build();
    }

    public boolean hasBrushes()
    {
        return !brushList.isEmpty();
    }

    public SubMeshBuilderContext startNewSubMeshWithMaterial(OgexIndexArray indexArray)
    {
        long material = indexArray.getMaterial();

        if (material >= brushList.size()) {
            material = brushList.size() - 1;
            Logger.warn("Model has been exported with multiple Material IDs, but not enough have been defined in the export.");
        }
        if (material > Integer.MAX_VALUE) {
            material = 0;
            Logger.warn("Material index exceeded the bounds of an integer, it has been replaced with index 0. (How many materials do you have!?)");
        }

        final Brush brush = brushList.get((int)material);

        if (!listToBuild.containsKey(brush)) {
            listToBuild.put(brush, new SubMeshBuilderContext(this, indexArray, brush, ogexMesh.getType()));
        }
        return listToBuild.get(brush);
    }

    public MeshType getGeometryType()
    {
        return ogexMesh.getType();
    }

    public Iterable<OgexIndexArray> getSubMeshes()
    {
        return ogexMesh.getIndexArrays();
    }

    public OgexMesh getMesh()
    {
        return ogexMesh;
    }

    Matrix4f getUpMatrix()
    {
        return nodeBuilderContext.getUpMatrix();
    }

    int getStartingBoneIndex(int vertexIndex)
    {
        if (vertexIndex >= boneIndexTransformation.length) {
            throw new OpenGEXException("Attempt to get the bone index of a non-existent bone.");
        }

        return boneIndexTransformation[vertexIndex];
    }
}

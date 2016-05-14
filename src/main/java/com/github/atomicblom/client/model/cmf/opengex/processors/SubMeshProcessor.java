package com.github.atomicblom.client.model.cmf.opengex.processors;

import com.github.atomicblom.client.model.cmf.common.Brush;
import com.github.atomicblom.client.model.cmf.common.Vertex;
import com.github.atomicblom.client.model.cmf.opengex.ogex.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.util.List;
import java.util.Map;

class SubMeshProcessor
{
    private final SceneProcessor sceneProcessor;
    private final Matrix4f upMatrix;
    private final Matrix4f upMatrixInverted;
    private final OgexMesh ogexMesh;
    private final List<Brush> brushList;
    private int[] boneIndexTransformation = new int[0];
    private boolean boneIndexTransformPending = true;
    private final Map<Brush, BrushSubMeshCombiner> brushesToBuild = Maps.newHashMap();

    SubMeshProcessor(SceneProcessor sceneProcessor, Matrix4f upMatrix, OgexMesh ogexMesh, Iterable<Brush> brushList) {
        this.sceneProcessor = sceneProcessor;

        this.upMatrix = upMatrix;
        (this.upMatrixInverted = new Matrix4f(upMatrix)).invert();
        this.ogexMesh = ogexMesh;
        this.brushList = ImmutableList.copyOf(brushList);
    }

    private void buildBoneIndexTransformation()
    {
        int[] boneIndexTransform = new int[0];
        final OgexSkin skin = ogexMesh.getSkin();
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
        }
        boneIndexTransformation = boneIndexTransform;
        boneIndexTransformPending = false;
    }


    void processPartialMesh(OgexIndexArray indexArray)
    {
        if (boneIndexTransformPending) {
            buildBoneIndexTransformation();
        }
        final BrushSubMeshCombiner brushSubMeshCombiner = getBrushContentsForIndexArray(indexArray);


        for (final long[] polygonGroup : (long[][]) indexArray.getArray())
        {
            processPolygonGroup(polygonGroup, brushSubMeshCombiner);
        }
    }

    private void processPolygonGroup(long[] polyGroup, BrushSubMeshCombiner brushSubMeshCombiner)
    {
        final Vertex[] vertices = new Vertex[polyGroup.length];
        Vector3f normal = new Vector3f();

        for (int i = 0; i < polyGroup.length; i++)
        {
            final int vertexIndex = (int) polyGroup[i];
            final Pair<Vertex, Vector3f> createVertexResult = getVertex(ogexMesh, vertexIndex);
            vertices[i] = createVertexResult.getLeft();
            normal = createVertexResult.getRight();

            final OgexSkin skin = ogexMesh.getSkin();
            if (skin != null) {
                final ImmutableList<JointWeightPose> jointWeightPoses = createJointWeightPoseMatricesForVertex(vertexIndex, skin);
                brushSubMeshCombiner.addJointWeightPoseToVertex(vertices[i], jointWeightPoses);
            }
        }

        brushSubMeshCombiner.addFace(vertices, normal);
    }

    private ImmutableList<JointWeightPose> createJointWeightPoseMatricesForVertex(int vertexIndex, OgexSkin skin)
    {
        final Builder<JointWeightPose> jointWeightPoses = ImmutableList.builder();

        final int boneCount = skin.getBoneCount().asIntArray()[vertexIndex];
        final int startingBoneIndex = boneIndexTransformation[vertexIndex];

        for (int boneOffset = 0; boneOffset < boneCount; ++boneOffset) {
            final int boneIndex = skin.getBoneIndex().asIntArray()[startingBoneIndex + boneOffset];

            final OgexBoneNode ogexNode = (OgexBoneNode)skin.getSkeleton().getBoneNodes()[boneIndex];
            final float jointWeight = skin.getBoneWeight()[startingBoneIndex + boneOffset];

            final Matrix4f poseTranformation = new Matrix4f(skin.getSkeleton().getTransforms()[boneIndex]);
            poseTranformation.transpose();
            poseTranformation.invert();
            poseTranformation.mul(upMatrix, poseTranformation);
            poseTranformation.mul(upMatrixInverted);

            jointWeightPoses.add(new JointWeightPose(jointWeight, ogexNode, poseTranformation));
        }

        return jointWeightPoses.build();
    }


    Iterable<BrushSubMeshCombiner> getCombinedBrushes()
    {
        return brushesToBuild.values();
    }

    private BrushSubMeshCombiner getBrushContentsForIndexArray(OgexIndexArray indexArray) {
        int material = (int) indexArray.getMaterial();

        if (material >= brushList.size()) {
            material = brushList.size() - 1;
            sceneProcessor.warn("Model has been exported with multiple Material IDs, but not enough have been defined in the export.");
        }

        final Brush brush = brushList.get(material);

        if (!brushesToBuild.containsKey(brush)) {
            brushesToBuild.put(brush, new BrushSubMeshCombiner(brush, ogexMesh.getType()));
        }
        return brushesToBuild.get(brush);
    }

    private Pair<Vertex, Vector3f> getVertex(OgexMesh mesh, int vertexIndex)
    {
        float[] positionArray = null;
        float[] normalArray = null;
        float[] texcoordArray = null;
        float[] colourArray = null;

        for (final OgexVertexArray array : mesh.getVertexArrays())
        {
            if ("position".equals(array.getName()))
            {
                positionArray = array.getArray2()[vertexIndex];
            } else if ("normal".equals(array.getName()))
            {
                normalArray = array.getArray2()[vertexIndex];
            } else if ("color".equals(array.getName()))
            {
                colourArray = array.getArray2()[vertexIndex];
            } else if ("texcoord".equals(array.getName()))
            {
                texcoordArray = array.getArray2()[vertexIndex];
            }
        }

        final Vector3f position = new Vector3f(0, 0, 0);
        final Vector4f colour = new Vector4f(1, 1, 1, 1);
        final Vector3f normal = new Vector3f(0, 0, 0);
        Vector4f[] uvs = { new Vector4f(0.5f, 0.4f, 0, 1) };

        if (positionArray != null)
        {
            position.set(positionArray);
            upMatrix.transform(position);
        }

        if (normalArray != null)
        {
            normal.set(normalArray);
            upMatrix.transform(normal);
        }

        if (colourArray != null)
        {
            colour.set(colourArray);
        }

        if (texcoordArray != null)
        {
            uvs = new Vector4f[] {
                    new Vector4f(texcoordArray[0], 1 - texcoordArray[1], 0, 1)
            };
        }

        final Vertex vertex = new Vertex(position, normal, colour, uvs);

        return Pair.of(vertex, normal);
    }

}

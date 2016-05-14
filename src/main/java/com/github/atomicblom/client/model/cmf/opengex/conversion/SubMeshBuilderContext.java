package com.github.atomicblom.client.model.cmf.opengex.conversion;

import com.github.atomicblom.client.model.cmf.common.Brush;
import com.github.atomicblom.client.model.cmf.common.Face;
import com.github.atomicblom.client.model.cmf.common.Vertex;
import com.github.atomicblom.client.model.cmf.opengex.OpenGEXException;
import com.github.atomicblom.client.model.cmf.opengex.ogex.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.minecraftforge.common.model.TRSRTransformation;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class SubMeshBuilderContext
{
    private final MeshBuilderContext meshBuilderContext;
    private final OgexIndexArray indexArray;
    private final Brush brush;
    private final MeshType type;
    private final ImmutableList.Builder<Face> faces = ImmutableList.builder();
    private final ImmutableMultimap.Builder<Vertex, JointWeightPose> vertexWeights = ImmutableMultimap.builder();

    SubMeshBuilderContext(MeshBuilderContext meshBuilderContext, OgexIndexArray indexArray, Brush brush, MeshType type) {
        this.meshBuilderContext = meshBuilderContext;
        this.indexArray = indexArray;
        this.brush = brush;
        this.type = type;
    }

    public void addFace(Vertex[] vertices, Vector3f normal)
    {
        final Face face;
        if (type == MeshType.Triangles) {
            face = new Face(vertices[0], vertices[1], vertices[2], brush, normal);
        } else if (type == MeshType.Quads) {
            face = new Face(vertices[0], vertices[1], vertices[2], vertices[3], brush, normal);
        } else {
            throw new OpenGEXException("Attempting to generate a cmf for an unsupported OpenGL Mesh Type: " + type);
        }
        faces.add(face);
    }

    public void addJointWeightPoseToVertex(Vertex vertice, float jointWeight, OgexBoneNode ogexBoneNode, Matrix4f poseTranformation)
    {
        vertexWeights.put(vertice, new JointWeightPose(jointWeight, ogexBoneNode, new TRSRTransformation(poseTranformation)));
    }

    public void finish()
    {
        //FIXME: What do I do here again?
    }

    Multimap<Vertex, JointWeightPose> getVertexWeights() {
        return vertexWeights.build();
    }

    public List<OgexVertexArray> getVertexArrays()
    {
        return meshBuilderContext.getMesh().getVertexArrays();
    }

    public long[][] getIndexArrays()
    {

        final Object array = indexArray.getArray();
        if (!(array instanceof long[][])) {
            throw new OpenGEXException("Index array was an unexpected type: " + array.getClass());
        }
        return (long[][]) array;
    }

    public Matrix4f getUpMatrix()
    {
        return meshBuilderContext.getUpMatrix();
    }

    public Iterable<JointWeightTransformation> iterateBones(int vertexIndex)
    {
        final OgexSkin skin = meshBuilderContext.getMesh().getSkin();

        if (skin == null)
        {
            return Lists.newArrayList();
        }

        final int boneCount = skin.getBoneCount().asIntArray()[vertexIndex];
        final int startingBoneIndex = meshBuilderContext.getStartingBoneIndex(vertexIndex);

        return new Iterable<JointWeightTransformation>()
        {
            @Override
            public Iterator<JointWeightTransformation> iterator()
            {
                return new JointWeightTransformationIterator(boneCount, skin, startingBoneIndex);
            }
        };
    }

    ImmutableList.Builder<Face> getFaces()
    {
        return faces;
    }

    public static class JointWeightTransformation
    {
        private final float jointWeight;
        private final OgexBoneNode ogexBoneNode;
        private final Matrix4f poseTranformation;

        JointWeightTransformation(float jointWeight, OgexBoneNode ogexBoneNode, Matrix4f poseTranformation) {
            this.jointWeight = jointWeight;
            this.ogexBoneNode = ogexBoneNode;
            this.poseTranformation = poseTranformation;
        }

        public float getJointWeight()
        {
            return jointWeight;
        }

        public OgexBoneNode getOgexBoneNode()
        {
            return ogexBoneNode;
        }

        public Matrix4f getPoseTranformation()
        {
            return poseTranformation;
        }

        @Override
        public String toString()
        {
            return "JointWeightTransformation{" +
                    "jointWeight=" + jointWeight +
                    ", ogexBoneNode=" + ogexBoneNode +
                    ", poseTranformation=" + poseTranformation +
                    '}';
        }
    }

    private static class JointWeightTransformationIterator implements Iterator<JointWeightTransformation>
    {

        private final int boneCount;
        private final OgexSkin skin;
        private final int startingBoneIndex;
        private int boneOffset;

        private JointWeightTransformationIterator(int boneCount, OgexSkin skin, int startingBoneIndex)
        {

            this.boneCount = boneCount;
            this.skin = skin;
            this.startingBoneIndex = startingBoneIndex;
        }

        @Override
        public boolean hasNext()
        {
            return boneOffset < boneCount;
        }

        @Override
        public JointWeightTransformation next()
        {
            if (boneOffset >= boneCount) {
                throw new NoSuchElementException("attempt to iterate past the end of the bone list");
            }

            final int boneIndex = skin.getBoneIndex().asIntArray()[startingBoneIndex + boneOffset];

            final OgexBoneNode ogexNode = (OgexBoneNode) skin.getSkeleton().getBoneNodes()[boneIndex];
            final float jointWeight = skin.getBoneWeight()[startingBoneIndex + boneOffset];

            final Matrix4f poseTranformation = new Matrix4f(skin.getSkeleton().getTransforms()[boneIndex]);

            ++boneOffset;
            return new JointWeightTransformation(jointWeight, ogexNode, poseTranformation);
        }

        @Override
        public void remove()
        {
            throw new OpenGEXException("This iterator is read-only, and I'm only implementing this because I have to.");
        }

        @Override
        public String toString()
        {
            return "JointWeightTransformationIterator{" +
                    "boneCount=" + boneCount +
                    ", skin=" + skin +
                    ", startingBoneIndex=" + startingBoneIndex +
                    ", boneOffset=" + boneOffset +
                    '}';
        }
    }
}

package com.github.atomicblom.client.model.cmf.opengex;

import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexRotation.AxisRotation;
import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexRotation.ComponentRotation;
import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexRotation.QuaternionRotation;
import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexScale.ComponentScale;
import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexScale.XyzScale;
import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexTranslation.ComponentTranslation;
import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexTranslation.XyzTranslation;
import com.google.common.collect.*;
import com.github.atomicblom.client.model.cmf.common.*;
import com.github.atomicblom.client.model.cmf.opengex.ogex.*;
import com.google.common.collect.ImmutableMultimap.Builder;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.vecmath.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.Map.Entry;

class Parser {
    private static final Logger Logger = LogManager.getLogger();

    private final InputStream inputStream;
    private final Matrix4f upMatrix = new Matrix4f();
    private final Matrix4f upMatrixInverted = new Matrix4f();

    Parser(InputStream inputStream)
    {
        this.inputStream = inputStream;
        upMatrix.setIdentity();
        upMatrixInverted.setIdentity();
    }

    private final Map<OgexMaterial, Brush> brushes = Maps.newHashMap();
    private final Map<OgexBoneNode, Node<Bone>> ogexBoneToForgeBoneMap = Maps.newHashMap();
    private final List<Texture> textures = Lists.newArrayList();
    private final Queue<UnprocessedMeshBoneMapEntry> meshBoneMapQueue = Lists.newLinkedList();

    Model parse() throws IOException {
        final OgexParser ogexParser = new OgexParser();
        final Reader reader = new InputStreamReader(inputStream);
        final OgexScene ogexScene = ogexParser.parseScene(reader);
        final Axis up = ogexScene.getMetrics().getUp();
        upMatrix.set(getMatrixForUpAxis(up));
        upMatrixInverted.set(upMatrix);
        upMatrixInverted.invert();

        getBrushes(ogexScene);
        final Node<?> rootNode = createNode(ogexScene);

        processMeshBoneMapQueue();

        //Meshes is not currently used.
        Model model = new Model(textures, brushes.values(), rootNode);
        return model;
    }

    private static Matrix4f getMatrixForUpAxis(Axis up)
    {
        final Matrix4f upMatrix = new Matrix4f();
        if (up == Axis.Y) {
            upMatrix.setIdentity();
        } else if (up == Axis.Z) {
            upMatrix.m01 = 1;
            upMatrix.m12 = 1;
            upMatrix.m20 = 1;
            upMatrix.m33 = 1;
        } else {
            upMatrix.m02 = 1;
            upMatrix.m10 = 1;
            upMatrix.m21 = 1;
            upMatrix.m33 = 1;
        }
        return upMatrix;
    }

    private void processMeshBoneMapQueue()
    {
        for (final UnprocessedMeshBoneMapEntry meshBoneMapEntry : meshBoneMapQueue)
        {
            final Mesh mesh = meshBoneMapEntry.mesh;
            final Builder<Vertex, BoneWeight> boneWeightMapBuilder = ImmutableMultimap.builder();
            final Set<Node<Bone>> bonesUsed = Sets.newHashSet();

            for (final Vertex vertex : meshBoneMapEntry.getVertices())
            {
                for (final BoneWeightPose boneWeightPose : meshBoneMapEntry.getBoneWeightPose(vertex))
                {
                    final OgexBoneNode ogexBoneNode = boneWeightPose.ogexBoneNode;
                    final TRSRTransformation invBindPose = boneWeightPose.invertedBindPose;

                    final Node<Bone> boneNode = ogexBoneToForgeBoneMap.get(ogexBoneNode);
                    boneNode.getKind().getData().add(new VertexWeight(vertex, boneWeightPose.weight));
                    // TODO check if bone instance can be shared with multiple inv bind poses, and make a copy if true
                    boneNode.getKind().setInvBindPose(invBindPose);
                    boneWeightMapBuilder.put(vertex, new BoneWeight(boneNode, boneWeightPose.weight));
                    if (!bonesUsed.contains(boneNode))
                    {
                        bonesUsed.add(boneNode);
                    }
                }
            }
            mesh.setBones(bonesUsed);
            mesh.setWeightMap(boneWeightMapBuilder.build());
        }
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private Node<?> createNode(OpenGEXNode openGEXNode) {
        if (openGEXNode instanceof OgexCameraNode) {
            return null;
        }
        if (openGEXNode instanceof OgexLightNode) {
            return null;
        }

        final List<Node<?>> childNodes = Lists.newArrayList();
        for (final OgexNode childOgexNode : openGEXNode) {
            final Node<?> childNode = createNode(childOgexNode);
            if (childNode != null) {
                childNodes.add(childNode);
            }
        }

        TRSRTransformation objectOnlyTrsr = TRSRTransformation.identity();
        TRSRTransformation optionalGlobalTrsr = TRSRTransformation.identity();
        String name = "";
        IAnimation animation = null;

        boolean hasObjectOnly = false;

        if (openGEXNode instanceof OgexNode) {
            final OgexNode ogexNode = (OgexNode) openGEXNode;
            final Matrix4f actualNodeTransformation = new Matrix4f();
            actualNodeTransformation.setIdentity();
            final Matrix4f objectOnlyTransformations = new Matrix4f();
            objectOnlyTransformations.setIdentity();

            final Matrix4f transform = new Matrix4f();
            for (final OgexTransform ogexTransform : ogexNode.getTransforms()) {
                transform.set(ogexTransform.toMatrix());
                transform.mul(upMatrix, transform);
                transform.mul(upMatrixInverted);

                if (ogexTransform.isObjectOnly()) {
                    hasObjectOnly = true;
                    objectOnlyTransformations.mul(transform);
                } else
                {
                    actualNodeTransformation.mul(transform);
                }
            }
            //TODO: Are these transformations assigned to the right children?
            objectOnlyTrsr = new TRSRTransformation(actualNodeTransformation);
            optionalGlobalTrsr = new TRSRTransformation(objectOnlyTransformations);

            name = ogexNode.getName();
            // FIXME more than 1 clip
            if(!ogexNode.getAnimations().isEmpty()) {
                final OgexAnimation ogexAnimation = ogexNode.getAnimations().iterator().next();
                animation = new OpenGEXAnimation(ogexNode.getTransforms(), ogexAnimation, upMatrix);
            }
        }

        Node<?> node;
        if (openGEXNode instanceof OgexGeometryNode) {
            final OgexGeometryNode ogexGeometryNode = (OgexGeometryNode) openGEXNode;
            final List<Mesh> createdMeshes = createMeshes(ogexGeometryNode);
            if (createdMeshes.size() == 1) {
                final Mesh mesh = createdMeshes.get(0);
                node = mesh != null ?
                        Node.create(name, objectOnlyTrsr, childNodes, mesh) :
                        Node.create(name, objectOnlyTrsr, childNodes, new Pivot());
            } else {
                int itemIndex = 0;
                for (final Mesh mesh : createdMeshes)
                {
                    childNodes.add(Node.create(name + "-MeshChild#" + itemIndex, objectOnlyTrsr, Lists.<Node<?>>newArrayList(), mesh));
                    itemIndex++;
                }

                node = Node.create(name, objectOnlyTrsr, childNodes, new Pivot());
            }
        } else
        {
            if (openGEXNode instanceof OgexBoneNode)
            {
                final OgexBoneNode ogexBoneNode = (OgexBoneNode) openGEXNode;
                final Node<Bone> boneNode = Node.create(name, objectOnlyTrsr, childNodes, new Bone());
                ogexBoneToForgeBoneMap.put(ogexBoneNode, boneNode);
                node = boneNode;
            } else
            {
                node = Node.create(name, objectOnlyTrsr, childNodes, new Pivot());
            }
        }

        if (hasObjectOnly) {
            final List<Node<?>> autoNodeChildren = Lists.<Node<?>>newArrayList(node);
            node = Node.create(name + "-Auto-ObjectOnlySeperator", optionalGlobalTrsr, autoNodeChildren, new Pivot());
        }

        if(animation != null)
        {
            //FIXME: It's entirely possible that an object-only element has an animation against it. The animation will need to be split up if this ever occurs.
            node.setAnimation(animation);
        }
        return node;
    }

    private List<Mesh> createMeshes(OgexGeometryNode ogexGeometryNode) {
        final List<Mesh> meshes = Lists.newArrayList();

        final OgexMesh ogexMesh = ogexGeometryNode.getGeometry().getMesh();
        final MeshType type = ogexMesh.getType();
        if (type != MeshType.Quads && type != MeshType.Triangles)
        {
            throw new OpenGEXException("Attempting to generate a cmf for an unsupported OpenGL Mesh Type: " + type);
        }

        final OgexSkin skin = ogexMesh.getSkin();
        final int[] boneIndexTransform = getBoneIndexTransformation(skin);

        final List<Brush> brushList = getBrushesForGeometryNode(ogexGeometryNode);

        if (brushList.isEmpty()) {
            return meshes;
        }

        final Map<Brush, BrushContents> listToBuild = Maps.newHashMap();

        for (final OgexIndexArray indexArray : ogexMesh.getIndexArrays())
        {
            int material = (int) indexArray.getMaterial();
            if (material >= brushList.size()) {
                material = brushList.size() - 1;
                Logger.warn("Model has been exported with multiple Material IDs, but not enough have been defined in the export.");
            }

            final Brush brush = brushList.get(material);

            if (!listToBuild.containsKey(brush)) {
                listToBuild.put(brush, new BrushContents(brush, type));
            }
            final BrushContents brushContents = listToBuild.get(brush);

            processPartialMesh(ogexMesh, boneIndexTransform, indexArray, brushContents);
        }

        for (final Entry<Brush, BrushContents> brushPairEntry : listToBuild.entrySet())
        {
            final BrushContents brushContents = brushPairEntry.getValue();

            final Mesh mesh = new Mesh(brushPairEntry.getKey(), brushContents.faces);

            meshBoneMapQueue.add(new UnprocessedMeshBoneMapEntry(mesh, brushContents.getVertexWeights()));

            meshes.add(mesh);
        }

        return meshes;
    }

    private void processPartialMesh(OgexMesh ogexMesh, int[] boneIndexTransform, OgexIndexArray indexArray, BrushContents brushContents)
    {
        final OgexSkin skin = ogexMesh.getSkin();
        for (final long[] polyGroup : (long[][]) indexArray.getArray())
        {
            final Vertex[] vertices = new Vertex[polyGroup.length];
            Vector3f normal = new Vector3f();

            for (int i = 0; i < polyGroup.length; i++)
            {
                final int vertexIndex = (int) polyGroup[i];
                final Pair<Vertex, Vector3f> createVertexResult = getVertex(ogexMesh, vertexIndex);
                vertices[i] = createVertexResult.getLeft();
                normal = createVertexResult.getRight();

                if (skin != null) {
                    final int boneCount = skin.getBoneCount().asIntArray()[vertexIndex];
                    final int startingBoneIndex = boneIndexTransform[vertexIndex];

                    for (int boneOffset = 0; boneOffset < boneCount; ++boneOffset) {
                        final int boneIndex = skin.getBoneIndex().asIntArray()[startingBoneIndex + boneOffset];

                        final OgexBoneNode ogexNode = (OgexBoneNode)skin.getSkeleton().getBoneNodes()[boneIndex];
                        final float boneWeight = skin.getBoneWeight()[startingBoneIndex + boneOffset];

                        final Matrix4f poseTranformation = new Matrix4f(skin.getSkeleton().getTransforms()[boneIndex]);
                        poseTranformation.transpose();
                        poseTranformation.invert();
                        poseTranformation.mul(upMatrix, poseTranformation);
                        poseTranformation.mul(upMatrixInverted);

                        final BoneWeightPose boneWeightPose = new BoneWeightPose(boneWeight, ogexNode, poseTranformation);
                        brushContents.addBoneWeightPoseToVertex(vertices[i], boneWeightPose);
                    }
                }
            }

            brushContents.addFace(vertices, normal);
        }
    }

    private List<Brush> getBrushesForGeometryNode(OgexGeometryNode ogexGeometryNode)
    {
        final List<Brush> brushList = Lists.newArrayList();
        final Iterable<OgexMaterial> materials = ogexGeometryNode.getMaterials();
        for (final OgexMaterial ogexMaterial : materials) {
            final Brush brush = brushes.get(ogexMaterial);
            brushList.add(brush);
        }
        return brushList;
    }

    private static int[] getBoneIndexTransformation(OgexSkin skin)
    {
        int[] boneIndexTransform = null;
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
        return boneIndexTransform;
    }

    private Pair<Vertex, Vector3f> getVertex(OgexMesh mesh, int vertexIndex)
    {
        float[] positionArray = null;
        float[] normalArray = null;
        float[] texcoordArray = null;
        float[] colorArray = null;

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
                colorArray = array.getArray2()[vertexIndex];
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
            position.x = positionArray[0];
            position.y = positionArray[1];
            position.z = positionArray[2];
            upMatrix.transform(position);
        }

        if (normalArray != null)
        {
            normal.x = normalArray[0];
            normal.y = normalArray[1];
            normal.z = normalArray[2];
            upMatrix.transform(normal);
        }

        if (colorArray != null)
        {
            colour.x = colorArray[0];
            colour.y = colorArray[1];
            colour.z = colorArray[2];
            colour.w = colorArray[3];
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

    private void getBrushes(OgexScene ogexScene) {
        for (final OgexMaterial ogexMaterial : ogexScene.getMaterials()) {
            final OgexColor diffuse = ogexMaterial.getColor("diffuse");
            Vector4f color = new Vector4f(1, 1, 1, 0);
            if (diffuse != null) {
                color = new Vector4f(diffuse.getRed(), diffuse.getGreen(), diffuse.getBlue(), diffuse.getAlpha()) ;
            }
            final String name = ogexMaterial.getName();

            final List<Texture> brushTextures = Lists.newArrayList();

            Texture texture = getTextureFromOgexTexture(ogexMaterial.getTexture("diffuse"));
            texture = getSingleTextureFromList(texture, textures);

            brushTextures.add(texture);

            final int blend = 1;
            final int fx = 0;
            final float shininess = 0.0f;
            brushes.put(ogexMaterial, new Brush(name, color, shininess, blend, fx, brushTextures));
        }
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private static Texture getTextureFromOgexTexture(OgexTexture texture) {
        if (texture == null) {
            return Texture.White;
        }
        final String path = texture.getTexture();

        Vector2f scale = new Vector2f(1, 1);
        Vector2f translation = new Vector2f(0, 0);
        float rotation = 0.0f;
        for (final OgexTransform ogexTransform : texture.getTransforms()) {
            if (ogexTransform instanceof OgexScale) {
                scale = getOgexScale((OgexScale)ogexTransform);
            } else if (ogexTransform instanceof OgexRotation) {
                rotation = getOgexRotation((OgexRotation)ogexTransform);
            } else if (ogexTransform instanceof OgexTranslation) {
                translation = getOgexTranslation((OgexTranslation)ogexTransform);
            }
        }

        //I have no idea what opengex's intention was with these two flags.
        final int flags = 0;
        final int blend = 0;

        return new Texture(path, flags, blend, translation, scale, rotation);
    }

    @SuppressWarnings({"ChainOfInstanceofChecks", "UnnecessarilyQualifiedInnerClassAccess"})
    private static Vector2f getOgexTranslation(OgexTranslation ogexTransform)
    {
        final Vector2f translation = new Vector2f();
        if (ogexTransform instanceof ComponentTranslation) {
            final ComponentTranslation ogexTranslation = (ComponentTranslation) ogexTransform;
            if (ogexTranslation.getKind() == OgexTranslation.Kind.X)
            {
                translation.x = ogexTranslation.getTranslation();

            } else if (ogexTranslation.getKind() == OgexTranslation.Kind.Y)
            {
                translation.y = ogexTranslation.getTranslation();

            } else if (ogexTranslation.getKind() == OgexTranslation.Kind.Z)
            {
                throw new UnsupportedOperationException("OGEX: Attempt to translate a texture by the 3rd dimension?");
            }
        } else if (ogexTransform instanceof XyzTranslation) {
            final XyzTranslation ogexTranslation = (XyzTranslation) ogexTransform;
            translation.x = ogexTranslation.getTranslation()[0];
            translation.y = ogexTranslation.getTranslation()[1];
        }
        return translation;
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private static float getOgexRotation(OgexRotation ogexTransform)
    {
        float rotation = 0;
        if (ogexTransform instanceof AxisRotation) {
            final AxisRotation ogexRotation = (AxisRotation) ogexTransform;
            //note: I have *NO* idea which axis this comes in as.
            rotation = ogexRotation.getAngle();
        } else if (ogexTransform instanceof ComponentRotation) {
            final ComponentRotation ogexRotation = (ComponentRotation) ogexTransform;
            rotation = ogexRotation.getAngle();
        } else if (ogexTransform instanceof QuaternionRotation) {
            throw new UnsupportedOperationException("OGEX: Attempt to rotate a texture using a Quaternion");
        }
        return rotation;
    }

    @SuppressWarnings({"ChainOfInstanceofChecks", "UnnecessarilyQualifiedInnerClassAccess"})
    private static Vector2f getOgexScale(OgexScale ogexTransform)
    {
        final Vector2f scale = new Vector2f();
        if (ogexTransform instanceof ComponentScale) {
            final ComponentScale ogexScale = (ComponentScale) ogexTransform;

            if (ogexScale.getKind() == OgexScale.Kind.X)
            {
                scale.x = ogexScale.getScale();

            } else if (ogexScale.getKind() == OgexScale.Kind.Y)
            {
                scale.y = ogexScale.getScale();

            } else if (ogexScale.getKind() == OgexScale.Kind.Z)
            {
                throw new UnsupportedOperationException("OGEX: Attempt to scale a texture by the 3rd dimension?");
            }
        } else if (ogexTransform instanceof XyzScale) {
            final XyzScale ogexScale = (XyzScale) ogexTransform;
            scale.x = ogexScale.getScale()[0];
            scale.y = ogexScale.getScale()[1];
        }
        return scale;
    }

    private static Texture getSingleTextureFromList(Texture texture, List<Texture> textures) {
        for (final Texture existingTexture : textures) {
            boolean matches = existingTexture.getPath().equals(texture.getPath());
            matches &= existingTexture.getBlend() == texture.getBlend();
            matches &= existingTexture.getFlags() == texture.getFlags();
            matches &= existingTexture.getRot() == texture.getRot();
            matches &= existingTexture.getPos().x == texture.getPos().x;
            matches &= existingTexture.getPos().y == texture.getPos().y;
            matches &= existingTexture.getScale().x == texture.getScale().x;
            matches &= existingTexture.getScale().y == texture.getScale().y;

            if (matches) {
                return existingTexture;
            }
        }
        textures.add(texture);
        return texture;
    }

    @SuppressWarnings("ClassHasNoToStringMethod")
    private static class BoneWeightPose
    {
        private final Float weight;
        private final OgexBoneNode ogexBoneNode;
        private final TRSRTransformation invertedBindPose;

        private BoneWeightPose(Float weight, OgexBoneNode ogexBoneNode, Matrix4f invertedBindPose) {
            this.weight = weight;
            this.ogexBoneNode = ogexBoneNode;
            this.invertedBindPose = new TRSRTransformation(invertedBindPose);
        }
    }

    @SuppressWarnings("ClassHasNoToStringMethod")
    private static class UnprocessedMeshBoneMapEntry {
        private final Mesh mesh;
        private final Multimap<Vertex, BoneWeightPose> verticesToProcess;

        UnprocessedMeshBoneMapEntry(Mesh mesh, Multimap<Vertex, BoneWeightPose> verticesToProcess) {

            this.mesh = mesh;
            this.verticesToProcess = verticesToProcess;
        }

        Set<Vertex> getVertices() {
            return verticesToProcess.keySet();
        }

        Iterable<BoneWeightPose> getBoneWeightPose(Vertex vertex)
        {
            return verticesToProcess.get(vertex);
        }
    }

    private static class BrushContents
    {
        private final List<Face> faces = Lists.newArrayList();
        private final Builder<Vertex, BoneWeightPose> vertexWeights = ImmutableMultimap.builder();
        private final Brush brush;
        private final MeshType type;

        BrushContents(Brush brush, MeshType type)
        {
            this.brush = brush;

            this.type = type;
        }

        void addFace(Vertex[] vertices, Vector3f normal)
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

        void addBoneWeightPoseToVertex(Vertex vertex, BoneWeightPose boneWeightPose)
        {
            vertexWeights.put(vertex, boneWeightPose);
        }

        Multimap<Vertex, BoneWeightPose> getVertexWeights() {
            return vertexWeights.build();
        }
    }
}

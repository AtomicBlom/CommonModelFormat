package com.github.atomicblom.client.model.cmf.opengex;

import com.google.common.collect.*;
import com.github.atomicblom.client.model.cmf.common.*;
import com.github.atomicblom.client.model.cmf.opengex.ogex.*;
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

public class Parser {
    private static Logger Logger = LogManager.getLogger();

    private final InputStream inputStream;
    private Axis up;
    private Matrix4f upMatrix;
    private Matrix4f upMatrixInverted;

    public Parser(InputStream inputStream)
    {
        this.inputStream = inputStream;
    }

    private final Map<OgexMaterial, Brush> brushes = Maps.newHashMap();
    private final Map<OgexBoneNode, Node<Bone>> ogexBoneToForgeBoneMap = Maps.newHashMap();
    private final List<Texture> textures = Lists.newArrayList();
    private final Queue<Pair<Mesh, ImmutableMultimap<Vertex, Pair<Float, OgexBoneNode>>>> meshBoneMapQueue = Lists.newLinkedList();

    public Model parse() throws IOException {
        final OgexParser ogexParser = new OgexParser();
        final Reader reader = new InputStreamReader(inputStream);
        final OgexScene ogexScene = ogexParser.parseScene(reader);
        up = ogexScene.getMetrics().getUp();
        upMatrix = getMatrixForUpAxis(up);
        upMatrixInverted = new Matrix4f(upMatrix);
        upMatrixInverted.invert();

        getBrushes(ogexScene);
        final Node<?> rootNode = createNode(ogexScene);

        processMeshBoneMapQueue();

        //Meshes is not currently used.
        Model model = new Model(textures, brushes.values(), rootNode, null);
        return model;
    }

    private Matrix4f getMatrixForUpAxis(Axis up)
    {
        Matrix4f upMatrix = new Matrix4f();
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
        for (final Pair<Mesh, ImmutableMultimap<Vertex, Pair<Float, OgexBoneNode>>> meshImmutableMultimapPair : meshBoneMapQueue)
        {
            final Mesh mesh = meshImmutableMultimapPair.getLeft();
            final ImmutableMultimap.Builder<Vertex, Pair<Float, Node<Bone>>> boneWeightMapBuilder = ImmutableMultimap.builder();
            final Set bonesUsed = Sets.newHashSet();
            for (final Vertex vertex : meshImmutableMultimapPair.getRight().keySet())
            {
                final ImmutableCollection<Pair<Float, OgexBoneNode>> pairs = meshImmutableMultimapPair.getRight().get(vertex);
                for (final Pair<Float, OgexBoneNode> pair : pairs)
                {
                    final Float weight = pair.getLeft();
                    final OgexBoneNode ogexBoneNode = pair.getRight();

                    final Node<Bone> boneNode = ogexBoneToForgeBoneMap.get(ogexBoneNode);
                    boneNode.getKind().getData().add(Pair.of(vertex, weight));
                    boneWeightMapBuilder.put(vertex, Pair.of(weight, boneNode));
                    if (!bonesUsed.contains(boneNode)) {
                        bonesUsed.add(boneNode);
                    }
                }
            }
            mesh.setBones(bonesUsed);
            mesh.setWeightMap(boneWeightMapBuilder.build());

        }
    }

    private Node<?> createNode(OpenGEXNode openGEXNode) {
        final Node<?> node;

        if (openGEXNode instanceof OgexCameraNode) {
            return null;
        }
        if (openGEXNode instanceof OgexLightNode) {
            return null;
        }

        final List<Node<?>> childNodes = Lists.newArrayList();
        for (final OgexNode childOgexNode : openGEXNode) {
            Node<?> childNode = createNode(childOgexNode);
            if (childNode != null) {
                childNodes.add(childNode);
            }
        }
        TRSRTransformation trsr = TRSRTransformation.identity();
        String name = "";
        IAnimation animation = null;

        if (openGEXNode instanceof OgexNode) {
            final OgexNode ogexNode = (OgexNode) openGEXNode;
            trsr = getTRSRTransformationFromTransforms(ogexNode.getTransforms());
            name = ogexNode.getName();
            // FIXME more than 1 clip
            if(!ogexNode.getAnimations().isEmpty()) {
                final OgexAnimation ogexAnimation = ogexNode.getAnimations().iterator().next();
                animation = new OpenGEXAnimation(ogexNode.getTransforms(), ogexAnimation, upMatrix);
            }
        }

        if (openGEXNode instanceof OgexGeometryNode) {
            final OgexGeometryNode ogexGeometryNode = (OgexGeometryNode) openGEXNode;
            final List<Mesh> mesh1 = createMesh(ogexGeometryNode);
            if (mesh1.size() == 1) {
                final Mesh mesh = mesh1.get(0);
                if (mesh != null)
                {
                    node = Node.create(name, trsr, childNodes, mesh);
                } else
                {
                    node = Node.create(name, trsr, childNodes, new Pivot());
                }
            } else {
                int itemIndex = 0;
                for (final Mesh mesh : mesh1)
                {
                    childNodes.add(Node.create(name + "-MeshChild#" + (itemIndex++), trsr, Lists.<Node<?>>newArrayList(), mesh));
                }

                node = Node.create(name, trsr, childNodes, new Pivot());
            }


        } else
        {
            if (openGEXNode instanceof OgexBoneNode)
            {
                final OgexBoneNode ogexBoneNode = (OgexBoneNode) openGEXNode;
                Node<Bone> boneNode = Node.create(name, trsr, childNodes, new Bone());
                ogexBoneToForgeBoneMap.put(ogexBoneNode, boneNode);
                node = boneNode;
            } else
            {
                node = Node.create(name, trsr, childNodes, new Pivot());
            }
        }

        if(animation != null)
        {
            node.setAnimation(animation);
        }
        return node;
    }

    private List<Mesh> createMesh(OgexGeometryNode ogexGeometryNode) {
        List<Mesh> meshes = Lists.newArrayList();

        final OgexMesh mesh = ogexGeometryNode.getGeometry().getMesh();
        final MeshType type = mesh.getType();
        if (type != MeshType.Quads && type != MeshType.Triangles)
        {
            throw new OpenGEXException("Attempting to generate a cmf for an unsupported OpenGL Mesh Type: " + type);
        }

        final OgexSkin skin = mesh.getSkin();
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

        final List<Brush> brushList = Lists.newArrayList();
        final Iterable<OgexMaterial> materials = ogexGeometryNode.getMaterials();
        for (final OgexMaterial ogexMaterial : materials) {
            final Brush brush = brushes.get(ogexMaterial);
            brushList.add(brush);
        }

        if (brushList.isEmpty()) {
            return meshes;
        }

        final List<OgexVertexArray> vertexArrays = mesh.getVertexArrays();

        final Set<Integer> mappedVertices = Sets.newHashSet();

        Map<Brush, Pair<ArrayList<Face>, ImmutableMultimap.Builder<Vertex, Pair<Float, OgexBoneNode>>>> listToBuild = Maps.newHashMap();

        for (final OgexIndexArray indexArray : mesh.getIndexArrays())
        {
            int material = (int) indexArray.getMaterial();
            if (material >= brushList.size()) {
                material = brushList.size() - 1;
                Logger.warn("Model has been exported with multiple Material IDs, but not enough have been defined in the export.");
            }

            final Brush brush = brushList.get(material);

            if (!listToBuild.containsKey(brush)) {
                listToBuild.put(brush,
                        Pair.of(Lists.<Face>newArrayList(), ImmutableMultimap.<Vertex, Pair<Float,OgexBoneNode>>builder()));
            }
            final Pair<ArrayList<Face>, ImmutableMultimap.Builder<Vertex, Pair<Float, OgexBoneNode>>> arrayListBuilderPair = listToBuild.get(brush);
            final List<Face> faces = arrayListBuilderPair.getLeft();
            final ImmutableMultimap.Builder<Vertex, Pair<Float, OgexBoneNode>> boneWeightMapBuilder = arrayListBuilderPair.getRight();

            for (final long[] polyGroup : (long[][]) indexArray.getArray())
            {
                final Vertex[] vertices = new Vertex[polyGroup.length];
                final Vector3f normal = new Vector3f();

                for (int i = 0; i < polyGroup.length; i++)
                {
                    final long longVertexIndex = polyGroup[i];
                    final int vertexIndex = (int) longVertexIndex;

                    float[] positionArray = null;
                    float[] normalArray = null;
                    float[] texcoordArray = null;
                    float[] colorArray = null;

                    for (final OgexVertexArray array : vertexArrays)
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

                    final Vector3f position = new Vector3f();
                    final Vector4f colour = new Vector4f();

                    position.x = positionArray[0];
                    position.y = positionArray[1];
                    position.z = positionArray[2];

                    normal.x = normalArray[0];
                    normal.y = normalArray[1];
                    normal.z = normalArray[2];

                    upMatrix.transform(position);
                    upMatrix.transform(normal);

                    //FIXME: don't assume arrays are populated, update for appropriate format.

                    if (colorArray != null)
                    {
                        colour.x = colorArray[0];
                        colour.y = colorArray[1];
                        colour.z = colorArray[2];
                        colour.w = colorArray[3];
                    } else {
                        colour.x = 1;
                        colour.y = 1;
                        colour.z = 1;
                        colour.w = 1;
                    }

                    final Vector4f[] uvs;
                    if (texcoordArray != null)
                    {
                        uvs = new Vector4f[] {
                                new Vector4f(texcoordArray[0], 1 - texcoordArray[1], 0, 1)
                        };
                    } else {
                        uvs = new Vector4f[] {
                                new Vector4f(0.5f, 0.4f, 0, 1)
                        };
                    }

                    final Vertex vertex = new Vertex(position, normal, colour, uvs);

                    if (skin != null /*&& !mappedVertices.contains(vertexIndex)*/) {
                        final int boneCount = skin.getBoneCount().asIntArray()[vertexIndex];
                        final int aBone = boneIndexTransform[vertexIndex];
                        for (int boneId = 0; boneId < boneCount; ++boneId) {
                            final int boneIndex = skin.getBoneIndex().asIntArray()[aBone + boneId];
                            final OgexBoneNode ogexNode = (OgexBoneNode)skin.getSkeleton().getBoneNodes()[boneIndex];
                            final float boneWeight = skin.getBoneWeight()[aBone + boneId];

                            boneWeightMapBuilder.put(vertex, Pair.of(boneWeight, ogexNode));
                        }
                        mappedVertices.add(vertexIndex);
                    }

                    vertices[i] = vertex;
                }

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
        }

        for (final Map.Entry<Brush, Pair<ArrayList<Face>, ImmutableMultimap.Builder<Vertex, Pair<Float, OgexBoneNode>>>> brushPairEntry : listToBuild.entrySet())
        {
            final Brush brush = brushPairEntry.getKey();
            final Pair<ArrayList<Face>, ImmutableMultimap.Builder<Vertex, Pair<Float, OgexBoneNode>>> arrayListBuilderPair = brushPairEntry.getValue();
            final List<Face> faces = arrayListBuilderPair.getLeft();
            final ImmutableMultimap.Builder<Vertex, Pair<Float, OgexBoneNode>> boneWeightMapBuilder = arrayListBuilderPair.getRight();

            final Mesh mesh1 = new Mesh(Pair.of(brush, faces));

            meshBoneMapQueue.add(Pair.of(mesh1, boneWeightMapBuilder.build()));

            meshes.add(mesh1);
        }

        return meshes;
    }

    private TRSRTransformation getTRSRTransformationFromTransforms(List<OgexTransform> transforms) {
        final Matrix4f transformation = new Matrix4f();
        transformation.setIdentity();

        final Matrix4f transform = new Matrix4f();
        for (final OgexTransform ogexTransform : transforms) {
            transform.set(ogexTransform.toMatrix());
            transform.mul(upMatrix, transform);
            transform.mul(upMatrixInverted);
            transformation.mul(transform);
        }
        return new TRSRTransformation(transformation);
    }

    private void getBrushes(OgexScene ogexScene) {
        for (final OgexMaterial ogexMaterial : ogexScene.getMaterials()) {
            final OgexColor diffuse = ogexMaterial.getColor("diffuse");
            Vector4f color = new Vector4f(1, 1, 1, 0);
            if (diffuse != null) {
                color = new Vector4f(diffuse.getRed(), diffuse.getGreen(), diffuse.getBlue(), diffuse.getAlpha()) ;
            }
            final String name = ogexMaterial.getName();

            final float shininess = 0.0f;
            final int blend = 1;
            final int fx = 0;

            final List<Texture> brushTextures = Lists.newArrayList();

            Texture texture = getTextureFromOgexTexture(ogexMaterial.getTexture("diffuse"));
            texture = getSingleTextureFromList(texture, textures);

            brushTextures.add(texture);

            brushes.put(ogexMaterial, new Brush(name, color, shininess, blend, fx, brushTextures));
        }
    }

    private Texture getTextureFromOgexTexture(OgexTexture texture) {
        if (texture == null) {
            return Texture.White;
        }
        final String path = texture.getTexture();
        final Vector2f scale = new Vector2f(1, 1);
        final Vector2f position = new Vector2f(0, 0);
        float rotation = 0f;
        for (final OgexTransform ogexTransform : texture.getTransforms()) {
            if (ogexTransform instanceof OgexScale.ComponentScale) {
                final OgexScale.ComponentScale ogexScale = (OgexScale.ComponentScale) ogexTransform;
                if (ogexScale.getKind().getOgexName().equals("X"))
                {
                    scale.x = ogexScale.getScale();

                } else if (ogexScale.getKind().getOgexName().equals("Y"))
                {
                    scale.y = ogexScale.getScale();

                } else if (ogexScale.getKind().getOgexName().equals("Z"))
                {
                    throw new UnsupportedOperationException("OGEX: Attempt to scale a texture by the 3rd dimension?");
                }
            } else if (ogexTransform instanceof OgexScale.XyzScale) {
                final OgexScale.XyzScale ogexScale = (OgexScale.XyzScale) ogexTransform;
                scale.x = ogexScale.getScale()[0];
                scale.y = ogexScale.getScale()[1];
            } else if (ogexTransform instanceof OgexRotation.AxisRotation) {
                final OgexRotation.AxisRotation ogexRotation = (OgexRotation.AxisRotation) ogexTransform;
                //FIXME: I have *NO* idea which axis this comes in as.
                rotation = ogexRotation.getAngle();
            } else if (ogexTransform instanceof OgexRotation.ComponentRotation) {
                final OgexRotation.ComponentRotation ogexRotation = (OgexRotation.ComponentRotation) ogexTransform;
                rotation = ogexRotation.getAngle();
            } else if (ogexTransform instanceof OgexRotation.QuaternionRotation) {
                throw new UnsupportedOperationException("OGEX: Attempt to rotate a texture using a Quaternion");
            } else if (ogexTransform instanceof OgexTranslation.ComponentTranslation) {
                final OgexTranslation.ComponentTranslation ogexTranslation = (OgexTranslation.ComponentTranslation) ogexTransform;
                if (ogexTranslation.getKind().getOgexName().equals("X"))
                {
                    position.x = ogexTranslation.getTranslation();

                } else if (ogexTranslation.getKind().getOgexName().equals("Y"))
                {
                    position.y = ogexTranslation.getTranslation();

                } else if (ogexTranslation.getKind().getOgexName().equals("Z"))
                {
                    throw new UnsupportedOperationException("OGEX: Attempt to translate a texture by the 3rd dimension?");
                }
            } else if (ogexTransform instanceof OgexTranslation.XyzTranslation) {
                final OgexTranslation.XyzTranslation ogexTranslation = (OgexTranslation.XyzTranslation) ogexTransform;
                position.x = ogexTranslation.getTranslation()[0];
                position.y = ogexTranslation.getTranslation()[1];
            }
        }

        //FIXME: I have no idea what opengex's intention was with these two flags.
        final int flags = 0;
        final int blend = 0;

        return new Texture(path, flags, blend, position, scale, rotation);
    }

    private Texture getSingleTextureFromList(Texture texture, List<Texture> textures) {
        for (final Texture existingTexture : textures) {
            boolean matches;
            matches = existingTexture.getPath().equals(texture.getPath());
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

}

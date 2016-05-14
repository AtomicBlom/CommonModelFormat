package com.github.atomicblom.client.model.cmf.opengex;

import com.github.atomicblom.client.model.cmf.common.*;
import com.github.atomicblom.client.model.cmf.opengex.conversion.*;
import com.github.atomicblom.client.model.cmf.opengex.ogex.*;
import com.google.common.base.*;
import com.google.common.collect.*;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings({"UnnecessarilyQualifiedInnerClassAccess", "MethodWithMultipleLoops", "ObjectAllocationInLoop"})
final class Parser {
    private Parser() {}

    static Model parse(InputStream inputStream) throws IOException
    {
        final OgexParser ogexParser = new OgexParser();
        final Reader reader = new InputStreamReader(inputStream);
        final OgexScene ogexScene = ogexParser.parseScene(reader);
        final Axis up = ogexScene.getMetrics().getUp();

        final ModelBuilderContext modelBuilderContext = new ModelBuilderContext();
        modelBuilderContext.setUpMatrix(getMatrixForUpAxis(up));

        processSceneBrushes(modelBuilderContext, ogexScene);
        final NodeBuilderContext nodeBuilderContext = modelBuilderContext.startNewNodeBuilderContext(ogexScene);
        processNode(nodeBuilderContext);
        nodeBuilderContext.finish();

        processMeshJointQueue(modelBuilderContext);
        return modelBuilderContext.finish();
    }

    private static void processMeshJointQueue(ModelBuilderContext modelBuilderContext)
    {
        for (final ModelBuilderContext.UnprocessedMeshJointMapEntry meshJointMapEntry : modelBuilderContext.getMeshJointMapQueue())
        {
            final Mesh mesh = meshJointMapEntry.getMesh();
            final ImmutableMultimap.Builder<Vertex, JointWeight> jointWeightMapBuilder = ImmutableMultimap.builder();
            final Set<Node<Joint>> jointsUsed = Sets.newHashSet();

            for (final Vertex vertex : meshJointMapEntry.getVertices())
            {
                for (final JointWeightPose jointWeightPose : meshJointMapEntry.getJointWeightPose(vertex))
                {
                    final OgexBoneNode ogexBoneNode = jointWeightPose.getOgexBoneNode();
                    final TRSRTransformation invBindPose = jointWeightPose.getInvertedBindPose();

                    final Node<Joint> jointNode = modelBuilderContext.getJointForBone(ogexBoneNode).getParent();
                    jointNode.getKind().getData().add(new VertexWeight(vertex, jointWeightPose.getWeight()));
                    // TODO check if bone instance can be shared with multiple inv bind poses, and make a copy if true
                    jointNode.getKind().setInvBindPose(invBindPose);
                    jointWeightMapBuilder.put(vertex, new JointWeight(jointNode, jointWeightPose.getWeight()));
                    if (!jointsUsed.contains(jointNode))
                    {
                        jointsUsed.add(jointNode);
                    }
                }
            }
            mesh.setJoints(jointsUsed);
            mesh.setWeightMap(jointWeightMapBuilder.build());
        }
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

    static TRSRTransformation combineTransforms(Iterable<OgexTransform> transforms, NodeBuilderContext nodeBuilderContext)
    {
        Matrix4f m = new Matrix4f(), t = new Matrix4f();
        m.set(nodeBuilderContext.getUpMatrix());
        for(OgexTransform transform : transforms)
        {
            t.set(transform.toMatrix());
            m.mul(t);
        }
        final Matrix4f inverseUpMatrix = nodeBuilderContext.getUpMatrix();
        inverseUpMatrix.invert();
        m.mul(inverseUpMatrix);
        return new TRSRTransformation(m);
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private static void processNode(NodeBuilderContext nodeBuilderContext) {

        if (!nodeBuilderContext.isUsable()) {
            return;
        }

        TRSRTransformation globalTrsr = TRSRTransformation.identity();
        TRSRTransformation objectTrsr = TRSRTransformation.identity();
        String name = "";
        Function<? super Node<?>, ? extends IAnimation> animFactoryGlobal = null, animFactoryObject = null;

        boolean hasObjectOnly = false;

        final Iterable<OgexNode> openGEXNode = nodeBuilderContext.getNode();
        if (openGEXNode instanceof OgexNode) {
            final OgexNode ogexNode = (OgexNode) openGEXNode;
            final Matrix4f actualNodeTransformation = new Matrix4f();
            actualNodeTransformation.setIdentity();
            final Matrix4f objectOnlyTransformations = new Matrix4f();
            objectOnlyTransformations.setIdentity();

            Predicate<OgexTransform> isObjectOnly = new Predicate<OgexTransform>()
            {
                public boolean apply(OgexTransform transform)
                {
                    return transform.isObjectOnly();
                }
            };
            final Iterable<OgexTransform> globalTransforms = Iterables.filter(ogexNode.getTransforms(), Predicates.not(isObjectOnly));
            final Iterable<OgexTransform> objectTransforms = Iterables.filter(ogexNode.getTransforms(), isObjectOnly);
            hasObjectOnly = objectTransforms.iterator().hasNext();

            globalTrsr = combineTransforms(globalTransforms, nodeBuilderContext);
            objectTrsr = combineTransforms(objectTransforms, nodeBuilderContext);

            name = ogexNode.getName();

            if (name == null) {
                name = UUID.randomUUID().toString();
            }

            // FIXME more than 1 clip
            if(!ogexNode.getAnimations().isEmpty()) {
                final OgexAnimation ogexAnimation = ogexNode.getAnimations().iterator().next();
                animFactoryGlobal = Functions.constant(new OpenGEXAnimation(globalTransforms, ogexAnimation, nodeBuilderContext.getUpMatrix()));
                if(hasObjectOnly)
                {
                    animFactoryObject = Functions.constant(new OpenGEXAnimation(objectTransforms, ogexAnimation, nodeBuilderContext.getUpMatrix()));
                }
            }
        }

        //final List<Node<?>> childNodes = Lists.newArrayList();
        for (final OgexNode childOgexNode : openGEXNode) {

            final NodeBuilderContext childContext = nodeBuilderContext.startNewNodeBuilderContext(childOgexNode);
            //final Optional<Node<?>> childNode =
            processNode(childContext);
            childContext.finish();

            /*if (childNode.isPresent()) {
                childNodes.createNode(childNode.get());
            }*/
        }

        IKind kind;
        if (openGEXNode instanceof OgexGeometryNode) {
            final OgexGeometryNode ogexGeometryNode = (OgexGeometryNode) openGEXNode;

            //FIXME: Start mesh node
            final List<Mesh> createdMeshes = createMeshes(nodeBuilderContext, ogexGeometryNode);
            //FIXME: End mesh node
            if (createdMeshes.size() == 1) {
                final Mesh mesh = createdMeshes.get(0);
                kind = mesh != null ? mesh : new Pivot();
            } else {
                int itemIndex = 0;
                for (final Mesh mesh : createdMeshes)
                {
                    //FIXME: Don't null
                    final NodeBuilderContext childContext = nodeBuilderContext.startNewNodeBuilderContext(null);
                    childContext.createNode(name + "-MeshChild#" + itemIndex, TRSRTransformation.identity(), mesh, null, false);
                    childContext.finish();
                    itemIndex++;
                }

                kind = new Pivot();
            }
        } else
        {
            if (openGEXNode instanceof OgexBoneNode)
            {
                final OgexBoneNode ogexBoneNode = (OgexBoneNode) openGEXNode;
                final Joint joint = new Joint();
                nodeBuilderContext.associateBoneToJoint(ogexBoneNode, joint);
                kind = joint;
            } else
            {
                kind = new Pivot();
            }
        }

        //FIXME: It's entirely possible that an object-only element has an animation against it. The animation will need to be applied somehow if this ever occurs.
        if (hasObjectOnly) {
            //FIXME: Don't null
            final NodeBuilderContext childContext = nodeBuilderContext.startNewNodeBuilderContext(null);
            //childNodes.createNode(Node.create(name, objectTrsr, ImmutableList.<Node<?>>of(), kind, animFactoryObject, false));
            //childNodes.createNode(Node.create(name, objectTrsr, ImmutableList.<Node<?>>of(), kind, null, false));
            nodeBuilderContext.createNode(name, objectTrsr, kind, null, false);
            childContext.finish();
            name += "-Auto-ObjectOnlySeperator";
            kind = new Pivot();
        }

        //return Node.create(name, globalTrsr, childNodes, kind, animFactoryGlobal, false);
        nodeBuilderContext.createNode(name, globalTrsr, kind, animFactoryGlobal, false);
    }

    private static List<Mesh> createMeshes(NodeBuilderContext nodeBuilderContext, OgexGeometryNode ogexGeometryNode) {
        final MeshBuilderContext meshBuilderContext = nodeBuilderContext.startMeshBuilderContext(ogexGeometryNode);

        if (!meshBuilderContext.hasBrushes()) {
            return Lists.newArrayList();
        }

        final MeshType type = meshBuilderContext.getGeometryType();
        if (type != MeshType.Quads && type != MeshType.Triangles)
        {
            throw new OpenGEXException("Attempting to generate a cmf for an unsupported OpenGL Mesh Type: " + type);
        }

        for (final OgexIndexArray subMesh : meshBuilderContext.getSubMeshes())
        {
            final SubMeshBuilderContext subMeshBuilderContext = meshBuilderContext.startNewSubMeshWithMaterial(subMesh);
            processPartialMesh(subMeshBuilderContext);
            subMeshBuilderContext.finish();
        }

        return meshBuilderContext.finish();
    }

    private static void processPartialMesh(SubMeshBuilderContext subMeshBuilderContext)
    {
        final Matrix4f upMatrix = subMeshBuilderContext.getUpMatrix();
        final Matrix4f invertedUpMatrix = subMeshBuilderContext.getUpMatrix();
        invertedUpMatrix.invert();

        for (final long[] polyGroup : (long[][]) subMeshBuilderContext.getIndexArrays())
        {
            final Vertex[] vertices = new Vertex[polyGroup.length];
            Vector3f normal = new Vector3f();

            for (int i = 0; i < polyGroup.length; i++)
            {
                final int vertexIndex = (int) polyGroup[i];
                final Pair<Vertex, Vector3f> createVertexResult = getVertex(subMeshBuilderContext, vertexIndex);
                vertices[i] = createVertexResult.getLeft();
                normal = createVertexResult.getRight();

                for (final SubMeshBuilderContext.JointWeightTransformation jointWeightPose : subMeshBuilderContext.iterateBones(vertexIndex))
                {
                    final Matrix4f poseTranformation = jointWeightPose.getPoseTranformation();

                    poseTranformation.transpose();
                    poseTranformation.invert();
                    poseTranformation.mul(upMatrix, poseTranformation);
                    poseTranformation.mul(invertedUpMatrix);

                    subMeshBuilderContext.addJointWeightPoseToVertex(vertices[i], jointWeightPose.getJointWeight(), jointWeightPose.getOgexBoneNode(), poseTranformation);
                }
            }

            subMeshBuilderContext.addFace(vertices, normal);
        }
    }

    @SuppressWarnings({"MethodWithMoreThanThreeNegations", "IfStatementWithTooManyBranches"})
    private static Pair<Vertex, Vector3f> getVertex(SubMeshBuilderContext subMeshBuilderContext, int vertexIndex)
    {
        float[] positionArray = null;
        float[] normalArray = null;
        float[] texcoordArray = null;
        float[] colorArray = null;

        final Matrix4f upMatrix = subMeshBuilderContext.getUpMatrix();

        for (final OgexVertexArray array : subMeshBuilderContext.getVertexArrays())
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
            position.set(positionArray);
            upMatrix.transform(position);
        }

        if (normalArray != null)
        {
            normal.set(normalArray);
            upMatrix.transform(normal);
        }

        if (colorArray != null)
        {
            colour.set(colorArray);
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

    private static void processSceneBrushes(ModelBuilderContext parentContext, OgexScene ogexScene) {
        final BrushSetBuilderContext context = parentContext.startBrushSetBuilderContext();
        for (final OgexMaterial ogexMaterial : ogexScene.getMaterials()) {
            final OgexColor diffuse = ogexMaterial.getColor("diffuse");
            Vector4f color = new Vector4f(1, 1, 1, 0);
            if (diffuse != null) {
                color = new Vector4f(diffuse.getRed(), diffuse.getGreen(), diffuse.getBlue(), diffuse.getAlpha()) ;
            }
            final String name = ogexMaterial.getName();

            final List<Texture> brushTextures = Lists.newArrayList();

            final Texture texture = context.getTextureFromOgexTexture(ogexMaterial.getTexture("diffuse"));
            brushTextures.add(texture);

            final int blend = 1;
            final int fx = 0;
            final float shininess = 0.0f;
            context.mapMaterialToBrush(ogexMaterial, new Brush(name, color, shininess, blend, fx, brushTextures));
        }
        context.finish();
    }
}

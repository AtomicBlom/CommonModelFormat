package com.github.atomicblom.client.model.cmf.opengex.processors;

import com.github.atomicblom.client.model.cmf.common.*;
import com.github.atomicblom.client.model.cmf.opengex.OpenGEXAnimation;
import com.github.atomicblom.client.model.cmf.opengex.OpenGEXNode;
import com.github.atomicblom.client.model.cmf.opengex.ogex.*;
import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraftforge.common.model.TRSRTransformation;
import javax.vecmath.Matrix4f;
import java.util.List;
import java.util.UUID;

public class NodeProcessor
{
    private final SceneProcessor sceneProcessor;
    private final Matrix4f upMatrix;
    private final Matrix4f upMatrixInverted;

    public NodeProcessor(SceneProcessor sceneProcessor, Axis upAxis)
    {
        this.sceneProcessor = sceneProcessor;
        upMatrix = getMatrixForUpAxis(upAxis);
        (upMatrixInverted = new Matrix4f(upMatrix)).invert();
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    Optional<Node<?>> createNode(OpenGEXNode openGEXNode) {
        if (!isUsableNode(openGEXNode)) {
            return Optional.absent();
        }

        TRSRTransformation globalTrsr = TRSRTransformation.identity();
        TRSRTransformation objectTrsr = TRSRTransformation.identity();
        String name = "";
        Function<? super Node<?>, ? extends IAnimation> animFactoryGlobal = null;
        Function<? super Node<?>, ? extends IAnimation> animFactoryObject = null;

        boolean hasObjectOnly = false;

        if (openGEXNode instanceof OgexNode) {
            final OgexNode ogexNode = (OgexNode) openGEXNode;
            final Matrix4f actualNodeTransformation = new Matrix4f();
            actualNodeTransformation.setIdentity();
            final Matrix4f objectOnlyTransformations = new Matrix4f();
            objectOnlyTransformations.setIdentity();

            final Predicate<OgexTransform> isObjectOnly = new Predicate<OgexTransform>()
            {
                @Override
                public boolean apply(OgexTransform transform)
                {
                    return transform.isObjectOnly();
                }
            };
            final Iterable<OgexTransform> globalTransforms = Iterables.filter(ogexNode.getTransforms(), Predicates.not(isObjectOnly));
            final Iterable<OgexTransform> objectTransforms = Iterables.filter(ogexNode.getTransforms(), isObjectOnly);
            hasObjectOnly = objectTransforms.iterator().hasNext();

            globalTrsr = combineTransforms(globalTransforms);
            objectTrsr = combineTransforms(objectTransforms);

            name = ogexNode.getName();

            if (name == null) {
                name = UUID.randomUUID().toString();
            }

            // FIXME more than 1 clip
            if(!ogexNode.getAnimations().isEmpty()) {
                final OgexAnimation ogexAnimation = ogexNode.getAnimations().iterator().next();
                animFactoryGlobal = Functions.constant(new OpenGEXAnimation(globalTransforms, ogexAnimation, upMatrix));
                if(hasObjectOnly)
                {
                    animFactoryObject = Functions.constant(new OpenGEXAnimation(objectTransforms, ogexAnimation, upMatrix));
                }
            }
        }

        final List<Node<?>> childNodes = Lists.newArrayList();
        for (final OgexNode childOgexNode : openGEXNode) {
            final Optional<Node<?>> childNode = createNode(childOgexNode);
            if (childNode.isPresent()) {
                childNodes.add(childNode.get());
            }
        }

        IKind kind;
        if (openGEXNode instanceof OgexGeometryNode) {
            final OgexGeometryNode ogexGeometryNode = (OgexGeometryNode) openGEXNode;
            final MeshProcessor meshProcessor = new MeshProcessor(sceneProcessor, upMatrix);
            final List<Mesh> createdMeshes = meshProcessor.createMeshes(ogexGeometryNode);
            if (createdMeshes.size() == 1) {
                final Mesh mesh = createdMeshes.get(0);
                kind = mesh != null ? mesh : new Pivot();
            } else {
                int itemIndex = 0;
                for (final Mesh mesh : createdMeshes)
                {
                    childNodes.add(Node.create(name + "-MeshChild#" + itemIndex, TRSRTransformation.identity(), Lists.<Node<?>>newArrayList(), mesh, null, false));
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
                sceneProcessor.associateOgexBoneWithJoint(ogexBoneNode, joint);
                kind = joint;
            } else
            {
                kind = new Pivot();
            }
        }

        if (hasObjectOnly) {
            childNodes.add(Node.create(name, objectTrsr, ImmutableList.<Node<?>>of(), kind, animFactoryObject, false));
            name += "-Auto-ObjectOnlySeperator";
            kind = new Pivot();
        }

        final Node<?> node = Node.create(name, globalTrsr, childNodes, kind, animFactoryGlobal, false);
        return Optional.<Node<?>>of(node);
    }

    private static boolean isUsableNode(OpenGEXNode openGEXNode)
    {
        if (openGEXNode instanceof OgexCameraNode) {
            return false;
        }
        if (openGEXNode instanceof OgexLightNode) {
            return false;
        }
        return true;
    }

    private TRSRTransformation combineTransforms(Iterable<OgexTransform> transforms)
    {
        final Matrix4f m = new Matrix4f();
        final Matrix4f t = new Matrix4f();

        m.set(upMatrix);
        for(final OgexTransform transform : transforms)
        {
            t.set(transform.toMatrix());
            m.mul(t);
        }
        m.mul(upMatrixInverted);
        return new TRSRTransformation(m);
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
}

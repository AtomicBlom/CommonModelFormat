package com.github.atomicblom.client.model.cmf.opengex.conversion;

import com.github.atomicblom.client.model.cmf.common.*;
import com.github.atomicblom.client.model.cmf.opengex.OpenGEXException;
import com.github.atomicblom.client.model.cmf.opengex.ogex.*;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import net.minecraftforge.common.model.TRSRTransformation;
import javax.vecmath.Matrix4f;

public class NodeBuilderContext implements INodeBuilderCreator
{
    private final ModelBuilderContext modelContext;
    private final Iterable<OgexNode> ogexNode;
    protected final ImmutableList.Builder<Node<?>> childNodes = ImmutableList.builder();
    protected Optional<Node<?>> thisNode = Optional.absent();

    NodeBuilderContext(ModelBuilderContext modelContext, Iterable<OgexNode> ogexNode)
    {
        this.modelContext = modelContext;
        this.ogexNode = ogexNode;
    }

    public Matrix4f getUpMatrix()
    {
        return modelContext.getUpMatrix();
    }

    public MeshBuilderContext startMeshBuilderContext(OgexGeometryNode ogexGeometryNode)
    {
        return new MeshBuilderContext(this, ogexGeometryNode);
    }

    ImmutableMap<OgexMaterial, Brush> getBrushSet()
    {
        return modelContext.getBrushSet();
    }

    public void associateBoneToJoint(OgexBoneNode ogexBoneNode, Joint joint)
    {
        modelContext.associateBoneToJoint(ogexBoneNode, joint);
    }

    void addUnprocessedMeshJoint(Mesh mesh, Multimap<Vertex, JointWeightPose> vertexWeights)
    {
        modelContext.addUnprocessedMeshJoint(new ModelBuilderContext.UnprocessedMeshJointMapEntry(mesh, vertexWeights));
    }

    @Override
    public NodeBuilderContext startNewNodeBuilderContext(Iterable<OgexNode> ogexNode)
    {
        return new ChildNodeBuilderContext(modelContext, this, ogexNode);
    }

    public void finish() {
        if (!thisNode.isPresent()) {
            throw new OpenGEXException("No root node created");
        }
        modelContext.setRootNode(thisNode.get());
    }

    public boolean isUsable()
    {
        if (ogexNode instanceof OgexCameraNode) {
            return false;
        }
        if (ogexNode instanceof OgexLightNode) {
            return false;
        }
        return true;
    }

    public Iterable<OgexNode> getNode()
    {
        return ogexNode;
    }

    public <K extends IKind<K>> void createNode(String name, TRSRTransformation transformation, K kind, Function<? super Node<?>, ? extends IAnimation> animationFactory, boolean passAnimation)
    {
        thisNode = Optional.<Node<?>>of(Node.create(name, transformation, childNodes.build(), kind, animationFactory, passAnimation));
    }

}

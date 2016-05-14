package com.github.atomicblom.client.model.cmf.opengex.conversion;

import com.github.atomicblom.client.model.cmf.common.*;
import com.github.atomicblom.client.model.cmf.opengex.OpenGEXException;
import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexBoneNode;
import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexMaterial;
import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexNode;
import com.google.common.collect.*;
import javax.vecmath.Matrix4f;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ModelBuilderContext implements INodeBuilderCreator
{

    private Matrix4f upMatrix;

    private ImmutableMap<OgexMaterial, Brush> brushes = null;
    private ImmutableSet<Texture> textures = null;

    private final Queue<UnprocessedMeshJointMapEntry> meshJointMapQueue = Lists.newLinkedList();
    private final Map<OgexBoneNode, Joint> ogexBoneToJointMap = Maps.newHashMap();
    private Node<?> rootNode;

    public void setUpMatrix(Matrix4f upMatrix)
    {
        this.upMatrix = upMatrix;
    }

    public BrushSetBuilderContext startBrushSetBuilderContext()
    {
        return new BrushSetBuilderContext(this);
    }

    public NodeBuilderContext startNewNodeBuilderContext(Iterable<OgexNode> ogexNode)
    {
        return new NodeBuilderContext(this, ogexNode);
    }

    public Model finish()
    {
        return new Model(textures, brushes.values(), rootNode);
    }

    void setBrushes(ImmutableMap<OgexMaterial, Brush> brushes)
    {
        if (this.brushes != null)
        {
            throw new OpenGEXException("Attempt to overwrite the existing set of brushes");
        }
        this.brushes = brushes;
    }

    void setTextures(ImmutableSet<Texture> textures)
    {
        if (this.textures != null)
        {
            throw new OpenGEXException("Attempt to overwrite the existing set of textures");
        }
        this.textures = textures;
    }

    Matrix4f getUpMatrix()
    {
        return new Matrix4f(upMatrix);
    }

    ImmutableMap<OgexMaterial, Brush> getBrushSet()
    {
        return brushes;
    }

    public Queue<UnprocessedMeshJointMapEntry> getMeshJointMapQueue()
    {
        return meshJointMapQueue;
    }

    public Joint getJointForBone(OgexBoneNode ogexBoneNode)
    {
        return ogexBoneToJointMap.get(ogexBoneNode);
    }

    void associateBoneToJoint(OgexBoneNode ogexBoneNode, Joint joint)
    {
        ogexBoneToJointMap.put(ogexBoneNode, joint);
    }

    void addUnprocessedMeshJoint(UnprocessedMeshJointMapEntry unprocessedMeshJointMapEntry)
    {
        meshJointMapQueue.add(unprocessedMeshJointMapEntry);
    }

    public void setRootNode(Node<?> rootNode)
    {
        this.rootNode = rootNode;
    }

    @SuppressWarnings("ClassHasNoToStringMethod")
    public static class UnprocessedMeshJointMapEntry {
        private final Mesh mesh;
        private final Multimap<Vertex, JointWeightPose> verticesToProcess;

        UnprocessedMeshJointMapEntry(Mesh mesh, Multimap<Vertex, JointWeightPose> verticesToProcess) {

            this.mesh = mesh;
            this.verticesToProcess = verticesToProcess;
        }

        public Set<Vertex> getVertices() {
            return verticesToProcess.keySet();
        }

        public Iterable<JointWeightPose> getJointWeightPose(Vertex vertex)
        {
            return verticesToProcess.get(vertex);
        }

        public Mesh getMesh()
        {
            return mesh;
        }
    }

}

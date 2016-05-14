package com.github.atomicblom.client.model.cmf.opengex.processors;

import com.github.atomicblom.client.model.cmf.common.*;
import com.github.atomicblom.client.model.cmf.opengex.OpenGEXException;
import com.github.atomicblom.client.model.cmf.opengex.ogex.Axis;
import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexBoneNode;
import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexMaterial;
import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexScene;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import com.google.common.collect.ImmutableMultimap.Builder;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class SceneProcessor
{
    private static final Logger Logger = LogManager.getLogger();

    private final OgexScene ogexScene;
    private final BrushProcessor brushProcessor;
    private final Queue<UnprocessedMeshJointMapEntry> meshJointMapQueue = Lists.newLinkedList();
    private final Map<OgexBoneNode, Joint> ogexBoneToJointMap = Maps.newHashMap();
    private final Set<String> warningMessages = Sets.newHashSet();

    public SceneProcessor(OgexScene ogexScene)
    {

        this.ogexScene = ogexScene;
        brushProcessor = new BrushProcessor(ogexScene);
    }

    public Model createModel()
    {
        final Axis up = ogexScene.getMetrics().getUp();

        final NodeProcessor nodeProcessor = new NodeProcessor(this, up);
        final Optional<Node<?>> rootNode = nodeProcessor.createNode(ogexScene);
        if (!rootNode.isPresent()) {
            throw new OpenGEXException("Scene did not contain a root node.");
        }

        processMeshJoints();

        return new Model(brushProcessor.getTextures(), brushProcessor.getBrushes(), rootNode.get());
    }

    private void processMeshJoints()
    {
        for (final UnprocessedMeshJointMapEntry meshJointMapEntry : meshJointMapQueue)
        {

            processMeshJoint(meshJointMapEntry);
        }
    }

    private void processMeshJoint(UnprocessedMeshJointMapEntry meshJointMapEntry)
    {
        final Mesh mesh = meshJointMapEntry.getMesh();
        final Builder<Vertex, JointWeight> jointWeightMapBuilder = ImmutableMultimap.builder();
        final Set<Node<Joint>> jointsUsed = Sets.newHashSet();

        for (final Vertex vertex : meshJointMapEntry.getVertices())
        {
            for (final JointWeightPose jointWeightPose : meshJointMapEntry.getJointWeightPose(vertex))
            {
                final OgexBoneNode ogexBoneNode = jointWeightPose.getOgexBoneNode();
                final TRSRTransformation invBindPose = jointWeightPose.getInvertedBindPose();

                final Node<Joint> jointNode = ogexBoneToJointMap.get(ogexBoneNode).getParent();
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

    void associateOgexBoneWithJoint(OgexBoneNode ogexBoneNode, Joint joint)
    {
        ogexBoneToJointMap.put(ogexBoneNode, joint);
    }

    void associateMeshWithVertexWeights(Mesh mesh, Multimap<Vertex, JointWeightPose> vertexWeights)
    {
        meshJointMapQueue.add(new UnprocessedMeshJointMapEntry(mesh, vertexWeights));
    }

    Brush getBrushForMaterial(OgexMaterial ogexMaterial)
    {
        return brushProcessor.getBrushForMaterial(ogexMaterial);
    }

    void warn(String warningMessage)
    {
        if (!warningMessages.contains(warningMessage)) {
            warningMessages.add(warningMessage);
            Logger.warn(warningMessage);
        }
    }
}

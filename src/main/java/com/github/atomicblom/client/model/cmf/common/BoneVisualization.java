package com.github.atomicblom.client.model.cmf.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import net.minecraftforge.common.model.TRSRTransformation;
import javax.vecmath.*;

public class BoneVisualization
{
    private static final Brush jointDebugBrush = new Brush("armature", new Vector4f(1, 1, 1, 1), 0, 1, 0, ImmutableList.of(Texture.White));

    public BoneVisualization() { }

    Mesh generateBoneVisualizationMesh(Node<?> node)
    {
        // pose from .getInvBindPose, red
        TRSRTransformation pose = node.getInvBindPose().inverse();
        ImmutableList<Face> staticFaces = buildJointFaces(jointDebugBrush, pose, 1, 0, 0, null, null);
        ImmutableList<Face> staticBoneFaces = buildBoneFaces(jointDebugBrush, node, 1, 0, 0, null);
        // pose from forward hierarchy, blue
        TRSRTransformation forwardPose = TRSRTransformation.identity();
        Node<?> parentNode = node;
        while (parentNode != null)
        {
            forwardPose = parentNode.getTransformation().compose(forwardPose);
            parentNode = parentNode.getParent();
        }
        ImmutableList<Face> staticFaces2 = buildJointFaces(jointDebugBrush, forwardPose, 0, 0, 1, null, null);

        // deformable joints, green
        ImmutableMultimap.Builder<Vertex, JointWeight> weightBuilder = ImmutableMultimap.builder();
        JointWeight jointWeight = new JointWeight((Node<Joint>) node, 1f);

        ImmutableList<Face> dynamicFaces = buildJointFaces(jointDebugBrush, pose, 0, 1, 0, weightBuilder, jointWeight);
        ImmutableList<Face> dynamicBoneFaces = buildBoneFaces(jointDebugBrush, node, 0, 1, 0, weightBuilder);

        Mesh mesh = new Mesh(jointDebugBrush, ImmutableList.copyOf(Iterables.concat(staticFaces, staticBoneFaces, staticFaces2, dynamicFaces, dynamicBoneFaces)));
        // setting dummy mesh node
        Node.create("DummyBoneMeshNode", TRSRTransformation.identity(), ImmutableList.<Node<?>>of(), mesh, null, false);
        mesh.setWeightMap(weightBuilder.build());
        mesh.setJoints(ImmutableSet.of((Node<Joint>) node));
        return mesh;
    }

    Face makeOctahedronFace(Brush brush, Matrix4f pose, int i, float r, float g, float b)
    {
        int x = i & 1;
        int y = (i >> 1) & 1;
        int z = (i >> 2) & 1;
        Vector4f p = new Vector4f();
        p.set(x * 2 - 1, 0, 0, 1);
        pose.transform(p);
        Vertex v0 = new Vertex(new Vector3f(p.x / p.w, p.y / p.w, p.z / p.w), null, new Vector4f(r, g, b, 1), new Vector4f[]{new Vector4f(.5f, .5f, 0, 1)});
        p.set(0, y * 2 - 1, 0, 1);
        pose.transform(p);
        Vertex v1 = new Vertex(new Vector3f(p.x / p.w, p.y / p.w, p.z / p.w), null, new Vector4f(r, g, b, 1), new Vector4f[]{new Vector4f(.5f, .5f, 0, 1)});
        p.set(0, 0, z * 2 - 1, 1);
        pose.transform(p);
        Vertex v2 = new Vertex(new Vector3f(p.x / p.w, p.y / p.w, p.z / p.w), null, new Vector4f(r, g, b, 1), new Vector4f[]{new Vector4f(.5f, .5f, 0, 1)});
        if ((x ^ y ^ z) == 1)
        {
            return new Face(v0, v1, v2, brush);
        } else
        {
            return new Face(v2, v1, v0, brush);
        }

    }

    ImmutableList<Face> buildJointFaces(Brush brush, TRSRTransformation pose, float r, float g, float b, ImmutableMultimap.Builder<Vertex, JointWeight> weightBuilder, JointWeight bw)
    {
        ImmutableList.Builder<Face> faceBuilder = ImmutableList.builder();
        Matrix4f pm = pose.compose(new TRSRTransformation(null, null, new Vector3f(.1f, .1f, .1f), null)).getMatrix();
        for (int i = 0; i < 8; i++)
        {
            Face face = makeOctahedronFace(brush, pm, i, r, g, b);
            faceBuilder.add(face);
            if (weightBuilder != null)
            {
                weightBuilder.put(face.getV1(), bw);
                weightBuilder.put(face.getV2(), bw);
                weightBuilder.put(face.getV3(), bw);
            }
        }
        return faceBuilder.build();
    }

    ImmutableList<Face> buildBoneFaces(Brush brush, Node<?> node, float r, float g, float b, ImmutableMultimap.Builder<Vertex, JointWeight> weightBuilder)
    {
        if (!(node.getParent().getKind() instanceof Joint))
        {
            return ImmutableList.of();
        }
        TRSRTransformation poseStart = node.getParent().getInvBindPose().inverse();
        ImmutableList.Builder<Face> faceBuilder = ImmutableList.builder();
        Vector3f from = new Vector3f(0, 1, 0), to = new Vector3f(), axis = new Vector3f();
        Vector4f t = new Vector4f(0, 0, 0, 1);
        poseStart.inverse().compose(node.getInvBindPose().inverse()).getMatrix().transform(t);
        to.set(t.x / t.w, t.y / t.w, t.z / t.w);
        if (to.length() < 1e-4)
        {
            // bone too short
            return ImmutableList.of();
        }
        float scale = to.length();
        to.normalize();
        axis.cross(from, to);
        float angle = (float) Math.acos(from.dot(to));
        Quat4f rot = new Quat4f();
        rot.set(new AxisAngle4f(axis, angle));
        TRSRTransformation local = new TRSRTransformation(null, rot, new Vector3f(scale, scale, scale), null);
        Matrix4f global = poseStart.compose(local).compose(new TRSRTransformation(new Vector3f(0, .5f, 0), null, new Vector3f(.1f, .5f, .1f), null)).getMatrix();
        JointWeight bs = new JointWeight((Node<Joint>) (node.getParent()), 1f);
        JointWeight bsm = new JointWeight((Node<Joint>) (node.getParent()), .5f);
        JointWeight bem = new JointWeight((Node<Joint>) node, .5f);
        JointWeight be = new JointWeight((Node<Joint>) node, 1f);
        for (int i = 0; i < 8; i++)
        {
            Face face = makeOctahedronFace(brush, global, i, r, g, b);
            faceBuilder.add(face);
            if (weightBuilder != null)
            {
                weightBuilder.put(face.getV1(), bsm);
                weightBuilder.put(face.getV1(), bem);
                weightBuilder.put(face.getV3(), bsm);
                weightBuilder.put(face.getV3(), bem);
                weightBuilder.put(face.getV2(), (i & 2) == 0 ? bs : be);
            }
        }
        return faceBuilder.build();
    }
}
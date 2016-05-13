package com.github.atomicblom.client.model.cmf.common;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

import javax.vecmath.Matrix4f;
import java.util.*;

public class Mesh implements IKind<Mesh> {
    private Node<Mesh> parent;
    private final Brush brush;
    private final ImmutableList<Face> faces;

    private Set<Node<Joint>> joints = new HashSet<Node<Joint>>();

    private ImmutableMultimap<Vertex, JointWeight> weightMap = ImmutableMultimap.of();

    public Mesh(Brush brush, List<Face> faces) {
        this.brush = brush;
        this.faces = ImmutableList.copyOf(faces);
    }

    public ImmutableMultimap<Vertex, JointWeight> getWeightMap() {
        return weightMap;
    }

    public ImmutableList<Face> bake(Function<Node<?>, Matrix4f> animator) {
        ImmutableList.Builder<Face> builder = ImmutableList.builder();
        for (Face f : getFaces()) {
            Vertex v1 = f.getV1().bake(this, animator);
            Vertex v2 = f.getV2().bake(this, animator);
            Vertex v3 = f.getV3().bake(this, animator);
            builder.add(new Face(v1, v2, v3, f.getBrush()));
        }
        return builder.build();
    }

    public Brush getBrush() {
        return brush;
    }

    public ImmutableList<Face> getFaces() {
        return faces;
    }

    public ImmutableSet<Node<Joint>> getJoints() {
        return ImmutableSet.copyOf(joints);
    }
    public void setJoints(Set<Node<Joint>> joints) {
        this.joints = joints;
    }

    @Override
    public String toString() {
        return String.format("Mesh [pivot=%s, brush=%s, data=...]", super.toString(), brush);
    }

    public void setWeightMap(ImmutableMultimap<Vertex, JointWeight> weightMap) {
        this.weightMap = weightMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setParent(Node<Mesh> parent) {
        this.parent = parent;
        Deque<Node<?>> queue = new ArrayDeque<Node<?>>(parent.getNodes().values());
        while (!queue.isEmpty()) {
            Node<?> node = queue.pop();
            if (node.getKind() instanceof Joint) {
                joints.add((Node<Joint>) node);
                queue.addAll(node.getNodes().values());
            }
        }
    }

    @Override
    public Node<Mesh> getParent() {
        return parent;
    }
}

package com.github.atomicblom.client.model.cmf.common;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;

import javax.vecmath.Matrix4f;
import java.util.*;

public class Mesh implements IKind<Mesh> {
    private Node<Mesh> parent;
    private final Brush brush;
    private final ImmutableList<Face> faces;
    //private final ImmutableList<Bone> bones;

    private Set<Node<Bone>> bones = new HashSet<Node<Bone>>();

    private ImmutableMultimap<Vertex, BoneWeight> weightMap = ImmutableMultimap.of();

    public Mesh(Brush brush, List<Face> faces) {
        this.brush = brush;
        this.faces = ImmutableList.copyOf(faces);
    }

    public ImmutableMultimap<Vertex, BoneWeight> getWeightMap() {
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

    public ImmutableSet<Node<Bone>> getBones() {
        return ImmutableSet.copyOf(bones);
    }
    public void setBones(Set<Node<Bone>> bones) {
        this.bones = bones;
    }

    @Override
    public String toString() {
        return String.format("Mesh [pivot=%s, brush=%s, data=...]", super.toString(), brush);
    }

    public void setWeightMap(ImmutableMultimap<Vertex, BoneWeight> weightMap) {
        this.weightMap = weightMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setParent(Node<Mesh> parent) {
        this.parent = parent;
        Deque<Node<?>> queue = new ArrayDeque<Node<?>>(parent.getNodes().values());
        while (!queue.isEmpty()) {
            Node<?> node = queue.pop();
            if (node.getKind() instanceof Bone) {
                bones.add((Node<Bone>) node);
                queue.addAll(node.getNodes().values());
            }
        }
    }

    @Override
    public Node<Mesh> getParent() {
        return parent;
    }
}

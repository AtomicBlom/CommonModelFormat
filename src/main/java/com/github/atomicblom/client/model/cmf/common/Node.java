package com.github.atomicblom.client.model.cmf.common;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import net.minecraftforge.common.model.TRSRTransformation;

import java.util.List;

public class Node<K extends IKind<K>> {
    private final String name;
    private final TRSRTransformation transformation;
    private final ImmutableMap<String, Node<?>> nodes;
    private IAnimation animation = null;
    private final K kind;
    private Node<? extends IKind<?>> parent;

    public static <K extends IKind<K>> Node<K> create(String name, TRSRTransformation transformation, List<Node<?>> nodes, K kind, Function<? super Node<?>, ? extends IAnimation> animFactory, boolean passAnimation) {
        return new Node<K>(name, transformation, nodes, kind, animFactory, passAnimation);
    }

    public Node(String name, TRSRTransformation transformation, List<Node<?>> nodes, K kind, Function<? super Node<?>, ? extends IAnimation> animFactory, boolean passAnimation) {
        this.name = name;
        this.transformation = transformation;
        this.nodes = buildNodeMap(nodes);
        this.kind = kind;
        if(animFactory != null)
        {
            IAnimation animation = animFactory.apply(this);
            if(passAnimation)
            {
                setAnimation(animation);
            }
            else
            {
                this.animation = animation;
            }
        }
        kind.setParent(this);
        for (Node<?> child : this.nodes.values()) {
            child.setParent(this);
        }
    }

    private void setAnimation(IAnimation animation)
    {
        this.animation = animation;
        for(Node<?> child : nodes.values())
        {
            if(child.animation == null)
            {
                child.setAnimation(animation);
            }
        }
    }

    private ImmutableMap<String, Node<?>> buildNodeMap(List<Node<?>> nodes) {
        Builder<String, Node<?>> builder = ImmutableMap.builder();
        for (Node<?> node : nodes) {
            final String name = node.getName();
            builder.put(name, node);
        }
        return builder.build();
    }

    public String getName() {
        return name;
    }

    public K getKind() {
        return kind;
    }

    public ImmutableMap<String, Node<?>> getNodes() {
        return nodes;
    }

    public IAnimation getAnimation() {
        return animation;
    }

    public Node<? extends IKind<?>> getParent() {
        return parent;
    }

    public void setParent(Node<? extends IKind<?>> parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return String.format("Node [name=%s, kind=%s, transformation=%s, keys=..., nodes=..., animation=%s]", name, kind, transformation, animation);
    }

    public TRSRTransformation getTransformation()
    {
        return transformation;
    }

    public TRSRTransformation getInvBindPose()
    {
        if(kind instanceof Joint)
        {
            Joint joint = (Joint) kind;
            if(joint.getInvBindPose() != null)
            {
                return joint.getInvBindPose();
            }
        }
        TRSRTransformation pose = transformation.inverse();

        if (parent != null)
        {
            TRSRTransformation parentTr = parent.getInvBindPose();
            pose = pose.compose(parentTr);
        }
        return pose;
    }
}

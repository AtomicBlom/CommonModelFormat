package mod.steamnsteel.client.model.common;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Triple;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Created by steblo on 23/03/2016.
 */
public class Node<K extends IKind<K>> {
    private final String name;
    private final TRSRTransformation transformation;
    private final ImmutableMap<String, Node<?>> nodes;
    private Animation animation;
    private final K kind;
    private Node<? extends IKind<?>> parent;

    public static <K extends IKind<K>> Node<K> create(String name, TRSRTransformation transformation, List<Node<?>> nodes, K kind) {
        return new Node<K>(name, transformation, nodes, kind);
    }

    public Node(String name, TRSRTransformation transformation, List<Node<?>> nodes, K kind) {
        this.name = name;
        this.transformation = transformation;
        this.nodes = buildNodeMap(nodes);
        this.kind = kind;
        kind.setParent(this);
        for (Node<?> child : this.nodes.values()) {
            child.setParent(this);
        }
    }

    public void setAnimation(Animation animation) {
        this.animation = animation;
        Deque<Node<?>> q = new ArrayDeque<Node<?>>(nodes.values());

        while (!q.isEmpty()) {
            Node<?> node = q.pop();
            if (node.getAnimation() != null) continue;
            node.setAnimation(animation);
            q.addAll(node.getNodes().values());
        }
    }

    public void setAnimation(Triple<Integer, Integer, Float> animData, Table<Integer, Optional<Node<?>>, Key> keyData) {
        ImmutableTable.Builder<Integer, Node<?>, Key> builder = ImmutableTable.builder();
        for (Cell<Integer, Optional<Node<?>>, Key> key : keyData.cellSet()) {
            builder.put(key.getRowKey(), key.getColumnKey().or(this), key.getValue());
        }
        setAnimation(new Animation(animData.getLeft(), animData.getMiddle(), animData.getRight(), builder.build()));
    }

    private ImmutableMap<String, Node<?>> buildNodeMap(List<Node<?>> nodes) {
        Builder<String, Node<?>> builder = ImmutableMap.builder();
        for (Node<?> node : nodes) {
            builder.put(node.getName(), node);
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

    public Animation getAnimation() {
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
}

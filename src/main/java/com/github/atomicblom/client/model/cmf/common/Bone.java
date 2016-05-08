package com.github.atomicblom.client.model.cmf.common;

import com.google.common.collect.Lists;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class Bone implements IKind<Bone> {
    private Node<Bone> parent;
    private TRSRTransformation invBindPose;
    private final List<VertexWeight> data;

    public Bone(List<VertexWeight> data) {
        this.data = data;
    }

    public Bone() {
        data = Lists.newArrayList();
    }

    public List<VertexWeight> getData() {
        return data;
    }

    /*@Override
    public String toString()
    {
        return String.format("Bone [data=%s]", data);
    }*/

    @Override
    public void setParent(Node<Bone> parent) {
        this.parent = parent;
    }

    @Override
    public Node<Bone> getParent() {
        return parent;
    }

    public void setInvBindPose(TRSRTransformation invBindPose)
    {
        this.invBindPose = invBindPose;
    }

    public TRSRTransformation getInvBindPose()
    {
        return invBindPose;
    }
}

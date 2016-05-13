package com.github.atomicblom.client.model.cmf.common;

import com.google.common.collect.Lists;
import net.minecraftforge.common.model.TRSRTransformation;

import java.util.List;

public class Joint implements IKind<Joint> {
    private Node<Joint> parent;
    private TRSRTransformation invBindPose;
    private final List<VertexWeight> data;

    public Joint(List<VertexWeight> data) {
        this.data = data;
    }

    public Joint() {
        data = Lists.newArrayList();
    }

    public List<VertexWeight> getData() {
        return data;
    }

    /*@Override
    public String toString()
    {
        return String.format("Joint [data=%s]", data);
    }*/

    @Override
    public void setParent(Node<Joint> parent) {
        this.parent = parent;
    }

    @Override
    public Node<Joint> getParent() {
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

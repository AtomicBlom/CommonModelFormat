package com.github.atomicblom.client.model.cmf.opengex.conversion;

import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexBoneNode;
import net.minecraftforge.common.model.TRSRTransformation;
import javax.vecmath.Matrix4f;

/**
 * Created by codew on 14/05/2016.
 */
@SuppressWarnings("ClassHasNoToStringMethod")
public class JointWeightPose
{
    private final Float weight;
    private final OgexBoneNode ogexBoneNode;
    private final TRSRTransformation invertedBindPose;

    JointWeightPose(Float weight, OgexBoneNode ogexBoneNode, TRSRTransformation invertedBindPose)
    {
        this.weight = weight;
        this.ogexBoneNode = ogexBoneNode;
        this.invertedBindPose = invertedBindPose;
    }

    public Float getWeight()
    {
        return weight;
    }

    public OgexBoneNode getOgexBoneNode()
    {
        return ogexBoneNode;
    }

    public TRSRTransformation getInvertedBindPose()
    {
        return invertedBindPose;
    }
}

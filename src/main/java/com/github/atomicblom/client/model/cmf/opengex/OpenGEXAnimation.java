package com.github.atomicblom.client.model.cmf.opengex;

import com.github.atomicblom.client.model.cmf.common.IAnimation;
import com.github.atomicblom.client.model.cmf.common.Node;
import com.github.atomicblom.client.model.cmf.opengex.ogex.*;
import com.google.common.collect.ImmutableMap;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.NotImplementedException;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;

class OpenGEXAnimation implements IAnimation
{
    private final ImmutableMap<OgexTransform, OgexTrack> tracks;
    private final ImmutableMap<OgexTransform, TRSRTransformation> transforms;
    private final Matrix4f upAxis;
    private final Matrix4f upAxisInverted;

    public OpenGEXAnimation(Iterable<OgexTransform> transforms, OgexAnimation ogexAnimation, Matrix4f upAxis)
    {
        this.upAxis = upAxis;
        this.upAxisInverted = new Matrix4f(upAxis);
        upAxisInverted.invert();

        final ImmutableMap.Builder<OgexTransform, OgexTrack> trackMapBuilder = ImmutableMap.builder();
        for(OgexTrack track : ogexAnimation)
        {
            trackMapBuilder.put((OgexTransform) track.getTarget(), track);
        }
        tracks = trackMapBuilder.build();
        final ImmutableMap.Builder<OgexTransform, TRSRTransformation> mapBuilder = ImmutableMap.builder();
        for (final OgexTransform ogexTransform : transforms)
        {
            final Matrix4f transform = new Matrix4f();
            transform.set(ogexTransform.toMatrix());
            transform.mul(upAxis, transform);
            transform.mul(upAxisInverted);
            mapBuilder.put(ogexTransform, new TRSRTransformation(transform));
        }
        this.transforms = mapBuilder.build();
    }

    @Override
    public TRSRTransformation apply(float time, Node<?> node)
    {
        TRSRTransformation ret = TRSRTransformation.identity();
        for(OgexTransform transform : transforms.keySet())
        {
            if(tracks.containsKey(transform))
            {
                ret = ret.compose(applyTrack(tracks.get(transform), time));
            }
            else
            {
                ret = ret.compose(transforms.get(transform));
            }
        }
        return ret;
    }

    private TRSRTransformation applyTrack(OgexTrack track, float time)
    {
        if (track.getTime().getCurve() != Curve.Linear || track.getValue().getCurve() != Curve.Linear)
        {
            //throw new NotImplementedException("Only linear for now");
        }
        float[] times = (float[]) track.getTime().getKeys()[0].getData();
        if (time == 0)
        {
            return getTrackData(track, 0);
        }
        int i0 = 0, i1 = times.length - 1;
        // can't be bothered to write a binsearch, FIXME later
        // TODO metric
        while (i0 + 1 < times.length && times[i0 + 1] <= time) i0++;
        while (i1 - 1 >= 0 && times[i1 - 1] > time) i1--;

        float t0 = times[i0];
        float t1 = times[i1];
        float s;

        if (Math.abs(t0 - t1) < 1e-5)
        {
            s = t0;
        }
        else
        {
            s = (time - t0) / (t1 - t0);
        }

        TRSRTransformation v0 = getTrackData(track, i0);
        TRSRTransformation v1 = getTrackData(track, i1);

        return v0.slerp(v1, s);
    }

    private TRSRTransformation getTrackData(OgexTrack track, int keyIndex)
    {
        Object v = track.getValue().getKeys()[0].getData();
        Object target = track.getTarget();
        Matrix4f transform = new Matrix4f();
        if (target instanceof OgexMatrixTransform)
        {
            OgexMatrixTransform mt = (OgexMatrixTransform) target;
            transform.set(((float[][]) v)[keyIndex]);
            transform.transpose();
        } else if (target instanceof OgexRotation.ComponentRotation)
        {
            OgexRotation.ComponentRotation rot = (OgexRotation.ComponentRotation) target;
            float angle = ((float[]) v)[keyIndex];
            AxisAngle4f aa = new AxisAngle4f(0, 0, 0, angle);
            switch (rot.getKind())
            {
                case X:
                    aa.x = 1;
                    break;
                case Y:
                    aa.y = 1;
                    break;
                case Z:
                    aa.z = 1;
                    break;
                default:
                    throw new IllegalStateException("ComponentRotation has kind" + rot.getKind());
            }
            transform.set(aa);
        } else
        {
            throw new NotImplementedException("Only Matrix Transform for now");
        }
        transform.mul(upAxis, transform);
        transform.mul(upAxisInverted);
        return new TRSRTransformation(transform);
    }
}

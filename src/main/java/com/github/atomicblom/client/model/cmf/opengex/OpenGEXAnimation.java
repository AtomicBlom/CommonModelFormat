package com.github.atomicblom.client.model.cmf.opengex;

import com.github.atomicblom.client.model.cmf.common.IAnimation;
import com.github.atomicblom.client.model.cmf.common.Node;
import com.github.atomicblom.client.model.cmf.common.NodeClip;
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
        if(node.getParent() != null)
        {
            ret = ret.compose(NodeClip.getTransform(time, node.getParent()));
        }
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
        getCurrentValueForTrack(track, time, 1); //TODO: fps

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

    private static float getCurrentValueForTrack(OgexTrack track, float animationTime, float timeScale) {
        final TimeData keyData = getKeyData(track.getTime());
        final ValueData valueData = getValueData(track.getValue());
        int index = getKeyIndexForTime(keyData, timeScale, animationTime);

        float adjustedTime = 1;
        if (track.getTime().getCurve() == Curve.Bezier) {
            adjustedTime = getAdjustedBezierTime(keyData, timeScale, animationTime, index);
        } else if (track.getTime().getCurve() == Curve.Linear) {
            adjustedTime = getAdjustedLinearTime(keyData, timeScale, animationTime, index);
        }

        float trackValue = 1;
        if (track.getValue().getCurve() == Curve.Bezier) {
            trackValue = getAdjustedBezierValue(valueData, index, adjustedTime);
        } else if (track.getValue().getCurve() == Curve.Linear) {
            trackValue = getAdjustedLinearValue(valueData, index, adjustedTime);
        }


        return trackValue;
    }

    private static float getAdjustedLinearValue(ValueData valueData, int index, float adjustedTime) {
        final float[] value = valueData.value;
        final float s = adjustedTime;
        final float v1 = value[index - 1];
        final float v2 = value[index];

        //float si = (s - v1) / (v2 - v1);

        return (s * (v2 - v1)) + v1;
    }

    private static float getAdjustedBezierValue(ValueData valueData, int index, float adjustedTime) {
        final float[] value = valueData.value;
        final float s = adjustedTime;
        final float v1 = value[index - 1];
        final float p1 = valueData.positiveControl[index - 1];
        final float p2 = valueData.negativeControl[index];
        final float v2 = value[index];

        float v = (((1 - s) * (1 - s) * (1 - s)) * v1) +
                (3 * s * ((1 - s) * (1 - s)) * p1) +
                (3 * (s * s) * (1 - s) * p2) +
                (( s * s * s) * v2);

        return v;
    }


    private static float getAdjustedLinearTime(TimeData keyData, float scale, float currentTime, int index) {
        final float[] value = keyData.value;
        float si;

        float t = currentTime;

        final float t1 = value[index - 1] * scale;
        final float t2 = value[index] * scale;

        // Returns 0 when (t1 < t2) fails.
        if (t1 >= t2)
            return 0;

        si = (t - t1) / (t2 - t1);

        float finalTime = value[value.length - 1];
        if (si > finalTime) {
            si = finalTime;
        }

        return si;
    }

    private static float getAdjustedBezierTime(TimeData keyData, float scale, float currentTime, int index) {
        final float[] value = keyData.value;
        float[] positiveControl = keyData.positiveControl;
        float[] negativeControl = keyData.negativeControl;

        //TODO: This is a nasty hack
        float si = 0;

        float t = currentTime;

        final float t1 = value[index - 1] * scale;
        final float c1 = positiveControl[index - 1] * scale;
        final float c2 = negativeControl[index] * scale;
        final float t2 = value[index] * scale;

        // Returns 0 when (t1 < c1 < c2 < t2) fails.
        if (t1 >= c1 || c1 >= c2 || c2 >= t2)
            return 0;

        int i = 0;
        float oldSi = 0;

        // Newton's Method to calculate the closet value for the time
        while (true) {
            if (i == 0) {
                si = (t - t1) / (t2 - t1);
            } else {
                float splusone = si - (
                        ((t2 - (3 * c2) + (3 * c1) - t1) * (si * si * si)) +
                                (3 * (c2 - (2 * c1) + t1) * (si * si)) +
                                (3 * (c1 - t1) * si) +
                                t1 - t
                ) / (
                        (3 * (t2 - (3 * c2) + (3 * c1) - t1) * (si * si)) +
                                (6 * (c2 - (2 * c1) + t1) * si) +
                                3 * (c1 - t1)
                );

                si = splusone;
            }

            // If both are old s and s are equal within 4 decimal points then we reached the value,
            // Limited by 4 attempts. Testing suggests 1 is usually enough.
            if ( Math.floor(oldSi * 10000) == Math.floor(si * 10000) || i >= 4)
                break;
            i++;
            oldSi = si;
        }

        float finalTime = value[value.length - 1];
        if (si > finalTime) {
            si = finalTime;
        }

        return si;
    }

    private static int getKeyIndexForTime(TimeData keyData, float scale, float currentTime) {
        final float[] value = keyData.value;

        int i = 0;
        for (; i < value.length; i++) {
            if ((value[i] * scale) > currentTime) {
                break;
            }
        }

        if (i >= value.length) return value.length-1;
        if (i <= 0) return 0;
        return i;
    }

    private static TimeData getKeyData(OgexTime time) {
        final OgexKey[] keys = time.getKeys();
        TimeData timeData = new TimeData(keys);

        for (OgexKey key : keys) {
            switch (key.getKind()) {
                case Value:
                    timeData.value = (float[]) key.getData();
                    break;
                case PositiveControl:
                    timeData.positiveControl = (float[]) key.getData();
                    break;
                case NegativeControl:
                    timeData.negativeControl = (float[]) key.getData();
                    break;
            }
        }
        return timeData;
    }

    private static ValueData getValueData(OgexValue value) {
        final OgexKey[] keys = value.getKeys();
        ValueData timeData = new ValueData(keys);

        for (OgexKey key : keys) {
            switch (key.getKind()) {
                case Value:
                    timeData.value = (float[]) key.getData();
                    break;
                case PositiveControl:
                    timeData.positiveControl = (float[]) key.getData();
                    break;
                case NegativeControl:
                    timeData.negativeControl = (float[]) key.getData();
                    break;
                case Bias:
                    timeData.bias = (float[]) key.getData();
                    break;
                case Continuity:
                    timeData.continuity = (float[]) key.getData();
                    break;
                case Tension:
                    timeData.tension = (float[]) key.getData();
                    break;
            }
        }
        return timeData;
    }

    private static class ValueData {
        OgexKey[] keys;
        float[] value;
        float[] positiveControl;
        float[] negativeControl;
        float[] bias;
        float[] continuity;
        float[] tension;

        public ValueData(OgexKey[] keys) {
            this.keys = keys;
        }
    }

    private static class TimeData {
        OgexKey[] keys;
        float[] value;
        float[] positiveControl;
        float[] negativeControl;

        public TimeData(OgexKey[] keys) {
            this.keys = keys;
        }
    }
}

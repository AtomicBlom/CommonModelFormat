package com.github.atomicblom.client.model.cmf.opengex;

import com.github.atomicblom.client.model.cmf.common.*;
import com.github.atomicblom.client.model.cmf.opengex.ogex.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.NotImplementedException;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;

class OpenGEXAnimation implements IAnimation
{
    private final ImmutableMap<OgexTransform, OgexTrack> tracks;
    private final ImmutableMap<OgexTransform, TRSRTransformation> transforms;
    private final ImmutableList<OgexTransform> transformKeys;
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
        transformKeys = ImmutableList.copyOf(transforms);
        this.transforms = mapBuilder.build();
    }

    @Override
    public TRSRTransformation apply(float time, Node<?> node)
    {
        TRSRTransformation ret = TRSRTransformation.identity();
        for(OgexTransform transform : transformKeys)
        {
            if(tracks.containsKey(transform))
            {
                ret = ret.compose(getCurrentValueForTrack(tracks.get(transform), time, 1)); // TODO: fps
            }
            else
            {
                ret = ret.compose(transforms.get(transform));
            }
        }
        return ret;
    }

    private TRSRTransformation getTrackData(OgexTrack track, float[] value)
    {
        Object target = track.getTarget();
        Matrix4f transform = new Matrix4f();
        if (target instanceof OgexMatrixTransform)
        {
            OgexMatrixTransform mt = (OgexMatrixTransform) target;
            transform.set(value);
            transform.transpose();
        } else if (target instanceof OgexRotation.ComponentRotation)
        {
            OgexRotation.ComponentRotation rot = (OgexRotation.ComponentRotation) target;
            float angle = value[0];
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
            throw new NotImplementedException("Only Matrix Transform or simple rotation for now");
        }
        transform.mul(upAxis, transform);
        transform.mul(upAxisInverted);
        return new TRSRTransformation(transform);
    }

    private TRSRTransformation getCurrentValueForTrack(OgexTrack track, float animationTime, float timeScale) {
        final TimeData keyData = getKeyData(track.getTime());
        final ValueData valueData = getValueData(track.getValue());
        int index = getKeyIndexForTime(keyData, timeScale, animationTime);

        float adjustedTime = 1;
        if (track.getTime().getCurve() == Curve.Bezier) {
            adjustedTime = getAdjustedBezierTime(keyData, timeScale, animationTime, index);
        } else if (track.getTime().getCurve() == Curve.Linear) {
            adjustedTime = getAdjustedLinearTime(keyData, timeScale, animationTime, index);
        }

        TRSRTransformation trackValue = TRSRTransformation.identity();
        if (track.getValue().getCurve() == Curve.Bezier) {
            trackValue = getAdjustedBezierValue(track, valueData, index, adjustedTime);
        } else if (track.getValue().getCurve() == Curve.Linear) {
            trackValue = getAdjustedLinearValue(track, valueData, index, adjustedTime);
        }


        return trackValue;
    }

    private TRSRTransformation getAdjustedLinearValue(OgexTrack track, ValueData valueData, int index, float adjustedTime) {
        final float[][] value = valueData.value;
        final float s = adjustedTime;
        // FIXME: be more careful with indices
        final float[] v1 = value[index == 0 ? 0 : index - 1];
        final float[] v2 = value[index];

        // do per-component if not a full matrix
        if(v1.length < 16)
        {
            // scalar argument, do scalar interpolation
            float[] v = new float[v1.length];
            for (int i = 0; i < v1.length; i++)
            {
                v[i] = v1[i] * (1 - s) + v2[i] * s;
            }
            return getTrackData(track, v);
        }

        // do matrix lerp if full matrix
        TRSRTransformation t1 = getTrackData(track, v1);
        TRSRTransformation t2 = getTrackData(track, v2);
        return t1.slerp(t2, s);
    }

    private TRSRTransformation getAdjustedBezierValue(OgexTrack track, ValueData valueData, int index, float adjustedTime) {
        final float[][] value = valueData.value;
        final float s = adjustedTime;
        // FIXME: be more careful with indices
        final float[] v1 = value[index == 0 ? 0 : index - 1];
        final float[] p1 = valueData.positiveControl[index == 0 ? 0 : index - 1];
        final float[] p2 = valueData.negativeControl[index];
        final float[] v2 = value[index];

        // do per-component if not a full matrix
        if(v1.length < 16)
        {
            // scalar argument, do scalar interpolation
            float[] v = new float[v1.length];
            for(int i = 0; i < v1.length; i++)
            {
                v[i] =
                    v1[i] * (1 - s) * (1 - s) * (1 - s) +
                    p1[i] * 3 * s * (1 - s) * (1 - s) +
                    p2[i] * 3 * s * s * (1 - s) +
                    v2[i] * s * s * s;
            }
            return getTrackData(track, v);
        }
        // do matrix lerp if full matrix
        Matrix4f m = getTrackData(track, v1).getMatrix(), t;
        m.mul((1 - s) * (1 - s) * (1 - s));
        t = getTrackData(track, p1).getMatrix();
        t.mul(3 * s * (1 - s) * (1 - s));
        m.add(t);
        t = getTrackData(track, p2).getMatrix();
        t.mul(3 * s * s * (1 - s));
        m.add(t);
        t = getTrackData(track, v2).getMatrix();
        t.mul(s * s * s);
        m.add(t);

        return new TRSRTransformation(m);
    }

    private static float getAdjustedLinearTime(TimeData keyData, float scale, float currentTime, int index) {
        final float[] value = keyData.value;
        float si;

        float t = currentTime;

        // FIXME: be more careful with indices
        final float t1 = value[index == 0 ? 0 : index - 1] * scale;
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

        // FIXME: be more careful with indices
        final float t1 = value[index == 0 ? 0 : index - 1] * scale;
        final float c1 = positiveControl[index == 0 ? 0 : index - 1] * scale;
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

    private static float[][] expandArrays(Object data)
    {
        if(data instanceof float[][])
        {
            return (float[][]) data;
        }
        float[] values = (float[]) data;
        float[][] ret = new float[values.length][];
        for(int i = 0; i < values.length; i++)
        {
            ret[i] = new float[]{values[i]};
        }
        return ret;
    }

    private static ValueData getValueData(OgexValue value) {
        final OgexKey[] keys = value.getKeys();
        ValueData timeData = new ValueData(keys);

        for (OgexKey key : keys) {
            switch (key.getKind()) {
                case Value:
                    timeData.value = expandArrays(key.getData());
                    break;
                case PositiveControl:
                    timeData.positiveControl = expandArrays(key.getData());
                    break;
                case NegativeControl:
                    timeData.negativeControl = expandArrays(key.getData());
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
        float[][] value;
        float[][] positiveControl;
        float[][] negativeControl;
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

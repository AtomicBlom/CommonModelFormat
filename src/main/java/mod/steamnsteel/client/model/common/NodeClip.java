package mod.steamnsteel.client.model.common;

import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.model.animation.IJointClip;

/**
 * Created by codew on 1/05/2016.
 */
class NodeClip implements IJointClip
{
    private final Node<?> node;

    public NodeClip(Node<?> node)
    {
        this.node = node;
    }

    @Override
    public TRSRTransformation apply(float time)
    {
        TRSRTransformation ret = TRSRTransformation.identity();
        if (node.getAnimation() == null)
        {
            return ret.compose(node.getTransformation());
        }
        int start = Math.max(1, (int) Math.round(Math.floor(time)));
        int end = Math.min(start + 1, (int) Math.round(Math.ceil(time)));
        float progress = time - (float) Math.floor(time);
        Key keyStart = node.getAnimation().getKeys().get(start, node);
        Key keyEnd = node.getAnimation().getKeys().get(end, node);
        TRSRTransformation startTr = keyStart == null ? null : new TRSRTransformation(keyStart.getPos(), keyStart.getRot(), keyStart.getScale(), null);
        TRSRTransformation endTr = keyEnd == null ? null : new TRSRTransformation(keyEnd.getPos(), keyEnd.getRot(), keyEnd.getScale(), null);
        if (keyStart == null)
        {
            if (keyEnd == null)
            {
                ret = ret.compose(node.getTransformation());
            }
            // TODO animated TRSR for speed?
            else
            {
                ret = ret.compose(endTr);
            }
        } else if (progress < 1e-5 || keyEnd == null)
        {
            ret = ret.compose(startTr);
        } else
        {
            ret = ret.compose(startTr.slerp(endTr, progress));
        }
        return ret;
    }
}

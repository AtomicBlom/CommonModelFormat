package com.github.atomicblom.client.model.cmf.common;

import com.google.common.base.Optional;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.model.IModelPart;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;

/**
 * Created by codew on 24/03/2016.
 */
public final class GenericState implements IModelState
{
    private final IAnimation animation;
    private final int frame;
    private final int nextFrame;
    private final float progress;
    private final IModelState parent;

    public GenericState(IAnimation animation, int frame)
    {
        this(animation, frame, frame, 0);
    }

    public GenericState(IAnimation animation, int frame, IModelState parent)
    {
        this(animation, frame, frame, 0, parent);
    }

    public GenericState(IAnimation animation, int frame, int nextFrame, float progress)
    {
        this(animation, frame, nextFrame, progress, null);
    }

    public GenericState(IAnimation animation, int frame, int nextFrame, float progress, IModelState parent)
    {
        this.animation = animation;
        this.frame = frame;
        this.nextFrame = nextFrame;
        this.progress = MathHelper.clamp_float(progress, 0, 1);
        this.parent = getParent(parent);
    }

    private IModelState getParent(IModelState parent)
    {
        if (parent == null) return null;
        else if (parent instanceof GenericState) return ((GenericState) parent).parent;
        return parent;
    }

    public IAnimation getAnimation()
    {
        return animation;
    }

    public int getFrame()
    {
        return frame;
    }

    public int getNextFrame()
    {
        return nextFrame;
    }

    public float getProgress()
    {
        return progress;
    }

    public IModelState getParent()
    {
        return parent;
    }

    @Override
    public Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part)
    {
        // TODO optionify better
        if (!part.isPresent()) return parent.apply(part);
        if (!(part.get() instanceof NodeJoint))
        {
            return Optional.absent();
        }
        Node<?> node = ((NodeJoint) part.get()).getNode();
        TRSRTransformation nodeTransform;
        if (progress < 1e-5 || frame == nextFrame)
        {
            nodeTransform = getNodeMatrix(node, frame);
        } else if (progress > 1 - 1e-5)
        {
            nodeTransform = getNodeMatrix(node, nextFrame);
        } else
        {
            nodeTransform = getNodeMatrix(node, frame);
            nodeTransform = nodeTransform.slerp(getNodeMatrix(node, nextFrame), progress);
        }
        if (parent != null && node.getParent() == null)
        {
            return Optional.of(parent.apply(part).or(TRSRTransformation.identity()).compose(nodeTransform));
        }
        return Optional.of(nodeTransform);
    }

    //FIXME: Reenable
    /*private static final LoadingCache<Triple<IAnimation, Node<?>, Integer>, TRSRTransformation> cache = CacheBuilder.newBuilder()
            .maximumSize(16384)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .build(new CacheLoader<Triple<IAnimation, Node<?>, Integer>, TRSRTransformation>()
            {
                @Override
                public TRSRTransformation load(Triple<IAnimation, Node<?>, Integer> key) throws Exception
                {
                    return getNodeMatrix(key.getLeft(), key.getMiddle(), key.getRight());
                }
            });
*/
    public TRSRTransformation getNodeMatrix(Node<?> node)
    {
        return getNodeMatrix(node, frame);
    }

    public TRSRTransformation getNodeMatrix(Node<?> node, int frame)
    {
        //return cache.getUnchecked(Triple.of(animation, node, frame));
        return getNodeMatrix(animation, node, frame);
    }

    public static TRSRTransformation getNodeMatrix(IAnimation animation, Node<?> node, int frame)
    {
        TRSRTransformation ret = TRSRTransformation.identity();
        TRSRTransformation local;
        if (animation != null) {
            local = animation.apply(frame, node);
        } else if (node.getAnimation() != null && node.getAnimation() != animation) {
            local = node.getAnimation().apply(frame, node);
        } else {
            local = node.getTransformation();
        }
        Node<?> parent = node.getParent();
        if (parent != null)
        {
            // parent cmf-global current pose
            TRSRTransformation pm = getNodeMatrix(animation, node.getParent(), frame);//cache.getUnchecked(Triple.of(animation, node.getParent(), frame));
            ret = ret.compose(pm);
            // joint offset in the parent coords
            ret = ret.compose(parent.getTransformation());
        }
        // current node local pose
        ret = ret.compose(local);
        // this part moved inside the cmf
        // inverse bind of the curent node
        /*Matrix4f rm = new TRSRTransformation(node.getPos(), node.getRot(), node.getScale(), null).getMatrix();
        rm.invert();
        ret = ret.compose(new TRSRTransformation(rm));
        if(parent != null)
        {
            // inverse bind of the parent
            rm = new TRSRTransformation(parent.getPos(), parent.getRot(), parent.getScale(), null).getMatrix();
            rm.invert();
            ret = ret.compose(new TRSRTransformation(rm));
        }*/
        // TODO cache
        TRSRTransformation invBind = new NodeJoint(node).getInvBindPose();
        ret = ret.compose(invBind);
        return ret;
    }
}

package mod.steamnsteel.client.model.common;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.model.IModelPart;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.Properties;
import org.apache.commons.lang3.tuple.Pair;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by codew on 24/03/2016.
 */
public class BakedWrapper implements IPerspectiveAwareModel
{
    private final Node<?> node;
    private final IModelState state;
    private final boolean smooth;
    private final boolean gui3d;
    private final VertexFormat format;
    private final ImmutableSet<String> meshes;
    private final ImmutableMap<String, TextureAtlasSprite> textures;
    private final LoadingCache<Integer, GenericState> cache;

    private ImmutableList<BakedQuad> quads;

    public BakedWrapper(final Node<?> node, final IModelState state, final boolean smooth, final boolean gui3d, final VertexFormat format, final ImmutableSet<String> meshes, final ImmutableMap<String, TextureAtlasSprite> textures)
    {
        this(node, state, smooth, gui3d, format, meshes, textures, CacheBuilder.newBuilder()
                .maximumSize(128)
                .expireAfterAccess(2, TimeUnit.MINUTES)
                .<Integer, GenericState>build(new CacheLoader<Integer, GenericState>()
                {
                    @Override
                    public GenericState load(Integer frame) throws Exception
                    {
                        IModelState parent = state;
                        Animation newAnimation = node.getAnimation();
                        if (parent instanceof GenericState)
                        {
                            GenericState ps = (GenericState) parent;
                            parent = ps.getParent();
                        }
                        return new GenericState(newAnimation, frame, frame, 0, parent);
                    }
                }));
    }

    public BakedWrapper(Node<?> node, IModelState state, boolean smooth, boolean gui3d, VertexFormat format, ImmutableSet<String> meshes, ImmutableMap<String, TextureAtlasSprite> textures, LoadingCache<Integer, GenericState> cache)
    {
        this.node = node;
        this.state = state;
        this.smooth = smooth;
        this.gui3d = gui3d;
        this.format = format;
        this.meshes = meshes;
        this.textures = textures;
        this.cache = cache;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand)
    {
        if (side != null) return ImmutableList.of();
        IModelState modelState = this.state;
        if (state instanceof IExtendedBlockState)
        {
            IExtendedBlockState exState = (IExtendedBlockState) state;
            if (exState.getUnlistedNames().contains(GenericFrameProperty.INSTANCE))
            {
                GenericState s = exState.getValue(GenericFrameProperty.INSTANCE);
                if (s != null)
                {
                    //return getCachedModel(s.getFrame());
                    IModelState parent = this.state;
                    Animation newAnimation = s.getAnimation();
                    if (parent instanceof GenericState)
                    {
                        GenericState ps = (GenericState) parent;
                        parent = ps.getParent();
                    }
                    if (newAnimation == null)
                    {
                        newAnimation = node.getAnimation();
                    }
                    if (s.getFrame() == s.getNextFrame())
                    {
                        modelState = cache.getUnchecked(s.getFrame());
                    } else
                    {
                        modelState = new GenericState(newAnimation, s.getFrame(), s.getNextFrame(), s.getProgress(), parent);
                    }
                }
            } else if (exState.getUnlistedNames().contains(Properties.AnimationProperty))
            {
                // FIXME: should animation state handle the parent state, or should it remain here?
                IModelState parent = this.state;
                if (parent instanceof GenericState)
                {
                    GenericState ps = (GenericState) parent;
                    parent = ps.getParent();
                }
                IModelState newState = exState.getValue(Properties.AnimationProperty);
                if (newState != null)
                {
                    modelState = new ModelStateComposition(parent, newState);
                }
            }
        }
        if (quads == null)
        {
            ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
            generateQuads(builder, node, this.state);
            quads = builder.build();
        }
        // TODO: caching?
        if (this.state != modelState)
        {
            ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
            generateQuads(builder, node, modelState);
            return builder.build();
        }
        return quads;
    }

    private void generateQuads(ImmutableList.Builder<BakedQuad> builder, Node<?> node, final IModelState state)
    {
        for (Node<?> child : node.getNodes().values())
        {
            generateQuads(builder, child, state);
        }
        //if (node.getKind() instanceof Mesh && meshes.contains(node.getName()))
        if (node.getKind() instanceof Mesh)
        {
            Mesh mesh = (Mesh) node.getKind();
            Collection<Face> faces = mesh.bake(new Function<Node<?>, Matrix4f>()
            {
                private final TRSRTransformation global = state.apply(Optional.<IModelPart>absent()).or(TRSRTransformation.identity());
                private final LoadingCache<Node<?>, TRSRTransformation> localCache = CacheBuilder.newBuilder()
                        .maximumSize(32)
                        .build(new CacheLoader<Node<?>, TRSRTransformation>()
                        {
                            @Override
                            public TRSRTransformation load(Node<?> node) throws Exception
                            {
                                return state.apply(Optional.of(new NodeJoint(node))).or(TRSRTransformation.identity());
                            }
                        });

                @Override
                public Matrix4f apply(Node<?> node)
                {
                    final TRSRTransformation unchecked = state.apply(Optional.of(new NodeJoint(node))).or(TRSRTransformation.identity());//localCache.getUnchecked(node);
                    return global.compose(unchecked).getMatrix();
                }
            });
            for (Face f : faces)
            {
                UnpackedBakedQuad.Builder quadBuilder = new UnpackedBakedQuad.Builder(format);
                quadBuilder.setQuadOrientation(EnumFacing.getFacingFromVector(f.getNormal().x, f.getNormal().y, f.getNormal().z));
                List<Texture> textures = null;
                if (f.getBrush() != null) textures = f.getBrush().getTextures();
                TextureAtlasSprite sprite;
                if (textures == null || textures.isEmpty()) sprite = this.textures.get("missingno");
                else if (textures.get(0) == Texture.White) sprite = ModelLoader.White.INSTANCE;
                else sprite = this.textures.get(textures.get(0).getPath());
                quadBuilder.setTexture(sprite);
                putVertexData(quadBuilder, f.getV1(), f.getNormal(), sprite);
                putVertexData(quadBuilder, f.getV2(), f.getNormal(), sprite);
                putVertexData(quadBuilder, f.getV3(), f.getNormal(), sprite);
                if (f.getVertexCount() == 3)
                {
                    putVertexData(quadBuilder, f.getV3(), f.getNormal(), sprite);
                } else {
                    putVertexData(quadBuilder, f.getV4(), f.getNormal(), sprite);
                }
                builder.add(quadBuilder.build());
            }
        }
    }

    private final void putVertexData(UnpackedBakedQuad.Builder builder, Vertex v, Vector3f faceNormal, TextureAtlasSprite sprite)
    {
        // TODO handle everything not handled (texture transformations, bones, transformations, normals, e.t.c)
        for (int e = 0; e < format.getElementCount(); e++)
        {
            switch (format.getElement(e).getUsage())
            {
                case POSITION:
                    builder.put(e, v.getPos().x, v.getPos().y, v.getPos().z, 1);
                    break;
                case COLOR:
                    if (v.getColor() != null)
                    {
                        builder.put(e, v.getColor().x, v.getColor().y, v.getColor().z, v.getColor().w);
                    } else
                    {
                        builder.put(e, 1, 1, 1, 1);
                    }
                    break;
                case UV:
                    // TODO handle more brushes
                    if (format.getElement(e).getIndex() < v.getTexCoords().length)
                    {
                        builder.put(e,
                                sprite.getInterpolatedU(v.getTexCoords()[0].x * 16),
                                sprite.getInterpolatedV(v.getTexCoords()[0].y * 16),
                                0,
                                1
                        );
                    } else
                    {
                        builder.put(e, 0, 0, 0, 1);
                    }
                    break;
                case NORMAL:
                    if (v.getNormal() != null)
                    {
                        builder.put(e, v.getNormal().x, v.getNormal().y, v.getNormal().z, 0);
                    } else
                    {
                        builder.put(e, faceNormal.x, faceNormal.y, faceNormal.z, 0);
                    }
                    break;
                default:
                    builder.put(e);
            }
        }
    }

    @Override
    public boolean isAmbientOcclusion()
    {
        return smooth;
    }

    @Override
    public boolean isGui3d()
    {
        return gui3d;
    }

    @Override
    public boolean isBuiltInRenderer()
    {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture()
    {
        // FIXME somehow specify particle texture in the model
        return textures.values().asList().get(0);
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms()
    {
        return ItemCameraTransforms.DEFAULT;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType)
    {
        return MapWrapper.handlePerspective(this, state, cameraTransformType);
    }

    @Override
    public ItemOverrideList getOverrides()
    {
        // TODO handle items
        return ItemOverrideList.NONE;
    }
}

package com.github.atomicblom.client.model.cmf.common;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.*;
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

import javax.vecmath.*;
import java.util.Collection;
import java.util.List;

public class BakedWrapper implements IPerspectiveAwareModel
{

    private final Node<?> node;
    private final IModelState state;
    private final boolean smooth;
    private final boolean gui3d;
    private final VertexFormat format;
    private final ImmutableSet<String> meshes;
    private final ImmutableMap<String, TextureAtlasSprite> textures;

    private ImmutableList<BakedQuad> quads;

    public BakedWrapper(Node<?> node, IModelState state, boolean smooth, boolean gui3d, VertexFormat format, ImmutableSet<String> meshes, ImmutableMap<String, TextureAtlasSprite> textures)
    {
        this.node = node;
        this.state = state;
        this.smooth = smooth;
        this.gui3d = gui3d;
        this.format = format;
        this.meshes = meshes;
        this.textures = textures;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand)
    {
        if (side != null) return ImmutableList.of();
        IModelState modelState = this.state;
        if (state instanceof IExtendedBlockState)
        {
            IExtendedBlockState exState = (IExtendedBlockState) state;
            if (exState.getUnlistedNames().contains(Properties.AnimationProperty))
            {
                // FIXME: should animation state handle the parent state, or should it remain here?
                IModelState parent = this.state;
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

    private static final Brush jointDebugBrush = new Brush("armature", new Vector4f(1, 1, 1, 1), 0, 1, 0, ImmutableList.of(Texture.White));

    private void generateQuads(ImmutableList.Builder<BakedQuad> builder, Node<?> node, final IModelState state)
    {
        for (Node<?> child : node.getNodes().values())
        {
            generateQuads(builder, child, state);
        }
        boolean boneDebugging = true;

        if (node.getKind() instanceof Bone && boneDebugging) {
            // pose from .getInvBindPose, red
            TRSRTransformation pose = node.getInvBindPose().inverse();
            ImmutableList<Face> staticFaces = buildJointFaces(jointDebugBrush, pose, 1, 0, 0, null, null);
            ImmutableList<Face> staticBoneFaces = buildBoneFaces(jointDebugBrush, node, 1, 0, 0, null);
            // pose from forward hierarchy, blue
            TRSRTransformation forwardPose = TRSRTransformation.identity();
            Node<?> parentNode = node;
            while(parentNode != null)
            {
                forwardPose = parentNode.getTransformation().compose(forwardPose);
                parentNode = parentNode.getParent();
            }
            ImmutableList<Face> staticFaces2 = buildJointFaces(jointDebugBrush, forwardPose, 0, 0, 1, null, null);

            // deformable joints, green
            ImmutableMultimap.Builder<Vertex, BoneWeight> weightBuilder = ImmutableMultimap.builder();
            BoneWeight bw = new BoneWeight((Node<Bone>) node, 1f);

            ImmutableList<Face> dynamicFaces = buildJointFaces(jointDebugBrush, pose, 0, 1, 0, weightBuilder, bw);
            ImmutableList<Face> dynamicBoneFaces = buildBoneFaces(jointDebugBrush, node, 0, 1, 0, weightBuilder);

            Mesh mesh = new Mesh(jointDebugBrush, ImmutableList.copyOf(Iterables.concat(staticFaces, staticBoneFaces, staticFaces2, dynamicFaces, dynamicBoneFaces)));
            // setting dummy mesh node
            Node.create("DummyBoneMeshNode", TRSRTransformation.identity(), ImmutableList.<Node<?>>of(), mesh, null, false);
            mesh.setWeightMap(weightBuilder.build());
            mesh.setBones(ImmutableSet.of((Node<Bone>) node));
            buildMesh(builder, state, mesh);
        }

        //if (node.getKind() instanceof Mesh && meshes.contains(node.getName()))
        // show all meshes, helpful for debugging
        if (node.getKind() instanceof Mesh)
        {
            Mesh mesh = (Mesh) node.getKind();
            buildMesh(builder, state, mesh);
        }
    }

    private Face makeOctahedronFace(Brush brush, Matrix4f pose, int i, float r, float g, float b)
    {
        int x = i & 1;
        int y = (i >> 1) & 1;
        int z = (i >> 2) & 1;
        Vector4f p = new Vector4f();
        p.set(x * 2 - 1, 0, 0, 1);
        pose.transform(p);
        Vertex v0 = new Vertex(new Vector3f(p.x / p.w, p.y / p.w, p.z / p.w), null, new Vector4f(r, g, b, 1), new Vector4f[]{new Vector4f(.5f, .5f, 0, 1)});
        p.set(0, y * 2 - 1, 0, 1);
        pose.transform(p);
        Vertex v1 = new Vertex(new Vector3f(p.x / p.w, p.y / p.w, p.z / p.w), null, new Vector4f(r, g, b, 1), new Vector4f[]{new Vector4f(.5f, .5f, 0, 1)});
        p.set(0, 0, z * 2 - 1, 1);
        pose.transform(p);
        Vertex v2 = new Vertex(new Vector3f(p.x / p.w, p.y / p.w, p.z / p.w), null, new Vector4f(r, g, b, 1), new Vector4f[]{new Vector4f(.5f, .5f, 0, 1)});
        if((x ^ y ^ z) == 1)
        {
            return new Face(v0, v1, v2, brush);
        }
        else
        {
            return new Face(v2, v1, v0, brush);
        }

    }

    private ImmutableList<Face> buildJointFaces(Brush brush, TRSRTransformation pose, float r, float g, float b, ImmutableMultimap.Builder<Vertex, BoneWeight> weightBuilder, BoneWeight bw)
    {
        ImmutableList.Builder<Face> faceBuilder = ImmutableList.builder();
        Matrix4f pm = pose.compose(new TRSRTransformation(null, null, new Vector3f(.1f, .1f, .1f), null)).getMatrix();
        for(int i = 0; i < 8; i++)
        {
            Face face = makeOctahedronFace(brush, pm, i, r, g, b);
            faceBuilder.add(face);
            if(weightBuilder != null)
            {
                weightBuilder.put(face.getV1(), bw);
                weightBuilder.put(face.getV2(), bw);
                weightBuilder.put(face.getV3(), bw);
            }
        }
        return faceBuilder.build();
    }

    private ImmutableList<Face> buildBoneFaces(Brush brush, Node<?> node, float r, float g, float b, ImmutableMultimap.Builder<Vertex, BoneWeight> weightBuilder)
    {
        if(!(node.getParent().getKind() instanceof Bone))
        {
            return ImmutableList.of();
        }
        TRSRTransformation poseStart = node.getParent().getInvBindPose().inverse();
        ImmutableList.Builder<Face> faceBuilder = ImmutableList.builder();
        Vector3f from = new Vector3f(0, 1, 0), to = new Vector3f(), axis = new Vector3f();
        Vector4f t = new Vector4f(0, 0, 0, 1);
        poseStart.inverse().compose(node.getInvBindPose().inverse()).getMatrix().transform(t);
        to.set(t.x / t.w, t.y / t.w, t.z / t.w);
        if(to.length() < 1e-4)
        {
            // bone too short
            return ImmutableList.of();
        }
        float scale = to.length();
        to.normalize();
        axis.cross(from, to);
        float angle = (float)Math.acos(from.dot(to));
        Quat4f rot = new Quat4f();
        rot.set(new AxisAngle4f(axis, angle));
        TRSRTransformation local = new TRSRTransformation(null, rot, new Vector3f(scale, scale, scale), null);
        Matrix4f global = poseStart.compose(local).compose(new TRSRTransformation(new Vector3f(0, .5f, 0), null, new Vector3f(.1f, .5f, .1f), null)).getMatrix();
        BoneWeight bs = new BoneWeight((Node<Bone>)(node.getParent()), 1f);
        BoneWeight bsm = new BoneWeight((Node<Bone>)(node.getParent()), .5f);
        BoneWeight bem = new BoneWeight((Node<Bone>)node, .5f);
        BoneWeight be = new BoneWeight((Node<Bone>)node, 1f);
        for(int i = 0; i < 8; i++)
        {
            Face face = makeOctahedronFace(brush, global, i, r, g, b);
            faceBuilder.add(face);
            if(weightBuilder != null)
            {
                weightBuilder.put(face.getV1(), bsm);
                weightBuilder.put(face.getV1(), bem);
                weightBuilder.put(face.getV3(), bsm);
                weightBuilder.put(face.getV3(), bem);
                weightBuilder.put(face.getV2(), (i & 2) == 0 ? bs : be);
            }
        }
        return faceBuilder.build();
    }

    private void buildMesh(ImmutableList.Builder<BakedQuad> builder, final IModelState state, Mesh mesh)
    {
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
                final TRSRTransformation unchecked = localCache.getUnchecked(node);
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
            if (sprite == null) {
                sprite = ModelLoader.White.INSTANCE;
            }

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
        // FIXME somehow specify particle texture in the cmf
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

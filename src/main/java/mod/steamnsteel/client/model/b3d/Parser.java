package mod.steamnsteel.client.model.b3d;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Table;
import mod.steamnsteel.client.model.common.*;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.*;

public class Parser
{
    static final Logger logger = LogManager.getLogger(Parser.class);
    private static final boolean printLoadedModels = "true".equals(System.getProperty("b3dloader.printLoadedModels"));

    private static final int version = 0001;
    private final ByteBuffer buf;

    private byte[] tag = new byte[4];
    private int length;
    public Parser(InputStream in) throws IOException
    {
        if(in instanceof FileInputStream)
        {
            // fast shorthand for normal files
            FileChannel channel = ((FileInputStream)in).getChannel();
            buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()).order(ByteOrder.LITTLE_ENDIAN);
        }
        else
        {
            // slower default for others
            IOUtils.readFully(in, tag);
            byte[] tmp = new byte[4];
            IOUtils.readFully(in, tmp);
            int l = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if(l < 0 || l + 8 < 0) throw new IOException("File is too large");
            buf = ByteBuffer.allocate(l + 8).order(ByteOrder.LITTLE_ENDIAN);
            buf.clear();
            buf.put(tag);
            buf.put(tmp);
            buf.put(IOUtils.toByteArray(in, l));
            buf.flip();
        }
    }

    private String dump = "";
    private void dump(String str)
    {
        if(printLoadedModels)
        {
            dump += str + "\n";
        }
    }

    private GenericModel res;

    public GenericModel parse() throws IOException
    {
        if(res != null) return res;
        dump = "\n";
        readHeader();
        res = bb3d();
        if(printLoadedModels)
        {
            logger.info(dump);
        }
        return res;
    }

    private final List<Texture> textures = new ArrayList<Texture>();

    private Texture getTexture(int texture)
    {
        if(texture > textures.size())
        {
            logger.error(String.format("texture %s is out of range", texture));
            return null;
        }
        else if(texture == -1) return Texture.White;
        return textures.get(texture);
    }

    private final List<Brush> brushes = new ArrayList<Brush>();

    private Brush getBrush(int brush)
    {
        if(brush > brushes.size())
        {
            logger.error(String.format("brush %s is out of range", brush));
            return null;
        }
        else if(brush == -1) return null;
        return brushes.get(brush);
    }

    private final List<Vertex> vertices = new ArrayList<Vertex>();

    private Vertex getVertex(int vertex)
    {
        if(vertex > vertices.size())
        {
            logger.error(String.format("vertex %s is out of range", vertex));
            return null;
        }
        return vertices.get(vertex);
    }

    private final ImmutableMap.Builder<String, Node<Mesh>> meshes = ImmutableMap.builder();

    private void readHeader() throws IOException
    {
        buf.get(tag);
        length = buf.getInt();
    }

    private boolean isChunk(String tag) throws IOException
    {
        return Arrays.equals(this.tag, tag.getBytes("US-ASCII"));
    }

    private void chunk(String tag) throws IOException
    {
        if(!isChunk(tag)) throw new IOException("Expected chunk " + tag + ", got " + new String(this.tag, "US-ASCII"));
        pushLimit();
    }

    private String readString() throws IOException
    {
        int start = buf.position();
        while(buf.get() != 0);
        int end = buf.position();
        byte[] tmp = new byte[end - start - 1];
        buf.position(start);
        buf.get(tmp);
        buf.get();
        String ret =  new String(tmp, "UTF8");
        return ret;
    }

    private Deque<Integer> limitStack = new ArrayDeque<Integer>();

    private void pushLimit()
    {
        limitStack.push(buf.limit());
        buf.limit(buf.position() + length);
    }

    private void popLimit()
    {
        buf.limit(limitStack.pop());
    }

    private GenericModel bb3d() throws IOException
    {
        chunk("BB3D");
        int version = buf.getInt();
        if(version / 100 > Parser.version / 100)
            throw new IOException("Unsupported major model version: " + ((float)version / 100));
        if(version % 100 > Parser.version % 100)
            logger.warn(String.format("Minor version differnce in model: ", ((float)version / 100)));
        List<Texture> textures = Collections.emptyList();
        List<Brush> brushes = Collections.emptyList();
        Node<?> root = null;
        dump("BB3D(version = " + version + ") {");
        while(buf.hasRemaining())
        {
            readHeader();
            if     (isChunk("TEXS")) textures = texs();
            else if(isChunk("BRUS")) brushes = brus();
            else if(isChunk("NODE")) root = node();
            else skip();
        }
        dump("}");
        popLimit();
        return new GenericModel(textures, brushes, root, meshes.build());
    }

    private List<Texture> texs() throws IOException
    {
        chunk("TEXS");
        List<Texture> ret = new ArrayList<Texture>();
        while(buf.hasRemaining())
        {
            String path = readString();
            int flags = buf.getInt();
            int blend = buf.getInt();
            Vector2f pos = new Vector2f(buf.getFloat(), buf.getFloat());
            Vector2f scale = new Vector2f(buf.getFloat(), buf.getFloat());
            float rot = buf.getFloat();
            ret.add(new Texture(path, flags, blend, pos, scale, rot));
        }
        dump("TEXS([" + Joiner.on(", ").join(ret) + "])");
        popLimit();
        this.textures.addAll(ret);
        return ret;
    }

    private List<Brush> brus() throws IOException
    {
        chunk("BRUS");
        List<Brush> ret = new ArrayList<Brush>();
        int n_texs = buf.getInt();
        while(buf.hasRemaining())
        {
            String name = readString();
            Vector4f color = new Vector4f(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat());
            float shininess = buf.getFloat();
            int blend = buf.getInt();
            int fx = buf.getInt();
            List<Texture> textures = new ArrayList<Texture>();
            for(int i = 0; i < n_texs; i++) textures.add(getTexture(buf.getInt()));
            ret.add(new Brush(name, color, shininess, blend, fx, textures));
        }
        dump("BRUS([" + Joiner.on(", ").join(ret) + "])");
        popLimit();
        this.brushes.addAll(ret);
        return ret;
    }

    private List<Vertex> vrts() throws IOException
    {
        chunk("VRTS");
        List<Vertex> ret = new ArrayList<Vertex>();
        int flags = buf.getInt();
        int tex_coord_sets = buf.getInt();
        int tex_coord_set_size = buf.getInt();
        while(buf.hasRemaining())
        {
            Vector3f v = new Vector3f(buf.getFloat(), buf.getFloat(), buf.getFloat()), n = null;
            Vector4f color = null;
            if((flags & 1) != 0)
            {
                n = new Vector3f(buf.getFloat(), buf.getFloat(), buf.getFloat());
            }
            if((flags & 2) != 0)
            {
                color = new Vector4f(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat());
            }
            Vector4f[] tex_coords = new Vector4f[tex_coord_sets];
            for(int i = 0; i < tex_coord_sets; i++)
            {
                switch(tex_coord_set_size)
                {
                    case 1:
                        tex_coords[i] = new Vector4f(buf.getFloat(), 0, 0, 1);
                        break;
                    case 2:
                        tex_coords[i] = new Vector4f(buf.getFloat(), buf.getFloat(), 0, 1);
                        break;
                    case 3:
                        tex_coords[i] = new Vector4f(buf.getFloat(), buf.getFloat(), buf.getFloat(), 1);
                        break;
                    case 4:
                        tex_coords[i] = new Vector4f(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat());
                        break;
                    default:
                        logger.error(String.format("Unsupported number of texture coords: ", tex_coord_set_size));
                        tex_coords[i] = new Vector4f(0, 0, 0, 1);
                }
            }
            ret.add(new Vertex(v, n, color, tex_coords));
        }
        dump("VRTS([" + Joiner.on(", ").join(ret) + "])");
        popLimit();
        this.vertices.clear();
        this.vertices.addAll(ret);
        return ret;
    }

    private List<Face> tris() throws IOException
    {
        chunk("TRIS");
        List<Face> ret = new ArrayList<Face>();
        int brush_id = buf.getInt();
        while(buf.hasRemaining())
        {
            ret.add(new Face(getVertex(buf.getInt()), getVertex(buf.getInt()), getVertex(buf.getInt()), getBrush(brush_id)));
        }
        dump("TRIS([" + Joiner.on(", ").join(ret) + "])");
        popLimit();
        return ret;
    }

    private Pair<Brush, List<Face>> mesh() throws IOException
    {
        chunk("MESH");
        int brush_id = buf.getInt();
        readHeader();
        dump("MESH(brush = " + brush_id + ") {");
        vrts();
        List<Face> ret = new ArrayList<Face>();
        while(buf.hasRemaining())
        {
            readHeader();
            ret.addAll(tris());
        }
        dump("}");
        popLimit();
        return Pair.of(getBrush(brush_id), ret);
    }

    private List<Pair<Vertex, Float>> bone() throws IOException
    {
        chunk("BONE");
        List<Pair<Vertex, Float>> ret = new ArrayList<Pair<Vertex, Float>>();
        while(buf.hasRemaining())
        {
            ret.add(Pair.of(getVertex(buf.getInt()), buf.getFloat()));
        }
        dump("BONE(...)");
        popLimit();
        return ret;
    }

    private final Deque<Table<Integer, Optional<Node<?>>, Key>> animations = new ArrayDeque<Table<Integer, Optional<Node<?>>, Key>>();

    private Map<Integer, Key> keys() throws IOException
    {
        chunk("KEYS");
        Map<Integer, Key> ret = new HashMap<Integer, Key>();
        int flags = buf.getInt();
        Vector3f pos = null, scale = null;
        Quat4f rot = null;
        while(buf.hasRemaining())
        {
            int frame = buf.getInt();
            if((flags & 1) != 0)
            {
                pos = new Vector3f(buf.getFloat(), buf.getFloat(), buf.getFloat());
            }
            if((flags & 2) != 0)
            {
                scale = new Vector3f(buf.getFloat(), buf.getFloat(), buf.getFloat());
            }
            if((flags & 4) != 0)
            {
                rot = readQuat();
            }
            Key key = new Key(pos, scale, rot);
            Key oldKey = animations.peek().get(frame, null);
            if(oldKey != null)
            {
                if(pos != null)
                {
                    if(oldKey.getPos() != null) logger.error("Duplicate keys: %s and %s (ignored)", oldKey, key);
                    else key = new Key(oldKey.getPos(), key.getScale(), key.getRot());
                }
                if(scale != null)
                {
                    if(oldKey.getScale() != null) logger.error("Duplicate keys: %s and %s (ignored)", oldKey, key);
                    else key = new Key(key.getPos(), oldKey.getScale(), key.getRot());
                }
                if(rot != null)
                {
                    if(oldKey.getRot() != null) logger.error("Duplicate keys: %s and %s (ignored)", oldKey, key);
                    else key = new Key(key.getPos(), key.getScale(), oldKey.getRot());
                }
            }
            animations.peek().put(frame, Optional.<Node<?>>absent(), key);
            ret.put(frame, key);
        }
        dump("KEYS([(" + Joiner.on("), (").withKeyValueSeparator(" -> ").join(ret) + ")])");
        popLimit();
        return ret;
    }

    private Triple<Integer, Integer, Float> anim() throws IOException
    {
        chunk("ANIM");
        int flags = buf.getInt();
        int frames = buf.getInt();
        float fps = buf.getFloat();
        dump("ANIM(" + flags + ", " + frames + ", " + fps + ")");
        popLimit();
        return Triple.of(flags, frames, fps);
    }

    private Node<?> node() throws IOException
    {
        chunk("NODE");
        animations.push(HashBasedTable.<Integer, Optional<Node<?>>, Key>create());
        Triple<Integer, Integer, Float> animData = null;
        Pair<Brush, List<Face>> mesh = null;
        List<Pair<Vertex, Float>> bone = null;
        Map<Integer, Key> keys = new HashMap<Integer, Key>();
        List<Node<?>> nodes = new ArrayList<Node<?>>();
        String name = readString();
        Vector3f pos = new Vector3f(buf.getFloat(), buf.getFloat(), buf.getFloat());
        Vector3f scale = new Vector3f(buf.getFloat(), buf.getFloat(), buf.getFloat());
        Quat4f rot = readQuat();
        dump("NODE(" + name + ", " + pos + ", " + scale + ", " + rot + ") {");
        while(buf.hasRemaining())
        {
            readHeader();
            if     (isChunk("MESH")) mesh = mesh();
            else if(isChunk("BONE")) bone = bone();
            else if(isChunk("KEYS")) keys.putAll(keys());
            else if(isChunk("NODE")) nodes.add(node());
            else if(isChunk("ANIM")) animData = anim();
            else skip();
        }
        dump("}");
        popLimit();
        Table<Integer, Optional<Node<?>>, Key> keyData = animations.pop();
        Node<?> node;
        if(mesh != null)
        {
            final Mesh meshKind = new Mesh(mesh);
            Node<Mesh> mNode = Node.create(name, new TRSRTransformation(pos, rot, scale, null), nodes, meshKind);

            ImmutableMultimap.Builder<Vertex, Pair<Float, Node<Bone>>> builder = ImmutableMultimap.builder();
            for (Node<Bone> childBone : meshKind.getBones()) {
                for (Pair<Vertex, Float> b : childBone.getKind().getData()) {
                    builder.put(b.getLeft(), Pair.of(b.getRight(), childBone));
                }
            }

            meshKind.setWeightMap(builder.build());

            meshes.put(name, mNode);
            node = mNode;
        }
        else if(bone != null) node = Node.create(name, new TRSRTransformation(pos, rot, scale, null), nodes, new Bone(bone));
        else node = Node.create(name, new TRSRTransformation(pos, rot, scale, null), nodes, new Pivot());
        if(animData == null)
        {
            for(Table.Cell<Integer, Optional<Node<?>>, Key> key : keyData.cellSet())
            {
                animations.peek().put(key.getRowKey(), key.getColumnKey().or(Optional.of(node)), key.getValue());
            }
        }
        else
        {
            node.setAnimation(animData, keyData);
        }
        return node;
    }

    private Quat4f readQuat()
    {
        float w = buf.getFloat();
        float x = buf.getFloat();
        float y = buf.getFloat();
        float z = buf.getFloat();
        return new Quat4f(x, y, z, w);
    }

    private void skip()
    {
        buf.position(buf.position() + length);
    }
}

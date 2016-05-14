package com.github.atomicblom.client.model.cmf.opengex.conversion;

import com.github.atomicblom.client.model.cmf.common.Brush;
import com.github.atomicblom.client.model.cmf.common.Texture;
import com.github.atomicblom.client.model.cmf.opengex.ogex.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import javax.vecmath.Vector2f;
import java.util.Set;

@SuppressWarnings({"ClassHasNoToStringMethod", "UnnecessarilyQualifiedInnerClassAccess"})
public class BrushSetBuilderContext
{

    private final ModelBuilderContext modelContext;
    private final Set<Texture> textures = Sets.newHashSet();
    private final ImmutableMap.Builder<OgexMaterial, Brush> brushMap = ImmutableMap.builder();

    BrushSetBuilderContext(ModelBuilderContext modelContext)
    {

        this.modelContext = modelContext;
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    public Texture getTextureFromOgexTexture(OgexTexture texture)
    {
        if (texture == null)
        {
            return Texture.White;
        }
        final String path = texture.getTexture();

        Vector2f scale = new Vector2f(1, 1);
        Vector2f translation = new Vector2f(0, 0);
        float rotation = 0.0f;
        for (final OgexTransform ogexTransform : texture.getTransforms())
        {
            if (ogexTransform instanceof OgexScale)
            {
                scale = getOgexScale((OgexScale) ogexTransform);
            } else if (ogexTransform instanceof OgexRotation)
            {
                rotation = getOgexRotation((OgexRotation) ogexTransform);
            } else if (ogexTransform instanceof OgexTranslation)
            {
                translation = getOgexTranslation((OgexTranslation) ogexTransform);
            }
        }

        //I have no idea what opengex's intention was with these two flags.
        final int flags = 0;
        final int blend = 0;

        return verifySingleTexture(new Texture(path, flags, blend, translation, scale, rotation));
    }

    private Texture verifySingleTexture(Texture texture)
    {
        final float EPSILON = 0.00001f;

        for (final Texture existingTexture : textures)
        {
            boolean matches = existingTexture.getPath().equals(texture.getPath());
            matches &= existingTexture.getBlend() == texture.getBlend();
            matches &= existingTexture.getFlags() == texture.getFlags();
            matches &= Math.abs(existingTexture.getRot()  - texture.getRot()) < EPSILON;
            matches &= Math.abs(existingTexture.getPos().x - texture.getPos().x) < EPSILON;
            matches &= Math.abs(existingTexture.getPos().y - texture.getPos().y) < EPSILON;
            matches &= Math.abs(existingTexture.getScale().x - texture.getScale().x) < EPSILON;
            matches &= Math.abs(existingTexture.getScale().y - texture.getScale().y) < EPSILON;

            if (matches)
            {
                return existingTexture;
            }
        }
        textures.add(texture);
        return texture;
    }

    public void mapMaterialToBrush(OgexMaterial ogexMaterial, Brush brush)
    {
        brushMap.put(ogexMaterial, brush);
    }

    public void finish()
    {
        modelContext.setBrushes(brushMap.build());
        modelContext.setTextures(ImmutableSet.<Texture>builder().addAll(textures).build());
    }

    @SuppressWarnings({"ChainOfInstanceofChecks", "UnnecessarilyQualifiedInnerClassAccess"})
    private static Vector2f getOgexTranslation(OgexTranslation ogexTransform)
    {
        final Vector2f translation = new Vector2f();
        if (ogexTransform instanceof OgexTranslation.ComponentTranslation) {
            final OgexTranslation.ComponentTranslation ogexTranslation = (OgexTranslation.ComponentTranslation) ogexTransform;
            if (ogexTranslation.getKind() == OgexTranslation.Kind.X)
            {
                translation.x = ogexTranslation.getTranslation();

            } else if (ogexTranslation.getKind() == OgexTranslation.Kind.Y)
            {
                translation.y = ogexTranslation.getTranslation();

            } else if (ogexTranslation.getKind() == OgexTranslation.Kind.Z)
            {
                throw new UnsupportedOperationException("OGEX: Attempt to translate a texture by the 3rd dimension?");
            }
        } else if (ogexTransform instanceof OgexTranslation.XyzTranslation) {
            final OgexTranslation.XyzTranslation ogexTranslation = (OgexTranslation.XyzTranslation) ogexTransform;
            translation.x = ogexTranslation.getTranslation()[0];
            translation.y = ogexTranslation.getTranslation()[1];
        }
        return translation;
    }

    @SuppressWarnings({"ChainOfInstanceofChecks", "UnnecessarilyQualifiedInnerClassAccess"})
    private static float getOgexRotation(OgexRotation ogexTransform)
    {
        float rotation = 0;
        if (ogexTransform instanceof OgexRotation.AxisRotation) {
            final OgexRotation.AxisRotation ogexRotation = (OgexRotation.AxisRotation) ogexTransform;
            //note: I have *NO* idea which axis this comes in as.
            rotation = ogexRotation.getAngle();
        } else if (ogexTransform instanceof OgexRotation.ComponentRotation) {
            final OgexRotation.ComponentRotation ogexRotation = (OgexRotation.ComponentRotation) ogexTransform;
            rotation = ogexRotation.getAngle();
        } else if (ogexTransform instanceof OgexRotation.QuaternionRotation) {
            throw new UnsupportedOperationException("OGEX: Attempt to rotate a texture using a Quaternion");
        }
        return rotation;
    }

    @SuppressWarnings({"ChainOfInstanceofChecks", "UnnecessarilyQualifiedInnerClassAccess"})
    private static Vector2f getOgexScale(OgexScale ogexTransform)
    {
        final Vector2f scale = new Vector2f();
        if (ogexTransform instanceof OgexScale.ComponentScale) {
            final OgexScale.ComponentScale ogexScale = (OgexScale.ComponentScale) ogexTransform;

            if (ogexScale.getKind() == OgexScale.Kind.X)
            {
                scale.x = ogexScale.getScale();

            } else if (ogexScale.getKind() == OgexScale.Kind.Y)
            {
                scale.y = ogexScale.getScale();

            } else if (ogexScale.getKind() == OgexScale.Kind.Z)
            {
                throw new UnsupportedOperationException("OGEX: Attempt to scale a texture by the 3rd dimension?");
            }
        } else if (ogexTransform instanceof OgexScale.XyzScale) {
            final OgexScale.XyzScale ogexScale = (OgexScale.XyzScale) ogexTransform;
            scale.x = ogexScale.getScale()[0];
            scale.y = ogexScale.getScale()[1];
        }
        return scale;
    }
}

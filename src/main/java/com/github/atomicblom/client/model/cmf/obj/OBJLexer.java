package com.github.atomicblom.client.model.cmf.obj;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import joptsimple.internal.Strings;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import javax.annotation.Nullable;
import java.io.*;
import java.util.Iterator;

public enum OBJToken {
    V() {
        @Override
        public Optional<Token> apply(String[] input) throws ModelLoaderRegistry.LoaderException
        {
            if (input.length == 3)
            {
                return Optional.<Token>of(new CoordVertex(input[0], input[1], input[2], "1.0"));
            } else if (input.length == 4) {
                return Optional.<Token>of(new CoordVertex(input[0], input[1], input[2], input[3]));
            } else {
                throw new ModelLoaderRegistry.LoaderException("attempt to create vertices out of " + input.length + "elements");
            }
        }
    },
    COMMENT() {
        @Override
        public Optional<Token> apply(String[] input)
        {
            return Optional.<Token>of(new CommentToken(Strings.join(input, " ")));
        }
    },
    ENDOFFILE;

    public Optional<Token> apply(@Nullable String[] input) throws ModelLoaderRegistry.LoaderException {
        return Optional.absent();
    }

}

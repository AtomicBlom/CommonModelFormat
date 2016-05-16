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
            if (input.length >= 3 && input.length <= 4)
            {
                return Optional.<Token>of(new CoordVertex(
                        input[0],
                        input[1],
                        input[2],
                        input.length == 4 ? input[3] : "1.0"));
            } else {
                throw new ModelLoaderRegistry.LoaderException("attempt to create vertices out of " + input.length + "elements");
            }
        }
    },
    VN() {
        @Override
        public Optional<Token> apply(@Nullable String[] input) throws ModelLoaderRegistry.LoaderException {
            if (input.length == 3) {
                return Optional.<Token>of(new CoordNormal(input[0], input[1], input[2]));
            } else {
                throw new ModelLoaderRegistry.LoaderException("attempt to create normal out of " + input.length + "elements");
            }
        }
    },
    CT() {
      //FIXME: Not sure what CT is
    },
    CN() {
        //FIXME: Not sure what CN is
    },
    CTN() {
        //FIXME: not sure what CTN is
    },
    VT() {
        @Override
        public Optional<Token> apply(@Nullable String[] input) throws ModelLoaderRegistry.LoaderException {
            if (input.length >= 1 && input.length <= 3) {
                return Optional.<Token>of(new CoordTexture(
                        input[0],
                        input.length >= 2 ? input[1] : "0.0",
                        input.length >= 3 ? input[1] : "0.0"));
            } else {
                throw new ModelLoaderRegistry.LoaderException("attempt to create texture coordinate set out of " + input.length + "elements");
            }
        }
    },
    G() {
        @Override
        public Optional<Token> apply(@Nullable String[] input) throws ModelLoaderRegistry.LoaderException {
            return Optional.<Token>of(new GroupToken(Strings.join(input, " ")));
        }
    },
    S() {
        @Override
        public Optional<Token> apply(@Nullable String[] input) throws ModelLoaderRegistry.LoaderException {
            if(input.length == 1) {
                return Optional.<Token>of(new SetSmoothingGroupStateToken(input[0]));
            } else {
                throw new ModelLoaderRegistry.LoaderException("attempt to create texture coordinate set out of " + input.length + " elements");
            }
        }
    },
    O() {
        @Override
        public Optional<Token> apply(@Nullable String[] input) throws ModelLoaderRegistry.LoaderException {
            if (input.length == 1) {
                return Optional.<Token>of(new ObjectToken(input[0]));
            } else {
                throw new ModelLoaderRegistry.LoaderException("attempt to create an object with " + input.length + " parameters");
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

    public Optional<Token> apply(String[] input) throws ModelLoaderRegistry.LoaderException {
        return Optional.absent();
    }

}

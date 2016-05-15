package com.github.atomicblom.client.model.cmf.obj;

import com.google.common.base.Optional;

/**
 * Created by codew on 16/05/2016.
 */
public class CommentToken extends Token
{
    private final String comment;

    public CommentToken(String comment) {

        this.comment = comment;
    }
}

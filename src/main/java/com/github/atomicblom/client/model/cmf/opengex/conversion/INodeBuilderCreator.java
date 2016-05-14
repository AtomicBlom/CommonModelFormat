package com.github.atomicblom.client.model.cmf.opengex.conversion;

import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexNode;

/**
 * Created by codew on 14/05/2016.
 */
public interface INodeBuilderCreator
{
    NodeBuilderContext startNewNodeBuilderContext(Iterable<OgexNode> ogexNode);
}

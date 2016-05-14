package com.github.atomicblom.client.model.cmf.opengex.conversion;

import com.github.atomicblom.client.model.cmf.opengex.ogex.OgexNode;

/**
 * Created by codew on 14/05/2016.
 */
class ChildNodeBuilderContext extends NodeBuilderContext
{
    private final NodeBuilderContext parent;

    ChildNodeBuilderContext(ModelBuilderContext modelContext, NodeBuilderContext parent, Iterable<OgexNode> node)
    {
        super(modelContext, node);
        this.parent = parent;
    }

    @Override
    public void finish()
    {
        if (thisNode.isPresent())
        {
            parent.childNodes.add(thisNode.get());
        }
    }
}

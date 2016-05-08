package com.github.atomicblom.client.model.cmf.common;

import net.minecraftforge.common.property.IUnlistedProperty;

public enum GenericFrameProperty implements IUnlistedProperty<GenericState>
{
    INSTANCE;

    @Override
    public String getName()
    {
        return "GenericFrame";
    }

    @Override
    public boolean isValid(GenericState value)
    {
        return value instanceof GenericState;
    }

    @Override
    public Class<GenericState> getType()
    {
        return GenericState.class;
    }

    @Override
    public String valueToString(GenericState value)
    {
        return value.toString();
    }
}

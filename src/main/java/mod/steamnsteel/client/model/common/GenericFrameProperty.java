package mod.steamnsteel.client.model.common;

import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * Created by codew on 24/03/2016.
 */
public enum GenericFrameProperty implements IUnlistedProperty<GenericState>
{
    INSTANCE;

    @Override
    public String getName()
    {
        return "B3DFrame";
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

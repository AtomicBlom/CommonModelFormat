package com.github.atomicblom.client.model.cmf.obj;

import com.google.common.base.Optional;

/**
 * Created by codew on 16/05/2016.
 */
public class SetSmoothingGroupStateToken extends Token {
    private final int smoothingGroup;
    private final boolean disabled;

    public SetSmoothingGroupStateToken(String smoothingGroup) {
        disabled = "off".equals(smoothingGroup.toLowerCase()) || "0".equals(smoothingGroup);
        if (!disabled) {
            this.smoothingGroup = Integer.parseInt(smoothingGroup);
        } else {
            this.smoothingGroup = 0;
        }
    }
}

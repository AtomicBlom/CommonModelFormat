package com.github.atomicblom.client.model.cmf.common;

/**
 * Created by steblo on 23/03/2016.
 */
public interface IKind<K extends IKind<K>> {
    void setParent(Node<K> parent);

    Node<K> getParent();
}

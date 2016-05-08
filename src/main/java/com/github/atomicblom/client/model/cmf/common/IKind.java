package com.github.atomicblom.client.model.cmf.common;

public interface IKind<K extends IKind<K>> {
    void setParent(Node<K> parent);

    Node<K> getParent();
}

package com.iobeam.api.resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A base type for collections of resource objects.
 */
public abstract class ResourceList<T> implements Serializable {

    private final ArrayList<T> list = new ArrayList<T>();

    public void add(final T r) {
        list.add(r);
    }

    protected List<T> getList() {
        return list;
    }

    public int size() {
        return list.size();
    }

    @Override
    public String toString() {
        return list.toString();
    }
}

package fr.free.nrw.commons.caching;

import fr.free.nrw.commons.caching.QuadTree;

public interface Func {
    public void call(QuadTree quadTree, Node node);
}

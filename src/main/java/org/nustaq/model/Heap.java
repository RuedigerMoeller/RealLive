package org.nustaq.model;

import de.ruedigermoeller.kontraktor.Callback;
import de.ruedigermoeller.kontraktor.Future;

/**
 * Created by ruedi on 01.06.14.
 */
public interface Heap<T> extends AsyncMap<Long,T> {

    public void addIndex(Key<T> k);
    public void getByIndex(Key<T> k, Callback<T> result);
    public Future<T> getFirstByIndex(Key<T> k);

}

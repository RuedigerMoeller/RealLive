package de.ruedigermoeller.reallive.facade.collection;

import de.ruedigermoeller.fastcast.remoting.FCFutureResultHandler;

/**
 * Created with IntelliJ IDEA.
 * User: ruedi
 * Date: 05.11.13
 * Time: 13:55
 * To change this template use File | Settings | File Templates.
 */
public interface RLCollectionMutator<T extends RLRow> {
    public T allocateInstanceForAdd();
    public T getInstanceForUpdate(T current);
    public void add(T t);
    public void remove(long id);
    public void update(T updated);
    public void sync();
}

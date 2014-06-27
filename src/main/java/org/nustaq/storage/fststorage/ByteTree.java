package org.nustaq.storage.fststorage;

import org.nustaq.heapoff.bytez.Bytez;
import org.nustaq.heapoff.bytez.onheap.HeapBytez;

/**
 * Created by ruedi on 27.06.14.
 */
public class ByteTree {

    static int bacount = 0;

    Object arr[] = new Object[256];
    int keyLen = 16;

    public long lookup( Bytez key, long index, Object arr[] ) {
        byte b = key.get(index);
        Object lookup = arr[(b + 256) & 0xff];
        if ( lookup instanceof byte[] ) {
            return lookup(key,index++, (Object[]) lookup);
        }
        return 0;
    }

    public Long put( Bytez key, long index, Object arr[], Long toPut ) {
        byte b = key.get(index);
        int i = ((int)b + 256) & 0xff;
        Object lookup = arr[i];
        if ( index == keyLen - 1 ) {
            arr[i] = toPut;
            return (Long) lookup;
        }
        if ( lookup == null ) {
            arr[i] = new Object[256];
            bacount++;
            return put( key, index+1, (Object[]) arr[i], toPut);
        }
        return put( key, index+1, (Object[]) lookup, toPut);
    }

    public Long get( Bytez key, long index, Object arr[] ) {
        byte b = key.get(index);
        int i = ((int)b + 256) & 0xff;
        Object lookup = arr[i];
        if ( index == keyLen - 1 ) {
            return (Long) lookup;
        }
        if ( lookup == null ) {
            return null;
        }
        return get( key, index+1, (Object[]) lookup);
    }

    public static void main(String a[]) {
        ByteTree bt = new ByteTree();

        long tim = System.currentTimeMillis();
        int MAX = 1000000;
        for ( int i = 0; i < MAX; i++ ) {
            byte b[] = new byte[bt.keyLen];
            byte kb[] = ("test:"+Integer.toHexString(i)).getBytes();
            System.arraycopy(kb,0,b,b.length-kb.length,kb.length);
            HeapBytez heapBytez = new HeapBytez(b);
            Long put = bt.put(heapBytez, 0, bt.arr, (long) i);
            if ( put != null )
                System.out.println("err");
        }
        long dur = System.currentTimeMillis() - tim;
        System.out.println("PUT need "+ dur +" for "+MAX+" recs. "+(MAX/dur)+" per ms ");
        System.out.println("ba count "+bacount);

        tim = System.currentTimeMillis();
        for ( int i = 0; i < MAX; i++ ) {
            byte b[] = new byte[bt.keyLen];
            byte kb[] = ("test:"+Integer.toHexString(i)).getBytes();
            System.arraycopy(kb,0,b,b.length-kb.length,kb.length);
            HeapBytez heapBytez = new HeapBytez(b);
            bt.put(heapBytez, 0, bt.arr, (long) i);
        }
        dur = System.currentTimeMillis() - tim;
        System.out.println("PUT need "+ dur +" for "+MAX+" recs. "+(MAX/dur)+" per ms ");
        System.out.println("ba count "+bacount);

        tim = System.currentTimeMillis();
        for ( int i = 0; i < MAX; i++ ) {
            byte b[] = new byte[bt.keyLen];
            byte kb[] = ("test:"+Integer.toHexString(i)).getBytes();
            System.arraycopy(kb,0,b,b.length-kb.length,kb.length);
            HeapBytez heapBytez = new HeapBytez(b);
            Long put = bt.get(heapBytez, 0, bt.arr);
            if ( put.longValue() != i )
                System.out.println("err");
        }
        dur = System.currentTimeMillis() - tim;
        System.out.println("GET need "+ dur +" for "+MAX+" recs. "+(MAX/dur)+" per ms ");
        for ( int i = 0; i < 5; i++ ) {
            System.gc();
            System.out.println("mem "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024/1024+" MB");
        }

    }

}

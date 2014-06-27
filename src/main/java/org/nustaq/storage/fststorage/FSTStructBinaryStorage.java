package org.nustaq.storage.fststorage;

/**
 * Created by ruedi on 25.06.14.
 */
public class FSTStructBinaryStorage {

    public static void main( String a[] ) {
        FSTStructBinaryStorage store = new FSTStructBinaryStorage();
//        int maxEntries = 5 * 1000 * 1000;
//        store.init(maxEntries);
//        StructMap<StructString,HashValue> map = store.map;
//        long tim = System.currentTimeMillis();
////        int MAX = 5*1000000;
//        int MAX = 10000;
//        StructString key = new StructString(20);
//        HashValue val = new HashValue(0,0);
//        for ( int i = 0; i < MAX; i++ ) {
//            key.setString("test:"+i);
//            val.setValueOffset(i);
//            val.setChunkNum(i);
//            map.put( key, val );
////            System.out.println("k "+key+" val:"+val+" get:"+map.get(key));
//            if ( (i % 1000) == 0) {
//                System.out.println("i "+i);
//            }
//        }
//        long dur = System.currentTimeMillis() - tim;
//        System.out.println("need "+ dur +" for "+MAX+" recs. "+(MAX/dur)+" per ms ");
//
//        tim = System.currentTimeMillis();
//        for ( int i = 0; i < MAX; i++ ) {
//
//            key.setString("test:"+i);
//
//            HashValue tmpVal = map.get(key);
//            if ( tmpVal.getValueOffset() != i || tmpVal.getChunkNum() != i ) {
//                System.out.println( "k: "+key + " v:" + tmpVal.getChunkNum());
//            }
//            if ( (i % 1000) == 0) {
//                System.out.println("i "+i);
//            }
//        }
//
//        dur = System.currentTimeMillis() - tim;
//        System.out.println("need "+ dur +" for "+MAX+" recs. "+(MAX/dur)+" per ms ");
    }

}

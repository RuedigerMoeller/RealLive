package de.ruedigermoeller.reallive.client;

import de.ruedigermoeller.heapoff.structs.FSTStructChange;
import de.ruedigermoeller.reallive.facade.collection.RLChangeTarget;
import de.ruedigermoeller.reallive.facade.collection.RLRow;
import de.ruedigermoeller.reallive.facade.collection.RLRowMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
* Created with IntelliJ IDEA.
* User: moelrue
* Date: 11/11/13
* Time: 1:55 PM
* To change this template use File | Settings | File Templates.
*/

// requires external locking
public class SubscriptionEntry {
    int subsId;
    int tableId;
    RLChangeTarget listener;
    volatile boolean isRecording = true;
    RLRowMatcher matcher;
    ConcurrentHashMap<String,PerSenderSubsentry> perSenderMap =  new ConcurrentHashMap<>();

    class PerSenderSubsentry {
        List<GlobalMessage> history = new ArrayList<GlobalMessage>();
        volatile long currentVersion;

        List<GlobalMessage> getHistory() {
            return history;
        }

        void setHistory(List<GlobalMessage> history) {
            this.history = history;
        }

        long getCurrentVersion() {
            return currentVersion;
        }

        void setCurrentVersion(long currentVersion) {
            this.currentVersion = currentVersion;
        }
    }

    public SubscriptionEntry(int subsId, int tableId, RLChangeTarget listener, RLRowMatcher filter) {
        this.subsId = subsId;
        this.tableId = tableId;
        this.listener = listener;
        this.matcher = filter;
    }

    public boolean isRecording() {
        return isRecording;
    }

    boolean tmpPreUpdMatches = false;
    RLRow   tmpPreUpdMatchesRow = null;
    public void record(String sender,GlobalMessage msg) {
        // filter immediate to reduce size of recorded data
        switch (msg.getType()) {
            case ADD:
            case REM:
                if ( ! matcher.matches(msg.getRow()) ) {
                    return;
                }
                break;
            // TODO:
//                case PREUPD:
//                    tmpPreUpdMatches = matcher.matches(msg.getRow());
//                    break;
//                case UPD:
//                    boolean matches = matcher.matches(msg.getRow());
//                    if ( )
        }
        getPerSenderEntry(sender).getHistory().add(msg);
    }

    private PerSenderSubsentry getPerSenderEntry(String sender) {
        PerSenderSubsentry perSenderSubsentry = perSenderMap.get(sender);
        if ( perSenderSubsentry == null ) {
            perSenderSubsentry = new PerSenderSubsentry();
            perSenderMap.put(sender,perSenderSubsentry);
        }
        return perSenderSubsentry;
    }

    public void replay(String sender, long version) {
        PerSenderSubsentry pss = perSenderMap.get(sender);
        if ( pss == null ) {
            System.out.println("no pss found for "+sender);
            isRecording = false;
            return;
        }
        pss.currentVersion = version;
        List<GlobalMessage> history = pss.history;
        System.out.println("REPLAY "+history.size()+" entries start version "+version);
        for (int i = 0; i < history.size(); i++) {
            GlobalMessage globalMessage = history.get(i);
            long msgVersion = globalMessage.getVersion();
            if ( msgVersion >= version ) {
                switch (globalMessage.getType()) {
                    case ADD:
                        dispatchAdd(sender,msgVersion,globalMessage.getRow());
                        break;
                    case REM:
                        dispatchRem(sender,msgVersion, globalMessage.getRow());
                        break;
                    case PREUPD:
                        dispatchPreUpd(sender,msgVersion, globalMessage.getRow());
                        break;
                    case UPD:
                        dispatchUpd(sender,msgVersion, globalMessage.getRow(), globalMessage.getChange());
                        break;
                }
            } else {
                System.out.println("dropped message with version "+msgVersion);
            }
        }
        isRecording = false;
    }

    public void dispatchAdd(String sender, long version, RLRow row) {
        PerSenderSubsentry pss = getPerSenderEntry(sender);
        if ( version > pss.currentVersion) {
            pss.currentVersion = version;
            if ( matcher.matches(row) )
                listener.added(version, row);
        } else {
            System.out.println("lower version in $add "+version+" curr "+pss.currentVersion);
        }
    }

    public void dispatchRem(String sender, long version, RLRow row) {
        PerSenderSubsentry pss = getPerSenderEntry(sender);
        if ( version > pss.currentVersion) {
            pss.currentVersion = version;
            if ( matcher.matches(row) )
                listener.removed(version, row);
        } else {
            System.out.println("lower version in $remove");
        }
    }

    public void dispatchPreUpd(String sender, long version, RLRow row) {
        PerSenderSubsentry pss = getPerSenderEntry(sender);
        if ( version >= pss.currentVersion) {
            tmpPreUpdMatches = matcher.matches(row);
            tmpPreUpdMatchesRow = row;
        }else {
            System.out.println("lower version in preUpd "+version+" curr "+pss.currentVersion);
        }
    }

    public void dispatchUpd(String sender, long version, RLRow updateRow, FSTStructChange update) {
        PerSenderSubsentry pss = getPerSenderEntry(sender);
//        if ( version > pss.currentVersion)  preUpdate and update are generated single threaded with same version on global listener
        {
            pss.currentVersion = version;
            if ( matcher.matches(updateRow) ) {
                if ( tmpPreUpdMatches ) {
                    listener.preUpdate(version, update, tmpPreUpdMatchesRow);
                    listener.updated(version,update,updateRow);
                } else {
                    listener.added(version, updateRow);
                }
            } else {
                if ( tmpPreUpdMatches ) {
                    listener.removed(version, updateRow);
                }
            }
        }
    }

    //        @Override
//        public int hashCode() {
//            return subsId;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if ( obj instanceof SubscriptionEntry)
//                return ((SubscriptionEntry) obj).subsId == subsId;
//            return super.equals(obj);
//        }
}

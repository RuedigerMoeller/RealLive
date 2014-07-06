package org.nustaq.reallive;

/**
 * Created by ruedi on 05.07.14.
 */
public interface ChangeBroadcastReceiver<T extends Record> {

    public void onChangeReceived(ChangeBroadcast<T> changeBC);

}

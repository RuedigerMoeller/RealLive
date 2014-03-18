package de.ruedigermoeller.reallive.client.wsgate;

import de.ruedigermoeller.heapoff.structs.FSTStruct;
import de.ruedigermoeller.heapoff.structs.FSTStructChange;
import de.ruedigermoeller.reallive.client.SystemSchema;
import de.ruedigermoeller.reallive.client.wsgate.protocol.*;
import de.ruedigermoeller.reallive.facade.collection.RLChangeSource;
import de.ruedigermoeller.reallive.facade.collection.RLRow;
import de.ruedigermoeller.reallive.facade.collection.RLRowMatcher;

import static de.ruedigermoeller.reallive.query.RLQueryHelper.*;
import de.ruedigermoeller.reallive.client.SystemSchema.*;
import de.ruedigermoeller.serialization.dson.Dson;

/**
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 * <p/>
 * Date: 19.12.13
 * Time: 21:20
 * To change this template use File | Settings | File Templates.
 */
class WSGSession {

    WebSocketGate gate;
    boolean authenticated = false;

    WSGSession(WebSocketGate gate) {
        this.gate = gate;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void processRequest( BasicDsonMsg msg ) {
        if ( msg instanceof AuthReq ) {
            AuthReq auth = (AuthReq) msg;
            processAuthRequest(msg, auth);
        } else if ( msg instanceof Request ) {
            String unparsedRequest = ((Request) msg).getUnparsedRequest();
            int responderId = msg.getReqId();
            try {
                BasicDsonMsg parsed = (BasicDsonMsg) Dson.getInstance().readObject(unparsedRequest);
                parsed.setReqId(msg.getReqId());
                parsed.setCtx(msg.getCtx());
                parsed.setRespToId(msg.getRespToId());
                processRequest(parsed);
            } catch (Throwable th) {
                th.printStackTrace();
                msg.reply(new ErrorMsg(0, "failure in parsing:"+th.getMessage(),0,responderId));
            }
        } else if ( msg instanceof QueryReq ) {
            final QueryReq req = (QueryReq) msg;
            if ( req.isSubscribe() ) {
                Subscribe subscribe = new Subscribe( getTable( req.getTable() ), req ) {
                    @Override
                    public void added(long version, RLRow added) {
                        if ( added instanceof FSTStruct ) {
                            Object fieldValues = ((FSTStruct) added).getFieldValues();
                            System.out.println("added "+ fieldValues);
                            req.reply(new AddRowMsg(fieldValues)); // fixme: reuse
                        } else {
                            System.out.println("NON STRUCT added "+added);
                            req.reply(new AddRowMsg( added ));
                        }
                    }

                    @Override
                    public void preUpdate(long version, FSTStructChange change, RLRow oldRow) {
//                        System.out.println("preupd");
//                        super.preUpdate(version, change, oldRow);
                    }

                    @Override
                    public void updated(long l, FSTStructChange change, RLRow updatedRow) {
                        if ( updatedRow instanceof FSTStruct ) {
                            Object fieldValues = ((FSTStruct) updatedRow).getFieldValues();
//                            System.out.println("updated");
                            req.reply(new UpdateRowMsg(fieldValues,change.getChangedFields())); // fixme: reuse
                        } else {
                            System.out.println("NON STRUCT updated "+updatedRow);
                            req.reply(new UpdateRowMsg(updatedRow,change.getChangedFields()));
                        }
                    }

                    @Override
                    public void removed(long version, RLRow removed) {
                        req.reply( new RemRowMsg(removed.getId()) );
                    }

                    @Override
                    public void queryFinished(Object error) {
                        System.out.println("snap fin:");
                        super.queryFinished(error);
                    }
                };
            }
        } else
            System.out.println("unhandled message "+Dson.getInstance().writeObject(msg) );
    }

    protected void processAuthRequest(final BasicDsonMsg msg, AuthReq auth) {
        final String name = auth.getUserName();
        final String pwd = auth.getPassWord();
        Select sel = new Select(getTable(User.class), new RLRowMatcher<User>() {
            public boolean matches(User row) {
                return row.getUid().toString().equals(name) && row.getPassword().toString().equals(pwd);
            }
        })
        {
            boolean hadRes = false;

            @Override public void queryFinished(Object error) {
                if ( !hadRes ) {
                    msg.reply(new ErrorMsg().setErrNo(1).setText("Authentication failed"));
                }
                super.queryFinished(error);
            }

            @Override public void added(long version, RLRow added) {
                hadRes = true;
                authenticated = true;
                msg.reply(new AuthResponse(gate.getMetaData()));
            }
        };
    }

    protected RLChangeSource<User> getTable(Class c) {
        return gate.getClient().getTable(c);
    }

    protected RLChangeSource<User> getTable(String name) {
        return gate.getClient().getTable(name);
    }

}
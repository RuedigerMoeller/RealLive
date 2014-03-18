package de.ruedigermoeller.reallive.client.wsgate.protocol;

import de.ruedigermoeller.reallive.client.wsgate.BasicDsonMsg;

import java.io.Serializable;

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
 * Date: 20.12.13
 * Time: 21:30
 * To change this template use File | Settings | File Templates.
 */
public class ErrorMsg extends BasicDsonMsg implements Serializable {

    int errNo;
    String text;

    public ErrorMsg() {
    }

    public ErrorMsg(int errNo, String text, int reqId, int respondsTo) {
        this.errNo = errNo;
        this.text = text;
        setReqId(reqId);
        setRespToId(respondsTo);
    }

    public int getErrNo() {
        return errNo;
    }
    public ErrorMsg setErrNo(int errNo) {
        this.errNo = errNo; return this;
    }
    public String getText() {
        return text;
    }
    public ErrorMsg setText(String text) {
        this.text = text; return this;
    }
}

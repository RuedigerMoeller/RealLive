package de.ruedigermoeller.reallive.client.wsgate.protocol;

import de.ruedigermoeller.serialization.dson.Dson;
import de.ruedigermoeller.serialization.dson.generators.DartDsonGen;

import java.io.File;

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
 * Date: 22.12.13
 * Time: 16:28
 * To change this template use File | Settings | File Templates.
 */
public class Gen {

    static Class[] classes = { RemRowMsg.class, UpdateRowMsg.class, Scheme.class, TableMetaData.class, TableAttribute.class, AddRowMsg.class, Request.class, Expression.class, And.class, Or.class, Condition.class, AuthReq.class, ErrorMsg.class, QueryReq.class, AuthResponse.class};

    public static void registerClasses() {
        Dson.defaultMapper.map(classes)
           .map("field", Condition.class)
           .map("Query", QueryReq.class)
           .implyAttrFromType("field", "field")

           .implyAttrFromType("Or", "expr")
           .implyAttrFromType("And", "expr")
           .implyAttrFromType("QueryReq", "table")
           .implyAttrFromType("Query", "table")
        ;
    }

    public static void main(String arg[]) throws Exception {
        new DartDsonGen().generate(
            new File("C:\\Users\\ruedi\\Documents\\GitHub\\DartPlay\\TableTest\\web\\protocol\\RealLive.dart"),
            classes
        );
    }

    public static void main0(String arg[]) throws Exception {
        registerClasses();
         final Object read = Dson.getInstance().readObject(new File("c:\\tmp\\test1.dson"));
        System.out.println(Dson.getInstance().writeObject(read));
    }
}

/*
 * Copyright 2011, Allanbank Consulting, Inc. 
 *           All Rights Reserved
 */

package com.allanbank.mongodb.connection.messsage;

import com.allanbank.mongodb.bson.Document;
import com.allanbank.mongodb.bson.builder.BuilderFactory;
import com.allanbank.mongodb.bson.builder.DocumentBuilder;
import com.allanbank.mongodb.connection.Connection;

/**
 * Provides a convenient mechanism for creating a <a
 * href="http://www.mongodb.org/display/DOCS/serverStatus+Command"
 * >serverStatus</a> command.
 * <p>
 * This is a helper class for retrieving the status of a MongoDB server. The
 * results of this command will look like: <blockquote>
 * 
 * <pre>
 * <code>
 * {
 *     "host" : "mongos.example.com",
 *     "version" : "1.8.2",
 *     "process" : "mongos",
 *     "uptime" : 1403,
 *     "localTime" : ISODate("2011-12-06T00:11:56.822Z"),
 *     "mem" : {
 *         "resident" : 5,
 *         "virtual" : 621,
 *         "supported" : true
 *     },
 *     "connections" : {
 *         "current" : 1,
 *         "available" : 818
 *     },
 *     "extra_info" : {
 *         "note" : "fields vary by platform",
 *         "heap_usage_bytes" : 94560,
 *         "page_faults" : 0
 *     },
 *     "opcounters" : {
 *         "insert" : 0,
 *         "query" : 9,
 *         "update" : 0,
 *         "delete" : 0,
 *         "getmore" : 0,
 *         "command" : 33
 *     },
 *     "ops" : {
 *         "sharded" : {
 *             "insert" : 0,
 *             "query" : 0,
 *             "update" : 0,
 *             "delete" : 0,
 *             "getmore" : 0,
 *             "command" : 0
 *         },
 *         "notSharded" : {
 *             "insert" : 0,
 *             "query" : 9,
 *             "update" : 0,
 *             "delete" : 0,
 *             "getmore" : 0,
 *             "command" : 33
 *         }
 *     },
 *     "shardCursorType" : {
 *         
 *     },
 *     "asserts" : {
 *         "regular" : 0,
 *         "warning" : 0,
 *         "msg" : 0,
 *         "user" : 3,
 *         "rollovers" : 0
 *     },
 *     "network" : {
 *         "bytesIn" : 3205,
 *         "bytesOut" : 6698,
 *         "numRequests" : 45
 *     },
 *     "ok" : 1
 * }
 * </code>
 * </pre>
 * 
 * </blockquote>
 * </p>
 * 
 * @copyright 2011, Allanbank Consulting, Inc., All Rights Reserved
 */
public class ServerStatus extends Query {

    /** The administration database name. */
    private static final String ADMIN_DATABASE = "admin";

    /** The serverStatus "query" document. */
    private static final Document SERVER_STATUS;

    static {
        final DocumentBuilder builder = BuilderFactory.start();
        builder.addInteger("serverStatus", 1);
        SERVER_STATUS = builder.get();
    }

    /**
     * Create a new ServerStatus command.
     */
    public ServerStatus() {
        super(ADMIN_DATABASE, Connection.COMMAND_COLLECTION, SERVER_STATUS,
                null, 1, 0, false, false, false, false, false, false);
    }
}

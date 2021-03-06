/*
 * #%L
 * ClusterType.java - mongodb-async-driver - Allanbank Consulting, Inc.
 * %%
 * Copyright (C) 2011 - 2014 Allanbank Consulting, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.allanbank.mongodb.client;

/**
 * ClusterType provides an enumeration of the types of cluster configurations
 * supported.
 *
 * @api.no This class is <b>NOT</b> part of the drivers API. This class may be
 *         mutated in incompatible ways between any two releases of the driver.
 * @copyright 2012-2013, Allanbank Consulting, Inc., All Rights Reserved
 */
public enum ClusterType {

    /** The replica set type cluster. */
    REPLICA_SET,

    /** The sharded type cluster using 'mongos' servers. */
    SHARDED,

    /** A single 'mongod' server. */
    STAND_ALONE;

    /**
     * Returns true if the cluster is sharded, false otherwise.
     * 
     * @return True if the cluster is sharded.
     */
    public boolean isSharded() {
        return this == SHARDED;
    }
}

/*
 * Copyright 2014, Allanbank Consulting, Inc.
 *           All Rights Reserved
 */

package com.allanbank.mongodb.client;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * NamedReference provides a Reference that tracks a name. We use a weak
 * reference as the base class as we want the garbage collector to reclaim the
 * instance as soon as possible.
 *
 * @param <T>
 *            The type of the referent.
 *
 * @api.no This class is <b>NOT</b> part of the drivers API. This class may be
 *         mutated in incompatible ways between any two releases of the driver.
 * @copyright 2014, Allanbank Consulting, Inc., All Rights Reserved
 */
/* package */class NamedReference<T> extends WeakReference<T> {

    /** The name for the referent. */
    private final String myName;

    /**
     * Creates a new NamedReference.
     *
     * @param name
     *            The name for the referent.
     * @param referent
     *            The named referent.
     * @param q
     *            The queue to add the reference to when reclaimed.
     */
    protected NamedReference(final String name, final T referent,
            final ReferenceQueue<? super T> q) {
        super(referent, q);
        myName = name;
    }

    /**
     * Returns the name for the referent.
     *
     * @return The name for the referent.
     */
    public String getName() {
        return myName;
    }
}
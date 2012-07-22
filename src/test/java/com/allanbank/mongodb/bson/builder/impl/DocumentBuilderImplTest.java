/*
 * Copyright 2012, Allanbank Consulting, Inc. 
 *           All Rights Reserved
 */

package com.allanbank.mongodb.bson.builder.impl;

import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * DocumentBuilderImplTest provides tests for a {@link DocumentBuilderImpl}.
 * 
 * @copyright 2012, Allanbank Consulting, Inc., All Rights Reserved
 */
public class DocumentBuilderImplTest {

    /**
     * Test method for {@link DocumentBuilderImpl#build()} .
     */
    @Test
    public void testGetWithoutUniqueNames() {
        final DocumentBuilderImpl builder = new DocumentBuilderImpl();
        builder.addBoolean("bool", true);
        builder.addBoolean("bool", true);

        try {
            builder.build();
            fail("Should not be able to create a document without unique names.");
        }
        catch (final AssertionError error) {
            // good.
        }
    }

}

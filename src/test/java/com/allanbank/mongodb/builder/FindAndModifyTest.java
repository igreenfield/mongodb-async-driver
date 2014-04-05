/*
 * Copyright 2012-2013, Allanbank Consulting, Inc.
 *           All Rights Reserved
 */

package com.allanbank.mongodb.builder;

import static com.allanbank.mongodb.builder.Sort.asc;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.allanbank.mongodb.bson.Document;
import com.allanbank.mongodb.bson.builder.BuilderFactory;

/**
 * FindAndModifyTest provides tests for the {@link FindAndModify} command.
 *
 * @copyright 2012-2013, Allanbank Consulting, Inc., All Rights Reserved
 */
public class FindAndModifyTest {

    /**
     * Test method for {@link FindAndModify#FindAndModify}.
     */
    @Test
    public void testFindAndModify() {
        final Document query = Find.ALL;
        final Document update = BuilderFactory.start().addInteger("foo", 3)
                .build();
        final Document sort = BuilderFactory.start().addInteger("foo", 3)
                .build();
        final Document fields = BuilderFactory.start().addBoolean("foo", true)
                .build();

        final FindAndModify.Builder builder = new FindAndModify.Builder();
        builder.query(query);
        builder.update(update);
        builder.fields(fields);
        builder.remove(true);
        builder.returnNew(true);
        builder.sort(sort);
        builder.upsert(true);

        final FindAndModify request = builder.build();
        assertSame(query, request.getQuery());
        assertSame(update, request.getUpdate());
        assertSame(sort, request.getSort());
        assertSame(fields, request.getFields());
        assertTrue(request.isRemove());
        assertTrue(request.isReturnNew());
        assertTrue(request.isUpsert());
    }

    /**
     * Test method for {@link FindAndModify#FindAndModify}.
     */
    @Test
    public void testFindAndModifyFluent() {
        final Document query = Find.ALL;
        final Document update = BuilderFactory.start().addInteger("foo", 3)
                .build();
        final Document sort = BuilderFactory.start().addInteger("foo", 3)
                .build();
        final Document fields = BuilderFactory.start().addBoolean("foo", true)
                .build();

        final FindAndModify.Builder builder = new FindAndModify.Builder();
        builder.query(query).update(update).fields(fields).remove().returnNew()
        .sort(sort).upsert();

        FindAndModify request = builder.build();
        assertSame(query, request.getQuery());
        assertSame(update, request.getUpdate());
        assertSame(sort, request.getSort());
        assertSame(fields, request.getFields());
        assertTrue(request.isRemove());
        assertTrue(request.isReturnNew());
        assertTrue(request.isUpsert());

        boolean built = false;
        try {
            builder.reset().build();
            built = true;
        }
        catch (final IllegalArgumentException expected) {
            // Good.
        }
        assertFalse(
                "Should have failed to create a FindAndModify command without a query.",
                built);
        builder.setQuery(query);
        builder.setRemove(true);

        request = builder.build();
        assertSame(query, request.getQuery());
        assertNull(request.getUpdate());
        assertNull(request.getSort());
        assertTrue(request.isRemove());
        assertFalse(request.isReturnNew());
        assertFalse(request.isUpsert());
    }

    /**
     * Test method for {@link FindAndModify#FindAndModify}.
     */
    @Test
    public void testFindAndModifyMinimal() {
        final Document query = Find.ALL;
        final Document update = BuilderFactory.start().addInteger("foo", 3)
                .build();

        FindAndModify.Builder builder = new FindAndModify.Builder();
        builder.setQuery(query);
        builder.setUpdate(update);

        FindAndModify request = builder.build();
        assertSame(query, request.getQuery());
        assertSame(update, request.getUpdate());
        assertNull(request.getSort());
        assertFalse(request.isRemove());
        assertFalse(request.isReturnNew());
        assertFalse(request.isUpsert());

        builder = new FindAndModify.Builder();
        builder.setQuery(query);
        builder.setRemove(true);

        request = builder.build();
        assertSame(query, request.getQuery());
        assertNull(request.getUpdate());
        assertNull(request.getSort());
        assertTrue(request.isRemove());
        assertFalse(request.isReturnNew());
        assertFalse(request.isUpsert());
    }

    /**
     * Test method for {@link FindAndModify#FindAndModify}.
     */
    @Test
    public void testFindAndModifyNoQuery() {
        final Document update = BuilderFactory.start().addInteger("foo", 3)
                .build();

        final FindAndModify.Builder builder = FindAndModify.builder();

        builder.setUpdate(update);

        boolean built = false;
        try {
            builder.build();
            built = true;
        }
        catch (final IllegalArgumentException expected) {
            // Good.
        }
        assertFalse(
                "Should have failed to create a FindAndModify command without a query.",
                built);
    }

    /**
     * Test method for {@link FindAndModify#FindAndModify}.
     */
    @Test
    public void testFindAndModifyNoUpdate() {
        final Document query = Find.ALL;

        final FindAndModify.Builder builder = new FindAndModify.Builder();
        builder.setQuery(query);

        boolean built = false;
        try {
            builder.build();
            built = true;
        }
        catch (final IllegalArgumentException expected) {
            // Good.
        }
        assertFalse(
                "Should have failed to create a FindAndModify command without an update.",
                built);
    }

    /**
     * Test method for {@link FindAndModify#FindAndModify}.
     */
    @Test
    public void testFindAndModifyNoUpdateIsRemove() {
        final Document query = Find.ALL;

        final FindAndModify.Builder builder = new FindAndModify.Builder();
        builder.query(query).remove();

        try {
            builder.build();
        }
        catch (final IllegalArgumentException expected) {
            fail("Should be OK to not have an update with a remove");
        }
    }

    /**
     * Test method for {@link FindAndModify#FindAndModify}.
     */
    @Test
    public void testFindAndModifyWithSort() {
        final Document query = Find.ALL;
        final Document update = BuilderFactory.start().addInteger("foo", 3)
                .build();
        final Document sort = BuilderFactory.start().addInteger("foo", 1)
                .build();
        final Document fields = BuilderFactory.start().addBoolean("foo", true)
                .build();

        final FindAndModify.Builder builder = new FindAndModify.Builder();
        builder.setQuery(query);
        builder.setUpdate(update);
        builder.setFields(fields);
        builder.setRemove(true);
        builder.setReturnNew(true);
        builder.sort(asc("foo"));
        builder.setUpsert(true);

        final FindAndModify request = builder.build();
        assertSame(query, request.getQuery());
        assertSame(update, request.getUpdate());
        assertEquals(sort, request.getSort());
        assertSame(fields, request.getFields());
        assertTrue(request.isRemove());
        assertTrue(request.isReturnNew());
        assertTrue(request.isUpsert());
    }

    /**
     * Test method for
     * {@link FindAndModify.Builder#setMaximumTimeMilliseconds(long)} .
     */
    @Test
    public void testMaximumTimeMillisecondsDefault() {
        final Document query = Find.ALL;
        final Document update = BuilderFactory.start().addInteger("foo", 3)
                .build();

        final FindAndModify.Builder b = FindAndModify.builder();
        b.setQuery(query);
        b.setUpdate(update);

        final FindAndModify command = b.build();

        assertThat(command.getMaximumTimeMilliseconds(), is(0L));
    }

    /**
     * Test method for
     * {@link FindAndModify.Builder#setMaximumTimeMilliseconds(long)} .
     */
    @Test
    public void testMaximumTimeMillisecondsViaFluent() {
        final Random random = new Random(System.currentTimeMillis());
        final Document query = Find.ALL;
        final Document update = BuilderFactory.start().addInteger("foo", 3)
                .build();

        final FindAndModify.Builder b = FindAndModify.builder();
        b.setQuery(query);
        b.setUpdate(update);

        final long value = random.nextLong();
        b.maximumTime(value, TimeUnit.MILLISECONDS);

        final FindAndModify command = b.build();

        assertThat(command.getMaximumTimeMilliseconds(), is(value));
    }

    /**
     * Test method for
     * {@link FindAndModify.Builder#setMaximumTimeMilliseconds(long)} .
     */
    @Test
    public void testMaximumTimeMillisecondsViaSetter() {
        final Random random = new Random(System.currentTimeMillis());
        final Document query = Find.ALL;
        final Document update = BuilderFactory.start().addInteger("foo", 3)
                .build();

        final FindAndModify.Builder b = FindAndModify.builder();
        b.setQuery(query);
        b.setUpdate(update);

        final long value = random.nextLong();
        b.setMaximumTimeMilliseconds(value);

        final FindAndModify command = b.build();

        assertThat(command.getMaximumTimeMilliseconds(), is(value));
    }
}

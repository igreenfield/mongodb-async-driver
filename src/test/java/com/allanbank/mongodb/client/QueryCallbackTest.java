/*
 * Copyright 2012-2013, Allanbank Consulting, Inc. 
 *           All Rights Reserved
 */

package com.allanbank.mongodb.client;

import static com.allanbank.mongodb.client.connection.CallbackReply.reply;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import com.allanbank.mongodb.Callback;
import com.allanbank.mongodb.MongoDbException;
import com.allanbank.mongodb.MongoIterator;
import com.allanbank.mongodb.ReadPreference;
import com.allanbank.mongodb.bson.Document;
import com.allanbank.mongodb.bson.builder.BuilderFactory;
import com.allanbank.mongodb.client.connection.message.Query;
import com.allanbank.mongodb.client.connection.message.Reply;
import com.allanbank.mongodb.error.CursorNotFoundException;
import com.allanbank.mongodb.error.QueryFailedException;
import com.allanbank.mongodb.error.ReplyException;
import com.allanbank.mongodb.error.ShardConfigStaleException;

/**
 * QueryCallbackTest provides tests for the {@link QueryCallback} class.
 * 
 * @copyright 2012-2013, Allanbank Consulting, Inc., All Rights Reserved
 */
public class QueryCallbackTest {

    /**
     * Test method for {@link QueryCallback#asError(Reply, int, int, String)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAsErrorReplyIntIntString() {
        final Query q = new Query("db", "c", BuilderFactory.start().build(),
                null, 0, 0, 0, false, ReadPreference.PRIMARY, false, false,
                false, false);
        final Reply reply = reply();

        final Callback<MongoIterator<Document>> mockCallback = createMock(Callback.class);

        replay(mockCallback);

        final QueryCallback callback = new QueryCallback(null, q, mockCallback);

        final MongoDbException e = callback.asError(reply, 0, 1234,
                "Now this is broken.");

        assertThat(e, instanceOf(ReplyException.class));

        final ReplyException re = (ReplyException) e;
        assertEquals(1234, re.getErrorNumber());
        assertEquals("Now this is broken.", re.getMessage());
        assertEquals(0, re.getOkValue());
        assertSame(reply, re.getReply());
        assertSame(q, re.getRequest());

        verify(mockCallback);
    }

    /**
     * Test method for {@link QueryCallback#convert(Reply)} .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertReply() {
        final Query q = new Query("db", "c", BuilderFactory.start().build(),
                null, 0, 0, 0, false, ReadPreference.PRIMARY, false, false,
                false, false);
        final Reply reply = reply();

        final Callback<MongoIterator<Document>> mockCallback = createMock(Callback.class);

        replay(mockCallback);

        final QueryCallback callback = new QueryCallback(null, q, mockCallback);
        callback.setAddress("server");
        final MongoIteratorImpl mIter = (MongoIteratorImpl) callback
                .convert(reply);
        assertEquals("server", mIter.getReadPerference().getServer());

        verify(mockCallback);
    }

    /**
     * Test method for {@link QueryCallback#getAddress()} and
     * {@link QueryCallback#setAddress}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetAddress() {
        final Query q = new Query("db", "c", BuilderFactory.start().build(),
                null, 0, 0, 0, false, ReadPreference.PRIMARY, false, false,
                false, false);
        final Reply reply = reply();

        final Callback<MongoIterator<Document>> mockCallback = createMock(Callback.class);

        replay(mockCallback);

        QueryCallback callback = new QueryCallback(null, q, mockCallback);
        assertNull(callback.getAddress());
        callback.setAddress("server");
        assertEquals("server", callback.getAddress());

        verify(mockCallback);

        reset(mockCallback);

        replay(mockCallback);

        callback = new QueryCallback(null, q, mockCallback);
        callback.callback(reply);
        verify(mockCallback);

        reset(mockCallback);

        final Capture<MongoIterator<Document>> capture2 = new Capture<MongoIterator<Document>>();
        mockCallback.callback(capture(capture2));
        EasyMock.expectLastCall();

        replay(mockCallback);

        assertNull(callback.getAddress());
        callback.setAddress("server");
        assertEquals("server", callback.getAddress());

        final MongoIterator<Document> iter = capture2.getValue();
        assertThat(iter, instanceOf(MongoIteratorImpl.class));
        final MongoIteratorImpl mIter = (MongoIteratorImpl) iter;
        assertEquals("server", mIter.getReadPerference().getServer());

        verify(mockCallback);
    }

    /**
     * Test method for {@link QueryCallback#verify(Reply)} .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testVerifyOnCursorNotFound() {
        final List<Document> docs = Collections.emptyList();
        final Query q = new Query("db", "c", BuilderFactory.start().build(),
                null, 0, 0, 0, false, ReadPreference.PRIMARY, false, false,
                false, false);
        final Reply reply = new Reply(0, 0, 0, docs, false, true, false, false);

        final Callback<MongoIterator<Document>> mockCallback = createMock(Callback.class);
        final Capture<Throwable> capture = new Capture<Throwable>();

        mockCallback.exception(capture(capture));

        replay(mockCallback);

        final QueryCallback callback = new QueryCallback(null, q, mockCallback);
        callback.callback(reply);

        verify(mockCallback);

        final Throwable thrown = capture.getValue();
        assertThat(thrown, instanceOf(CursorNotFoundException.class));
    }

    /**
     * Test method for {@link QueryCallback#verify(Reply)} .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testVerifyOnQueryFailed() {
        final List<Document> docs = Collections.emptyList();
        final Query q = new Query("db", "c", BuilderFactory.start().build(),
                null, 0, 0, 0, false, ReadPreference.PRIMARY, false, false,
                false, false);
        final Reply reply = new Reply(0, 0, 0, docs, false, false, true, false);

        final Callback<MongoIterator<Document>> mockCallback = createMock(Callback.class);
        final Capture<Throwable> capture = new Capture<Throwable>();

        mockCallback.exception(capture(capture));

        replay(mockCallback);

        final QueryCallback callback = new QueryCallback(null, q, mockCallback);
        callback.callback(reply);

        verify(mockCallback);

        final Throwable thrown = capture.getValue();
        assertThat(thrown, instanceOf(QueryFailedException.class));
    }

    /**
     * Test method for {@link QueryCallback#verify(Reply)} .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testVerifyOnShardConfigStale() {
        final List<Document> docs = Collections.emptyList();
        final Query q = new Query("db", "c", BuilderFactory.start().build(),
                null, 0, 0, 0, false, ReadPreference.PRIMARY, false, false,
                false, false);
        final Reply reply = new Reply(0, 0, 0, docs, false, false, false, true);

        final Callback<MongoIterator<Document>> mockCallback = createMock(Callback.class);
        final Capture<Throwable> capture = new Capture<Throwable>();

        mockCallback.exception(capture(capture));

        replay(mockCallback);

        final QueryCallback callback = new QueryCallback(null, q, mockCallback);
        callback.callback(reply);

        verify(mockCallback);

        final Throwable thrown = capture.getValue();
        assertThat(thrown, instanceOf(ShardConfigStaleException.class));
    }
}

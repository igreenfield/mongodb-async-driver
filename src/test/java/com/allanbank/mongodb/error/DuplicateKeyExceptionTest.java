/*
 * #%L
 * DuplicateKeyExceptionTest.java - mongodb-async-driver - Allanbank Consulting, Inc.
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

package com.allanbank.mongodb.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.allanbank.mongodb.bson.Document;
import com.allanbank.mongodb.client.message.Reply;

/**
 * DuplicateKeyExceptionTest provides tests for the
 * {@link DuplicateKeyException}.
 *
 * @copyright 2012-2013, Allanbank Consulting, Inc., All Rights Reserved
 */
public class DuplicateKeyExceptionTest {

    /**
     * Test method for
     * {@link DuplicateKeyException#DuplicateKeyException(int, int, String, Reply)}
     * .
     */
    @Test
    public void testDuplicateKeyException() {
        final List<Document> docs = Collections.emptyList();

        final Reply r = new Reply(1, 1, 1, docs, false, false, false, false);

        final DuplicateKeyException re = new DuplicateKeyException(1234, 12345,
                "foo", r);
        assertSame(r, re.getReply());
        assertEquals("foo", re.getMessage());
        assertNull(re.getCause());
        assertEquals(12345, re.getErrorNumber());
        assertEquals(1234, re.getOkValue());
        assertNull(re.getRequest());
    }
}

/*
 * Copyright 2012, Allanbank Consulting, Inc. 
 *           All Rights Reserved
 */

package com.allanbank.mongodb.bson.element;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.allanbank.mongodb.bson.Element;
import com.allanbank.mongodb.bson.ElementType;
import com.allanbank.mongodb.bson.Visitor;

/**
 * LongElementTest provides tests for the {@link LongElement} class.
 * 
 * @copyright 2012, Allanbank Consulting, Inc., All Rights Reserved
 */
public class LongElementTest {

    /**
     * Test method for
     * {@link LongElement#accept(com.allanbank.mongodb.bson.Visitor)} .
     */
    @Test
    public void testAccept() {
        final LongElement element = new LongElement("foo", 1010101);

        final Visitor mockVisitor = createMock(Visitor.class);

        mockVisitor.visitLong(eq("foo"), eq(1010101L));
        expectLastCall();

        replay(mockVisitor);

        element.accept(mockVisitor);

        verify(mockVisitor);
    }

    /**
     * Test method for {@link LongElement#equals(java.lang.Object)} .
     */
    @Test
    public void testEqualsObject() {
        final Random random = new Random(System.currentTimeMillis());

        final List<Element> objs1 = new ArrayList<Element>();
        final List<Element> objs2 = new ArrayList<Element>();

        for (final String name : Arrays.asList("1", "foo", "bar", "baz", "2",
                null)) {
            for (int i = 0; i < 10; ++i) {
                final long value = random.nextLong();
                objs1.add(new LongElement(name, value));
                objs2.add(new LongElement(name, value));
            }
        }

        // Sanity check.
        assertEquals(objs1.size(), objs2.size());

        for (int i = 0; i < objs1.size(); ++i) {
            final Element obj1 = objs1.get(i);
            Element obj2 = objs2.get(i);

            assertTrue(obj1.equals(obj1));
            assertNotSame(obj1, obj2);
            assertEquals(obj1, obj2);

            assertEquals(obj1.hashCode(), obj2.hashCode());

            for (int j = i + 1; j < objs1.size(); ++j) {
                obj2 = objs2.get(j);

                assertFalse(obj1.equals(obj2));
                assertFalse(obj1.hashCode() == obj2.hashCode());
            }

            assertFalse(obj1.equals("foo"));
            assertFalse(obj1.equals(null));
            assertFalse(obj1.equals(new MaxKeyElement(obj1.getName())));
        }
    }

    /**
     * Test method for {@link LongElement#getDoubleValue()}.
     */
    @Test
    public void testGetDoubleValue() {
        final LongElement element = new LongElement("foo", 1010101);

        assertEquals(1010101.0, element.getDoubleValue(), 0.001);
    }

    /**
     * Test method for {@link LongElement#getIntValue()}.
     */
    @Test
    public void testGetIntValue() {
        final LongElement element = new LongElement("foo", 1010101);

        assertEquals(1010101, element.getIntValue());
    }

    /**
     * Test method for {@link LongElement#getLongValue()} .
     */
    @Test
    public void testGetLongValue() {
        final LongElement element = new LongElement("foo", 1010101);

        assertEquals(1010101, element.getLongValue(), 0.0001);
    }

    /**
     * Test method for {@link LongElement#getValue()}.
     */
    @Test
    public void testGetValue() {
        final LongElement element = new LongElement("foo", 1010101);

        assertEquals(1010101L, element.getValue());
    }

    /**
     * Test method for {@link LongElement#LongElement(java.lang.String, Long)} .
     */
    @Test
    public void testLongElement() {
        final LongElement element = new LongElement("foo", 1010101);

        assertEquals("foo", element.getName());
        assertEquals(1010101, element.getValue(), 0.0001);
        assertEquals(ElementType.LONG, element.getType());
    }

    /**
     * Test method for {@link LongElement#toString()}.
     */
    @Test
    public void testToString() {
        final LongElement element = new LongElement("foo", 1010101);

        assertEquals("\"foo\" : 1010101", element.toString());
    }
}

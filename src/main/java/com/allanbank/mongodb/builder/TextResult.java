/*
 * Copyright 2013, Allanbank Consulting, Inc. 
 *           All Rights Reserved
 */

package com.allanbank.mongodb.builder;

import com.allanbank.mongodb.bson.Document;
import com.allanbank.mongodb.bson.DocumentAssignable;
import com.allanbank.mongodb.bson.NumericElement;
import com.allanbank.mongodb.bson.element.DocumentElement;

/**
 * TextResult provides a wrapper for a single result of a {@link Text text}
 * command.
 * <p>
 * The result of a {@code text} command is a document that looks like the
 * following:<blockquote>
 * 
 * <pre>
 * <code>
 * > db.collection.runCommand( { "text": "collection" , search: "coffee magic" } )
 * {
 *     "queryDebugString" : "coffe|magic||||||",
 *     "language" : "english",
 *     "results" : [
 *         {
 *             "score" : 2.25,
 *             "obj" : {
 *                 "_id" : ObjectId("51376ab8602c316554cfe248"),
 *                 "content" : "Coffee is full of magical powers."
 *             }
 *         },
 *         {
 *             "score" : 0.625,
 *             "obj" : {
 *                 "_id" : ObjectId("51376a80602c316554cfe246"),
 *                 "content" : "Now is the time to drink all of the coffee."
 *             }
 *         }
 *     ],
 *     "stats" : {
 *         "nscanned" : 3,
 *         "nscannedObjects" : 0,
 *         "n" : 2,
 *         "nfound" : 2,
 *         "timeMicros" : 97
 *     },
 *     "ok" : 1
 * }
 * </code>
 * </pre>
 * 
 * </blockquote>
 * </p>
 * <p>
 * This class wraps a single entry from the {@code results} array.
 * </p>
 * 
 * @api.no <b>This class is NOT part of the Public API.</b> This class may be
 *         mutated in incompatible ways between any two releases of the driver.
 *         This class <b>WILL</b>, eventually, be part of the driver's API.
 *         Until 10gen finalizes the text query interface we are keeping this
 *         class out of the Public API so we can track any changes more closely.
 * @copyright 2013, Allanbank Consulting, Inc., All Rights Reserved
 * @see <a
 *      href="http://docs.mongodb.org/manual/release-notes/2.4/#text-queries">
 *      MongoDB Text Queries</a>
 * @since MongoDB 2.4
 */
public class TextResult {
    /** The document. */
    private final Document myDocument;

    /** The score for the document. */
    private final Document myRawDocument;

    /** The score for the document. */
    private final double myScore;

    /**
     * Creates a new Text.
     * 
     * @param document
     *            The document containing the 'score' and 'obj' fields.
     * @throws AssertionError
     *             On the search term not being set.
     */
    public TextResult(final DocumentAssignable document) {
        myRawDocument = document.asDocument();

        final NumericElement score = myRawDocument.get(NumericElement.class,
                "score");
        if (score != null) {
            myScore = score.getDoubleValue();
        }
        else {
            myScore = -1;
        }

        final DocumentElement obj = myRawDocument.get(DocumentElement.class,
                "obj");
        if (obj != null) {
            myDocument = obj.getDocument();
        }
        else {
            myDocument = null;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to compare equal to an equivalent text result.
     * </p>
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = false;
        if (this == object) {
            result = true;
        }
        else if ((object != null) && (getClass() == object.getClass())) {
            final TextResult other = (TextResult) object;

            result = myRawDocument.equals(other.myRawDocument);
        }
        return result;
    }

    /**
     * Returns the document.
     * 
     * @return The document.
     */
    public Document getDocument() {
        return myDocument;
    }

    /**
     * Returns the un-processed result document. It is expected to be a document
     * containing two fields: 'score' and 'obj'.
     * 
     * @return The un-processed result document.
     */
    public Document getRawDocument() {
        return myRawDocument;
    }

    /**
     * Returns the score for the document.
     * 
     * @return The score for the document.
     */
    public double getScore() {
        return myScore;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to return the hashCode of the raw document.
     * </p>
     */
    @Override
    public int hashCode() {
        return myRawDocument.hashCode();
    }
}
/*
 * Copyright 2011-2013, Allanbank Consulting, Inc. 
 *           All Rights Reserved
 */

package com.allanbank.mongodb.client.connection.rs;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.allanbank.mongodb.Callback;
import com.allanbank.mongodb.MongoClientConfiguration;
import com.allanbank.mongodb.MongoDbException;
import com.allanbank.mongodb.ReadPreference;
import com.allanbank.mongodb.client.Message;
import com.allanbank.mongodb.client.connection.Connection;
import com.allanbank.mongodb.client.connection.ReconnectStrategy;
import com.allanbank.mongodb.client.connection.proxy.ProxiedConnectionFactory;
import com.allanbank.mongodb.client.message.Reply;
import com.allanbank.mongodb.client.state.Cluster;
import com.allanbank.mongodb.client.state.Server;
import com.allanbank.mongodb.error.ConnectionLostException;
import com.allanbank.mongodb.util.IOUtils;

/**
 * Provides a {@link Connection} implementation for connecting to a replica-set
 * environment.
 * 
 * @api.no This class is <b>NOT</b> part of the drivers API. This class may be
 *         mutated in incompatible ways between any two releases of the driver.
 * @copyright 2011-2013, Allanbank Consulting, Inc., All Rights Reserved
 */
public class ReplicaSetConnection implements Connection {

    /** The logger for the {@link ReplicaSetConnection}. */
    protected static final Logger LOG = Logger
            .getLogger(ReplicaSetConnection.class.getCanonicalName());

    /** The MongoDB client configuration. */
    protected final MongoClientConfiguration myConfig;

    /** Support for emitting property change events. */
    protected final PropertyChangeSupport myEventSupport;

    /** Set to false when the connection is closed. */
    protected final AtomicBoolean myOpen;

    /** Set to true when the connection should be gracefully closed. */
    protected final AtomicBoolean myShutdown;

    /** The servers this connection is connected to. */
    /* package */final ConcurrentMap<Server, Connection> myServers;

    /** The state of the cluster for finding secondary connections. */
    private final Cluster myCluster;

    /** The connection factory for opening secondary connections. */
    private final ProxiedConnectionFactory myFactory;

    /** The most recently used connection. */
    private final AtomicReference<Connection> myLastUsedConnection;

    /** The listener for changes in the cluster and connections. */
    private final PropertyChangeListener myListener;

    /** The primary server this connection is connected to. */
    private volatile Server myPrimaryServer;

    /** The strategy for reconnecting/finding the primary. */
    private volatile ReplicaSetReconnectStrategy myReconnectStrategy;

    /**
     * Creates a new {@link ReplicaSetConnection}.
     * 
     * @param proxiedConnection
     *            The connection being proxied.
     * @param server
     *            The primary server this connection is connected to.
     * @param cluster
     *            The state of the cluster for finding secondary connections.
     * @param factory
     *            The connection factory for opening secondary connections.
     * @param config
     *            The MongoDB client configuration.
     * @param strategy
     *            The strategy for reconnecting/finding the primary.
     */
    public ReplicaSetConnection(final Connection proxiedConnection,
            final Server server, final Cluster cluster,
            final ProxiedConnectionFactory factory,
            final MongoClientConfiguration config,
            final ReplicaSetReconnectStrategy strategy) {
        myPrimaryServer = server;
        myCluster = cluster;
        myFactory = factory;
        myConfig = config;
        myReconnectStrategy = strategy;

        myOpen = new AtomicBoolean(true);
        myShutdown = new AtomicBoolean(false);
        myEventSupport = new PropertyChangeSupport(this);
        myServers = new ConcurrentHashMap<Server, Connection>();
        myLastUsedConnection = new AtomicReference<Connection>(
                proxiedConnection);

        myListener = new ClusterAndConnectionListener();
        myCluster.addListener(myListener);

        if (proxiedConnection != null) {
            cacheConnection(server, proxiedConnection);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to add this listener to this connection's event source.
     * </p>
     */
    @Override
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        myEventSupport.addPropertyChangeListener(listener);
    }

    /**
     * Closes the underlying connection.
     * 
     * @see Connection#close()
     */
    @Override
    public void close() throws IOException {

        myOpen.set(false);
        myCluster.removeListener(myListener);

        for (final Connection conn : myServers.values()) {
            try {
                conn.removePropertyChangeListener(myListener);
                conn.close();
            }
            catch (final IOException ioe) {
                LOG.log(Level.WARNING, "Could not close the connection: "
                        + conn, ioe);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Forwards the call to the proxied {@link Connection}.
     * </p>
     * 
     * @see java.io.Flushable#flush()
     */
    @Override
    public void flush() throws IOException {
        for (final Connection conn : myServers.values()) {
            try {
                conn.flush();
            }
            catch (final IOException ioe) {
                LOG.log(Level.WARNING, "Could not flush the connection: "
                        + conn, ioe);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to return the pending count for the last connection used to
     * send a message.
     * </p>
     */
    @Override
    public int getPendingCount() {
        return myLastUsedConnection.get().getPendingCount();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to return the name of the primary server.
     * </p>
     */
    @Override
    public String getServerName() {
        if (myPrimaryServer != null) {
            return myPrimaryServer.getCanonicalName();
        }
        return "UNKNOWN";
    }

    /**
     * {@inheritDoc}
     * <p>
     * True if the connection is open and not shutting down.
     * </p>
     */
    @Override
    public boolean isAvailable() {
        return isOpen() && !isShuttingDown();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to return if the last used connection is idle.
     * </p>
     */
    @Override
    public boolean isIdle() {
        return myLastUsedConnection.get().isIdle();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to return if this connection has any open connections.
     * </p>
     */
    @Override
    public boolean isOpen() {
        return myOpen.get();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to return if the last used connection is shutting down.
     * </p>
     */
    @Override
    public boolean isShuttingDown() {
        return myShutdown.get();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to raise the errors with all of the underlying connections.
     * </p>
     */
    @Override
    public void raiseErrors(final MongoDbException exception) {
        for (final Connection conn : myServers.values()) {
            conn.raiseErrors(exception);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to remove the listener from this connection.
     * </p>
     */
    @Override
    public void removePropertyChangeListener(
            final PropertyChangeListener listener) {
        myEventSupport.removePropertyChangeListener(listener);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Locates all of the potential servers that can receive all of the
     * messages. Tries to then send the messages to a server with a connection
     * already open or failing that tries to open a connection to open of the
     * servers.
     * </p>
     */
    @Override
    public String send(final Message message,
            final Callback<Reply> replyCallback) throws MongoDbException {
        return send(message, null, replyCallback);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Locates all of the potential servers that can receive all of the
     * messages. Tries to then send the messages to a server with a connection
     * already open or failing that tries to open a connection to open of the
     * servers.
     * </p>
     */
    @Override
    public String send(final Message message1, final Message message2,
            final Callback<Reply> replyCallback) throws MongoDbException {

        if (!isAvailable()) {
            throw new ConnectionLostException("Connection shutting down.");
        }

        final List<Server> servers = findPotentialServers(message1, message2);

        // First we try and send to a server with a connection already open.
        final String result = trySend(servers, message1, message2,
                replyCallback);

        if (result == null) {
            throw new MongoDbException(
                    "Could not send the messages to any of the potential servers.");
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to shutdown all of the underlying connections.
     * </p>
     */
    @Override
    public void shutdown(final boolean force) {
        myShutdown.set(true);
        for (final Connection conn : myServers.values()) {
            conn.shutdown(force);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to return the socket information.
     * </p>
     */
    @Override
    public String toString() {
        return "ReplicaSet(" + myLastUsedConnection.get() + ")";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to wait for all of the underlying connections to close.
     * </p>
     */
    @Override
    public void waitForClosed(final int timeout, final TimeUnit timeoutUnits) {
        final long millis = timeoutUnits.toMillis(timeout);
        long now = System.currentTimeMillis();
        final long deadline = now + millis;

        for (final Connection conn : myServers.values()) {
            if (now < deadline) {
                conn.waitForClosed((int) (deadline - now),
                        TimeUnit.MILLISECONDS);
                now = System.currentTimeMillis();
            }
        }
    }

    /**
     * Sends the message on the connection.
     * 
     * @param conn
     *            The connection to send on.
     * @param message1
     *            The first message to send.
     * @param message2
     *            The second message to send, may be <code>null</code>.
     * @param reply
     *            The reply {@link Callback}.
     * @return The server the message was sent to.
     */
    protected String doSend(final Connection conn, final Message message1,
            final Message message2, final Callback<Reply> reply) {

        // Use the connection for metrics etc.
        myLastUsedConnection.lazySet(conn);

        if (message2 == null) {
            return conn.send(message1, reply);
        }
        return conn.send(message1, message2, reply);

    }

    /**
     * Locates the set of servers that can be used to send the specified
     * messages. This method will attempt to connect to the primary server if
     * there is not a current connection to the primary.
     * 
     * @param message1
     *            The first message to send.
     * @param message2
     *            The second message to send. May be <code>null</code>.
     * @return The servers that can be used.
     * @throws MongoDbException
     *             On a failure to locate a server that all messages can be sent
     *             to.
     */
    protected List<Server> findPotentialServers(final Message message1,
            final Message message2) throws MongoDbException {
        List<Server> servers = doFindPotentialServers(message1, message2);

        if (servers.isEmpty()) {
            // If we get here and a reconnect is in progress then
            // block for the reconnect. The primary will have been marked a
            // secondary... Once the reconnect is complete, try again.
            if (myPrimaryServer == null) {
                // Wait for a reconnect.
                final ReplicaSetConnectionInfo newConnInfo = myReconnectStrategy
                        .reconnectPrimary();
                if (newConnInfo != null) {
                    updatePrimary(newConnInfo);
                    servers = doFindPotentialServers(message1, message2);
                }
            }

            if (servers.isEmpty()) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Could not find any servers for the following set of read preferences: ");
                final Set<ReadPreference> seen = new HashSet<ReadPreference>();
                for (final Message message : Arrays.asList(message1, message2)) {
                    if (message != null) {
                        final ReadPreference prefs = message
                                .getReadPreference();
                        if (seen.add(prefs)) {
                            if (seen.size() > 1) {
                                builder.append(", ");
                            }
                            builder.append(prefs);
                        }
                    }
                }

                builder.append('.');

                throw new MongoDbException(builder.toString());
            }
        }

        return servers;
    }

    /**
     * Tries to reconnect previously open {@link Connection}s. If a connection
     * was being closed then cleans up the remaining state.
     * 
     * @param connection
     *            The connection that was closed.
     */
    protected synchronized void handleConnectionClosed(
            final Connection connection) {

        if (!myOpen.get()) {
            return;
        }

        final Server server = findServerForConnection(connection);

        try {
            // If this is the last connection then go ahead and close this
            // replica set connection so the number of active connections can
            // shrink. Only close this connection on a graceful primary
            // shutdown to pick up when a primary change happens.
            final Server primary = myPrimaryServer;
            if ((myServers.size() == 1)
                    && (!server.equals(primary) || connection.isShuttingDown())) {

                // Mark this a graceful shutdown.
                removeCachedConnection(server, connection);
                shutdown(true);

                myEventSupport.firePropertyChange(Connection.OPEN_PROP_NAME,
                        true, isOpen());
            }
            // If the connection that closed was the primary then we need to
            // reconnect.
            else if (server.equals(primary) && isOpen()) {
                // Not sure who is primary any more.
                myPrimaryServer = null;

                LOG.info("Primary MongoDB Connection closed: ReplicaSet("
                        + connection + "). Will try to reconnect.");

                // Need to use the reconnect logic to find the new primary.
                final ReplicaSetConnectionInfo newConn = myReconnectStrategy
                        .reconnectPrimary();
                if (newConn != null) {
                    removeCachedConnection(server, connection);
                    updatePrimary(newConn);
                }
                // Else could not find a primary. Likely in a bad state but let
                // the connection stay for secondary queries if we have another
                // connection.
                else if (myServers.size() == 1) {
                    // Mark this a graceful shutdown.
                    removeCachedConnection(server, connection);
                    shutdown(false);

                    myEventSupport.firePropertyChange(
                            Connection.OPEN_PROP_NAME, true, isOpen());
                }
            }
            // Just remove the connection (above).
            else {
                LOG.fine("MongoDB Connection closed: ReplicaSet(" + connection
                        + ").");
            }
        }
        finally {
            // Make sure we always remove the closed connection.
            removeCachedConnection(server, connection);
            connection.raiseErrors(new ConnectionLostException(
                    "Connection closed."));
        }
    }

    /**
     * Reconnects the connection.
     * 
     * @param conn
     *            The connection to reconnect.
     * @return The new connection if the reconnect was successful.
     */
    protected Connection reconnect(final Connection conn) {
        final ReconnectStrategy strategy = myFactory.getReconnectStrategy();
        final Connection newConn = strategy.reconnect(conn);
        IOUtils.close(conn);
        return newConn;
    }

    /**
     * Remove the connection from the cache.
     * 
     * @param server
     *            The server to remove the connection for.
     * @param connection
     *            The connection to remove (if known).
     */
    protected void removeCachedConnection(final Server server,
            final Connection connection) {
        Connection conn = connection;
        if (connection == null) {
            conn = myServers.remove(server);
        }
        else if (!myServers.remove(server, connection)) {
            // Different connection found.
            conn = null;
        }

        if (conn != null) {
            conn.removePropertyChangeListener(myListener);
            conn.shutdown(true);
        }
    }

    /**
     * Tries to send the messages to the first server with either an open
     * connection or that we can open a connection to.
     * 
     * @param servers
     *            The servers the messages can be sent to.
     * @param message1
     *            The first message to send.
     * @param message2
     *            The second message to send. May be <code>null</code>.
     * @param reply
     *            The callback for the replies.
     * @return The token for the server that the messages were sent to or
     *         <code>null</code> if the messages could not be sent.
     */
    protected String trySend(final List<Server> servers,
            final Message message1, final Message message2,
            final Callback<Reply> reply) {
        for (final Server server : servers) {

            Connection conn = myServers.get(server);

            // See if we need to create a connection.
            if (conn == null) {
                // Create one.
                conn = connect(server);
            }
            else if (!conn.isAvailable()) {

                removeCachedConnection(server, conn);

                final ReconnectStrategy strategy = myFactory
                        .getReconnectStrategy();
                conn = strategy.reconnect(conn);
                if (conn != null) {
                    conn = cacheConnection(server, conn);
                }
            }

            if (conn != null) {
                return doSend(conn, message1, message2, reply);
            }
        }

        return null;
    }

    /**
     * Caches the connection to the server if there is not already a connection
     * in the cache. If there is a connection already in the cache then the one
     * provided is closed and the cached connection it returned.
     * 
     * @param server
     *            The server connected to.
     * @param conn
     *            The connection to cache, if possible.
     * @return The connection in the cache.
     */
    private Connection cacheConnection(final Server server,
            final Connection conn) {
        final Connection existing = myServers.putIfAbsent(server, conn);
        if (existing != null) {
            conn.shutdown(true);
            return existing;
        }

        // Listener to the connection for it to close.
        conn.addPropertyChangeListener(myListener);

        return conn;
    }

    /**
     * Attempts to create a connection to the server, catching any exceptions
     * thrown.
     * 
     * @param server
     *            The server to connect to.
     * @return The connection to the {@link Server}.
     */
    private Connection connect(final Server server) {
        Connection conn = null;
        try {
            conn = myFactory.connect(server, myConfig);

            conn = cacheConnection(server, conn);
        }
        catch (final IOException e) {
            LOG.info("Could not connect to the server '"
                    + server.getCanonicalName() + "': " + e.getMessage());
        }
        return conn;
    }

    /**
     * Locates the set of servers that can be used to send the specified
     * messages.
     * 
     * @param message1
     *            The first message to send.
     * @param message2
     *            The second message to send. May be <code>null</code>.
     * @return The servers that can be used.
     */
    private List<Server> doFindPotentialServers(final Message message1,
            final Message message2) {
        List<Server> servers = Collections.emptyList();

        if (message1 != null) {
            List<Server> potentialServers = myCluster
                    .findCandidateServers(message1.getReadPreference());
            servers = potentialServers;

            if (message2 != null) {
                servers = new ArrayList<Server>(potentialServers);
                potentialServers = myCluster.findCandidateServers(message2
                        .getReadPreference());
                servers.retainAll(potentialServers);
            }
        }
        return servers;
    }

    /**
     * Finds the server for the connection.
     * 
     * @param connection
     *            The connection to remove.
     * @return The Server for the connection.
     */
    private Server findServerForConnection(final Connection connection) {
        for (final Map.Entry<Server, Connection> entry : myServers.entrySet()) {
            if (entry.getValue() == connection) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Update the state with the new primary server.
     * 
     * @param newConn
     *            The new primary server.
     */
    private void updatePrimary(final ReplicaSetConnectionInfo newConn) {
        myPrimaryServer = newConn.getPrimaryServer();

        // Add the connection to the cache. This also gets the listener
        // attached.
        cacheConnection(newConn.getPrimaryServer(), newConn.getConnection());
    }

    /**
     * ClusterListener provides a listener for changes in the cluster.
     * 
     * @api.no This class is <b>NOT</b> part of the drivers API. This class may
     *         be mutated in incompatible ways between any two releases of the
     *         driver.
     * @copyright 2013, Allanbank Consulting, Inc., All Rights Reserved
     */
    protected final class ClusterAndConnectionListener implements
            PropertyChangeListener {
        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            final String propName = event.getPropertyName();
            if (Cluster.SERVER_PROP.equals(propName)
                    && (event.getNewValue() == null)) {
                // A Server has been removed. Close the connection.
                removeCachedConnection((Server) event.getOldValue(), null);
            }
            else if (Connection.OPEN_PROP_NAME.equals(event.getPropertyName())
                    && Boolean.FALSE.equals(event.getNewValue())) {
                handleConnectionClosed((Connection) event.getSource());
            }
        }

    }
}
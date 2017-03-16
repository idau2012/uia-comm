/*******************************************************************************
 * Copyright 2017 UIA
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package uia.comm;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import uia.comm.protocol.Protocol;
import uia.comm.protocol.ProtocolEventArgs;
import uia.comm.protocol.ProtocolEventHandler;
import uia.comm.protocol.ProtocolMonitor;
import uia.utils.ByteUtils;

/**
 * The socket server
 *
 * @author Kyle K. Lin
 *
 */
public class SocketServer implements ProtocolEventHandler<SocketDataController> {

    public enum ConnectionStyle {
        NORMAL, ONE_EACH_CLIENT, ONLYONE
    }

    private final static Logger logger = Logger.getLogger(SocketServer.class);

    private final int port;

    private final Protocol<SocketDataController> protocol;

    private final MessageManager manager;

    private final TreeMap<String, MessageCallIn<SocketDataController>> callIns;

    private final TreeMap<String, TreeMap<String, MessageCallOut>> clientCallouts;

    private final ArrayList<SocketServerListener> listeners;

    private final TreeMap<String, SocketDataController> controllers;

    private final String aliasName;

    private final ConnectionStyle connectionStyle;

    private boolean started;

    private Selector serverSelector;

    private ServerSocketChannel ch;

    private Timer polling;

    private int idleTime;

    static {

    }

    public SocketServer(Protocol<SocketDataController> protocol, int port, MessageManager manager, String aliasName) throws Exception {
        this(protocol, port, manager, aliasName, ConnectionStyle.NORMAL);
    }

    /**
     * Constructor.
     *
     * @param protocol The protocol on this socket channel.
     * @param port Socket port.
     * @param manager Protocol manager.
     * @param aliasName Alias name.
     * @throws Exception Raise construct failure.
     */
    public SocketServer(Protocol<SocketDataController> protocol, int port, MessageManager manager, String aliasName, ConnectionStyle connectionStyle) throws Exception {
        this.aliasName = aliasName;
        this.protocol = protocol;
        this.protocol.addMessageHandler(this);
        this.manager = manager;
        this.callIns = new TreeMap<String, MessageCallIn<SocketDataController>>();
        this.clientCallouts = new TreeMap<String, TreeMap<String, MessageCallOut>>();
        this.started = false;
        this.connectionStyle = connectionStyle;
        this.controllers = new TreeMap<String, SocketDataController>();
        this.listeners = new ArrayList<SocketServerListener>();

        this.idleTime = 60000;
        this.port = port;
    }

    public int getClientCount() {
        return this.controllers.size();
    }

    public boolean exists(String socketId) {
        return this.controllers.containsKey(socketId);
    }

    public void setIdleTime(int idleTime) {
        this.idleTime = idleTime;
    }

    public int getIdleTime() {
        return this.idleTime;
    }

    /**
     * Get name.
     *
     * @return The name.
     */
    public String getName() {
        return this.aliasName;
    }

    /**
     * Get protocol on this socket channel.
     *
     * @return The protocol.
     */
    public Protocol<SocketDataController> getProtocol() {
        return this.protocol;
    }

    /**
     * Add a listener of states of server.
     *
     * @param listener The listener.
     */
    public void addServerListener(SocketServerListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Remove a listener of states of server.
     *
     * @param listener The listener.
     */
    public void removeServerListener(SocketServerListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Register call in worker to handle message send from client actively.
     *
     * @param callIn Call in worker.
     */
    public void registerCallin(MessageCallIn<SocketDataController> callIn) {
        this.callIns.put(callIn.getCmdName(), callIn);
    }

    /**
     * Send data to specific socket client.
     *
     * @param clientName Client name.
     * @param data Data.
     * @return Send success or not.
     * @throws SocketException Raise if not started or client missing.
     */
    public boolean send(final String clientName, final byte[] data) throws SocketException {
        return send(clientName, data, 1);
    }

    /**
     * Send data to specific socket client.
     *
     * @param clientName Client name.
     * @param data Data.
     * @param times Retry times.
     * @return Send success or not.
     * @throws SocketException Raise if not started or client missing.
     */
    public boolean send(final String clientName, final byte[] data, int times) throws SocketException {
        if (!this.started) {
            throw new SocketException(this.aliasName + "> is not started.");
        }

        final SocketDataController controller = this.controllers.get(clientName);
        if (controller == null) {
            throw new SocketException(clientName + "> missing");
        }

        return controller.send(data, times);
    }

    /**
     * send data to socket server and wait result.
     *
     * @param clientName Client name.
     * @param data Data to be sent.
     * @param txId Transaction id.
     * @param timeout Timeout millisecond.
     * @return Null if timeout.
     * @throws SocketException Raise if not started.
     */
    public byte[] send(final String clientName, final byte[] data, String txId, long timeout) throws SocketException {
        if (!this.started) {
            throw new SocketException(this.aliasName + "> is not started.");
        }

        final SocketDataController controller = this.controllers.get(clientName);
        if (controller == null) {
            throw new SocketException(clientName + "> missing");
        }

        MessageCallOutConcurrent callout = new MessageCallOutConcurrent(txId, timeout);
        ExecutorService threadPool = Executors.newSingleThreadExecutor();

        TreeMap<String, MessageCallOut> callOuts = this.clientCallouts.get(clientName);
        if (callOuts == null) {
            callOuts = new TreeMap<String, MessageCallOut>();
            this.clientCallouts.put(clientName, callOuts);
        }

        try {
            synchronized (callOuts) {
                callOuts.put(txId, callout);
            }

            if (controller.send(data, 1)) {
                logger.debug(String.format("%s> %s> send %s", this.aliasName, clientName, ByteUtils.toHexString(data, 100)));
                try {
                    Future<byte[]> future = threadPool.submit(callout);
                    return future.get();
                }
                catch (Exception e) {
                    logger.error(String.format("%s> %s> reply failure", this.aliasName, clientName));
                    throw new SocketException(String.format("%s> %s> reply failure", this.aliasName, clientName));
                }
            }
            else {
                logger.debug(String.format("%s> %s> send %s failure", this.aliasName, clientName, ByteUtils.toHexString(data, 100)));
                throw new SocketException(String.format("%s> %s> send failure", this.aliasName, clientName));
            }
        }
        finally {
            synchronized (callOuts) {
                callOuts.remove(txId);
            }
        }
    }

    /**
     * Send data to specific socket client.
     *
     * @param clientName Client name.
     * @param data Data
     * @param callOut Reply data worker.
     * @param timeout Timeout seconds.
     * @return Send success or not.
     * @throws SocketException Raise if not started or client missing.
     */
    public boolean send(final String clientName, final byte[] data, final MessageCallOut callOut, final long timeout) throws SocketException {
        if (!this.started) {
            throw new SocketException(this.aliasName + "> is not started.");
        }

        final SocketDataController controller = this.controllers.get(clientName);
        if (controller == null) {
            throw new SocketException(clientName + "> missing");
        }

        TreeMap<String, MessageCallOut> callOuts = this.clientCallouts.get(clientName);
        if (callOuts == null) {
            callOuts = new TreeMap<String, MessageCallOut>();
            this.clientCallouts.put(clientName, callOuts);
        }
        final String tx = callOut.getTxId();
        synchronized (callOuts) {
            callOuts.put(tx, callOut);
        }

        final TreeMap<String, MessageCallOut> callOutsRef = callOuts;
        if (controller.send(data, 1)) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    synchronized (callOutsRef) {
                        if (callOutsRef.containsKey(tx)) {
                            callOutsRef.remove(tx);
                            callOut.timeout();
                        }
                    }
                }

            }, timeout);
            return true;
        }
        else {
            synchronized (callOuts) {
                callOuts.remove(tx);
            }
            return false;
        }
    }

    /**
     * Check if server is started or not.
     * @return Started or not.
     */
    public boolean isStarted() {
        return this.started;
    }

    /**
     * Start this server.
     * @return Success or not.
     */
    public boolean start() {
        if (this.started) {
            return true;
        }

        this.controllers.clear();
        this.clientCallouts.clear();

        try {
            this.serverSelector = Selector.open();
            if (this.serverSelector == null) {
                return false;
            }

            this.ch = ServerSocketChannel.open();
            this.ch.socket().bind(new InetSocketAddress(this.port));
            this.ch.configureBlocking(false);
            this.ch.register(this.serverSelector, SelectionKey.OP_ACCEPT);
        }
        catch (Exception ex) {
            logger.error(ex);
            return false;
        }

        this.started = true;

        // handle connect and receive data
        new Thread(new Runnable() {

            @Override
            public void run() {
                running();
            }
        }).start();

        // polling
        this.polling = new Timer();
        this.polling.schedule(new TimerTask() {

            @Override
            public void run() {
                polling();
            }

        }, this.idleTime, this.idleTime);

        return true;
    }

    /**
     * Disconnect specific socket client.
     *
     * @param clientName Client name.
     */
    public void disconnect(String clientName) {
        SocketDataController controller = null;
        synchronized (this.controllers) {
            controller = this.controllers.remove(clientName);
        }

        this.callIns.remove(clientName);
        this.clientCallouts.remove(clientName);

        if (controller != null) {
            // logger.info(String.format("%s> %s disconnected", this.aliasName, clientName));
            SelectionKey key = controller.getChannel().keyFor(this.serverSelector);
            if (key != null) {
                key.cancel();
            }
            controller.stop();

            raiseDisconnected(controller);
        }
    }

    /**
     * Stop this server.
     *
     */
    public void stop() {
        if (!this.started) {
            return;
        }

        this.started = false;
        this.polling.cancel();

        try {
            this.serverSelector.wakeup();
            for (SocketDataController controller : this.controllers.values()) {
                controller.stop();

                SocketChannel ch = controller.getChannel();
                if (ch != null) {
                    SelectionKey key = ch.keyFor(this.serverSelector);
                    if (key != null) {
                        key.cancel();
                    }
                }
                raiseDisconnected(controller);
            }
            Thread.sleep(1000);
        }
        catch (Exception ex) {
            ex.printStackTrace();

        }
        finally {
            this.controllers.clear();
            this.clientCallouts.clear();
            try {
                this.serverSelector.close();
                this.ch.socket().close();
                this.ch.close();
            }
            catch (Exception ex) {

            }

            this.polling = null;
            this.ch = null;
            this.serverSelector = null;
            System.gc();
        }
    }

    @Override
    public synchronized void messageReceived(final ProtocolMonitor<SocketDataController> monitor, ProtocolEventArgs args) {
        if (args.getData() == null || args.getData().length == 0) {
            return;
        }

        if (this.controllers.get(monitor.getController().getName()) == null) {
            return;
        }

        final byte[] received = this.manager.decode(args.getData());
        if (!this.manager.validate(received)) {
            logger.debug(String.format("%s> data wrong: %s", this.aliasName, ByteUtils.toHexString(received, "-")));
            return;
        }

        // get command
        String cmd = this.manager.findCmd(received);
        if (cmd == null) {
            logger.debug(String.format("%s> %s> %s cmd:%s missing",
                    this.aliasName,
                    monitor.getName(),
                    monitor.getProtocol().getAliasName(),
                    cmd));
            return;
        }

        monitor.getController().lastUpdate();

        if (this.manager.isCallIn(cmd)) {
            final MessageCallIn<SocketDataController> callIn = this.callIns.get(cmd);
            if (callIn == null) {
                logger.debug(String.format("%s> %s> %s cmd:%s callIn missing",
                        this.aliasName,
                        monitor.getName(),
                        monitor.getProtocol().getAliasName(),
                        cmd));
                return;
            }

            logger.debug(String.format("%s> %s> %s cmd:%s callIn",
                    this.aliasName,
                    monitor.getName(),
                    monitor.getProtocol().getAliasName(),
                    cmd));
            new Thread(new Runnable() {

                @Override
                public void run() {
                    callIn.execute(received, monitor.getController());
                }

            }).start();
        }
        else {
            TreeMap<String, MessageCallOut> callOuts = this.clientCallouts.get(monitor.getController().getName());
            if (callOuts == null) {
                logger.debug(String.format("%s> %s> not found",
                        this.aliasName,
                        monitor.getController().getName()));
                return;
            }

            String tx = this.manager.findTx(received);
            final MessageCallOut callOut = callOuts.get(tx);
            if (callOut == null) {
                logger.debug(String.format("%s> %s> %s cmd:%s tx:%s callOut reply missing",
                        this.aliasName,
                        monitor.getController().getName(),
                        monitor.getProtocol().getAliasName(),
                        cmd,
                        tx));
                return;
            }

            synchronized (callOuts) {
                callOuts.remove(tx);
            }

            logger.debug(String.format("%s> %s> %s cmd:%s tx:%s callOut reply",
                    this.aliasName,
                    monitor.getController().getName(),
                    monitor.getProtocol().getAliasName(),
                    cmd,
                    tx));
            new Thread(new Runnable() {

                @Override
                public void run() {
                    callOut.execute(received);
                }

            }).start();
        }
    }

    @Override
    public void messageError(ProtocolMonitor<SocketDataController> monitor, ProtocolEventArgs args) {
        if (args.getData() == null || args.getData().length == 0) {
            return;
        }

        logger.debug(String.format("%s> %s> %s pack message error: %s(%s).",
                this.aliasName,
                monitor.getName(),
                monitor.getProtocol().getAliasName(),
                args.getErrorCode(),
                monitor.getClass().getName()));
        logger.debug(ByteUtils.toHexString(args.getData(), "-"));
    }

    private void polling() {
        if (this.started) {
            ArrayList<String> keys = new ArrayList<String>();
            Collection<SocketDataController> controllers = this.controllers.values();
            synchronized (this.controllers) {
                for (SocketDataController controller : controllers) {
                    if (controller.isIdle(this.idleTime)) {
                        keys.add(controller.getName());
                    }
                }
            }

            for (String key : keys) {
                disconnect(key);
            }
        }
        else {
            this.polling.cancel();
        }
    }

    private void running() {
        while (this.started) {
            try {
                this.serverSelector.select(); // wait NIO event.
            }
            catch (Exception ex) {
                logger.error("comm> ", ex);
                continue;
            }

            if (!this.serverSelector.isOpen()) {
                continue;

            }

            Iterator<SelectionKey> iter = this.serverSelector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();

                try {
                    if (key.isAcceptable()) {
                        ServerSocketChannel client = (ServerSocketChannel) key.channel();
                        SocketChannel ch = client.accept();
                        clientConnected(ch);
                    }
                    else if (key.isReadable()) {
                        SocketDataController controller = (SocketDataController) key.attachment();
                        if (!controller.receive()) {
                            disconnect(controller.getName());
                        }
                    }
                }
                catch (Exception ex) {

                }
            }
        }
    }

    private void clientConnected(SocketChannel client) {
        try {
            client.configureBlocking(false);
            String clientId = "";
            if (this.connectionStyle == ConnectionStyle.ONE_EACH_CLIENT) {
                clientId = client.socket().getInetAddress().getHostAddress();

            }
            else {
                clientId = client.socket().getRemoteSocketAddress().toString();

            }
            disconnect(clientId);

            if (this.connectionStyle == ConnectionStyle.ONLYONE) {
                for (String key : this.controllers.keySet()) {
                    disconnect(key);
                }
            }

            SocketDataController controller = new SocketDataController(
                    clientId,
                    client,
                    this.manager,
                    this.protocol.createMonitor(clientId));

            synchronized (this.controllers) {
                this.controllers.put(clientId, controller);
            }

            // use internal selector
            // controller.start();
            // use server selector
            client.register(this.serverSelector, SelectionKey.OP_READ, controller);

            logger.info(String.format("%s> %s controller added", this.aliasName, clientId));

            raiseConnected(controller);

        }
        catch (Exception ex) {
            ex.printStackTrace();
            logger.error(String.format("%s> client connected failure.", this.aliasName), ex);
        }
    }

    private void raiseConnected(SocketDataController controller) {
        for (SocketServerListener listener : this.listeners) {
            listener.connected(controller);
        }
    }

    private void raiseDisconnected(SocketDataController controller) {
        if (!this.started) {
            return;
        }

        for (SocketServerListener listener : this.listeners) {
            listener.disconnected(controller);
        }
    }
}

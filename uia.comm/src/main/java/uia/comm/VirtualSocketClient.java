package uia.comm;

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import uia.utils.ByteUtils;

/**
 * 
 * @author Kan
 *
 */
public class VirtualSocketClient {

    private final static Logger logger = Logger.getLogger(VirtualSocketClient.class);

    private SocketClient activeClient;

    private LinkedBlockingQueue<SocketClient> queues;

    public VirtualSocketClient() {
        this.queues = new LinkedBlockingQueue<SocketClient>();
    }

    public void add(SocketClient client) {
        if (this.activeClient == null) {
            this.activeClient = client;
        }
        this.queues.add(client);
    }

    public boolean tryConnect() {
        if (this.activeClient == null) {
            return false;
        }

        SocketClient current = this.activeClient;
        do {
            if (this.activeClient.tryConnect()) {
                logger.info(this.activeClient.getName() + "> active");
                return true;
            }
        }
        while (nextClient(current));

        return false;
    }

    public void disconnect() {
        this.activeClient.disconnect();
    }

    public void switchClient() {
        SocketClient current = this.activeClient;
        if (current != null) {
            nextClient(current);
        }
    }

    public boolean send(byte[] data) {
        return send(data, 3);
    }

    public boolean send(byte[] data, int times) {
        SocketClient current = this.activeClient;
        do {
            try {
                if (this.activeClient.tryConnect() && this.activeClient.send(data, times)) {
                    return true;
                }
                else {
                    this.activeClient.disconnect();
                }
            }
            catch (Exception ex) {
                this.activeClient.disconnect();
            }
        }
        while (nextClient(current));

        return false;
    }

    public byte[] send(byte[] data, String txId, long timeout) {
        SocketClient current = this.activeClient;
        do {
            try {
                if (this.activeClient.tryConnect()) {
                    byte[] reply = this.activeClient.send(data, txId, timeout);
                    if (reply != null) {
                        return reply;
                    }
                    else {
                        logger.error(this.activeClient.getName() + "> sned failure. tx:" + txId + ", data:" + ByteUtils.toHexString(data));
                        this.activeClient.disconnect();
                    }
                }
            }
            catch (Exception ex) {
                this.activeClient.disconnect();
            }
        }
        while (nextClient(current));

        return null;
    }

    public boolean send(byte[] data, MessageCallOut callOut, long timeout) {
        SocketClient current = this.activeClient;
        do {
            try {
                if (this.activeClient.tryConnect() && this.activeClient.send(data, callOut, timeout)) {
                    return true;
                }
                else {
                    this.activeClient.disconnect();
                }
            }
            catch (Exception ex) {
                this.activeClient.disconnect();
            }
        }
        while (nextClient(current));

        return false;
    }

    private boolean nextClient(SocketClient checkOne) {
        SocketClient client = this.queues.poll();
        this.activeClient = this.queues.peek();
        this.queues.add(client);
        return this.activeClient != checkOne;
    }
}
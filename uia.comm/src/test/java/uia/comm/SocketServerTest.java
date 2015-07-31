/*******************************************************************************
 * * Copyright (c) 2014, UIA * All rights reserved. * Redistribution and use in source and binary forms, with or without * modification, are permitted provided that the following conditions are met: * * * Redistributions of source code must retain
 * the above copyright * notice, this list of conditions and the following disclaimer. * * Redistributions in binary form must reproduce the above copyright * notice, this list of conditions and the following disclaimer in the * documentation and/or
 * other materials provided with the distribution. * * Neither the name of the {company name} nor the * names of its contributors may be used to endorse or promote products * derived from this software without specific prior written permission. * *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS "AS IS" AND ANY * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE * DISCLAIMED. IN NO
 * EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; * LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS * SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package uia.comm;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;

import uia.comm.my.MyManager;
import uia.comm.protocol.ng.NGProtocol;

public class SocketServerTest {

    public static Logger logger = Logger.getLogger(SocketServerTest.class);

    private final NGProtocol<SocketDataController> serverProtocol;

    private final NGProtocol<SocketDataController> clientProtocol;

    private final MyManager manager;

    public SocketServerTest() {
        PropertyConfigurator.configure("log4j.properties");

        this.manager = new MyManager();
        this.serverProtocol = new NGProtocol<SocketDataController>();
        this.clientProtocol = new NGProtocol<SocketDataController>();

    }

    @Test
    public void testConnect() throws Exception {
        SocketClient client = new SocketClient(this.clientProtocol, this.manager, "c1");

        SocketServer server = create();
        System.out.println("svr start:" + server.start());

        Thread.sleep(1000);
        System.out.println("clnt:" + client.connect("localhost", 5953));
        Thread.sleep(1000);
        client.disconnect();

        server.stop();

        server = create();
        System.out.println("svr start:" + server.start());

        Thread.sleep(1000);
        System.out.println("clnt:" + client.connect("localhost", 5953));
        Thread.sleep(1000);
        client.disconnect();

        server.stop();

        //SocketServer server1 = create();
        //System.out.println("svr1 start:" + server1.start());
        //Thread.sleep(1000);
        //SocketServer server2 = create();
        //System.out.println("svr2 start:" + server2.start());
        //Thread.sleep(1000);
        //server1.stop();

    }

    private SocketServer create() throws Exception {
        SocketServer server = new SocketServer(this.serverProtocol, 5953, this.manager, "TestServer");
        server.addServerListener(new SocketServerListener() {

            @Override
            public void connected(SocketDataController controller) {
                logger.info(controller.getName() + " connected");
            }

            @Override
            public void disconnected(SocketDataController controller) {
                logger.info(controller.getName() + " disconnected");
            }

        });
        return server;
    }
}

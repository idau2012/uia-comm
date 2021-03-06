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
package uia.comm.protocol.ht;

import uia.comm.protocol.ProtocolEventArgs;

/**
 *
 * @author Kyle K. Lin
 *
 * @param <C> Reference.
 */
public class BodyState<C> implements HTState<C> {

    private int headIdx;

    public BodyState() {
        this.headIdx = 0;
    }

    @Override
    public String toString() {
        return "BodyState";
    }

    @Override
    public void accept(HTProtocolMonitor<C> monitor, byte one) {
        if (one == monitor.protocol.head[this.headIdx])
        {
            this.headIdx++;
        }
        else
        {
            this.headIdx = 0;
        }

        if (this.headIdx > 0 && this.headIdx == monitor.protocol.head.length)
        {
            this.headIdx = 0;
            monitor.addOne(one);
            monitor.cancelPacking(ProtocolEventArgs.ErrorCode.ERR_HEAD_REPEAT);
            for (byte b : monitor.protocol.head)
            {
                monitor.addOne(b);
            }
        }
        else {
            if (one == monitor.protocol.tail[0]) {
                monitor.setState(new TailState<C>());
                monitor.read(one);
            }
            else {
                monitor.addOne(one);
            }
        }
    }
}

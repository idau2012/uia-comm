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
public class TailState<C> implements HTState<C> {

    @Override
    public String toString() {
        return "TailState";
    }

    @Override
    public void accept(HTProtocolMonitor<C> monitor, byte one) {
        if (one == monitor.protocol.tail[monitor.tailIdx]) {
            monitor.addOne(one);
            monitor.tailIdx++;
            if (monitor.tailIdx >= monitor.protocol.tail.length) {
                monitor.finsihPacking();
                monitor.reset();
                monitor.setState(new IdleState<C>());
            }
        }
        else {
            monitor.cancelPacking(ProtocolEventArgs.ErrorCode.ERR_TAIL);
            monitor.reset();
            if (one == monitor.protocol.head[0]) {
                monitor.setState(new HeadState<C>());
                monitor.read(one);
            }
            else {
                monitor.setState(new IdleState<C>());
            }
        }
    }
}

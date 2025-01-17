/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.sl.nodes.controlflow;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.sl.nodes.SLStatementNode;
import com.oracle.truffle.sl.runtime.SLContext;

import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

@NodeInfo(shortName = "pfor", description = "The node implementing a pfor loop")
public final class SLPForNode extends SLStatementNode {

    private final ArrayList<SLBlockNode> blocks;

    private final long start;
    private final long end;


    public SLPForNode(ArrayList<SLBlockNode> blocks, long start, long end) {
        this.blocks = blocks;
        this.start = start;
        this.end = end;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        List<Thread> threads = new ArrayList<Thread>();
        BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
        int numThreads = 4;
        for (int i = 0; i < numThreads; i++) {
            threads.add(SLContext.get(this).createThread(
                    () -> {
                        while (true) {
                            try {
                                Runnable task = tasks.poll(1000, TimeUnit.MICROSECONDS);
                                if (task != null)
                                    task.run();
                                else break;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
            ));
            threads.get(i).start();
        }

        for (long i = 0; i <= end - start; i++) {
            final int j = (int)i;
            tasks.add(() -> blocks.get(j).executeVoid(frame));
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}


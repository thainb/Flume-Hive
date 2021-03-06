/**
 * Licensed to Cloudera, Inc. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Cloudera, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.flume.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.flume.agent.durability.WALCompletionNotifier;
import com.cloudera.flume.conf.FlumeConfiguration;
import com.cloudera.flume.conf.FlumeSpecException;
import com.cloudera.flume.conf.FlumeConfigData;
import com.cloudera.flume.handlers.endtoend.AckListener.Empty;
import com.cloudera.util.Clock;
import com.google.common.base.Preconditions;

/**
 * This manages heartbeating the node with the master, notifications of
 * configuration updates, and spawning/decommissioning of logical nodes.
 * 
 * TODO (jon) rename to HeartbeatManager
 */
public class LivenessManager {
  static final Logger LOG = LoggerFactory.getLogger(LivenessManager.class);
  final long BACKOFF_MILLIS;

  MasterRPC master;
  LogicalNodeManager nodesman;
  HeartbeatThread t;
  final WALAckManager ackcheck;
  final WALCompletionNotifier walman;

  class RetryAckListener extends Empty {
    @Override
    public void end(String group) throws IOException {
      walman.toAcked(group);
    }

    @Override
    public void expired(String group) throws IOException {
      walman.retry(group);
    }
  };

  /**
   * Create a liveness manager with the specified managers.
   * 
   * LogicalNodeManager is necessary for tracking physical/logical node
   * mappings. MasterRPC is the connection to the master, WALCompletionNotifier
   * is necessary for check on acks
   */
  public LivenessManager(LogicalNodeManager nodesman, MasterRPC master,
      WALCompletionNotifier walman) {
    Preconditions.checkNotNull(nodesman);
    Preconditions.checkNotNull(master);
    BACKOFF_MILLIS = FlumeConfiguration.get().getHeartbeatBackoff();
    this.walman = walman;
    this.nodesman = nodesman;
    this.master = master;
    this.t = new HeartbeatThread();
    this.ackcheck = new WALAckManager(master, new RetryAckListener(),
        FlumeConfiguration.get().getAgentAckedRetransmit());
  }

  /**
   * Checks against the master to get new physical nodes or to learn about
   * decommissioned logical nodes
   * 
   * Invariant: There is always at least logical per physical node. When there
   * is one, it has the same name as the physical node.
   */
  public void checkLogicalNodes() throws IOException {
    // TODO (jon) Make this a single batched rpc call instead of
    // multiple calls

    String physNode = nodesman.getPhysicalNodeName();
    // get logical nodes list for this node.
    List<String> lns = master.getLogicalNodes(physNode);
    if (!lns.contains(physNode)) {
      // physical node node present? make sure it stays around.
      lns = new ArrayList<String>(lns); // copy the unmodifiable list
      lns.add(physNode);
    }
    for (String ln : lns) {
      // a logical node is not present? spawn it.
      if (nodesman.get(ln) == null) {
        try {
          nodesman.spawn(ln, "null", "null");
        } catch (FlumeSpecException e) {
          LOG.error("This should never happen", e);
        }
      }
    }
    // Update the Chokeinformation for the ChokeManager

    FlumeNode.getInstance().getChokeManager().updateChokeLimitMap(
        master.getChokeMap(physNode));

    nodesman.decommissionAllBut(lns);
  }

  /**
   * Checks registered nodes to see if they need a new configuraiton.
   */
  public void checkLogicalNodeConfigs() throws IOException {
    // TODO (jon) batch all these rpc requests into one multi-part rpc
    // request.

    for (LogicalNode nd : nodesman.getNodes()) {
      boolean needsCfg = master.heartbeat(nd);
      if (needsCfg) {
        final FlumeConfigData data = master.getConfig(nd);
        if (data == null) {
          LOG.debug("Logical Node '" + nd.getName()
              + "' not configured on master");
        }
        final LogicalNode node = nd;
        // TODO This is quite gross, but prevents heartbeat from blocking
        new Thread("SpawningLogicalNode " + nd.getName()) {
          public void run() {
            node.checkConfig(data);
          }
        }.start();
      }
    }
  }

  /**
   * All the core functionality of a heartbeat accessible without having to be
   * in the heartbeat thread.
   */
  public void heartbeatChecks() throws IOException {
    // these will call ensure open on the master
    checkLogicalNodes();

    checkLogicalNodeConfigs();

    // check for end to end acks.
    ackcheck.checkAcks();

  }

  /**
   * This thread periodically contacts the master with a heartbeat.
   */
  class HeartbeatThread extends Thread {
    volatile boolean done = false;
    long backoff = BACKOFF_MILLIS;
    long backoffLimit = FlumeConfiguration.get().getNodeHeartbeatBackoffLimit();
    long heartbeatPeriod = FlumeConfiguration.get().getConfigHeartbeatPeriod();
    CountDownLatch stopped = new CountDownLatch(1);

    HeartbeatThread() {
      super("Heartbeat");
    }

    public void run() {
      try {
        while (!done) {
          try {
            heartbeatChecks();
            backoff = BACKOFF_MILLIS; // was successful, reset backoff

            Clock.sleep(heartbeatPeriod);

          } catch (Exception e) {
            backoff *= 2; // sleep twice as long
            backoff = backoff > backoffLimit ? backoffLimit : backoff;

            LOG.warn("Connection to master(s) failed, " + e.getMessage()
                + ". Backing off for " + backoff + " ms ");
            LOG.debug("Current master is " + master.toString(), e);

            try {
              master.close();
            } catch (IOException e1) {
              LOG.error("Failed when attempting to close master", e1);
            }

            Clock.sleep(backoff);
          }
        }

      } catch (InterruptedException e) {
        LOG.error("Heartbeat interrupted, this is not expected!", e);
      }
      stopped.countDown();
    }

  };

  /**
   * Starts the heartbeat thread and then returns.
   */
  public void start() {
    t.start();
  }

  public void stop() {
    CountDownLatch stopped = t.stopped;
    t.done = true;
    try {
      stopped.await();
    } catch (InterruptedException e) {
      LOG.error("Problem waiting for livenessManager to stop", e);
    }
  }

  public WALAckManager getAckChecker() {
    return ackcheck;
  }
}

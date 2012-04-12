/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jclouds.cache;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;

import java.util.Properties;
import java.util.Set;

import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.predicates.RetryIfSocketNotYetOpen;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.net.IPSocket;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Module;

public class CacheServerAndClientInstallerLiveTest {

  private ComputeServiceContext vboxContext;

  @BeforeClass
  public void setUp() {
    Properties props = new Properties();
    props.setProperty(TIMEOUT_SCRIPT_COMPLETE, "2400000");
    vboxContext = new ComputeServiceContextFactory().createContext("virtualbox", "", "",
        ImmutableSet.<Module> of(new SLF4JLoggingModule(), new SshjSshClientModule(), new PackageCachingModule()),
        props);

  }

  @Test
  public void testInstallAptServer() {
    CacheServerInstaller serverInstaller = vboxContext.utils().injector().getInstance(CacheServerInstaller.class);
    Set<Cache> enabledCaches = serverInstaller.getCacheRepos();
    assertTrue(!enabledCaches.isEmpty());
    serverInstaller.apply(vboxContext.getComputeService().templateBuilder().build());
    Predicate<IPSocket> socketTester = vboxContext.getUtils().getInjector().getInstance(RetryIfSocketNotYetOpen.class);
    socketTester.apply(new IPSocket(Iterables.getFirst(serverInstaller.cacheServer.getPublicAddresses(), null), 3142));
  }

  @Test(dependsOnMethods = "testInstallAptServer")
  public void testInstallAptClient() throws RunNodesException {
    CacheClientInstaller clientInstaller = vboxContext.utils().injector().getInstance(CacheClientInstaller.class);
    NodeMetadata clientNode = Iterables.getOnlyElement(vboxContext.getComputeService().createNodesInGroup("test", 1));
    assertNotNull(clientNode);
    clientInstaller.apply(clientNode);
    // TODO test that the client uses the server someway, although in order for the client installer com
  }
}

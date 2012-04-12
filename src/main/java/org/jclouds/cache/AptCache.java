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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.scriptbuilder.domain.Statement;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * All that is required to install an start an apt-cacher server and configure clients to use it.
 * 
 * @author David Alves
 * 
 */
@Singleton
public class AptCache extends AbstractCache {

  private static final Iterable<String> DEPS = ImmutableSet.of();

  static class ServerStatement implements Statement {

    @Override
    public Iterable<String> functionDependencies(OsFamily family) {
      return DEPS;
    }

    @Override
    public String render(OsFamily family) {
      switch (family) {
        case UNIX:
          return "apt-get -y install apt-cacher\n"
              + "echo 'path_map = ubuntu us.archive.ubuntu.com/ubuntu' >> /etc/apt-cacher/apt-cacher.conf\n"
              + "sed -i 's/AUTOSTART=0/AUTOSTART=1/' /etc/default/apt-cacher\n" + "/etc/init.d/apt-cacher start\n";

        default:
          throw new UnsupportedOperationException();
      }
    }

  }

  public static class ClientStatement implements Statement {

    private String address;

    public ClientStatement(String address) {
      this.address = address;
    }

    @Override
    public Iterable<String> functionDependencies(OsFamily family) {
      return DEPS;
    }

    @Override
    public String render(OsFamily family) {
      switch (family) {
        case UNIX:
          return "sed -i 's/http:\\/\\//http:\\/\\/" + address + ":3142\\//' /etc/apt/sources.list \n"
              + "apt-get check \n";
        default:
          throw new UnsupportedOperationException();
      }
    }
  }

  @Override
  public String getName() {
    return "apt";
  }

  @Override
  public void installCacheServerInNode(ComputeServiceContext ctx, NodeMetadata server) {
    this.server = server;
    try {
      checkState(ctx.getComputeService()
          .submitScriptOnNode(server.getId(), new ServerStatement(), RunScriptOptions.NONE).get(20, TimeUnit.MINUTES)
          .getExitStatus() == 0);
      // TODO enable firewalls
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public void enableCacheClientInNode(ComputeServiceContext ctx, NodeMetadata clientNode) {
    checkNotNull(server, "server cannot be null");
    try {
      checkState(ctx
          .getComputeService()
          .submitScriptOnNode(clientNode.getId(),
              new ClientStatement(checkNotNull(Iterables.getFirst(server.getPrivateAddresses(), null))),
              RunScriptOptions.NONE).get(20, TimeUnit.MINUTES).getExitStatus() == 0);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public boolean isServerCompatible(Image serverImage) {
    // try to get the os family through metadata
    if (serverImage.getOperatingSystem() != null) {
      return serverImage.getOperatingSystem().getFamily() == org.jclouds.compute.domain.OsFamily.UBUNTU
          || serverImage.getOperatingSystem().getFamily() == org.jclouds.compute.domain.OsFamily.DEBIAN;
    }
    // often that is null (like in vbox) so try to get there by name
    String serverName = serverImage.getName().toLowerCase();
    return serverName.contains("ubuntu") || serverName.contains("debian");
  }

  @Override
  public boolean isClientCompatible(NodeMetadata clientNode) {
    // try to get the os family through metadata
    if (clientNode.getOperatingSystem() != null) {
      return clientNode.getOperatingSystem().getFamily() == org.jclouds.compute.domain.OsFamily.UBUNTU
          || clientNode.getOperatingSystem().getFamily() == org.jclouds.compute.domain.OsFamily.DEBIAN;
    }
    // often that is null (like in vbox) so try to get there by name
    String clientName = clientNode.getHostname().toLowerCase();
    return clientName.contains("ubuntu") || clientName.contains("debian");
  }

}

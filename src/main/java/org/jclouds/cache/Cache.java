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

import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;

/**
 * A factory that encapsulates particular cache repository types (apt,pip,yum,ruby gems etc).
 * 
 * @author David Alves
 * 
 */
public interface Cache {

  /**
   * Returns the name of the package repo that is being cached (e.g. apt, yum etc).
   * 
   * @return
   */
  public String getName();

  /**
   * The server that was prepared to serve this cache repo, or null if none was.
   * 
   * @return
   */
  public NodeMetadata getServer();

  /**
   * Whether a cache repository server can be installed in the provided {@link Image}.
   * 
   * @param os
   * @return
   */
  public boolean isServerCompatible(Image serverNode);

  /**
   * Whether the provided os can be configured to use this cache repository.
   * 
   * @param os
   * @return
   */
  public boolean isClientCompatible(NodeMetadata clientNode);

  /**
   * Does whatever needs to be done to enable the cache repository on the server.
   * 
   * @return
   */
  public void installCacheServerInNode(ComputeServiceContext ctx, NodeMetadata serverNode);

  /**
   * Does whatever needs to be done to enable the cache repository on the client.
   * 
   * @param serverNode
   * @return
   */
  public void enableCacheClientInNode(ComputeServiceContext ctx, NodeMetadata clientNode);

}

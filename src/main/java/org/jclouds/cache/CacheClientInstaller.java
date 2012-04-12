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

import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;

import com.google.common.base.Function;

/**
 * Configures the provided node to use all the available and compatible cache servers.
 * 
 * @author David Alves
 * 
 */
@Singleton
public class CacheClientInstaller extends CacheInstaller implements Function<NodeMetadata, Void> {

  @Inject
  public CacheClientInstaller(ComputeServiceContext ctx, Set<Cache> cacheRepos) {
    super(ctx, cacheRepos);
  }

  @Resource
  @Named(ComputeServiceConstants.COMPUTE_LOGGER)
  protected Logger logger = Logger.NULL;

  @Override
  public Void apply(NodeMetadata input) {
    for (Cache cacheRepo : cacheRepos) {
      if (cacheRepo.isClientCompatible(input)) {
        logger.info("Enabling local %s cache on node: %s", cacheRepo.getName(), input);
        cacheRepo.enableCacheClientInNode(ctx, input);
      }
    }
    return null;
  }
}

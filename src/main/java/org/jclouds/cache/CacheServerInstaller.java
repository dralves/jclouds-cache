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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;

/**
 * Installs all compatible cache repositories in a server with the provided template. If no server exists it is booted
 * up. All server scripts corresponding to all registered cache repositories are executed on the server.
 * 
 * @author David Alves
 * 
 */
@Singleton
public class CacheServerInstaller extends CacheInstaller implements Function<Template, Void> {

  @Inject
  public CacheServerInstaller(ComputeServiceContext ctx, Set<Cache> cacheRepos) {
    super(ctx, cacheRepos);
  }

  @Resource
  @Named(ComputeServiceConstants.COMPUTE_LOGGER)
  protected Logger    logger = Logger.NULL;

  @VisibleForTesting
  public NodeMetadata cacheServer;

  @Override
  public Void apply(Template input) {
    try {
      cacheServer = getCacheServer(input);
    } catch (RunNodesException e) {
      throw Throwables.propagate(e);
    }
    for (Cache repo : cacheRepos) {
      logger.info("Installing cache repository server %s on node: %s", repo.getName(), cacheServer);
      if (repo.isServerCompatible(input.getImage())) {
        repo.installCacheServerInNode(ctx, cacheServer);
      }
    }
    return null;
  }

  private NodeMetadata getCacheServer(final Template template) throws RunNodesException {
    try {
      return Iterables.getOnlyElement(ctx.getComputeService().listNodesDetailsMatching(
          new Predicate<ComputeMetadata>() {
            @Override
            public boolean apply(ComputeMetadata input) {
              checkArgument(input instanceof NodeMetadata);
              NodeMetadata node = (NodeMetadata) input;
              return node.getGroup().startsWith(CACHE_SERVER_GROUP)
                  && node.getGroup()
                      .endsWith(template.getImage().getOperatingSystem().getFamily().name().toLowerCase());
            }
          }));
    } catch (NoSuchElementException e) {
      logger.info("No cache server was found. Creating a new one [Template: %s ]", template);
      NodeMetadata node = Iterables.getOnlyElement(ctx.getComputeService().createNodesInGroup(
          CACHE_SERVER_GROUP + "-" + template.getImage().getOperatingSystem().getFamily().name().toLowerCase(), 1,
          template));
      logger.info("New cache server node created: %s ", node);
      ctx.getComputeService().runScriptOnNode(node.getId(), AdminAccess.standard());
      return node;
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Found multiple cache servers!");
    }
  }
}

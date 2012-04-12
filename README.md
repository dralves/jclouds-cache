#Package caching tool to speed up jclouds deployments

*Rationale*: Getting multiple nodes ready to work in different clouds requires downloading a good amount of files from different packaging systems such as apt, pip, gems or yum. This often increases deployments times and may incur in additional cost. 

This tools spins up appropriate servers for the different packaging systems. For instance it might spin up an ubuntu cache server to speed up ubuntu deployments by running apt-cacher, a pip mirror and a ruby gems mirror. The tool also runs scripts on the new spun-up nodes to make sure they use the cache servers.

#Usage

>// some context that includes PackageCachingModule   
>ComputeServiceContext                ctx;  
>CacheServerInstaller serverInstaller = ctx.utils().injector().getBinding(CacheServerInstaller.class);   
>
>// boot a cache server with the default template. If one exists that one is used.   
>// install and start all package caching systems that work on that template   
>serverInstaller.apply(ctx.getComputeService().templateBuilder().build());   
>
>// Now spin up some nodes   
>Set<NodeMetadata> nodes = ctx.createNodesInGroup("my_group",3);   
>
>// And make sure they use the cache server   
>CacheClientInstaller clientInstaller = ctx.utils().injector().getBinding(CacheClientInstaller.class);   
>Iterables.transform(nodes,clientInstaller);

For better results keep the cache server around, turning it off when not needed.

#Supported packaging systems
- Apt

#Note
The tool is in pre-alpha state please use and report any issues.
The live test leaves node dangling for the moment so remember to terminate.

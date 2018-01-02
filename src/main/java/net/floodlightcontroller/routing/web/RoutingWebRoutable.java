/**
 *    Copyright 2013, Big Switch Networks, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.floodlightcontroller.routing.web;

import net.floodlightcontroller.routing.VirtualSubnetNPTResource;
import org.restlet.Context;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class RoutingWebRoutable implements RestletRoutable {
    /**
     * Create the Restlet router and bind to the proper resources.
     */
    @Override
    public Router getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("/path/{src-dpid}/{src-port}/{dst-dpid}/{dst-port}/json", PathResource.class);
        router.attach("/paths/{src-dpid}/{dst-dpid}/{num-paths}/json", PathsResource.class);
        router.attach("/paths/fast/{src-dpid}/{dst-dpid}/{num-paths}/json", PathsResource.class);
        router.attach("/paths/slow/{src-dpid}/{dst-dpid}/{num-paths}/json", PathsResource.class);
        router.attach("/metric/json", PathMetricsResource.class);
        router.attach("/paths/force-recompute/json", ForceRecomputeResource.class);
        router.attach("/paths/max-fast-paths/json", MaxFastPathsResource.class);

        router.attach("/gateway/list/{gateway-name}/json", VirtualGatewayInfoResource.class);
        router.attach("/gateway/clear/{gateway-name}/json", VirtualGatewayResource.class);
        router.attach("/gateway/json", VirtualGatewayResource.class);

        router.attach("/gateway/{gateway-name}/list/{interface-name}/json", VirtualInterfaceResource.class);
        router.attach("/gateway/{gateway-name}/clear/{interface-name}/json", VirtualInterfaceResource.class);
        router.attach("/gateway/{gateway-name}/interface/json", VirtualInterfaceResource.class);

        router.attach("/subnet/list/{subnet-name}/json", VirtualSubnetInfoResource.class);
        router.attach("/subnet/switches/json", VirtualSubnetSwitchResource.class);
        router.attach("/subnet/switches/clear/{subnet-name}/json", VirtualSubnetSwitchResource.class);
        router.attach("/subnet/node-port-tuples/json", VirtualSubnetNPTResource.class);
        router.attach("/subnet/node-port-tuples/clear/{subnet-name}/json", VirtualSubnetNPTResource.class);

        return router;
    }

    /**
     * Set the base path for routing service
     */
    @Override
    public String basePath() {
        return "/wm/routing";
    }
}

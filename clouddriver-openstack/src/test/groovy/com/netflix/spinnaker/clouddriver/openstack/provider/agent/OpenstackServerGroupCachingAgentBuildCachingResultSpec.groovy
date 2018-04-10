package com.netflix.spinnaker.clouddriver.openstack.provider.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spectator.api.Registry
import com.netflix.spectator.api.Timer
import com.netflix.spinnaker.cats.cache.RelationshipCacheFilter
import com.netflix.spinnaker.cats.provider.ProviderCache
import com.netflix.spinnaker.clouddriver.openstack.cache.CacheResultBuilder
import com.netflix.spinnaker.clouddriver.openstack.client.OpenstackClientProvider
import com.netflix.spinnaker.clouddriver.openstack.security.OpenstackCredentials
import com.netflix.spinnaker.clouddriver.openstack.security.OpenstackNamedAccountCredentials
import org.openstack4j.model.heat.Stack
import org.openstack4j.model.network.ext.LoadBalancerV2StatusTree
import org.openstack4j.model.network.ext.status.LbPoolV2Status
import org.openstack4j.model.network.ext.status.ListenerV2Status
import org.openstack4j.model.network.ext.status.LoadBalancerV2Status
import org.openstack4j.model.network.ext.status.MemberV2Status
import org.openstack4j.openstack.heat.domain.HeatStack
import spock.lang.Specification

import static com.netflix.spinnaker.clouddriver.openstack.cache.Keys.Namespace.INSTANCES

class OpenstackServerGroupCachingAgentBuildCachingResultSpec extends Specification {

  OpenstackServerGroupCachingAgent cachingAgent
  OpenstackClientProvider provider
  OpenstackNamedAccountCredentials accountCredentials
  ObjectMapper objectMapper = new ObjectMapper()
  String region = "east1"
  String account = "test"

  void "setup"() {
    provider = Mock(OpenstackClientProvider)
    OpenstackCredentials credentials = Mock(OpenstackCredentials) {
      getProvider() >> provider
    }
    accountCredentials = Mock(OpenstackNamedAccountCredentials) {
      getCredentials() >> credentials
      getName() >> account
    }
    Registry registry = Stub(Registry) {
      timer(_, _) >> Mock(Timer)
    }
    cachingAgent = new OpenstackServerGroupCachingAgent(accountCredentials, region, objectMapper, registry)
  }

  void "test build caching result"() {
    given:
    ProviderCache providerCache = Mock(ProviderCache)
    CacheResultBuilder cacheResultBuilder = new CacheResultBuilder()
    String stackId = "stackId"
    String name = "app-stack-detail-v000"
    String loadBalancerId = "lbId"
    Stack stack = Mock(Stack) {
      getId() >> { stackId }
      getName() >> { name }
    }
    Collection<String> instanceKeys = ["openstack:instances:test:east1:test1", "openstack:instances:test:east1:test2"]
    MemberV2Status ipv4MemberStatus = Mock(MemberV2Status) {
      getAddress() >> "172.0.0.1"
      getOperatingStatus() >> "DISABLED"
    }
    MemberV2Status ipv6MemberStatus = Mock(MemberV2Status) {
      getAddress() >> "0:0:0:0:0:0:0:1"
      getOperatingStatus() >> "ENABLED"
    }
    List<MemberV2Status> memberV2Statuses = [ipv4MemberStatus, ipv6MemberStatus]
    List<LbPoolV2Status> poolStatuses = [Mock(LbPoolV2Status) {
      getMemberStatuses() >> memberV2Statuses
    }]
    List<ListenerV2Status> listenerStatuses = [Mock(ListenerV2Status) {
      getLbPoolV2Statuses() >> poolStatuses
    }]

    when:
    cachingAgent.buildCacheResult(providerCache, cacheResultBuilder, [stack])

    then:
    1 * providerCache.getAll(INSTANCES.ns, instanceKeys, _ as RelationshipCacheFilter) >> []
    1 * provider.getStack(region, name) >> Mock(HeatStack) {
      getName() >> name
      getParameters() >> ["load_balancers":"'[u''${loadBalancerId}'']'"] // TODO: See https://bugs.launchpad.net/heat/+bug/1613415
    }
    1 * provider.getLoadBalancerStatusTree(region, loadBalancerId) >> Mock(LoadBalancerV2StatusTree) {
      getLoadBalancerV2Status() >> Mock(LoadBalancerV2Status) {
        getName() >> "lbName"
        getId() >> loadBalancerId
        getListenerStatuses() >> listenerStatuses
      }
    }
    1 * provider.getInstanceIdsForStack(region, name) >> ["test1", "test2"]
    noExceptionThrown()
  }

}

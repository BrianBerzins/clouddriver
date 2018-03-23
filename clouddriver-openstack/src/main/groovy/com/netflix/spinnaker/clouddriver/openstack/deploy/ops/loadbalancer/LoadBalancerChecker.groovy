package com.netflix.spinnaker.clouddriver.openstack.deploy.ops.loadbalancer

import com.netflix.spinnaker.clouddriver.openstack.client.BlockingStatusChecker
import com.netflix.spinnaker.clouddriver.openstack.config.OpenstackConfigurationProperties.LbaasConfig
import com.netflix.spinnaker.clouddriver.openstack.deploy.exception.OpenstackProviderException
import org.openstack4j.model.network.ext.LbProvisioningStatus
import org.openstack4j.model.network.ext.LoadBalancerV2

class LoadBalancerChecker implements BlockingStatusChecker.StatusChecker<LoadBalancerV2> {
  Operation operation

  enum Operation {
    CREATE,
    UPDATE,
    DELETE
  }

  LoadBalancerChecker(Operation operation) {
    this.operation = operation
  }

  @Override
  boolean isReady(LoadBalancerV2 loadBalancer) {
    if (!loadBalancer) {
      if (operation == Operation.DELETE) {
        return true
      }
      throw new OpenstackProviderException("Cannot get status for null loadbalancer")
    }
    LbProvisioningStatus status = loadBalancer.provisioningStatus
    if (status == LbProvisioningStatus.ERROR) {
      throw new OpenstackProviderException("Error in load balancer provision: ${loadBalancer.name}, ${loadBalancer.id}")
    }
    return status == LbProvisioningStatus.ACTIVE
  }

  static BlockingStatusChecker from(LbaasConfig lbaasConfig, Operation operation) {
    LoadBalancerChecker checker = new LoadBalancerChecker(operation)
    BlockingStatusChecker.from(lbaasConfig.pollTimeout, lbaasConfig.pollInterval, checker)
  }

}

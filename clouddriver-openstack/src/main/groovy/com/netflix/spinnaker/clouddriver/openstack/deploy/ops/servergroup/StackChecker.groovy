package com.netflix.spinnaker.clouddriver.openstack.deploy.ops.servergroup

import com.netflix.spinnaker.clouddriver.openstack.client.BlockingStatusChecker
import com.netflix.spinnaker.clouddriver.openstack.deploy.exception.OpenstackProviderException
import org.openstack4j.model.heat.Stack\

class StackChecker implements BlockingStatusChecker.StatusChecker<Stack> {
  Operation operation

  enum Operation {
    CREATE,
    UPDATE,
    DELETE
  }

  StackChecker (Operation operation) {
    this.operation = operation
  }

  @Override
  boolean isReady(Stack stack) {
    if (!stack) {
      if (operation == Operation.DELETE) {
        return true
      }
      throw new OpenstackProviderException("Cannot get state for null stack")
    }

    switch (stack.status) {
      case operation.toString() + "_IN_PROGRESS":
        return false
      case operation.toString() + "_FAILED":
        throw new OpenstackProviderException("Failed to create stack ${stack.name}: ${stack.stackStatusReason}")
      case operation.toString() + "_COMPLETE":
        return true
      default:
        throw new OpenstackProviderException("Unknown status for stack ${stack.name}: ${stack.status} ${stack.stackStatusReason}")
    }
  }
}

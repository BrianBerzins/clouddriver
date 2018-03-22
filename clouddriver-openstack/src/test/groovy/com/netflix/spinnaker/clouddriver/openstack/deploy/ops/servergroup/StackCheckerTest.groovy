package com.netflix.spinnaker.clouddriver.openstack.deploy.ops.servergroup

import com.netflix.spinnaker.clouddriver.openstack.deploy.exception.OpenstackProviderException
import org.openstack4j.openstack.heat.domain.HeatStack
import spock.lang.Specification

class StackCheckerTest extends Specification {

  def "should return true when heat stack status is CREATE_COMPLETE"() {
    given:
    def checker = new StackChecker(StackChecker.Operation.CREATE)
    when:
    def ready = checker.isReady(new HeatStack(status: "CREATE_COMPLETE"))
    then:
    ready
  }

  def "should return false when heat stack status is CREATE_IN_PROGRESS"() {
    given:
    def checker = new StackChecker(StackChecker.Operation.CREATE)
    when:
    def ready = checker.isReady(new HeatStack(status: "CREATE_IN_PROGRESS"))
    then:
    !ready
  }

  def "should thrown an exception when heat stack status is CREATE_FAILED"() {
    given:
    def checker = new StackChecker(StackChecker.Operation.CREATE)
    when:
    checker.isReady(new HeatStack(status: "CREATE_FAILED"))
    then:
    thrown(OpenstackProviderException)
  }

  def "should thrown an exception when heat stack status is unknown"() {
    given:
    def checker = new StackChecker(StackChecker.Operation.CREATE)
    when:
    checker.isReady(new HeatStack(status: "UNKNOWN_STATUS"))
    then:
    thrown(OpenstackProviderException)
  }

  def "should thrown an exception when stack is null"() {
    given:
    def checker = new StackChecker(StackChecker.Operation.CREATE)
    when:
    checker.isReady(null)
    then:
    thrown(OpenstackProviderException)
  }

  def "should return true when stack is null but operation is delete"() {
    given:
    def checker = new StackChecker(StackChecker.Operation.DELETE)
    when:
    def ready = checker.isReady(null)
    then:
    ready
    noExceptionThrown()
  }

}

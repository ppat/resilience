package io.latent.resilience

import com.netflix.hystrix.HystrixCommand.Setter
import com.netflix.hystrix._
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy

/**
 * Hystrix Config
 *
 * It is preferrable to configure the optional values via properties as documented on the following link.
 * @see https://github.com/Netflix/Hystrix/wiki/Configuration
 *
 * @author peter@latent.io
 */
case class HystrixConfig(group: String,
                         command: String,
                         timeout: Option[Int] = None,
                         isolation: Option[ExecutionIsolationStrategy] = None,
                         isolationSemaphoreMax: Option[Int] = None,
                         threadPoolSize: Option[Int] = None) {

  private[resilience] def setter: Setter = {
    val setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(group))
                       .andCommandKey(HystrixCommandKey.Factory.asKey(command))
                       .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(command))

    val commandProperties = HystrixCommandProperties.Setter()
    for (timeoutInMilliseconds <- timeout) {
      commandProperties.withExecutionIsolationThreadTimeoutInMilliseconds(timeoutInMilliseconds)
    }
    for (isolationStrategy <- isolation) {
      commandProperties.withExecutionIsolationStrategy(isolationStrategy)
    }
    for (semaphoreMax <- isolationSemaphoreMax) {
      commandProperties.withExecutionIsolationSemaphoreMaxConcurrentRequests(semaphoreMax)
    }
    setter.andCommandPropertiesDefaults(commandProperties)

    val threadPoolProperties = HystrixThreadPoolProperties.Setter()
    for (poolSize <- threadPoolSize) {
      threadPoolProperties.withCoreSize(poolSize)
    }
    setter.andThreadPoolPropertiesDefaults(threadPoolProperties)

    setter
  }
}

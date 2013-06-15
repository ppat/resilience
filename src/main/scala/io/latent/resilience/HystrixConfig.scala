package io.latent.resilience

import com.netflix.hystrix.HystrixCommand.Setter
import com.netflix.hystrix._

/**
 * Hystrix Config
 *
 * @see https://github.com/Netflix/Hystrix/wiki/Configuration
 *
 * @author peter@latent.io
 */
case class HystrixConfig(commandGroup: String,
                         commandType: String,
                         timeout: Option[Int] = None,
                         threadPoolSize: Option[Int] = None) {

  private[resilience] def setter: Setter = {
    val setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(commandGroup))
                       .andCommandKey(HystrixCommandKey.Factory.asKey(commandType))
                       .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(commandType))

    for (timeoutInMilliseconds <- timeout) {
      val timed = HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(timeoutInMilliseconds)
      setter.andCommandPropertiesDefaults(timed)
    }
    for (poolSize <- threadPoolSize) {
      val pooled = HystrixThreadPoolProperties.Setter().withCoreSize(poolSize)
      setter.andThreadPoolPropertiesDefaults(pooled)
    }

    setter
  }
}

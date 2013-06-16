package io.latent.resilience

import com.google.common.cache.{Cache, CacheBuilder}
import java.util.concurrent.TimeUnit
import HystrixDSL._
import LastKnownGoodDSL._

/**
 * Will execute the given request and store the results in a Last Known Good store. If the same query fail in consequent
 * attempts, it will fallback to Last Known Good values from the store.
 *
 * This is intended for use with queries returning small data sets. Its intended for keeping application configuration
 * or application state but not user state or user data.
 *
 * @author peter@latent.io
 */
object LastKnownGoodDSL {
  private[resilience] val DEFAULT_EXPIRATION = 24 * 60 * 60 * 1000

  def LastKnownGood[K, V](key: K)(implicit config: LastKnownGoodConfig) = new Object {
    require(config.maxSize <= 250, "Last Known Good Query is intended for use with small data sets")

    // google, why do you not let me create a cache typed to [K, V]?
    val cache: Cache[Object, Object] = CacheBuilder.newBuilder()
                                                   .concurrencyLevel(1)
                                                   .maximumSize(config.maxSize)
                                                   .expireAfterWrite(config.expiration, TimeUnit.MILLISECONDS)
                                                   .build()

    /** execute the given query and fallback to last known good value if execution fails */
    def maintain(query: => V): V = {
      implicit val hystrixConfig = config.hystrixConfig
      gracefully {
        val result = query
        cache.put(key.asInstanceOf[Object], result.asInstanceOf[Object])
        result
      } fallback {
        state
      }
    }

    /** retrieve last known good value if available */
    def state: V = {
      cache.getIfPresent(key).asInstanceOf[V]
    }
  }
}


case class LastKnownGoodConfig(hystrixConfig: HystrixConfig,
                               maxSize: Int = 50,
                               expiration: Int = DEFAULT_EXPIRATION)

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

  /** execute the given query and fallback to last known good value if execution fails */
  def last(implicit request: LastKnownGoodConfig) = new Object {
    require(request.maxSize <= 250, "Last Known Good Query is intended for use with small data sets")

    // google, why do you not let me create a cache typed to [K, V]?
    val cache: Cache[Object, Object] = CacheBuilder.newBuilder()
                                                   .concurrencyLevel(1)
                                                   .maximumSize(request.maxSize)
                                                   .expireAfterWrite(request.expiration, TimeUnit.MILLISECONDS)
                                                   .build()

    def known = new Object {
      def good[K, V](query: K => V) = new Object {
        def keyedBy[X <: K](key: X): V = {
          gracefully {
            val result = query(key)
            cache.put(key.asInstanceOf[Object], result.asInstanceOf[Object])
            result
          } fallback {
            cache.getIfPresent(key).asInstanceOf[V]
          }
        }
      }
    }
  }
}


case class LastKnownGoodConfig(override val commandGroup: String,
                               override val commandType: String,
                               override val timeout: Option[Int] = None,
                               override val threadPoolSize: Option[Int] = None,
                               maxSize: Int = 50,
                               expiration: Int = DEFAULT_EXPIRATION) extends HystrixConfig(commandGroup, commandType, timeout, threadPoolSize)

package io.latent.resilience

import io.latent.resilience.cache.Cache
import io.latent.resilience.HystrixDSL._

/**
 * A DSL for building local, in-memory caches intended for use with queries returning small data sets.
 *
 * @author peter@latent.io
 */
object CacheDSL {
  /** get from cache if exists, otherwise load cache with query and return result. Falling back to None */
  def Cached[K, V](key: K)
                  (query: => V)
                  (implicit cache: Cache[K, V], config: HystrixConfig): Option[V] = {
    gracefully {
      cache.get(key, query)
    } fallback {
      None
    }
  }

  /** execute the given query and fallback to last known good value if execution fails */
  def LastKnownGoodConfig[K, V](key: K)
                               (query: => V)
                               (implicit cache: Cache[K, V], config: HystrixConfig): Option[V] = {
    gracefully {
      val result = query
      cache.put(key, result)
      Option(result)
    } fallback {
      cache.get(key)
    }
  }
}

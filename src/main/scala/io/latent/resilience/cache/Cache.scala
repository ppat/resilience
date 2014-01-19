package io.latent.resilience.cache

import scala.concurrent.Future
import scala.concurrent.duration.Duration

trait Cache[K, V] {
  /** Add to cache */
  def put(key: K, value: Future[V])

  /** Get if exists */
  def get(key: K): Option[Future[V]]

  /** Invalidate cache entry for key if exists */
  def invalidate(key: K): Unit
}

object Cache {
  def simple[K, V](maxSize: Int = 100,
                   timeToLive: Duration = Duration.Inf,
                   timeToIdle: Duration = Duration.Inf,
                   softValues: Boolean = false,
                   weakValues: Boolean = false,
                   weakKeys: Boolean = false): Cache[K, V] = {
    new SimpleCache[K, V](maxSize, timeToLive, timeToIdle, softValues, weakValues, weakKeys)
  }
}
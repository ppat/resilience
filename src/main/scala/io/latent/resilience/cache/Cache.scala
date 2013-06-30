package io.latent.resilience.cache

trait Cache[K, V] {
  /** Add to cache */
  def put(key: K, value: V)

  /** Get if exists */
  def get(key: K): Option[V]

  /** Will automatically load values when not present using the cacheLoader */
  def get(key: K, cacheLoader: => V): Option[V]
}

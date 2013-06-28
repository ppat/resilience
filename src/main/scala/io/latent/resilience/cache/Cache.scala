package io.latent.resilience.cache

trait Cache[K, V] {
  def put(key: K, value: V)

  def get(key: K): Option[V]

  def get(key: K, cacheLoader: => V): Option[V]
}

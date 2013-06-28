package io.latent.resilience.cache

import java.util.concurrent.{Callable, TimeUnit}

import com.google.common.cache.{Cache => GCache, CacheBuilder}
import com.google.common.base.Optional

/**
 * A local, in-memory cache backed by guava.
 *
 * @see com.google.common.cache.CacheBuilder for detailed configuration documentation
 */
class SimpleCache[K, V](config: CacheConfig) extends Cache[K, V] {
  def this() = this(new CacheConfig())

  // when a cacheLoader is not provided Google CacheBuilder types the cache as [Object, Object]
  private val cache: GCache[AnyRef, AnyRef] = create()

  def put(key: K,
          value: V): Unit = {
    if (value != null)
      cache.put(key.asInstanceOf[AnyRef], value.asInstanceOf[AnyRef])
  }

  /** Get if exists */
  def get(key: K): Option[V] = {
    val value = cache.getIfPresent(key)
    if (value == null) None else Some(value.asInstanceOf[V])
  }

  /** Will automatically load values when not present */
  def get(key: K,
          cacheLoader: => V): Option[V] = {
    val value = cache.get(key.asInstanceOf[AnyRef], toCallable(cacheLoader)).asInstanceOf[Optional[V]]
    guavaOptionalToScalaOption(value)
  }


  private def guavaOptionalToScalaOption(optionalValue: Optional[V]): Option[V] = {
    if (optionalValue.isPresent) Some(optionalValue.get()) else None
  }

  private def toCallable(cacheLoader: => V): Callable[AnyRef] = {
    new Callable[AnyRef] {
      def call(): AnyRef = {
        Optional.fromNullable(cacheLoader)
      }
    }
  }

  private def create(): GCache[Object, Object] = {
    CacheBuilder.newBuilder()
                .concurrencyLevel(config.concurrencyLevel)
                .maximumSize(config.maxSize)
                .expireAfterWrite(config.expiration, TimeUnit.MILLISECONDS)
                .build()
  }
}

case class CacheConfig(maxSize: Int = 100,
                       expiration: Int = 24 * 60 * 60 * 1000,
                       concurrencyLevel: Int = 3)

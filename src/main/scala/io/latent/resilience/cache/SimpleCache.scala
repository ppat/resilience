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

  // when a cacheLoader is not provided, Google CacheBuilder types the cache as [AnyRef, AnyRef]
  // which is unfortunate and leads to the mess that creates the need for use of asInstanceOf within
  // this whole class. While the Guava CacheBuilder provides an excellent implementation of a local
  // caching system, it's API is incredibly inelegant. Since it does not allow storage of null within
  // the cache that forces us to use Guava's version of Option(al) as scala.Option cannot be stored
  // within a purely java data structure without consequences. But we can hide all that monstrosity
  // and still leverage the power of the Guava Cache within the SimpleCache by way of encapsulation.
  private val cache: GCache[AnyRef, AnyRef] = create()

  def put(key: K,
          value: V): Unit = {
    cache.put(key.asInstanceOf[AnyRef], Optional.fromNullable(value).asInstanceOf[AnyRef])
  }

  def get(key: K): Option[V] = {
    val value = cache.getIfPresent(key)
    guavaOptionalToScalaOption(value)
  }

  def get(key: K,
          cacheLoader: => V): Option[V] = {
    val value = cache.get(key.asInstanceOf[AnyRef], toCallable(cacheLoader))
    guavaOptionalToScalaOption(value)
  }


  private def guavaOptionalToScalaOption(value: AnyRef): Option[V] = {
    if (value == null) return None
    val optionalValue = value.asInstanceOf[Optional[V]]
    if (optionalValue.isPresent) Some(optionalValue.get()) else None
  }

  private def toCallable(cacheLoader: => V): Callable[AnyRef] = {
    new Callable[AnyRef] {
      def call(): AnyRef = {
        Optional.fromNullable(cacheLoader)
      }
    }
  }

  private def create(): GCache[AnyRef, AnyRef] = {
    var builder = CacheBuilder.newBuilder()
                              .concurrencyLevel(config.concurrencyLevel)
                              .maximumSize(config.maxSize)
                              .expireAfterWrite(config.expiration, TimeUnit.MILLISECONDS)

    if (config.softValues) builder = builder.softValues()
    if (config.weakValues) builder = builder.weakValues()
    if (config.weakKeys) builder = builder.weakKeys()

    builder.build()
  }
}

case class CacheConfig(maxSize: Int = 100,
                       expiration: Int = 24 * 60 * 60 * 1000,
                       concurrencyLevel: Int = 4,
                       softValues: Boolean = false,
                       weakValues: Boolean = false,
                       weakKeys: Boolean = false)

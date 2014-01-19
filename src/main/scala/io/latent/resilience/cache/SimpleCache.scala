package io.latent.resilience.cache

import com.google.common.cache.{Cache => GCache, CacheBuilder}
import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
 * A local, in-memory cache backed by guava.
 *
 * @see com.google.common.cache.CacheBuilder for detailed configuration documentation
 */
class SimpleCache[K, V](maxSize: Int = 100,
                        timeToLive: Duration = Duration.Inf,
                        timeToIdle: Duration = Duration.Inf,
                        softValues: Boolean = false,
                        weakValues: Boolean = false,
                        weakKeys: Boolean = false) extends Cache[K, V] {

  // when a cacheLoader is not provided, Google CacheBuilder types the cache as [AnyRef, AnyRef]
  // which is unfortunate and leads to the mess that creates the need for use of asInstanceOf within
  // this whole class. While the Guava CacheBuilder provides an excellent implementation of a local
  // caching system, it's API is incredibly inelegant. Since it does not allow storage of null within
  // the cache that forces us to use Guava's version of Option(al) as scala.Option cannot be stored
  // within a purely java data structure without consequences. But we can hide all that monstrosity
  // and still leverage the power of the Guava Cache within the SimpleCache by way of encapsulation.
  private val cache: GCache[AnyRef, AnyRef] = create()

  def put(key: K,
          value: Future[V]): Unit = {
    cache.put(key.asInstanceOf[AnyRef], value.asInstanceOf[AnyRef])
  }

  def get(key: K): Option[Future[V]] = {
    cache.getIfPresent(key) match {
      case null => None
      case value => Some(value.asInstanceOf[Future[V]])
    }
  }

  def invalidate(key: K): Unit = {
    cache.invalidate(key.asInstanceOf[AnyRef])
  }

  private def create(): GCache[AnyRef, AnyRef] = {
    var builder = CacheBuilder.newBuilder()
                              .maximumSize(maxSize)

    if (timeToLive.isFinite()) builder = builder.expireAfterWrite(timeToLive.length, timeToLive.unit)
    if (timeToIdle.isFinite()) builder = builder.expireAfterAccess(timeToIdle.length, timeToIdle.unit)
    if (softValues) builder = builder.softValues()
    if (weakValues) builder = builder.weakValues()
    if (weakKeys) builder = builder.weakKeys()

    builder.build()
  }
}

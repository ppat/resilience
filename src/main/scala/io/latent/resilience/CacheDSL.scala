package io.latent.resilience

import io.latent.resilience.cache.Cache
import io.latent.resilience.HystrixDSL._
import scala.concurrent.{ExecutionContext, Future, Promise}
import ExecutionContext.Implicits.global


/**
 * A DSL for building local, in-memory caches intended for use with queries returning small data sets.
 *
 * @author peter@latent.io
 */
object CacheDSL {
  /** get from cache if exists, otherwise load cache with query and return result. Falling back to None */
  def cached[K, V](key: K)
                  (op: => V)
                  (implicit cache: Cache[K, V], config: HystrixConfig): Future[V] = {
    cache.get(key) match {
      case None => {
        val promise = Promise[V]()
        cache.put(key, promise.future)
        val future: Future[V] = buildFuture(op)
        future.onComplete {
          value => {
            promise.complete(value)
            if (value.isFailure) cache.invalidate(key)
          }
        }
        future
      }
      case Some(alreadyPresentFuture) => alreadyPresentFuture
    }
  }

  private def buildFuture[V](op: => V)(implicit config: HystrixConfig): Future[V] = {
    def action(): V = gracefully(op).degrade()
    Future[V](action)
  }
}

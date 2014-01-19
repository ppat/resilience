package io.latent.resilience

import io.latent.resilience.cache.Cache
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.concurrent.duration._

/**
 * A DSL for building local, in-memory caches intended for use with queries returning small data sets.
 *
 * @author peter@latent.io
 */
object CacheDSL {
  /** get from cache if exists, otherwise load cache with query and return result. Falling back to None */
  def cached[K, V](key: K)
                  (op: => V)
                  (implicit cache: Cache[K, V]): Future[V] = {
    cache.get(key) match {
      case None => {
        val promise = Promise[V]()
        cache.put(key, promise.future)
        val future = Future[V](op)
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

  implicit def blockingFuture[V](future: Future[V]) = new Object {
    /** Block for future to complete (for non-asynchronous usage). Fallback will be used if future does not complete in time or if
      * an error occurred and fallbackOnError is true */
    def block(timeout: Duration = 1 second, fallback: => Option[V] = None, fallbackOnError: Boolean = false): Option[V] = {
      Await.ready(future, timeout).value match {
        case Some(Success(value)) => {
          value match {
            case null => None
            case something => Some(something.asInstanceOf[V])
          }
        }
        case Some(Failure(e)) if (!fallbackOnError) => throw e
        case _ => fallback
      }
    }
  }
}

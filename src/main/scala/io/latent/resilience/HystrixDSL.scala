package io.latent.resilience

import com.netflix.hystrix.HystrixCommand
import java.util.concurrent.Future

/**
 * A DSL on Hystrix
 * @see https://github.com/Netflix/Hystrix
 *
 * Hystrix prevents cascading failures or cascading latencies by fallback to another option or failing fast (to speed
 * time to recovery).
 *
 * @author peter@latent.io
 */
object HystrixDSL {
  def gracefully[T](command: => T)(implicit config: HystrixConfig) = new Object {
    /** Gracefully execute one code block and fallback to another code block upon encountering an error */
    def fallback(fallback: => T): T = new GracefulFallback[T](command, fallback, config).execute()

    /** Gracefully degrade by failing fast to enable rapid recovery */
    def degrade(): T = new GracefulDegradation[T](command, config).execute()
  }

  def async[T](command: => T)(implicit config: HystrixConfig): Future[T] = {
    new GracefulDegradation[T](command, config).queue()
  }
}


private class GracefulFallback[T](command: => T, fallback: => T, config: HystrixConfig)
                                  extends HystrixCommand[T](Hystrix.CommandSetter(config)) {
  def run(): T = command

  override def getFallback: T = fallback
}

private class GracefulDegradation[T](command: => T, config: HystrixConfig)
                                     extends HystrixCommand[T](Hystrix.CommandSetter(config)) {
  def run(): T = command
}
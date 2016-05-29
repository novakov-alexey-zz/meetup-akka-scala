package meetup.akka.actor

import java.time.LocalDateTime

import akka.actor.{Actor, ActorPath}
import akka.event.Logging
import meetup.akka.om.{ExecuteOrder, ExecutedQuantity}

import scala.util.Random

class OrderExecutor(orderLogger: ActorPath) extends Actor {
  val log = Logging(context.system, this)
  val execQuantity = 3

  override def receive: Receive = {
    case eo: ExecuteOrder =>
      log.info("Going to execute order = {}", eo)
      val quantities = Seq.fill(execQuantity)(Random.nextInt(eo.quantity / execQuantity))
      quantities.par.foreach { q =>
        context.actorSelection(orderLogger) ! ExecutedQuantity(eo.orderId, q, LocalDateTime.now)
      }

    case m => log.warning("Cannot execute order = {}", m)
  }
}

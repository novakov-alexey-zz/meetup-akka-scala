package meetup.akka.actor

import java.time.LocalDateTime

import akka.actor.{ActorPath, FSM}
import meetup.akka.actor.OrderExecutor.{batchSize, execQuantity}
import meetup.akka.om.{ExecuteOrder, ExecutedQuantity}

import scala.util.Random

sealed trait State

case object Idle extends State

case object Active extends State

sealed trait Data

case object Uninitialized extends Data

final case class PendingBatch(queue: Seq[ExecuteOrder]) extends Data

object OrderExecutor {
  val execQuantity = 3
  val batchSize = 10
}

class OrderExecutor(orderLogger: ActorPath) extends FSM[State, Data] {
  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(eo: ExecuteOrder, Uninitialized) => goto(Active) using PendingBatch(Seq(eo))
  }

  when(Active) {
    case Event(eo: ExecuteOrder, b@PendingBatch(q)) if q.length < batchSize =>
      b.copy(queue = q :+ eo) match {
        case ub@PendingBatch(uq) if uq.length == batchSize =>
          flush(uq)
          goto(Idle) using Uninitialized
        case ub =>
          log.info("new message added = {}", eo)
          stay using ub
      }
  }

  onTransition {
    case Idle -> Active =>
      nextStateData match {
        case PendingBatch(q) => log.info("New batch created, first message = {}", q.head)
      }
  }

  private def flush(queue: Seq[ExecuteOrder]): Unit = {
    log.info("Going to execute next queue of orders = {}", queue)
    queue foreach { eo =>
      val quantities = Seq.fill(execQuantity)(Random.nextInt(eo.quantity / execQuantity))
      quantities.par.foreach { q =>
        context.actorSelection(orderLogger) ! ExecutedQuantity(eo.orderId, q, LocalDateTime.now)
      }
    }
  }
}

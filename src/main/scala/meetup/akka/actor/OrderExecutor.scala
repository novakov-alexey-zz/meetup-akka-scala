package meetup.akka.actor

import java.time.LocalDateTime

import akka.actor.{ActorPath, FSM}
import meetup.akka.actor.OrderExecutor.execQuantity
import meetup.akka.om.{AllAcksReceived, ExecuteOrder, ExecutedQuantity, ExecutionAck}

import scala.util.Random

sealed trait State

case object Idle extends State

case object Active extends State

case object Completion extends State

sealed trait Data

case object Uninitialized extends Data

final case class PendingBatch(queue: Seq[ExecuteOrder]) extends Data

final case class AckBatch(replies: Seq[ExecutionAck], remaining: Long) extends Data

object OrderExecutor {
  val execQuantity = 3
}

class OrderExecutor(orderLogger: ActorPath, batchSize: Int) extends FSM[State, Data] {
  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(eo: ExecuteOrder, _) => goto(Active) using PendingBatch(Seq(eo))
  }

  when(Active) {
    case Event(eo: ExecuteOrder, b@PendingBatch(q)) if q.length < batchSize =>
      b.copy(queue = q :+ eo) match {
        case ub@PendingBatch(uq) if uq.length == batchSize =>
          flush(uq)
          goto(Completion) using AckBatch(Seq(), batchSize * execQuantity)
        case ub =>
          log.info("new message is added = {}", eo)
          stay using ub
      }
  }

  when(Completion) {
    case Event(e: ExecutionAck, batch@AckBatch(rs, r)) =>
      batch.copy(rs :+ e, r - 1) match  {
        case uBatch@AckBatch(replies, nr) if nr == 0 =>
          context.actorSelection(orderLogger) ! AllAcksReceived(replies)
          goto(Idle) using Uninitialized
        case uBatch =>
          log.info("new Ack is added = {}", e)
          stay using uBatch
      }
  }

  //handler is not stacked
  whenUnhandled {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay replying Completion
  }

  onTransition {
    case Idle -> Active =>
      nextStateData match {
        case PendingBatch(q) => log.info("New batch created, first message = {}", q.head)
        case _ => log.error("State is not applicable")
      }

    case Completion -> Idle => log.info("all ACKs are received, going to the next round")
  }

  private def flush(queue: Seq[ExecuteOrder]): Unit = {
    log.info("Going to execute next queue of orders = {}", queue)
    val orderLoggerSelection = context.actorSelection(orderLogger)

    queue foreach { eo =>
      val quantities = Seq.fill(execQuantity)(Random.nextInt(eo.quantity / execQuantity))
      quantities.par.foreach { q =>
        orderLoggerSelection ! ExecutedQuantity(eo.orderId, q, LocalDateTime.now)
      }
    }
  }
}

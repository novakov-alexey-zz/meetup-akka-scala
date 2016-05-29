package meetup.akka.actor

import akka.actor.Actor
import akka.event.Logging
import meetup.akka.om.{Order, PreparedOrder}

class OrderIdGenerator extends Actor {
  val log = Logging(context.system, this)
  private var seqNo: Long = 0

  override def receive: Receive = {
    case o: Order ⇒
      val preparedOrder = PreparedOrder(o, nextSeqNo())
      log.info("Id generated = {}", preparedOrder)
      sender ! preparedOrder
  }

  def nextSeqNo(): Long = {
    seqNo += 1
    seqNo
  }
}

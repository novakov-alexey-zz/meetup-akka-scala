package meetup.akka.actor

import akka.actor.Actor
import meetup.akka.om.{Order, PreparedOrder}

class OrderIdGenerator extends Actor {
  private var seqNo: Long = 0

  override def receive: Receive = {
    case o: Order ⇒ sender() ! PreparedOrder(o, nextSeqNo())
  }

  def nextSeqNo(): Long = {
    seqNo += 1
    seqNo
  }
}

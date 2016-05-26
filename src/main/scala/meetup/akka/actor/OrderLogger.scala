package meetup.akka.actor

import akka.actor.Actor
import akka.event.Logging
import meetup.akka.dal.IOrderDao
import meetup.akka.om._

import scala.util.Random

class OrderLogger(orderDao: IOrderDao) extends Actor {
  val log = Logging(context.system, this)

  override def receive: Receive = {
    case p@PreparedOrderForAck(deliveryId: Long, preparedOrder: PreparedOrder) ⇒
      //randomFail(p)
      log.info("order to be persisted = {}", p)
      val order = new Order(preparedOrder.orderId, preparedOrder.order)
      orderDao.saveOrder(order)
      log.info("order saved = {}", order)
      sender ! LoggedOrder(deliveryId, order)

    case c: CompleteBatch ⇒
      orderDao.completeBatch(c.upToId, c.withDate)
      log.info("Batch completed.")
      sender ! BatchCompleted(c.upToId)
  }

  private def randomFail(p: PreparedOrderForAck) =
    if (Random.nextInt % 2 == 0) throw new RuntimeException("random fail on message: " + p)
}

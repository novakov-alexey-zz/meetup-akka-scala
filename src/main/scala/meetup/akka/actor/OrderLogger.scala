package meetup.akka.actor

import akka.actor.Actor
import akka.event.Logging
import meetup.akka.dal.IOrderDao
import meetup.akka.om._

import scala.util.Random

class OrderLogger(orderDao: IOrderDao, randomFail: Boolean) extends Actor {
  val log = Logging(context.system, this)

  @scala.throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    log.error(reason, "I am going to be restarted")
  }

  override def receive: Receive = {
    case p@LogOrder(deliveryId, preparedOrder) ⇒
      if (randomFail) randomFail(p)
      log.info("order to be persisted = {}", p)
      val order = new Order(preparedOrder.orderId, preparedOrder.order)
      orderDao.saveOrder(order)
      log.info("order saved = {}", order)
      sender ! LoggedOrder(deliveryId, order)

    case eq: ExecutedQuantity =>
      orderDao.insertExecution(Execution(eq.orderId, eq.quantity, eq.executionDate))
      log.info("saved execution = {}", eq)

    case c: CompleteBatch ⇒
      orderDao.completeBatch(c.upToId, c.withDate)
      log.info("Batch completed.")
      sender ! BatchCompleted(c.upToId)
  }

  private def randomFail(p: LogOrder) =
    if (Random.nextInt % 2 == 0) throw new UnsupportedOperationException("random fail on message: " + p)
}

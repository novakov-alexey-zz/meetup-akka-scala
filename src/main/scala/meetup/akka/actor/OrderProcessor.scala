package meetup.akka.actor

import akka.actor.{ActorPath, ActorRef, Props}
import akka.event.Logging
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import akka.routing.RoundRobinPool
import meetup.akka.dal.IOrderDao
import meetup.akka.om._

class OrderProcessor(orderDao: IOrderDao, orderIdGeneratorActor: Option[ActorRef] = None, orderLoggerActor: Option[ActorPath] = None)
  extends PersistentActor with AtLeastOnceDelivery {

  val log = Logging(context.system, this)

  val orderIdGenerator = orderIdGeneratorActor.getOrElse(context.actorOf(Props[OrderIdGenerator], "orderIdGenerator"))
  val loggerInst = 5
  val orderLogger = orderLoggerActor
    .getOrElse(context.actorOf(RoundRobinPool(loggerInst).props(Props(classOf[OrderLogger], orderDao)), "orderLogger").path)

  override def receiveRecover: Receive = {
    case m ⇒ generateOrderId(m)
  }

  override def receiveCommand: Receive = {
    case newOrder: NewOrder ⇒
      log.info("New order received. Going to generate an id: {}", newOrder)
      persist(newOrder)(generateOrderId)

    case preparedOrder@PreparedOrder(order, orderId) ⇒
      log.info("Prepared order received with id = {}, {}", orderId, order)
      persist(preparedOrder)(updateState)

    case loggedOrder: LoggedOrder ⇒
      log.info("Logging confirmation received for order: {}", loggedOrder)
      updateState(loggedOrder)
      log.info("Delivery confirmed for order = {}", loggedOrder)

    case c: CompleteBatch ⇒
      log.info("Going to complete batch.")
      context.actorSelection(orderLogger) ! c
  }

  override def persistenceId: String = "orders"

  private def generateOrderId(event: Any) = event match {
    case newOrder: NewOrder ⇒ orderIdGenerator ! newOrder.order
    case m ⇒ unhandled(m)
  }

  private def updateState(event: Any): Unit = event match {
    case p: PreparedOrder => deliver(orderLogger)(deliveryId ⇒ PreparedOrderForAck(deliveryId, p))
    case loggedOrder: LoggedOrder => confirmDelivery(loggedOrder.deliveryId)
    case m ⇒ log.error("Cannot update state for message: {}", m)
  }
}
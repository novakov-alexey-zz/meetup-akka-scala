package meetup.akka.actor

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{ActorPath, ActorRef, OneForOneStrategy, Props}
import akka.event.Logging
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import akka.routing.RoundRobinPool
import meetup.akka.dal.IOrderDao
import meetup.akka.om._

import scala.concurrent.duration._

class OrderProcessor(orderDao: IOrderDao, orderIdGeneratorActor: Option[ActorRef],
                     orderLoggerActor: Option[ActorPath], orderExecutorActor: Option[ActorRef])
  extends PersistentActor with AtLeastOnceDelivery {

  val orderIdGenerator = orderIdGeneratorActor.getOrElse(context.actorOf(Props[OrderIdGenerator], "orderIdGenerator"))
  val orderLogger = orderLoggerActor.getOrElse(
    context.actorOf(RoundRobinPool(nrOfInstances = 5).props(Props(classOf[OrderLogger], orderDao)), "orderLogger").path)
  val orderExecutor = orderExecutorActor.getOrElse(context.actorOf(Props(classOf[OrderExecutor], orderLogger)))
  val log = Logging(context.system, this)

  override def supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: UnsupportedOperationException => Resume
    case _: NullPointerException => Restart
    case _: IllegalArgumentException => Stop
    case _: Exception => Escalate
  }

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
      updateState(loggedOrder)
      log.info("Delivery confirmed for order = {}", loggedOrder)
      orderExecutor ! ExecuteOrder(loggedOrder.order.orderId, loggedOrder.order.quantity)

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
    case p: PreparedOrder => deliver(orderLogger)(deliveryId ⇒ LogOrder(deliveryId, p))
    case loggedOrder: LoggedOrder => confirmDelivery(loggedOrder.deliveryId)
    case m ⇒ log.error("Cannot update state for message: {}", m)
  }
}
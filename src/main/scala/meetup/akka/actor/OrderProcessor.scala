package meetup.akka.actor

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{ActorPath, ActorRef, OneForOneStrategy, Props}
import akka.event.Logging
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}
import akka.routing.RoundRobinPool
import meetup.akka.dal.IOrderDao
import meetup.akka.om._

import scala.concurrent.duration._

class OrderProcessor(orderDao: IOrderDao, idGeneratorActor: Option[ActorRef], loggerActor: Option[ActorPath],
                     executorActor: Option[ActorRef]) extends PersistentActor with AtLeastOnceDelivery {

  val idGenerator = idGeneratorActor.getOrElse(context.actorOf(Props[OrderIdGenerator], "orderIdGenerator"))
  val logger = loggerActor.getOrElse(context.actorOf(RoundRobinPool(nrOfInstances = 5)
    .props(Props(classOf[OrderLogger], orderDao, true)), "orderLogger").path)
  val executor = executorActor.getOrElse(context.actorOf(Props(classOf[OrderExecutor], logger)))
  val log = Logging(context.system, this)

  override def supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: UnsupportedOperationException ⇒ Resume
    case _: NullPointerException ⇒ Restart
    case _: IllegalArgumentException ⇒ Stop
    case _: Exception ⇒ Escalate
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
      executor ! ExecuteOrder(loggedOrder.order.orderId, loggedOrder.order.quantity)

    case c: CompleteBatch ⇒
      log.info("Going to complete batch.")
      context.actorSelection(logger) ! c
  }

  override def persistenceId: String = "orders"

  override def receiveRecover: Receive = {
    case m ⇒ generateOrderId(m)
  }

  private def generateOrderId(event: Any) = event match {
    case newOrder: NewOrder ⇒ idGenerator ! newOrder.order
    case m ⇒ unhandled(m)
  }

  private def updateState(event: Any): Unit = event match {
    case p: PreparedOrder => deliver(logger)(deliveryId ⇒ LogOrder(deliveryId, p))
    case loggedOrder: LoggedOrder => confirmDelivery(loggedOrder.deliveryId)
    case m ⇒ log.error("Cannot update state for message: {}", m)
  }
}
package meetup.akka.service

import akka.actor.{ActorSystem, Props}
import meetup.akka.actor.OrderProcessor
import meetup.akka.dal.{IOrderDao, OrderDaoImpl}
import meetup.akka.om.{CompleteBatch, NewOrder, Order}
import meetup.akka.service.OrderUtil.generateRandomOrder

class OrderGateway(orderDao: IOrderDao) {
  private val system = ActorSystem("OrderGatewaySystem")
  val orderProcessor = system.actorOf(Props.create(classOf[OrderProcessor], orderDao), "orderProcessor")

  def placeOrder: Order = {
    val order = generateRandomOrder
    orderProcessor ! new NewOrder(order)
    order
  }

  def completeBatch(upToId: Int) = orderProcessor ! CompleteBatch(upToId)

  def stop() = system.terminate()
}

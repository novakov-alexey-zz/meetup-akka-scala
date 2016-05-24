package meetup.akka.service

import java.time.LocalDateTime

import akka.actor.{ActorSystem, Props}
import meetup.akka.actor.OrderProcessor
import meetup.akka.dal.IOrderDao
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

  def completeBatch(upToId: Int, withDate: LocalDateTime) = orderProcessor ! CompleteBatch(upToId, withDate)

  def stop() = system.terminate()
}

package meetup.akka

import meetup.akka.dal.IOrderDao
import meetup.akka.om.Order
import meetup.akka.service.OrderGateway

import scala.annotation.tailrec
import scala.collection.parallel.immutable.ParRange


object Starter extends App {
  val orderDao = Config.injector.getInstance(classOf[IOrderDao])
  val orders = 10
  val orderGateway = new OrderGateway(orderDao)

  placeOrders()
  checkOrdersInStorage(orderDao.getOrders)
  completeBatch()
  orderGateway.stop()

  def placeOrders() = {
    ParRange(1, orders, 1, inclusive = true) foreach (i => orderGateway.placeOrder)
  }

  @tailrec
  def checkOrdersInStorage(orderSeq: Seq[Order]): Unit = {
    if (orderSeq.length < orders) {
      Thread.sleep(2000)
      checkOrdersInStorage(orderDao.getOrders)
    } else {
      println("\nOrders are in storage: ")
      orderSeq.foreach(println)
    }
  }

  def completeBatch() = {
    orderGateway.completeBatch(10)
    Thread.sleep(3000)
  }
}

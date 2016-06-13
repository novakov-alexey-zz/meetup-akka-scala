package meetup.akka.actor

import java.time.LocalDateTime

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit}
import meetup.akka.dal.IOrderDao
import meetup.akka.om._
import meetup.akka.service.OrderUtil
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpecLike, Matchers}

class OrderLoggerTest extends TestKit(ActorSystem("testSystem")) with FlatSpecLike with Matchers with MockFactory {

  it should "save order into database" in {
    //given
    val orderDao = stub[IOrderDao]
    val orderLoggerRef = testActor(orderDao)
    val generatedOrder = OrderUtil.generateRandomOrder

    //when
    orderLoggerRef ! LogOrder(1, PreparedOrder(generatedOrder, 2))

    //then
    orderDao.saveOrder _ verify new Order(2L, generatedOrder) once()
  }

  it should "save execution into database" in {
    //given
    val orderDao = stub[IOrderDao]
    val orderLoggerRef = testActor(orderDao)
    val generatedOrder = OrderUtil.generateRandomOrder
    val executionDate = LocalDateTime.now
    val quantity = 13
    val orderId = 2

    //when
    orderLoggerRef ! ExecutedQuantity(orderId, quantity, executionDate)

    //then
    orderDao.insertExecution _ verify Execution(orderId, quantity, executionDate) once()
  }

  def testActor(orderDao: IOrderDao) = TestActorRef[OrderLogger](Props(classOf[OrderLogger], orderDao, false))
}

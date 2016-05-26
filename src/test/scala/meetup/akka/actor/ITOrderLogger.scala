package meetup.akka.actor

import java.time.{LocalDate, LocalDateTime}

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import meetup.akka.dal.IOrderDao
import meetup.akka.om._
import meetup.akka.service.OrderUtil
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.util.Random

class ITOrderLogger extends TestKit(ActorSystem("OrderProcessing")) with FlatSpecLike with ImplicitSender
  with BeforeAndAfterAll with Matchers with MockFactory {
  behavior of "OrderLogger"

  it should "save order" in {
    //given
    val orderDao = mock[IOrderDao]
    val orderLogger = actor(orderDao)
    val order = OrderUtil.generateRandomOrder
    val orderWithId = new Order(2, order)
    //when
    orderDao.saveOrder _ expects orderWithId
    orderLogger ! PreparedOrderForAck(1L, PreparedOrder(order, 2))
    //then
    val savedOrder = expectMsgAnyClassOf(classOf[LoggedOrder])
    savedOrder.order should be(orderWithId)
  }

  it should "complete batch" in {
    //given
    val orderDao = mock[IOrderDao]
    val persistenceActor = actor(orderDao)
    val withDate = LocalDateTime.now
    //when
    orderDao.completeBatch _ expects (1L, withDate)
    persistenceActor ! CompleteBatch(1, withDate)
    //then
    val batchCompleted = expectMsgAnyClassOf(classOf[BatchCompleted])
    batchCompleted.upToId should be(1)
  }

  def actor(orderDao: IOrderDao) = system.actorOf(Props(classOf[OrderLogger], orderDao), "persist" + Random.nextInt())
}

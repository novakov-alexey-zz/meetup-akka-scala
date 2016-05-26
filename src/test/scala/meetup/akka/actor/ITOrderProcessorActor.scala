package meetup.akka.actor

import java.time.LocalDateTime

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import meetup.akka.dal.IOrderDao
import meetup.akka.om._
import meetup.akka.service.OrderUtil
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.language.postfixOps
import scala.util.Random

class ITOrderProcessorActor extends TestKit(ActorSystem("OrderProcessing")) with FlatSpecLike with ImplicitSender
  with BeforeAndAfterAll with Matchers with MockFactory {

  it should "generate id and persist incoming order" in {
    //given
    val orderIdGenerator = TestProbe()
    val orderLogger = TestProbe()
    val orderProcessor = orderProcessorActor(orderIdGenerator, orderLogger)
    val order = OrderUtil.generateRandomOrder
    //when
    orderProcessor ! NewOrder(order)
    //then
    val receivedOrder = orderIdGenerator.expectMsg(order)
    receivedOrder should be(order)

    //when
    orderProcessor ! PreparedOrder(order, 1)
    //then
    val preparedOrderForAck = orderLogger.expectMsgAnyClassOf(classOf[PreparedOrderForAck])
    preparedOrderForAck.preparedOrder.orderId should be(1)
  }

  it should "complete batch" in {
    //given
    val orderIdGenerator = TestProbe()
    val orderLogger = TestProbe()
    val execution = TestProbe()
    val orderProcessor = orderProcessorActor(orderIdGenerator, orderLogger)
    //when
    orderProcessor ! CompleteBatch(10, LocalDateTime.now)
    //then
    val completeBatchForId = orderLogger.expectMsgAnyClassOf(classOf[CompleteBatch])
    completeBatchForId.upToId should be(10)
  }

  def orderProcessorActor(orderIdGenerator: TestProbe, orderLogger: TestProbe) =
    system.actorOf(Props(classOf[OrderProcessor], mock[IOrderDao], Some(orderIdGenerator.ref), Some(orderLogger.ref.path)),
      "orderProcessor" + Random.nextInt)

  override def afterAll = TestKit.shutdownActorSystem(system)
}

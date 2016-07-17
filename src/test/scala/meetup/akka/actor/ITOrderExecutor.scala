package meetup.akka.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import meetup.akka.om.{AllAcksReceived, ExecuteOrder, ExecutedQuantity, ExecutionAck}
import org.scalatest.{FlatSpecLike, Matchers}

class ITOrderExecutor extends TestKit(ActorSystem("OrderProcessing")) with FlatSpecLike with ImplicitSender with Matchers {

  it should "batch orders and send to OrderLogger" in {
    //given
    val batchSize = 10
    val orderLogger = TestProbe()
    val orderExecutor = system.actorOf(Props(classOf[OrderExecutor], orderLogger.ref.path, batchSize), "orderExecutor")

    //when
    sendAllMessagesExceptOne(batchSize, orderExecutor)
    //then
    orderLogger.expectNoMsg()

    //when
    sendOneOrder(orderExecutor)
    //then
    expectAllMessageAreLogged(batchSize, orderLogger)

    //when
    sendOneOrder(orderExecutor)
    //then
    orderLogger.expectNoMsg()

    //when
    sendOneAck(orderExecutor)
    //then
    orderLogger.expectNoMsg()

    //when
    sendAllTheRestAcks(batchSize, orderExecutor)
    //then
    val allAcksReceived = orderLogger.expectMsgAnyClassOf(classOf[AllAcksReceived])
    allAcksReceived.replies.length should be(batchSize * OrderExecutor.execQuantity)
  }

  def sendAllTheRestAcks(batchSize: Int, orderExecutor: ActorRef): Unit =
    (1 until batchSize * OrderExecutor.execQuantity).par.foreach { i =>
      sendOneAck(orderExecutor)
    }

  def sendOneAck(orderExecutor: ActorRef) = orderExecutor ! ExecutionAck(1, 200, 1)

  def expectAllMessageAreLogged(batchSize: Int, orderLogger: TestProbe): Unit =
    1 to batchSize * OrderExecutor.execQuantity foreach { i =>
      orderLogger.expectMsgAnyClassOf(classOf[ExecutedQuantity])
    }

  def sendOneOrder(orderExecutor: ActorRef): Unit = orderExecutor ! ExecuteOrder(10, 400)

  def sendAllMessagesExceptOne(batchSize: Int, orderExecutor: ActorRef): Unit =
    (1 until batchSize).par.foreach { i =>
      sendOneOrder(orderExecutor)
    }
}
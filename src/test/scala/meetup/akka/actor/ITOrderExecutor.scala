package meetup.akka.actor

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import meetup.akka.om.{ExecuteOrder, ExecutedQuantity}
import org.scalatest.{FlatSpecLike, Matchers}

class ITOrderExecutor extends TestKit(ActorSystem("OrderProcessing")) with FlatSpecLike with ImplicitSender with Matchers {

  it should "batch orders and send to OrderLogger" in {
    //given
    val orderLogger = TestProbe()
    val orderExecutor = system.actorOf(Props(classOf[OrderExecutor], orderLogger.ref.path), "orderExecutor")

    //when
    (1 until OrderExecutor.batchSize).par.foreach { i =>
      orderExecutor ! ExecuteOrder(i, 200)
    }
    //then
    orderLogger.expectNoMsg()

    //when
    orderExecutor ! ExecuteOrder(10, 400)
    //then
    1 to OrderExecutor.batchSize * OrderExecutor.execQuantity foreach { i =>
      orderLogger.expectMsgAnyClassOf(classOf[ExecutedQuantity])
    }

    //when
    orderExecutor ! ExecuteOrder(1, 200)
    //then
    orderLogger.expectNoMsg()
  }
}
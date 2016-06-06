package meetup.akka.actor

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import meetup.akka.om.PreparedOrder
import meetup.akka.service.OrderUtil
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class ITOrderIdGeneratorActor extends TestKit(ActorSystem("OrderProcessing")) with FlatSpecLike with ImplicitSender with Matchers {
  behavior of "OrderIdGenerator"

  it should "generate next order" in {
    //given
    val orderIdGenerator = system.actorOf(Props(classOf[OrderIdGenerator]), "orderIdGenerator")
    val order = OrderUtil.generateRandomOrder
    //when
    orderIdGenerator ! order
    //then
    val preparedOrder = expectMsgAnyClassOf(classOf[PreparedOrder])
    preparedOrder.orderId should be(1)

    //when
    orderIdGenerator ! order
    //then
    val preparedOrder2 = expectMsgAnyClassOf(classOf[PreparedOrder])
    preparedOrder2.orderId should be(2)
  }
}

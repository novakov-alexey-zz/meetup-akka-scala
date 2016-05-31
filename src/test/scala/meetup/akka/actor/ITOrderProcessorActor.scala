package meetup.akka.actor

import java.nio.file.Paths
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

  //given
  val orderIdGenerator = TestProbe()
  val orderLogger = TestProbe()
  val orderExecutor = TestProbe()
  val dao = mock[IOrderDao]
  val orderProcessor = orderProcessorActor(dao, orderIdGenerator, orderLogger, orderExecutor)
  val order = OrderUtil.generateRandomOrder

  it should "generate id and persist incoming order" in {
    //when
    orderProcessor ! NewOrder(order)
    //then
    val receivedOrder = orderIdGenerator.expectMsgAnyClassOf(classOf[Order])
    receivedOrder should be(order)

    //when
    orderProcessor ! PreparedOrder(order, 1)
    //then
    val preparedOrderForAck = orderLogger.expectMsgAnyClassOf(classOf[LogOrder])
    preparedOrderForAck.preparedOrder.orderId should be(1)
  }

  it should "complete batch" in {
    //when
    orderProcessor ! CompleteBatch(10, LocalDateTime.now)
    //then
    val completeBatchForId = orderLogger.expectMsgAnyClassOf(classOf[CompleteBatch])
    completeBatchForId.upToId should be(10)
  }

  //remove journal to not recover the during the test
  override protected def beforeAll() = Paths.get("target/journal").toFile.listFiles().foreach(f => f.delete())

  def orderProcessorActor(orderDao: IOrderDao, orderIdGenerator: TestProbe, orderLogger: TestProbe, orderExecutor: TestProbe) =
    system.actorOf(Props(classOf[OrderProcessor], orderDao, Some(orderIdGenerator.ref), Some(orderLogger.ref.path), Some(orderExecutor.ref)),
      "orderProcessor" + Random.nextInt)

  override def afterAll = TestKit.shutdownActorSystem(system)
}

package meetup.akka.om

import java.math.BigDecimal
import java.time.LocalDateTime

import meetup.akka.om.OrderType.OrderType

case class Order(orderId: Long = -1, executionDate: LocalDateTime, orderType: OrderType,
                 executionPrice: BigDecimal, symbol: String, userId: Int, quantity: Int) {
  def this(orderType: OrderType, executionPrice: BigDecimal, symbol: String, userId: Int, quantity: Int) =
    this(-1, LocalDateTime.now, orderType, executionPrice, symbol, userId, quantity)

  def this(orderId: Long, order: Order) =
    this(orderId, order.executionDate, order.orderType, order.executionPrice, order.symbol, order.userId, order.quantity)
}

case class NewOrder(order: Order)

case class PreparedOrder(order: Order, orderId: Long)

case class LoggedOrder(deliveryId: Long, order: Order)

case class PreparedOrderForAck(deliveryId: Long, preparedOrder: PreparedOrder)

case class CompleteBatch(upToId: Int, withDate: LocalDateTime)

case class BatchCompleted(upToId: Int)
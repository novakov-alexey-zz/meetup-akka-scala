package meetup.akka.dal

import java.math.BigDecimal
import java.time.LocalDateTime

import meetup.akka.Config
import meetup.akka.om.Order
import meetup.akka.om.OrderType._
import org.mybatis.scala.mapping.{ResultMap, _}

object OrderDaoMapping {
  val selectOrders = new SelectList[OrderEntity] {
    resultMap = new ResultMap[OrderEntity] {
      id(property = "orderId", column = "orderId")
      result(property = "executionDate", column = "executionDate", typeHandler = T[meetup.akka.dal.LocalDateTimeTypeHandler])
      result(property = "orderType", column = "orderType", typeHandler = T[meetup.akka.dal.OrderTypeEnumTypeHandler])
      result(property = "executionPrice", column = "executionPrice")
      result(property = "symbol", column = "symbol")
      result(property = "userId", column = "userId")
      result(property = "quantity", column = "quantity")
    }

    def xsql = "SELECT * FROM OrderLog"
  }

  val saveOrder = new Insert[OrderEntity] {

    override def xsql =
      <xsql>
        INSERT INTO OrderLog (orderId, executionDate, orderType, executionPrice, symbol, userId, quantity)
        VALUES (
        #{{orderId}},
        #{{executionDate, typeHandler = meetup.akka.dal.LocalDateTimeTypeHandler}},
        #{{orderType, typeHandler = meetup.akka.dal.OrderTypeEnumTypeHandler}},
        #{{executionPrice}}, #{{symbol}}, #{{userId}}, #{{quantity}}
        )
      </xsql>
  }

  def bind: Seq[Statement] = Seq(selectOrders, saveOrder)
}

class OrderEntity {
  var orderId: Long = _
  var executionDate: LocalDateTime = _
  var orderType: OrderType = _
  var executionPrice: BigDecimal = _
  var symbol: String = _
  var userId: Int = _
  var quantity: Int = _

  def toOrder = new Order(orderId, executionDate, orderType, executionPrice, symbol, userId, quantity)
}

object OrderEntity {
  def toOrderEntity(order: Order): OrderEntity = new OrderEntity {
    orderId = order.orderId
    executionDate = order.executionDate
    orderType = order.orderType
    executionPrice = order.executionPrice
    symbol = order.symbol
    userId = order.userId
    quantity = order.quantity
  }
}

trait IOrderDao {
  def completeBatch(upToId: Long)

  def saveOrder(order: Order)

  def getOrders: Seq[Order]
}

class OrderDaoImpl extends IOrderDao {
  val db = Config.persistenceContext

  override def getOrders: Seq[Order] = db.readOnly { implicit session => OrderDaoMapping.selectOrders().map(_.toOrder) }

  override def completeBatch(upToId: Long): Unit = ???

  override def saveOrder(order: Order): Unit =
    db.transaction { implicit session => OrderDaoMapping.saveOrder(OrderEntity.toOrderEntity(order)) }
}

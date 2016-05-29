package meetup.akka.service

import java.math.BigDecimal

import meetup.akka.om.{Order, OrderType}

import scala.util.Random

object OrderUtil {
  val SYMBOLS = Array("APPL", "GOOG", "IBM", "YAH")

  def generateRandomOrder = new Order(
    OrderType(Random.nextInt(OrderType.values.size)),
    BigDecimal.valueOf(Random.nextDouble * 100),
    SYMBOLS(Random.nextInt(SYMBOLS.length)),
    Math.abs(Random.nextInt),
    Math.abs(Random.nextInt(500)))
}

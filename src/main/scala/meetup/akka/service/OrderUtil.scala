package meetup.akka.service

import java.math.BigDecimal

import meetup.akka.om.{Order, OrderType}

import scala.util.Random

object OrderUtil {
  val symbols = Array("APPL", "GOOG", "IBM", "YAH")

  def generateRandomOrder = new Order(
    OrderType(Random.nextInt(OrderType.values.size)),
    BigDecimal.valueOf(Random.nextDouble * 100),
    symbols(Random.nextInt(symbols.length)),
    Math.abs(Random.nextInt),
    Math.abs(100 + Random.nextInt(500)))
}

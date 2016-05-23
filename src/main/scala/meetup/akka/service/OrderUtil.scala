package meetup.akka.service

import java.math.BigDecimal
import java.util.Random

import meetup.akka.om.{Order, OrderType}

object OrderUtil {
  val SYMBOLS = Array("APPL", "GOOG", "IBM", "YAH")
  val random = new Random

  def generateRandomOrder: Order = new Order(
    OrderType(random.nextInt(OrderType.values.size)),
    BigDecimal.valueOf(random.nextDouble * 100),
    SYMBOLS(random.nextInt(SYMBOLS.length)),
    Math.abs(random.nextInt),
    Math.abs(random.nextInt(500)))
}

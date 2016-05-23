package meetup.akka.om

object OrderType extends Enumeration {
  type OrderType = Value

  val MARKET, LIMIT, STOP, STOP_LIMIT, WITH_OR_WITHOUT = Value
}
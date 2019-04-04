package pl.spark.learning.rdd

import net.liftweb.json.{NoTypeHints, Serialization}
import pl.spark.learning.generators.{OrderApproved, OrderCompleted, OrderCreated, OrderShipped, OrderValidated, UUIDserializer}

object OrderEventDeserializer {
  implicit val formats = Serialization.formats(NoTypeHints) + new UUIDserializer

  def toOrderCompleted(logMessage: String) = {
    val orderCompleted = Serialization.read[OrderCompleted](logMessage)
    val orderData = OrderData(
      orderCompleted.id, orderCompleted.eventName, orderCompleted.status, orderCompleted.orderId,
      completedDate = Some(orderCompleted.completedDate)
    )
    Some(orderData)
  }

  def toOrderShipped(logMessage: String) = {
    val orderShipped = Serialization.read[OrderShipped](logMessage)
    val orderData = OrderData(
      orderShipped.id, orderShipped.eventName, orderShipped.status, orderShipped.orderId,
      shippedDate = Some(orderShipped.shippedDate)
    )
    Some(orderData)
  }

  def toOrderApproved(logMessage: String) = {
    val orderApproved = Serialization.read[OrderApproved](logMessage)
    val orderData = OrderData(
      orderApproved.id, orderApproved.eventName, orderApproved.status, orderApproved.orderId,
      approvedDate = Some(orderApproved.approvedDate)
    )
    Some(orderData)
  }

  def toOrderValidated(logMessage: String) = {
    val orderValidated = Serialization.read[OrderValidated](logMessage)
    val orderData = OrderData(
      orderValidated.id, orderValidated.eventName, orderValidated.status, orderValidated.orderId,
      validatedDate = Some(orderValidated.validatedDate)
    )
    Some(orderData)
  }

  def toOrderCreated(logMessage: String) = {
    val orderCreated = Serialization.read[OrderCreated](logMessage)
    val orderData = OrderData(
      orderCreated.id, orderCreated.eventName, orderCreated.status, orderCreated.orderId,
      customerId = Some(orderCreated.customerId),
      items = Some(orderCreated.items),
      createdDate = Some(orderCreated.createdDate)
    )
    Some(orderData)
  }
}
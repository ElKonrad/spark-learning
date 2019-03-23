package pl.spark.learning.generators

import java.io.{File, FileWriter}
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.{Date, UUID}

import net.liftweb.json._
import org.joda.time.DateTime

import scala.util.Random

object EventsLogGenerator {
  implicit val formats = Serialization.formats(NoTypeHints) + new UUIDserializer

  val limitSizeFileInMb: Double = 0.1

  val threeDaysInSeconds = 259200
  val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"

  var nowDate = DateTime.now()
  val customerIds: List[UUID] = (0 to 1000000).map(_ => UUID.randomUUID()).toList

  var allEventsData: collection.mutable.Map[UUID, List[Event]] = collection.mutable.Map().withDefaultValue(List.empty[Event])

  def addEvent(orderId: UUID, event: Event): Unit = allEventsData(orderId) match {
    case xs: List[Event] => allEventsData(orderId) = xs :+ event
    case _ => allEventsData(orderId) = List(event)
  }

  def removeOrder(orderId: UUID): Unit = allEventsData.remove(orderId)

  def date: String = new SimpleDateFormat(DATE_FORMAT).format(nowDate.toDate)

  def nowDatePlusRandomMillis: Date = {
    nowDate = nowDate.plusMillis(randomBetween(1, 500))
    nowDate.toDate
  }

  def randomBetween(minInclusive: Int, maxExclusive: Int): Int = {
    require(minInclusive < maxExclusive, "Invalid bounds")

    val difference = maxExclusive - minInclusive
    Random.nextInt(difference) + minInclusive
  }

  def pid: String = Random.nextInt(20000).toString

  def thread: String = {
    val thread = Random.nextInt(50)
    s"thread-$thread"
  }

  def orderValidatedDate(lastEvent: OrderCreated): Date = {
    new DateTime(lastEvent.createdDate).plusSeconds(randomBetween(1, 300)).toDate
  }

  def orderApprovedDate(lastEvent: OrderValidated): Date = {
    new DateTime(lastEvent.validatedDate).plusSeconds(randomBetween(1, 1800)).toDate
  }

  def orderShippedDate(lastEvent: OrderApproved): Date = {
    val p = Math.random()

    if (0 <= p && p < 0.03) new DateTime(lastEvent.approvedDate).plusDays(randomBetween(30, 60)).toDate
    else if (0.03 <= p && p < 0.10) new DateTime(lastEvent.approvedDate).plusDays(randomBetween(10, 30)).toDate
    else if (0.10 <= p && p < 0.55) new DateTime(lastEvent.approvedDate).plusDays(randomBetween(3, 10)).toDate
    else new DateTime(lastEvent.approvedDate).plusSeconds(randomBetween(1, threeDaysInSeconds)).toDate
  }

  def orderCompletedDate(lastEvent: OrderShipped): Date = {
    val p = Math.random()

    if (0 <= p && p < 0.10) new DateTime(lastEvent.shippedDate).plusDays(randomBetween(7, 14)).toDate
    else if (0.10 <= p && p < 0.50) new DateTime(lastEvent.shippedDate).plusDays(randomBetween(3, 7)).toDate
    else new DateTime(lastEvent.shippedDate).plusSeconds(randomBetween(1, threeDaysInSeconds)).toDate
  }

  def event: Event = {
    val eventsDataKeys = allEventsData.keys.toList

    def getUUID: UUID = {
      if (eventsDataKeys.isEmpty || eventsDataKeys.size < 100)
        UUID.randomUUID()
      else if (allEventsData.groupBy(_._2.size).exists(_._2.size > 100))
        allEventsData.find(p => (1 to 4).contains(p._2.size)).get._1
      else
        eventsDataKeys(Random.nextInt(allEventsData.size))
    }

    val maybeOrderId = getUUID

    val allEventsDataSize = allEventsData(maybeOrderId)

    if (allEventsDataSize.size == 1) {

      val lastEvent = allEventsDataSize(0).asInstanceOf[OrderCreated]
      val orderValidated = OrderValidated(UUID.randomUUID(), maybeOrderId, orderValidatedDate(lastEvent))
      addEvent(maybeOrderId, orderValidated)
      orderValidated
    } else if (allEventsDataSize.size == 2) {

      val lastEvent = allEventsDataSize(1).asInstanceOf[OrderValidated]
      val orderApproved = OrderApproved(UUID.randomUUID(), maybeOrderId, orderApprovedDate(lastEvent))
      addEvent(maybeOrderId, orderApproved)
      orderApproved
    } else if (allEventsDataSize.size == 3) {

      val lastEvent = allEventsDataSize(2).asInstanceOf[OrderApproved]
      val orderShipped = OrderShipped(UUID.randomUUID(), maybeOrderId, orderShippedDate(lastEvent))
      addEvent(maybeOrderId, orderShipped)
      orderShipped
    } else if (allEventsDataSize.size == 4) {

      val lastEvent = allEventsDataSize(3).asInstanceOf[OrderShipped]
      val orderCompleted = OrderCompleted(UUID.randomUUID(), maybeOrderId, orderCompletedDate(lastEvent))
      removeOrder(maybeOrderId)
      orderCompleted
    } else {
      val orderId = UUID.randomUUID()
      val customerId = customerIds(Random.nextInt(customerIds.size))
      val category = Random.nextInt(5) match {
        case 0 => ACCESSORIES.toString
        case 1 => PERIPHERALS.toString
        case 2 => COMPUTERS.toString
        case 3 => SOFTWARE.toString
        case 4 => LAPTOPS.toString
      }
      val orderedItems = (1 to Random.nextInt(2) + 1)
        .map(t => Item(UUID.randomUUID(), s"Some item $t", 1 + Random.nextInt(5), Random.nextInt(10000).toDouble, category)).toList

      val orderCreated = OrderCreated(UUID.randomUUID(), orderId, customerId, orderedItems, nowDatePlusRandomMillis)
      addEvent(orderId, orderCreated)
      orderCreated
    }
  }

  def randomLog: (String, String) = {
    val e = event
    val eventName = e.eventName
    val message = Serialization.write(e)
    (s"$date INFO $pid --- [$thread] $eventName:$message\n", message)
  }

  def main(args: Array[String]): Unit = {
    var counter = 0
    val start = System.currentTimeMillis()
    val fileTxt = new File("src/main/resources/pl/spark/learning/logsexample.txt")
    val fileJson = new File("src/main/resources/pl/spark/learning/logsexample.json")
    val fwTxt = new FileWriter(fileTxt, true)
    val fwJson = new FileWriter(fileJson, true)
    try {
      while (fileTxt.length() < limitSizeFileInMb * 1000000) {
        val (log, messageJson) = randomLog
        fwTxt.write(log)
        fwJson.write(messageJson + "\n")

        if (counter % 10000 == 0) {
          println(1.0 * fileTxt.length() / 1000000 + "\t" + MILLISECONDS.toSeconds(System.currentTimeMillis() - start))
          counter = 0
        }
        counter = counter + 1
      }
    } finally {
      fwTxt.close()
      fwJson.close()
    }
    val end = System.currentTimeMillis()
    println("Seconds: " + MILLISECONDS.toSeconds(end - start))
    println("Minutes: " + MILLISECONDS.toMinutes(end - start))
  }
}

trait Event {
  val id: UUID
  val eventName: String
}

trait OrderEvent extends Event {
  val status: String
}

case class OrderCreated(override val id: UUID, orderId: UUID, customerId: UUID, items: List[Item], createdDate: Date,
                        override val eventName: String = "OrderCreated",
                        override val status: String = CREATED.toString) extends OrderEvent

case class OrderValidated(override val id: UUID, orderId: UUID, validatedDate: Date,
                          override val eventName: String = "OrderValidated",
                          override val status: String = VALIDATED.toString) extends OrderEvent

case class OrderApproved(override val id: UUID, orderId: UUID, approvedDate: Date,
                         override val eventName: String = "OrderApproved",
                         override val status: String = APPROVED.toString) extends OrderEvent

case class OrderShipped(override val id: UUID, orderId: UUID, shippedDate: Date,
                        override val eventName: String = "OrderShipped",
                        override val status: String = SHIPPED.toString) extends OrderEvent

case class OrderCompleted(override val id: UUID, orderId: UUID, completedDate: Date,
                          override val eventName: String = "OrderCompleted",
                          override val status: String = COMPLETED.toString) extends OrderEvent

case class Item(id: UUID, description: String, quantity: Int, price: Double, category: String)

sealed trait Status

case object CREATED extends Status

case object VALIDATED extends Status

case object APPROVED extends Status

case object SHIPPED extends Status

case object COMPLETED extends Status

sealed trait Category

case object COMPUTERS extends Category

case object LAPTOPS extends Category

case object ACCESSORIES extends Category

case object PERIPHERALS extends Category

case object SOFTWARE extends Category
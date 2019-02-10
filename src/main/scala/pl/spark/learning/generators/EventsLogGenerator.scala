package pl.spark.learning.generators

import java.io.{File, FileWriter}
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.{Date, UUID}

import net.liftweb.json.JsonAST.JString
import net.liftweb.json._
import org.joda.time.DateTime

import scala.util.Random

object EventsLogGenerator {

  implicit val formats = Serialization.formats(NoTypeHints) + new UUIDserializer

  val limitSizeFileInMb: Double = 0.1

  val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"
  var nowDate = DateTime.now()

  val customerIds: List[UUID] = (0 to 10000).map(_ => UUID.randomUUID()).toList
  var eventsData: collection.mutable.Map[UUID, List[Event]] = collection.mutable.Map().withDefaultValue(List.empty[Event])

  def addEvent(orderId: UUID, event: Event): Unit = eventsData(orderId) match {
    case xs: List[Event] => eventsData(orderId) = xs :+ event
    case _ => eventsData(orderId) = List(event)
  }

  def removeOrder(orderId: UUID): Unit = eventsData.remove(orderId)

  def date: String = new SimpleDateFormat(DATE_FORMAT).format(nowDatePlusRandomMinutes)

  def logLevel: String = {
    val p = Math.random()

    if (0 <= p && p < 0.10) "ERROR"
    else if (0.10 <= p && p < 0.15) "WARN"
    else if (0.15 <= p && p < 0.20) "DEBUG"
    else "INFO"
  }

  def pid: String = Random.nextInt(20000).toString

  def thread: String = {
    val thread = Random.nextInt(50)
    s"http-nio-exec-$thread"
  }

  def eventNameBy(logLevel: String, event: Event): String = {
    logLevel match {
      case "INFO" => event.eventName
      case "DEBUG" => "RetriedExternalService"
      case "WARN" => "RetryConnectToExternalService"
      case "ERROR" => "EventProcessingException"
    }
  }

  def messageBy(logLevel: String, event: Event): String = {
    logLevel match {
      case "INFO" =>
        Serialization.write(event)
      case "DEBUG" =>
        s"Retried to external service ${Random.nextInt(10)} times"
      case "WARN" =>
        "Retry connect to external service"
      case "ERROR" =>
        "Cannot process event"
    }
  }

  def event: Event = {
    val eventsDataKeys = eventsData.keys.toList

    def getUUID: UUID = {
      if (eventsDataKeys.isEmpty || eventsDataKeys.size < 50)
        UUID.randomUUID()
      else if (eventsData.groupBy(p => p._2.size).exists(p => p._2.size > 100))
        eventsData.find(p => (1 to 4).contains(p._2.size)).get._1
      else
        eventsDataKeys(Random.nextInt(eventsData.size))
    }

    val maybeOrderId = getUUID

    val eventsDataSize = eventsData(maybeOrderId).size

    if (eventsDataSize == 1) {

      val orderValidated = OrderValidated(UUID.randomUUID(), maybeOrderId, nowDatePlusRandomMinutes)
      addEvent(maybeOrderId, orderValidated)
      orderValidated
    } else if (eventsDataSize == 2) {

      val orderApproved = OrderApproved(UUID.randomUUID(), maybeOrderId, nowDatePlusRandomMinutes)
      addEvent(maybeOrderId, orderApproved)
      orderApproved
    } else if (eventsDataSize == 3) {

      val orderShipped = OrderShipped(UUID.randomUUID(), maybeOrderId, nowDatePlusRandomMinutes)
      addEvent(maybeOrderId, orderShipped)
      orderShipped
    } else if (eventsDataSize == 4) {

      val orderCompleted = OrderCompleted(UUID.randomUUID(), maybeOrderId, nowDatePlusRandomMinutes)
      removeOrder(maybeOrderId)
      orderCompleted
    } else {
      val orderId = UUID.randomUUID()
      val customerId = customerIds(Random.nextInt(customerIds.size))
      val category = Random.nextInt(5) match {
        case 0 => BOOKS.toString
        case 1 => AUTOMOTIVE.toString
        case 2 => FASHION.toString
        case 3 => SOFTWARE.toString
        case 4 => SPORTS.toString
      }
      val orderedItems = (1 to Random.nextInt(2) + 1)
        .map(t => Item(UUID.randomUUID(), s"Some item $t", Random.nextInt(5), Random.nextInt(10000).toDouble, category)).toList

      val orderCreated = OrderCreated(UUID.randomUUID(), orderId, customerId, orderedItems, nowDatePlusRandomMinutes)
      addEvent(orderId, orderCreated)
      orderCreated
    }
  }

  def nowDatePlusRandomMinutes: Date = {
    nowDate = nowDate.plusSeconds(Random.nextInt(15))
    nowDate.toDate
  }

  def randomLog(): String = {
    val logLvl = logLevel
    val e = logLvl match {
      case "INFO" => event
      case _ => null
    }
    s"$date $logLvl $pid --- [$thread] ${eventNameBy(logLvl, e)}:${messageBy(logLvl, e)}\n"
  }

  def main(args: Array[String]): Unit = {
    var counter = 0
    val start = System.currentTimeMillis()
    val file = new File("src/main/resources/pl/spark/learning//logsexample.txt")
    val fw = new FileWriter(file, true)
    try {
      while (file.length() < limitSizeFileInMb * 1000000) {
        fw.write(randomLog())

        if (counter % 10000 == 0) {
          println(1.0 * file.length() / 1000000 + "\t" + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start))
          counter = 0
        }
        counter = counter + 1
      }
    } finally fw.close()
    val end = System.currentTimeMillis()
    println("Seconds: " + TimeUnit.MILLISECONDS.toSeconds(end - start))
    println("Minutes: " + TimeUnit.MILLISECONDS.toMinutes(end - start))

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

case object BOOKS extends Category

case object AUTOMOTIVE extends Category

case object FASHION extends Category

case object SOFTWARE extends Category

case object SPORTS extends Category

class UUIDserializer extends Serializer[UUID] {

  private val UUIDClass = classOf[UUID]

  override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), UUID] = {
    case (TypeInfo(UUIDClass, _), json) => json match {
      case x: JString => {
        UUID.fromString(x.values)
      }
    }
  }

  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case x: UUID => JString(x.toString)
  }
}
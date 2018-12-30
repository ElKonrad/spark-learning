import java.io.{File, PrintWriter}
import java.text.SimpleDateFormat
import java.time.{LocalDateTime, ZoneOffset}
import java.util.{Date, UUID}

import scala.util.Random

object LogsGenerator {
  val dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")

  def main(args: Array[String]): Unit = {

    def date(secondShift: Int): String = dateFormat.format(Date.from(LocalDateTime.now().plusSeconds(secondShift).toInstant(ZoneOffset.UTC)))

    def uuid: String = UUID.randomUUID().toString

    val limit = 1000000

    val orders = new PrintWriter(new File("src/main/resources/orders.txt"))

    val visits = new PrintWriter(new File("src/main/resources/visits.txt"))

    val customers = (0 to 100).map(r => "c" + r)

    val products = (0 to 1000).map(r => "p" + r)

    def getSomeCustomer = Random.nextInt(customers.size)

    def getSomeProduct = Random.nextInt(products.size)

    1 to limit foreach { i =>
      val customer = customers(getSomeCustomer)
      val product = products(getSomeProduct)

      visits.write(s"${date(i)} $customer $product\n")
      if (Math.random() > 0.9)
        orders.write(s"${date(i + 1)} $customer $product $uuid\n")
    }

    visits.close()
    orders.close()
  }
}

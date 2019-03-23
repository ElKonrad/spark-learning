package pl.spark.learning.generators

import java.util.UUID

import net.liftweb.json.JsonAST.JString
import net.liftweb.json.{Formats, JValue, Serializer, TypeInfo}

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

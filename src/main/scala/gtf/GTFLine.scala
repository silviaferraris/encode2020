package gtf

import scala.collection.mutable


class GTFLine(val map : mutable.HashMap[String, String])
{

  def get(key : String) : String =
  {
    map.getOrElse(key, "NULL")
  }

  def isEmpty() : Boolean =
  {
    map.isEmpty
  }

}

object GTFLine
{
  val empty = new GTFLine(new mutable.HashMap[String, String]())
}

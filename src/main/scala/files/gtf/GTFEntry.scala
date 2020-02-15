package files.gtf

import scala.collection.mutable


class GTFEntry(val map : mutable.HashMap[String, String])
{

  def get(key : String) : String =
  {
    map.getOrElse(key, "NULL")
  }

  def isEmpty : Boolean =
  {
    map.isEmpty
  }

  override def toString: String = map.toString()

}

object GTFEntry
{
  val empty = new GTFEntry(new mutable.HashMap[String, String]())
}

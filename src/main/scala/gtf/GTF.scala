package gtf

import scala.collection.mutable

class GTF
{

  private val geneMap : mutable.HashMap[String, GTFLine] = new mutable.HashMap[String, GTFLine]()
  private val transcriptMap : mutable.HashMap[String, GTFLine] = new mutable.HashMap[String, GTFLine]()
  private val trnaMap : mutable.HashMap[String, GTFLine] = new mutable.HashMap[String, GTFLine]()


  def addLine(id : String, map: mutable.HashMap[String, String]): Unit =
  {
    if(id.startsWith("ENSG"))geneMap.+=((id, new GTFLine(map)))
    else if(id.startsWith("ENST"))transcriptMap.+=((id, new GTFLine(map)))
    else trnaMap.+=((id, new GTFLine(map)))
  }

  def get(id : String) : GTFLine =
  {
    if(id.startsWith("ENSG"))geneMap.getOrElse(id, GTFLine.empty)
    else if(id.startsWith("ENST"))transcriptMap.getOrElse(id, GTFLine.empty)
    else trnaMap.getOrElse(id, GTFLine.empty)
  }

  def getValue(id : String, key : String) : String =
  {
    get(id).get(key)
  }

  def exist(id : String) : Boolean =
  {
    if(id.startsWith("ENSG"))!geneMap.getOrElse(id, GTFLine.empty).isEmpty()
    else if(id.startsWith("ENST"))!transcriptMap.getOrElse(id, GTFLine.empty).isEmpty()
    else !trnaMap.getOrElse(id, GTFLine.empty).isEmpty()
  }

}

object GTF
{
  final val KEY_CHR = "chr"
  final val KEY_START = "start"
  final val KEY_STOP = "stop"
  final val KEY_STRAND = "strand"
  final val KEY_GENE_ID = "gene_id"
  final val KEY_TRANSCRIPT_ID = "transcript_id"
  final val KEY_GENE_TYPE = "gene_type"
  final val KEY_GENE_NAME = "gene_name"
  final val KEY_TRANSCRIPT_TYPE = "transcript_type"
  final val KEY_TRANSCRIPT_NAME = "transcript_name"
}

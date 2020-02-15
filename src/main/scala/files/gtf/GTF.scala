package files.gtf

import scala.collection.mutable

class GTF
{

  private val geneMap : mutable.HashMap[String, GTFEntry] = new mutable.HashMap[String, GTFEntry]()
  private val transcriptMap : mutable.HashMap[String, GTFEntry] = new mutable.HashMap[String, GTFEntry]()
  private val trnaMap : mutable.HashMap[String, GTFEntry] = new mutable.HashMap[String, GTFEntry]()


  def addLine(id : String, map: mutable.HashMap[String, String], isGene : Boolean, isTranscript : Boolean): Unit =
  {
    if(isGene)geneMap.+=((id, new GTFEntry(map)))
    else if(isTranscript)transcriptMap.+=((id, new GTFEntry(map)))
    else trnaMap.+=((id, new GTFEntry(map)))
  }

  def getGene(id : String) : GTFEntry =
  {
    geneMap.getOrElse(id, trnaMap.getOrElse(id, GTFEntry.empty))
  }

  def getTranscript(id : String) : GTFEntry =
  {
    transcriptMap.getOrElse(id, trnaMap.getOrElse(id, GTFEntry.empty))
  }

  def get(id : String, isGeneQuantification : Boolean) : GTFEntry =
  {
    if(isGeneQuantification)getGene(id)
    else getTranscript(id)
  }

  def exist(id : String) : Boolean =
  {
    if(id.startsWith("ENSG"))!geneMap.getOrElse(id, GTFEntry.empty).isEmpty
    else if(id.startsWith("ENST"))!transcriptMap.getOrElse(id, GTFEntry.empty).isEmpty
    else !trnaMap.getOrElse(id, GTFEntry.empty).isEmpty
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

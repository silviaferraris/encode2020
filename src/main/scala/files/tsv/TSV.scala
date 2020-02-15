package files.tsv

import java.io.File

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.control.Breaks.{break, breakable}

class TSV (val tsvFile : File)
{
  private val tsvSource = Source.fromFile(tsvFile)
  private val tsvLines = tsvSource.getLines().toArray[String] //create an array with all the lines fof the files.tsv file
  tsvSource.close()
  private val tsvHeader = tsvLines(0).split("\t") //extract the header fields from the first line
  private val _isGeneQuantification = tsvHeader(0).equals(TSV.KEY_GENE_ID)

  private val fileEntries : Seq[(String, mutable.HashMap[String, String])] = mapTSVFile()


  def isGeneQuantification : Boolean = {
    _isGeneQuantification
  }


  def foreach(f: (String, mutable.HashMap[String, String]) => Unit) ={
    fileEntries.foreach(entry => f.apply(entry._1, entry._2))
  }

  def getHeader : Array[String] = {
    tsvHeader
  }

  def getFileName : String = {
    tsvFile.getName
  }

  def getPath : String = {
    tsvFile.getAbsolutePath
  }

  private def mapTSVFile() : mutable.Seq[(String, mutable.HashMap[String, String])] =
  {
    val fileEntries = new ArrayBuffer[(String, mutable.HashMap[String, String])]
    for(i <- tsvLines.indices){
      breakable{
        if(i == 0)break
        val lineMap = mapTSVLine(tsvLines(i), tsvHeader)
        val key = if(_isGeneQuantification) TSV.KEY_GENE_ID else TSV.KEY_TRANSCRIPT_ID
        val id = lineMap(key)
        if(!id.contains("Spikein"))fileEntries.+=((id, lineMap))
      }
    }
    fileEntries
  }

  /**
    *
    * @param line The line to map
    * @param header The header of files.tsv file
    * @return An HashMap that contains the fields of the line as values and the header fields as keys
    */
  private def mapTSVLine(line : String, header : Array[String]): mutable.HashMap[String, String] =
  {
    val map = new mutable.HashMap[String, String]()
    val values = line.split("\t")
    for(i <- header.indices){
      map.+=((header(i), values(i)))
    }
    map
  }
}

object TSV
{
  //keys of tsv line data map
  final val KEY_GENE_ID = "gene_id"
  final val KEY_TRANSCRIPT_ID = "transcript_id"
  final val KEY_TPM = "TPM"
  final val KEY_FPKM = "FPKM"
  final val KEY_POSTERIOR_MEAN_COUNT = "posterior_mean_count"
  final val KEY_POSTERIOR_STANDARD_DEVIATION_OF_COUNT = "posterior_standard_deviation_of_count"
  final val KEY_PME_TPM = "pme_TPM"
  final val KEY_PME_FPKM = "pme_FPKM"
  final val KEY_TPM_CI_LOWER_BOUND = "TPM_ci_lower_bound"
  final val KEY_TPM_CI_UPPER_BOUND = "TPM_ci_upper_bound"
  final val KEY_FPKM_CI_LOWER_BOUND = "FPKM_ci_lower_bound"
}

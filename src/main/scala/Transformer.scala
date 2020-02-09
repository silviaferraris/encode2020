import java.io.File

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.{BufferedSource, Source}
import scala.util.control.Breaks.{break, breakable}

object Transformer
{
 // esempi di file con i quali provo a fare il matching
  final val TSV_1_PATH = "tsv/ENCFF918DYI(TSV_1).tsv"


  final val GENE_ID_PREFIX = "ENSG"
  final val TRANSCRIPT_ID_PREFIX = "ENST"

  //file di annotazione a cui riferirsi per fare il matching
  final val GENE_TRANSCRIPT_QUANTIFICATION_GRCH38_PATH : String = "gencode_files"+File.separator+"gencode.v24.primary_assembly.annotation.gtf"
  final val GENE__TRANSCRIPT_QUANTIFICATION_HG19_PATH: String = "gencode_files"+File.separator+"gencode.v19.annotation.gtf"
  final val TRNA_GRCH38_PATH : String = "gencode_files"+File.separator+"gencode.v24.tRNAs.gtf"
  final val TRNA_HG19_PATH : String = "gencode_files"+File.separator+"gencode.v19.tRNAs.gtf"


  final val COL_CHR = 0
  final val COL_FEATURE_TYPE = 2
  final val COL_START = 3
  final val COL_STOP = 4
  final val COL_STRAND = 6
  final val COL_DATA = 8

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



  def main(args: Array[String]): Unit = {
    transform(TSV_1_PATH)
  }

  def transform(tsvFilePath : String): Unit = {

    val tsvFile = Source.fromResource(tsvFilePath) // apro il file tsv scaricato
    val tsvLines = tsvFile.getLines().drop(1).toArray[String] // lo trasformo in un array per trattare meglio i vari campi del file
    tsvFile.close()

    val tsvIds = getIdsFromTsv(tsvLines) // estraggo gli id dal file tsv

    for(i <- tsvIds.indices){

      val tsvId = tsvIds(i)
      val tsvColumns = tsvLines(i).split("\t")
      val isGene = tsvId.startsWith(GENE_ID_PREFIX)

      print("search "+tsvId+"... ")

      val gencodeData = findId(tsvId)

      if(gencodeData == null)print("not found!")
      else gencodeData.foreach(print(_))
      println()

    }
  }


  /**
    *
    * @param tsvId The gene/transcript id to search in GTF files
    * @return An HashMap that contains all the information about the given id, contained in GTF files
    */
  def findId(tsvId : String) : mutable.HashMap[String, String] =
  {
    //Check the feature type (if the id start with ENSG than isGene = true, otherwise if the id start with ENST than isTranscript = true)
    val isGene = tsvId.startsWith(GENE_ID_PREFIX)
    val isTranscript = tsvId.startsWith(TRANSCRIPT_ID_PREFIX)

    //Open the GTF files
    val gencodeFiles : Array[BufferedSource] = new Array[BufferedSource](2)
    if(isGene || isTranscript){
      gencodeFiles(0) = Source.fromResource(GENE_TRANSCRIPT_QUANTIFICATION_GRCH38_PATH)
      gencodeFiles(1) = Source.fromResource(GENE__TRANSCRIPT_QUANTIFICATION_HG19_PATH)
    }
    else{
      gencodeFiles(0) = Source.fromResource(TRNA_GRCH38_PATH)
      gencodeFiles(1) = Source.fromResource(TRNA_HG19_PATH)
    }

    var gencodeDataMap : mutable.HashMap[String, String] = null

    var found = false

    //Iterate the two GTF files
    for(gencodeFile <- gencodeFiles){

      breakable{

        if(found)break

        //Iterate all the line of one GTF file
        for(line <- gencodeFile.getLines()){
          breakable{
            if(line.startsWith("##") || found)break

            val gencodeColumns = line.split("\t")
            val featureType = gencodeColumns(COL_FEATURE_TYPE)
            val isTrnaScan = featureType.equals("tRNAscan")

            if(!(featureType.equals("gene") && isGene || featureType.equals("transcript") && isTranscript || isTrnaScan))break

            //Map the additional information
            gencodeDataMap = mapGTFData(gencodeColumns(COL_DATA))

            val geneId = gencodeDataMap(KEY_GENE_ID)
            val transcriptId = gencodeDataMap.getOrElse(KEY_TRANSCRIPT_ID, "")

            //Check if the tsvId match with the id of the GTF file line and if the line has the correct feature type for that id
            if(((isGene || isTrnaScan) && !geneId.equals(tsvId)) || (isTranscript && !transcriptId.equals(tsvId)))break

            found = true

            //Add the coordinates in the map
            gencodeDataMap.+=((KEY_CHR, gencodeColumns(COL_CHR)),
              (KEY_START, gencodeColumns(COL_START)),
              (KEY_STOP, gencodeColumns(COL_STOP)),
              (KEY_STRAND, gencodeColumns(COL_STRAND)))

          }
        }
      }
    }

    //if the id is not found in the GTF files, than return an empty HashMap
    if(!found)return new mutable.HashMap[String, String]()
    gencodeDataMap
  }

  /**
    *
    * @param data The additional information contained in the ninth column of GTF file (format: key "value"; )
    * @return A HashMap that contains all the information divided by key and values
    */
  def mapGTFData(data : String) : mutable.HashMap[String, String] =
  {
    val map = new mutable.HashMap[String, String]()
    val pairs = data.split(";")
    pairs.foreach(pair =>
    {
      val splitted = pair.trim.split(" ")
      val key = splitted(0)
      val value = if(splitted(1).startsWith("\"")) splitted(1).substring(1, splitted(1).length-1) else splitted(1)
      map.+=((key, value))
    })
    map
  }

  /**
    *
    * @param tsvLines All the lines of a TSV file
    * @return All the gene/transcript ids (the first column of the file)
    */
  def getIdsFromTsv(tsvLines : Array[String]) : Array[String] =
  {
    tsvLines.map(line => line.split("\t")).map(columns => columns(0))
  }

}

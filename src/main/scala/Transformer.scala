import java.io.{File, FileWriter, PrintWriter}

import scala.collection.mutable
import scala.io.{BufferedSource, Source}
import scala.util.control.Breaks.{break, breakable}

object Transformer
{
  // esempi di file con i quali provo a fare il matching
  final val TSV_1_PATH = "tsv/ENCFF918DYI(TSV_1).tsv"


  final val GENE_ID_PREFIX = "ENSG"
  final val TRANSCRIPT_ID_PREFIX = "ENST"

  //gtf files (gencode files) for matching
  final val GENE_TRANSCRIPT_QUANTIFICATION_GRCH38_PATH : String = "gencode_files"+File.separator+"gencode.v24.primary_assembly.annotation.gtf"
  final val GENE__TRANSCRIPT_QUANTIFICATION_HG19_PATH: String = "gencode_files"+File.separator+"gencode.v19.annotation.gtf"
  final val TRNA_GRCH38_PATH : String = "gencode_files"+File.separator+"gencode.v24.tRNAs.gtf"
  final val TRNA_HG19_PATH : String = "gencode_files"+File.separator+"gencode.v19.tRNAs.gtf"

  //Indices of the columns of the gencode file
  final val COL_CHR = 0
  final val COL_FEATURE_TYPE = 2
  final val COL_START = 3
  final val COL_STOP = 4
  final val COL_STRAND = 6
  final val COL_DATA = 8

  //Keys of gencode data map
  final val GTF_KEY_CHR = "chr"
  final val GTF_KEY_START = "start"
  final val GTF_KEY_STOP = "stop"
  final val GTF_KEY_STRAND = "strand"
  final val GTF_KEY_GENE_ID = "gene_id"
  final val GTF_KEY_TRANSCRIPT_ID = "transcript_id"
  final val GTF_KEY_GENE_TYPE = "gene_type"
  final val GTF_KEY_GENE_NAME = "gene_name"
  final val GTF_KEY_TRANSCRIPT_TYPE = "transcript_type"
  final val GTF_KEY_TRANSCRIPT_NAME = "transcript_name"

  //keys of tsv line data map
  final val TSV_KEY_GENE_ID = "gene_id"
  final val TSV_KEY_TPM = "TPM"
  final val TSV_KEY_FPKM = "FPKM"
  final val TSV_KEY_POSTERIOR_MEAN_COUNT = "posterior_mean_count"
  final val TSV_KEY_POSTERIOR_STANDARD_DEVIATION_OF_COUNT = "posterior_standard_deviation_of_count"
  final val TSV_KEY_PME_TPM = "pme_TPM"
  final val TSV_KEY_PME_FPKM = "pme_FPKM"
  final val TSV_KEY_TPM_CI_LOWER_BOUND = "TPM_ci_lower_bound"
  final val TSV_KEY_TPM_CI_UPPER_BOUND = "TPM_ci_upper_bound"
  final val TSV_KEY_FPKM_CI_LOWER_BOUND = "FPKM_ci_lower_bound"


  final val outputFileColumnsGTF = Array(GTF_KEY_CHR, GTF_KEY_START, GTF_KEY_STOP, GTF_KEY_STRAND, GTF_KEY_GENE_ID, GTF_KEY_TRANSCRIPT_ID, GTF_KEY_GENE_NAME, GTF_KEY_GENE_TYPE, GTF_KEY_TRANSCRIPT_NAME, GTF_KEY_TRANSCRIPT_TYPE)
  final val outputFileColumnsTSV = Array(TSV_KEY_TPM, TSV_KEY_FPKM, TSV_KEY_POSTERIOR_MEAN_COUNT,
    TSV_KEY_POSTERIOR_STANDARD_DEVIATION_OF_COUNT, TSV_KEY_PME_TPM, TSV_KEY_PME_FPKM, TSV_KEY_TPM_CI_LOWER_BOUND, TSV_KEY_TPM_CI_UPPER_BOUND, TSV_KEY_FPKM_CI_LOWER_BOUND)

  def main(args: Array[String]): Unit = {
    transform(TSV_1_PATH)
  }

  def transform(tsvFilePath : String): Unit = {

    val tsvFile = Source.fromResource(tsvFilePath) //open the tsv file
    val tsvLines = tsvFile.getLines().toArray[String] //create an array with all the lines fof the tsv file
    tsvFile.close()

    print(getClass.getResource("gencode_files"))

    val tsvHeader = tsvLines(0).split("\t") //extract the header fields from the first line

    val tsvName = tsvFilePath.split("/").last
    val outputFile = new File("transformed/"+tsvName+".transformed.tsv")
    val printWriter = new PrintWriter(new FileWriter(outputFile))

    outputFileColumnsGTF.foreach(key => printWriter.print(key+"\t"))
    outputFileColumnsTSV.foreach(key => printWriter.print(key+"\t"))
    printWriter.println()

    //Iterate the tsv file's lines
    for(i <- tsvLines.indices){

      //if(i > 655) return //Limit the id to process for testing

      breakable{
        if(i == 0)break //skip the line with the header

        //covert the current line in an HashMap
        val tsvDataMap = mapTSVLine(tsvLines(i), tsvHeader)

        val tsvId = tsvDataMap(TSV_KEY_GENE_ID)
        val isGene = tsvId.startsWith(GENE_ID_PREFIX)

        print("search "+tsvId+"... ")

        //search the id in the gencode files
        val gencodeDataMap = findId(tsvId)

        if(gencodeDataMap.isEmpty)println("not found!")
        else{
          println("found")

          outputFileColumnsGTF.foreach(key => printWriter.print(gencodeDataMap.getOrElse(key, "NULL")+"\t"))

          for(i <- outputFileColumnsTSV.indices){
            val key = outputFileColumnsTSV(i)
            printWriter.print(tsvDataMap.getOrElse(key, "NULL"))
            if(i < outputFileColumnsTSV.length-1)printWriter.print("\t")
          }
          if(i < tsvLines.length-1)printWriter.println()
          printWriter.flush()

        }
      }
    }
    printWriter.close()
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

            val geneId = gencodeDataMap(GTF_KEY_GENE_ID)
            val transcriptId = gencodeDataMap.getOrElse(GTF_KEY_TRANSCRIPT_ID, "")

            //Check if the tsvId match with the id of the GTF file line and if the line has the correct feature type for that id
            if(((isGene || isTrnaScan) && !geneId.equals(tsvId)) || (isTranscript && !transcriptId.equals(tsvId)))break

            found = true

            //Add the coordinates in the map
            gencodeDataMap.+=((GTF_KEY_CHR, gencodeColumns(COL_CHR)),
              (GTF_KEY_START, gencodeColumns(COL_START)),
              (GTF_KEY_STOP, gencodeColumns(COL_STOP)),
              (GTF_KEY_STRAND, gencodeColumns(COL_STRAND)))

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
    * @param line The line to map
    * @param header The header of tsv file
    * @return An HashMap that contains the fields of the line as values and the header fields as keys
    */
  def mapTSVLine(line : String, header : Array[String]): mutable.HashMap[String, String] =
  {
    val map = new mutable.HashMap[String, String]()
    val values = line.split("\t")
    for(i <- header.indices){
      map.+=((header(i), values(i)))
    }
    map
  }

}

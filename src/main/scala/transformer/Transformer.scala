package transformer

import java.io.{File, FileWriter, IOException, PrintWriter}

import files.Utils
import files.gtf.{GTF, GTFEntry, GTFLoader}
import files.tsv.TSV
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory, Validator}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.xml.{Node, SAXException, XML}

object Transformer
{

  private val foldersConfig = new mutable.HashMap[String, String]()
  private val gQOutputFileConfig = new ArrayBuffer[(String, String)]
  private val tQOutputFileConfig = new ArrayBuffer[(String, String)]

  /*final val TRANSCRIPT_OUTPUT_FILE_COLUMNS_GTF = Array(GTF.KEY_CHR, GTF.KEY_START, GTF.KEY_STOP, GTF.KEY_STRAND,
    GTF.KEY_TRANSCRIPT_ID, GTF.KEY_GENE_NAME, GTF.KEY_GENE_TYPE, GTF.KEY_TRANSCRIPT_NAME, GTF.KEY_TRANSCRIPT_TYPE)
  final val TRANSCRIPT_OUTPUT_FILE_COLUMNS_TSV = Array(TSV.KEY_GENE_ID, TSV.KEY_TPM, TSV.KEY_FPKM, TSV.KEY_POSTERIOR_MEAN_COUNT,
    TSV.KEY_POSTERIOR_STANDARD_DEVIATION_OF_COUNT, TSV.KEY_PME_TPM, TSV.KEY_PME_FPKM, TSV.KEY_TPM_CI_LOWER_BOUND, TSV.KEY_TPM_CI_UPPER_BOUND, TSV.KEY_FPKM_CI_LOWER_BOUND)

  final val GENE_OUTPUT_FILE_COLUMNS_GTF = Array(GTF.KEY_CHR, GTF.KEY_START, GTF.KEY_STOP, GTF.KEY_STRAND, GTF.KEY_GENE_NAME, GTF.KEY_GENE_TYPE)
  final val GENE_OUTPUT_FILE_COLUMNS_TSV = Array(TSV.KEY_GENE_ID, TSV.KEY_TPM, TSV.KEY_FPKM, TSV.KEY_POSTERIOR_MEAN_COUNT,
    TSV.KEY_POSTERIOR_STANDARD_DEVIATION_OF_COUNT, TSV.KEY_PME_TPM, TSV.KEY_PME_FPKM, TSV.KEY_TPM_CI_LOWER_BOUND, TSV.KEY_TPM_CI_UPPER_BOUND, TSV.KEY_FPKM_CI_LOWER_BOUND)*/

  def main(args: Array[String]): Unit = {

    transform()

  }

  //Load the gtf files, search the tsv files in the input folder, and transform each tsv files
  def transform(): Unit =
  {
    readConfigFile()

    val inputFolderPath = foldersConfig("input_folder")
    val outputFolderPath = foldersConfig("output_folder")
    val gtfFolderPath = foldersConfig("gtf_folder")

    println("Loading gencode files...")
    val gtf = GTFLoader.loadFolder(gtfFolderPath)
    println("Gencode files loaded successfully.")

    println("Searching tsv files...")

    val inputFolderGene = new File(inputFolderPath+File.separator+"gene_quantification")
    if(!inputFolderGene.exists() || !inputFolderGene.isDirectory)throw new Exception("Invalid input folder! ("+inputFolderPath+")")

    val inputFolderTranscript = new File(inputFolderPath+File.separator+"transcript_quantification")
    if(!inputFolderTranscript.exists() || !inputFolderTranscript.isDirectory)throw new Exception("Invalid input folder! ("+inputFolderPath+")")

    val geneTSVFiles = Utils.getFilesFromFolder(inputFolderGene, "tsv")
    val transcriptTSVFiles = Utils.getFilesFromFolder(inputFolderTranscript, "tsv")

    println("Found "+(geneTSVFiles.length+transcriptTSVFiles.length)+" tsv files.")
    println("Start transformation...")

    transformFiles(geneTSVFiles, gtf, outputFolderPath+File.separator+"gene_quantification")
    transformFiles(transcriptTSVFiles, gtf, outputFolderPath+File.separator+"transcript_quantification")

    println("All tsv files has been transformed.")

  }

  //Iterate the array tsvFiles and transform each tsv
  private def transformFiles(tsvFiles : Array[File], gtf: GTF, outputFolderPath : String) : Unit =
  {
    val outputFolder = new File(outputFolderPath)
    if(outputFolder.exists() && !outputFolder.isDirectory)throw new Exception("Inavlid output folder!")
    else if(!outputFolder.exists() && !outputFolder.mkdirs())throw new Exception("Error while creating output folder!")

    for(tsvFile <- tsvFiles){

      print("Transforming "+tsvFile.getName+"... ")
      val tsv = new TSV(tsvFile)
      val outputFile = new File(outputFolderPath+File.separator+tsv.getFileName+".transformed.tsv")
      val printWriter = new PrintWriter(new FileWriter(outputFile))
      writeHeader(printWriter, tsv.isGeneQuantification)

      tsv.foreach((id, map) =>
      {
        val gtfEntry = gtf.get(id, tsv.isGeneQuantification)

        writeData(map, gtfEntry, printWriter, tsv.isGeneQuantification)
        printWriter.flush()
      })

      printWriter.flush()
      printWriter.close()

      println("Done.")
    }
  }

  //Write the data of each line of the current tsv file
  private def writeData(tsvDataMap: mutable.HashMap[String, String], gtfEntry : GTFEntry, printWriter: PrintWriter, isGeneQuantification : Boolean) : Unit =
  {
    val outputFileConfig = if(isGeneQuantification) gQOutputFileConfig else tQOutputFileConfig

    for(i <- outputFileConfig.indices){
      val source = outputFileConfig(i)._1
      val key = outputFileConfig(i)._2

      if(source.equals("tsv"))printWriter.print(tsvDataMap.getOrElse(key, "NULL"))
      else if(source.equals("gtf"))printWriter.print(gtfEntry.get(key))
      if(i < outputFileConfig.length-1)printWriter.print("\t")
    }

    printWriter.println()
  }


  //Write the header of the current tsv file
  private def writeHeader(printWriter : PrintWriter, isGeneQuantification : Boolean): Unit =
  {
    val outputFileConfig = if(isGeneQuantification) gQOutputFileConfig else tQOutputFileConfig

    for(i <- outputFileConfig.indices){
      printWriter.print(outputFileConfig(i)._2)
      if(i < outputFileConfig.length-1)printWriter.print("\t")
    }

    printWriter.println()
  }

  private def readConfigFile() : Unit =
  {
    println("Reading configuration file...")

    if(!validateXML("config/config.xml", "xsd/config.xsd"))throw new Exception("Invalid configuration file!")

    val configFile = XML.loadFile("config/config.xml")

    configFile.child.foreach(node =>
    {
      if(node.label.equals("input_folder") || node.label.equals("output_folder") || node.label.equals("gtf_folder")) foldersConfig.+=((node.label, node.text))
      else if(node.label.equals("gene_quantification_schema"))readNodeKeys(node, gQOutputFileConfig)
      else if(node.label.equals("transcript_quantification_schema"))readNodeKeys(node, tQOutputFileConfig)
    })

  }

  private def readNodeKeys(node : Node, keysArray : ArrayBuffer[(String, String)]) : Unit =
  {
    node.child.filter(node => node.label.equals("key")).foreach(keyNode =>{
      val source = keyNode.attribute("source").get.head.text
      keysArray.+=((source, keyNode.text))
    })
  }

  private def validateXML(xmlFilePath : String, xsdFilePath : String) : Boolean =
  {
    val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
    val xsdUrl = ClassLoader.getSystemResource(xsdFilePath)
    val schema: Schema = schemaFactory.newSchema(new StreamSource(xsdUrl.openStream()))

    val xmlUrl = new File(xmlFilePath).toURI.toURL

    val validator: Validator = schema.newValidator()

    try
    {
      validator.validate(new StreamSource(xmlUrl.openStream()))
      return true
    }
    catch {
      case e : SAXException => e.printStackTrace()
      case e : IOException => e.printStackTrace()
    }
    false
  }

}

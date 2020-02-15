package transformer

import java.io.{File, FileWriter, PrintWriter}

import files.Utils
import files.gtf.{GTF, GTFEntry, GTFLoader}
import files.tsv.TSV

import scala.collection.mutable

object Transformer
{

  final val TRANSCRIPT_OUTPUT_FILE_COLUMNS_GTF = Array(GTF.KEY_CHR, GTF.KEY_START, GTF.KEY_STOP, GTF.KEY_STRAND,
    GTF.KEY_TRANSCRIPT_ID, GTF.KEY_GENE_NAME, GTF.KEY_GENE_TYPE, GTF.KEY_TRANSCRIPT_NAME, GTF.KEY_TRANSCRIPT_TYPE)
  final val TRANSCRIPT_OUTPUT_FILE_COLUMNS_TSV = Array(TSV.KEY_GENE_ID, TSV.KEY_TPM, TSV.KEY_FPKM, TSV.KEY_POSTERIOR_MEAN_COUNT,
    TSV.KEY_POSTERIOR_STANDARD_DEVIATION_OF_COUNT, TSV.KEY_PME_TPM, TSV.KEY_PME_FPKM, TSV.KEY_TPM_CI_LOWER_BOUND, TSV.KEY_TPM_CI_UPPER_BOUND, TSV.KEY_FPKM_CI_LOWER_BOUND)

  final val GENE_OUTPUT_FILE_COLUMNS_GTF = Array(GTF.KEY_CHR, GTF.KEY_START, GTF.KEY_STOP, GTF.KEY_STRAND, GTF.KEY_GENE_NAME, GTF.KEY_GENE_TYPE)
  final val GENE_OUTPUT_FILE_COLUMNS_TSV = Array(TSV.KEY_GENE_ID, TSV.KEY_TPM, TSV.KEY_FPKM, TSV.KEY_POSTERIOR_MEAN_COUNT,
    TSV.KEY_POSTERIOR_STANDARD_DEVIATION_OF_COUNT, TSV.KEY_PME_TPM, TSV.KEY_PME_FPKM, TSV.KEY_TPM_CI_LOWER_BOUND, TSV.KEY_TPM_CI_UPPER_BOUND, TSV.KEY_FPKM_CI_LOWER_BOUND)

  def main(args: Array[String]): Unit = {

    transform("tsv", "transformed")

  }

  //Load the gtf files, search the tsv files in the input folder, and transform each tsv files
  def transform(inputFolderPath : String, outputFolderPath : String): Unit =
  {
    println("Loading gencode files...")
    val gtf = GTFLoader.loadFolder("gencode_files")
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
  def transformFiles(tsvFiles : Array[File], gtf: GTF, outputFolderPath : String) : Unit =
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
  def writeData(tsvDataMap: mutable.HashMap[String, String], gtfEntry : GTFEntry, printWriter: PrintWriter, isGeneQuantification : Boolean) : Unit =
  {
    val outputFileColumnsGTF = if(isGeneQuantification) GENE_OUTPUT_FILE_COLUMNS_GTF else TRANSCRIPT_OUTPUT_FILE_COLUMNS_GTF
    val outputFileColumnsTSV = if(isGeneQuantification) GENE_OUTPUT_FILE_COLUMNS_TSV else TRANSCRIPT_OUTPUT_FILE_COLUMNS_TSV

    outputFileColumnsGTF.foreach(key => printWriter.print(gtfEntry.get(key)+"\t"))

    for(i <- outputFileColumnsTSV.indices){
      printWriter.print(tsvDataMap(outputFileColumnsTSV(i)))
      if(i < outputFileColumnsTSV.length-1)printWriter.print("\t")
    }

    printWriter.println()
  }


  //Write the header of the current tsv file
  def writeHeader(printWriter : PrintWriter, isGeneQuantification : Boolean): Unit =
  {
    val outputFileColumnsGTF = if(isGeneQuantification) GENE_OUTPUT_FILE_COLUMNS_GTF else TRANSCRIPT_OUTPUT_FILE_COLUMNS_GTF
    val outputFileColumnsTSV = if(isGeneQuantification) GENE_OUTPUT_FILE_COLUMNS_TSV else TRANSCRIPT_OUTPUT_FILE_COLUMNS_TSV

    outputFileColumnsGTF.foreach(key => printWriter.print(key+"\t"))

    for(i <- outputFileColumnsTSV.indices){
      printWriter.print(outputFileColumnsTSV(i))
      if(i < outputFileColumnsTSV.length-1)printWriter.print("\t")
    }

    printWriter.println()
  }

}

package files.gtf

import java.io.File

import files.Utils

import scala.collection.mutable
import scala.io.Source
import scala.util.control.Breaks.{break, breakable}

object GTFLoader
{

  //Indices of the columns of the gencode file
  private final val COL_CHR = 0
  private final val COL_FEATURE_TYPE = 2
  private final val COL_START = 3
  private final val COL_STOP = 4
  private final val COL_STRAND = 6
  private final val COL_DATA = 8


  def load(path : String) : GTF =
  {
    val gtf = new GTF
    loadOn(path, gtf)
  }


  def loadOn(path : String, gtf : GTF) : GTF =
  {
    loadOn(new File(path), gtf)
  }

  def loadOn(gtfFile : File, gtf : GTF) : GTF =
  {
    val gencodeFile = Source.fromFile(gtfFile)

    for(line <- gencodeFile.getLines()){
      breakable{
        if(line.startsWith("##"))break

        val gencodeColumns = line.split("\t")
        val featureType = gencodeColumns(COL_FEATURE_TYPE)
        val isTrnaScan = featureType.equals("tRNAscan")
        val isGene = featureType.equals("gene")
        val isTranscript = featureType.equals("transcript")

        if(!(isGene || isTranscript || isTrnaScan))break

        //Map the additional information
        val lineMap = mapGTFData(gencodeColumns(COL_DATA))

        val geneId = lineMap(GTF.KEY_GENE_ID)
        val transcriptId = lineMap.getOrElse(GTF.KEY_TRANSCRIPT_ID, "")

        val id = if(isTranscript) transcriptId else geneId

        //Add the coordinates in the map
        lineMap.+=((GTF.KEY_CHR, gencodeColumns(COL_CHR)),
          (GTF.KEY_START, gencodeColumns(COL_START)),
          (GTF.KEY_STOP, gencodeColumns(COL_STOP)),
          (GTF.KEY_STRAND, gencodeColumns(COL_STRAND)))

        gtf.addLine(id, lineMap, isGene, isTranscript)
      }
    }
    gtf
  }

  def loadFolder(folderPath : String) : GTF =
  {
    val gtf = new GTF

    val gtfFiles = Utils.getFilesFromFolder(folderPath, "gtf")
    gtfFiles.foreach(file => loadOn(file, gtf))

    gtf
  }

  /**
    *
    * @param data The additional information contained in the ninth column of files.gtf.GTF file (format: key "value"; )
    * @return A HashMap that contains all the information divided by key and values
    */
  private def mapGTFData(data : String) : mutable.HashMap[String, String] =
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


}

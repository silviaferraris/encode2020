import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.control.Breaks.{break, breakable}

object Transformer
{
 // esempi di file con i quali provo a fare il matching
  final val TSV_1_PATH = "tsv/gene-transcript_quantification/ENCFF918DYI(TSV_1).tsv"
//  final val TSV_2_PATH = "tsv/gene-transcript_quantification/ENCFF688EJQ(TSV_2).tsv"
//  final val TSV_3_PATH = "tsv/gene-transcript_quantification/ENCFF075GHV(TSV_3).tsv"
//  final val TSV_4_PATH = "tsv/gene-transcript_quantification/ENCFF826APU(TSV_4).tsv"
//  final val TSV_5_PATH = "tsv/gene-transcript_quantification/ENCFF998SOW(TSV_5).tsv"
//  final val TSV_6_PATH = "tsv/gene-transcript_quantification/ENCFF370ZGU(TSV_6).tsv"
//  final val TSV_7_PATH = "tsv/trna/ENCFF520VDX(TSV_7).tsv"
//"schema" dei vari file tsv diversi---file.counts di Anna
  //gene-transcript_quantification/trna (0/1)  gen_id/transcript_id header_length
  final val TSV_1 = Array(0, 0, 1)
 /* final val TSV_2 = Array(0, 1, 1)
  final val TSV_3 = Array(0, 0, 1)
  final val TSV_4 = Array(0, 0, 4)
  final val TSV_5 = Array(0, 7, 0)
  final val TSV_6 = Array(0, 0, 2)*/

  final val TSV_7 = Array(1, 0, 1)

  final val GENE_ID_PREFIX = "ENSG"
  final val TRANSCRIPT_ID_PREFIX = "ENST"

  //file di annotazione a cui riferirsi per fare il matching
  final val GENE_TRANSCRIPT_QUANTIFICATION_GRCH38_PATH : String = "annotationfiles/gencode.v24.primary_assembly.annotation.gtf"
  final val TRNA_GRCH38_PATH : String = "annotationfiles/gencode.v24.tRNAs.gtf"
  final val GENE__TRANSCRIPT_QUANTIFICATION_HG19_PATH: String = "annotationfiles/gencode.v19.annotation.gtf_withproteinids"
  final val TRNA_HG19_PATH : String = "annotationfiles/gencode.v19.tRNAs.gtf"

  def main(args: Array[String]): Unit = {
    transform(TSV_1_PATH)
  }

  def transform(tsvFilePath : String): Unit = {
     // APRO I FILE DI ANNOTAZIONE
    val geneTranscriptAnnotationFileGrch38 = Source.fromResource(GENE_TRANSCRIPT_QUANTIFICATION_GRCH38_PATH)
    val geneTranscriptAnnotationFileHg19 = Source.fromResource(GENE__TRANSCRIPT_QUANTIFICATION_HG19_PATH)
    val trnaAnnotationFileGrch38 = Source.fromResource(TRNA_GRCH38_PATH)
    val trnaAnnotationFileHg19 = Source.fromResource(TRNA_HG19_PATH)

    //creo 2 ArrayBuffer, in uno ci metto tutte le linee dei quantification files
    //e nell'altro tutte le linee dei trna files( li accorpo dentro due mega array)

    val quantificationLines : ArrayBuffer[String] = new ArrayBuffer[String]()
    quantificationLines.++=:(geneTranscriptAnnotationFileGrch38.getLines())
    quantificationLines.++=:(geneTranscriptAnnotationFileHg19.getLines())

    val trnaLines : ArrayBuffer[String] = new ArrayBuffer[String]()
    trnaLines.++=:(trnaAnnotationFileGrch38.getLines())
    trnaLines.++=:(trnaAnnotationFileHg19.getLines())

    val tsvFile = Source.fromResource(tsvFilePath) // apro il file tsv scaricato
    val tsvLines = tsvFile.getLines().toArray[String] // lo trasformo in un array per trattare meglio i vari campi del file
    tsvFile.close()

    val coordinates : ArrayBuffer[String] = new ArrayBuffer[String]()

    getIdsFromTsv(tsvLines).foreach(id => {
      var isGene : Boolean = false
      var annotationFile: ArrayBuffer[String] = null
      if(id.startsWith(GENE_ID_PREFIX)){
        annotationFile = quantificationLines
        isGene = true
      }
      else if (id.startsWith(TRANSCRIPT_ID_PREFIX))
        annotationFile = quantificationLines
      else
        annotationFile = trnaLines

      annotationFile.foreach(line => {
        if (!line.startsWith("##")){
          val columns = line.split("\t")
          var typeLine = columns.apply(2)
          if(isGene && typeLine.equals("gene")){
            val data = columns(8).split(";").transform(d => d.trim)
            var geneId = data(0).split(" ")(1)
            geneId = geneId.substring(1, geneId.length-1) // estrapolo l'ENSG dell'annotation file
          }
        }
      })
    })


    /*val tsvLines = Source.fromResource(TSV_1_PATH).getLines().toArray[String]
    val tsvType = getTsvType(tsvLines(0))
    val ids = getGenIdsFromTsv(tsvLines, tsvType)

    val gencodeFiles : Array[String] = new Array[String](2)
    if(tsvType(0) == 0){
      gencodeFiles(0) = GENE_TRANSCRIPT_QUANTIFICATION_GRCH38
      gencodeFiles(1) = GENE__TRANSCRIPT_QUANTIFICATION_HG19
    }
    else{
      gencodeFiles(0) = TRNA_GRCH38 // tRNA
      gencodeFiles(1) = TRNA_HG19 // tRNA
    }

    val coordinates = findCoordinates(ids, tsvType(0) == 0, gencodeFiles)

    coordinates.foreach(coords => println(coords))*/

  }

  def findCoordinates(ids : Array[String], isGene : Boolean, gencodeFiles : Array[String]): Array[String] =
  {
    val coordinates : ArrayBuffer[String] = new ArrayBuffer[String]()
    for(id <- ids){
      print("check id: "+id+": ")
      var found = false
      breakable{

        for(gencodeFilePath <- gencodeFiles){

          if(found)break()

          val gencodeFile = Source.fromResource(gencodeFilePath)

          for(genocodeLine <- gencodeFile.getLines()){
            if(found)break
            breakable{
              if(genocodeLine.startsWith("##"))break
              val columns = genocodeLine.split("\t")

              breakable
              {
                if(isGene && !columns(2).equals("gene"))break
                val data = columns(8).split(";").transform(d => d.trim)
                var geneId = data(0).split(" ")(1)
                geneId = geneId.substring(1, geneId.length-1)
                if(!id.equals(geneId))break
                val coords = columns(0)+"\t"+columns(3)+"\t"+columns(4)+"\t"+columns(6)
                coordinates.+=(coords)
                found = true
              }
            }
          }
          gencodeFile.close()
        }

      }
      if(!found)coordinates.+=("")
      println(found)
    }
    coordinates.toArray[String]
  }

  //prende gli id della prima colonna del file tsv scaricato e li mette in un unico array
  def getIdsFromTsv(tsvLines : Array[String]) : Array[String] =
  {
    tsvLines.map(line => line.split("\t")).map(columns => columns(0)) //estrae gli id dalla prima colonna- programmazione funzionale
  }

//gene-transcript_quantification---get the gene_ids
  def getGenIdsFromTsv(lines : Array[String], tsvType : Array[Int]) : Array[String] =
  {
    val ids : ArrayBuffer[String] = new ArrayBuffer[String]()

    for(i <- lines.indices){
      breakable{
        if(i < tsvType.apply(2))break
      }
      val line = lines.apply(i)
      val columns = line.split("\t")
      val id = columns.apply(tsvType.apply(1))
      if(id.startsWith("ENSG"))
      {
        ids.+=(id)
      }
    }

    ids.toArray[String]
  }
  //trna---get the transcript_ids

  def getTransIdsFromTsv(lines : Array[String], tsvType : Array[Int]) : Array[String] =
  {
    val ids : ArrayBuffer[String] = new ArrayBuffer[String]()

    for(i <- lines.indices){
      breakable{
        if(i < tsvType.apply(2))break
      }
      val line = lines.apply(i)
      val columns = line.split("\t")
      val id = columns.apply(tsvType.apply(1))
      ids.+=(id)
    }

    ids.toArray[String]
  }

  /*def getTsvType(line : String): Array[Int] =
  {
    val columns = line.split("\t")

    columns.apply(0) match {
        case "gene_id" => TSV_1
        case "test_id" => TSV_2
        case "#transcript" => TSV_3
        case "N_unmapped" => TSV_4 // non rinserirlo al momento
        case "chr3" => TSV_5 // ---
        case "#" => TSV_6 // ---
        case _ => null
    }
  }*/
}

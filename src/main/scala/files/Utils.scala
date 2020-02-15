package files

import java.io.{File, FileFilter}

object Utils
{
  def getFilesFromFolder(folderPath : String, fileFormat : String): Array[File] =
  {
    val dir = new File(folderPath)
    getFilesFromFolder(dir, fileFormat)
  }

  def getFilesFromFolder(dir : File, fileFormat : String): Array[File] =
  {
    if(!dir.exists() || !dir.isDirectory)throw new Exception(dir.getAbsolutePath + " is not a valid folder path!")
    val files = dir.listFiles(new FileFilter {
      override def accept(pathname: File): Boolean = {
        pathname.isFile && pathname.getName.toLowerCase.endsWith(fileFormat.toLowerCase)
      }
    })
    files
  }
}

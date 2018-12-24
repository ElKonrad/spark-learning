object ResourceHelper {

  def getResourceFilepath(filename: String): String = getClass.getResource(filename).getPath
}

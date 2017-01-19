import com.typesafe.sbt.packager.MappingsHelper._

name := "wasp-template"

version := "1.0"

scalaVersion := "2.10.6"

// add WASP repository
resolvers += Resolver.bintrayRepo("agile-lab-dev", "WASP")
resolvers += Resolver.bintrayRepo("agile-lab-dev", "Spark-Solr")
resolvers += "Restlet Repository" at "http://maven.restlet.org"

// WASP dependencies
val waspVersion = "1.0.6"
val waspDependencies = Seq(
	"it.agilelab.bigdata.wasp" %% "wasp" % waspVersion,
	"it.agilelab.bigdata.wasp" %% "core" % waspVersion,
	"it.agilelab.bigdata.wasp" %% "consumers" % waspVersion,
	"it.agilelab.bigdata.wasp" %% "producers" % waspVersion,
	"it.agilelab.bigdata.wasp" %% "launcher" % waspVersion,
	"it.agilelab.bigdata.wasp" %% "webapp" % waspVersion
)
libraryDependencies ++= waspDependencies

// fix "Could not find creator property with name 'id' (in class org.apache.spark.rdd.RDDOperationScope)
// at [Source: {"id":"3","name":"transform"}; line: 1, column: 1]" error
dependencyOverrides ++= Set(
	"com.fasterxml.jackson.core" % "jackson-databind" % "2.4.4"
)

// set main class to the launcher implementation
mainClass in Compile := Some("it.agilelab.bigdata.wasp.pipegraph.metro.launchers.MetroLauncher")

/* Add any dependencies needed by the Spark executors at runtime here; their jars will be automagically added to the
 * spark-lib folder and will be available to them without further intervention.
 * Java and Scala dependencies are separated because the latter need the scalaVersion appended to their name.
 */
val sparkLibDependenciesJava: Seq[ModuleID]  = Seq()
val sparkLibDependenciesScala: Seq[ModuleID] = Seq()

// enable universal java packaging plugin
enablePlugins(JavaAppPackaging)

// fork a big JVM for runnning
fork in run := true
javaOptions in run += "-Xmx8G"


/**
	* ====================================================================================================================
	* BEGIN SBT DARK MAGIC - the code below is needed for WASP to work. Don't touch it unless you know what you're doing.
	* ====================================================================================================================
	*/

/* The code below automatically populates the spark-lib/managed subdirectory using custom SBT tasks.
 * You shouldn't ever have to modify it; if you need to add a jar which you don't get via dependencies, drop it into
 * spark-lib (NOT spark-lib/managed: it would get deleted!).
 */
// define helpers/contants
val sparkLibPath = "spark-lib/managed/"
val sparkLibListPath = "spark-lib.list"
def getJarFilesFromClasspath = (classpath: Seq[Attributed[File]], modules: Set[(String, String)]) => {
	val dummyModuleID = new ModuleID("", "", "", None)
	val sparkLibDependencies = classpath.filter {
		dep =>
			val m = dep.metadata.get(AttributeKey[ModuleID]("module-id")).getOrElse(dummyModuleID)
			modules((m.organization, m.name))
	}
	val sparkLibJarFiles = sparkLibDependencies.map {
		dep => dep.data.getAbsoluteFile
	}
	sparkLibJarFiles
}
// define tasks
lazy val populateSparkLib = taskKey[Seq[File]]("Populates the spark-lib directory")
populateSparkLib := {
	val log = streams.value.log
	val scalaSuffix = "_" + scalaVersion.value.split("\\.").take(2).mkString(".")
	
	log.info("Reading spark-lib list...")
	// get the wasp parent project jar
	val waspParentModule = Set(("it.agilelab.bigdata.wasp", "wasp" + scalaSuffix))
	val waspParentJar = getJarFilesFromClasspath((dependencyClasspath in Compile).value, waspParentModule)
		.head
		.getAbsoluteFile
	// extract it
	val tempDir = taskTemporaryDirectory.value
	val x = IO.unzip(waspParentJar, tempDir)
	// read spark-lib.list
	val sparkLibList = IO.read(tempDir / sparkLibListPath).split("\n")
	val waspDependenciesModules = sparkLibList.map(line => line.split(" ")).map(splitLine => (splitLine(0), splitLine(1))).toSet
	log.info("Done reading spark-lib list.")
	
	log.info("Populating spark-lib directory: \"" + sparkLibPath + "\".")
	
	log.info("Cleaning spark-lib... ")
	IO.delete(new File(sparkLibPath))
	log.info("Done cleaning.")
	
	log.info("Adding project dependency jars to spark-lib...")
	val projectDependenciesModules = {
		sparkLibDependenciesJava.map(dep => (dep.organization, dep.name)) ++
		sparkLibDependenciesScala.map(dep => (dep.organization, dep.name + scalaSuffix))
	}.toSet
	val projectDependenciesJars = getJarFilesFromClasspath((dependencyClasspath in Compile).value, projectDependenciesModules)
	log.info("Adding: " + projectDependenciesJars.mkString(","))
	IO.copy(projectDependenciesJars.map(jar => (jar, new File(sparkLibPath + jar.getName))))
	log.info("Done adding project dependency jars.")
	
	log.info("Adding WASP dependency jars to spark-lib...")
	val waspDependenciesJars = getJarFilesFromClasspath((dependencyClasspath in Compile).value, waspDependenciesModules)
	log.info("Adding: " + waspDependenciesJars.mkString(","))
	IO.copy(waspDependenciesJars.map(jar => (jar, new File(sparkLibPath + jar.getName))))
	log.info("Done adding WASP dependency jars.")
	
	log.info("Adding WASP jars to spark-lib...")
	val waspModules = waspDependencies.map(dep => (dep.organization, dep.name + scalaSuffix)).toSet
	val waspJars = getJarFilesFromClasspath((dependencyClasspath in Compile).value, waspModules)
	log.info("Adding: " + waspJars.mkString(","))
	IO.copy(waspJars.map(jar => (jar, new File(sparkLibPath + jar.getName))))
	log.info("Done adding WASP jars.")
	
	Seq.empty[File]
}
// add tasks to resource generators
resourceGenerators in Compile += populateSparkLib.taskValue
// force order
packageBin in Universal <<= (packageBin in Universal).dependsOn(populateSparkLib)

// add the spark-lib directory to the zip
mappings in Universal ++= directory(sparkLibPath)

// append hadoop and spark conf dirs environment variables to classpath
scriptClasspath += ":$HADOOP_CONF_DIR:$YARN_CONF_DIR"

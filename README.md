Peapod
==============

Peapod is a dependency and data pipeline management framework for Spark and Scala. The goals were to provide a framework that is simple to use, automatically saves/loads the output of tasks, and provides support for versioning. It is a work in progress and still very much experimental so new versions may introduce breaking changes.

Please let us know what you think and follow our blog [Teaching Machines](www.teachingmachines.io).

# Getting Started

If you're using maven then just add this or clone the repository (and then run mvn install):
```
<dependency>
    <groupId>io.teachingmachines</groupId>
    <artifactId>peapod</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

# Using

The first step is to create a Task. A StorableTask will automatically save and load the output of the Task's generate method (in this case a RDD[(Long,String)]) to disk. If an appropriate serialized version exists it will be loaded, otherwise it will be generated and then saved. We'll discuss where the storage happens a bit further down. StorableTask can only serialize, currently, RDDs, DataFrames and Serializable classes. You can override the read and write methods to save/laod other types of classes.
```
class RawPages(implicit _p: Peapod) extends StorableTask[RDD[(Long, String)]] {
  def generate =
    readWikiDump(p.sc, p.fs + p.raw + "/enwiki-20150304-pages-articles.xml.bz2")
}
```
Then you can create other tasks which depend on this task. You use the dep() method to wrap a task to have it be a dependency of the current task. You can then use the get() method of the dependency to access the output of the dependencies generate method. The outputs are cached so even if you create multiple instances of a Task (within a single Peapod, we'll get to that in a bit) their get methods will all point to the same data.
```
class ParsedPages(implicit _p: Peapod) extends StorableTask[RDD[Page]] {
  val rawPages = dep(new RawPages())
  def generate =
    parsePages(rawPages.get()).map(_._2)
        //Remove duplicate pages with the same title
        .keyBy(_.title).reduceByKey((l,r) => l).map(_._2)
}
```
To actually run Tasks you need to create a Peapod which "holds" tasks, keeps shared variables (such as where Task outputs are saved/loaded from) and manages shared state between Task instances. In the below example Task state would be stored in "s3n://tm-bucket/peapod-pipeline". If you use s3 be sure to set the appropriate values for fs.s3n.awsAccessKeyId and fs.s3n.awsSecretAccessKey in the SparkContext's hadoopConfiguration.
```
implicit val p = new Peapod(
  fs="s3n://",
  path="tm-bucket/peapod-pipeline",
  raw="tm-bucket/peapod-input"
)
```
You can now run Task's and get their outputs. Dependencies will be automatically run if necessary if the current Task isn't saved to disk.
```
new ParsedPages().get().count()
```
Tasks support versioning and the versioning flows through the pipeline so all dependent Tasks are also re-generated if the version of their dependency changes. For the below would cause ParsedPages and RawPages to be re-run even if they had previously had their output stored.
```
class RawPages(implicit _p: Peapod) extends StorableTask[RDD[(Long, String)]] {
  override val version = "2"
  def generate =
    readWikiDump(p.sc, p.fs + p.raw + "/enwiki-20150304-pages-articles.xml.bz2")
}
new ParsedPages().get().count()
```

# Detailed Description

In progress.

## Ephemeral Task
This is a Task which never saves or loads it's state from disk but always runs the generate method. This is useful for quick Tasks or Tasks which only run for a short period of time.
```
class RawPages(implicit _p: Peapod) extends EphemeralTask[RDD[(Long, String)]] {
  def generate =
    readWikiDump(p.sc, p.fs + p.raw + "/enwiki-20150304-pages-articles.xml.bz2")
}
```

# Known Issues
 * StorableTasks whose generate only performs a map operation on an RDD will not work if S3 is the storage location. You can get around this by running a repartition in the generate.

# The Future
 * Automatic Versioning: Give the option to use the actual code of the task to generate the version and automatically change the version if the code changes.
 * Unit Tests
 * Cleanup
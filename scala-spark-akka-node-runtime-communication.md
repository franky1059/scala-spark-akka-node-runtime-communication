

Spark Programmatic Node Communication
--------------------------------------
#### Distributed Computing
Distributed and parallel data processing has been discussed and explained a million different ways in a million different places, so we'll keep it brief here. Some of the best explanations as it pertains to open source big data frameworks are by [Digital Ocean](https://www.digitalocean.com/community/tutorials/hadoop-storm-samza-spark-and-flink-big-data-frameworks-compared), [Towards Data Science](https://towardsdatascience.com/apache-spark-101-3f961c89b8c5), and [Quora](https://www.quora.com/What-exactly-is-Apache-Spark-and-how-does-it-work-What-does-a-cluster-computing-system-mean) threads.

#### Spark and the Akka Actor System
Spark (pre v1.6) used the Akka distributed processing framework to manage master-worker server relationships and divide cluster resources across nodes to process data jobs. Here's some [background info](https://medium.com/akka-for-newbies/akka-actors-explained-dc9068c49568) on what the Akka system is. There's also some good links on their website about [actors](https://doc.akka.io/docs/akka/current/general/actors.html) and [actor systems](https://doc.akka.io/docs/akka/current/general/actor-systems.html). <br />
A crude summary (which I hope is correct) of how it all works is that Spark implements a processing cluster by communicating with different server OS's and running Akka daemons on them, which are orchestrated by the master node which manages an Akka actor system. When it's time to run a Spark job, e.g. RDD actions or streaming, Akka is used to send messages between the nodes to organize system resources and execute the intended process. 

#### Spark Job Execution Process
When we execute a spark job to process data using the resources across all the nodes in a cluster there's a series of steps the Spark framework performs to load and run the job. The entry point is through the [spark-submit](https://github.com/apache/spark/blob/v1.1.0/bin/spark-submit) shell command, which is essentially a wrapper for the [spark-class](https://github.com/apache/spark/blob/v1.1.0/bin/spark-class) shell command. The spark-class command collects and organizes all relevant passed arguments to ultimately run a compiled Spark class called [SparkSubmit](https://github.com/apache/spark/blob/v1.1.0/core/src/main/scala/org/apache/spark/deploy/SparkSubmit.scala).  SparkSubmit absorbs all of the passed-in command-line args, makes the decision as to which cluster manager to use (pre v1.6 there was Standalone, Akka Cluster, Yarn, or Mesos), it then instantiates an executor to run the desired program and submits all of this information to the master node. 

#### Spark REST API vs Programmatic Communication
Now we get to the main purpose of this article, which is to discuss how to communicate with an existing job running inside of a Spark cluster. We might want to do this for a number reasons, a very common one would be to check the status of an existing job, or maybe to just verify that a submitted job was properly started or not. <br />
The focus of this article is to discuss programmatic communication with running Spark cluster nodes, but it's important to note that Spark ships with an [http REST api](https://spark.apache.org/docs/latest/monitoring.html#rest-api) that also serves this purpose nicely. So it's most likely much easier to use some [shell scripts](https://gist.github.com/arturmkrtchyan/5d8559b2911ac951d34a) and communicate with your cluster [over http](https://stackoverflow.com/questions/33495623/how-to-get-all-jobs-status-through-spark-rest-api) rather than using something much more low-level, like java or scala, to do it. <br />
It just so happens there was a particular use-case that required the use of programmatic communication, which sent us down the road of using the Spark stack itself to communicate with a running cluster, and that is what we are going to demonstrate here. I believe it was something network ops related - for example they didn't want to open up dedicated ports for the http api, they only wanted to open ports at the time of job execution then shut those ports when the job was done. That combined with the reluctance to have any kind of http web traffic in the network at all sent us down the solution of communicating with nodes using the ports opened for the Akka actor system. Sounds crazy, but that's just par for the course when dealing with big corporate network operations departments - there's no shortage of paranoia when it comes to port security. 

#### Communicating with Nodes
The brass-tacks of how we talk to jobs that are running in Spark-Akka cluster mode is to use the Akka framework itself to issue "ask and reply" commands to see what's going on in the cluster. Spark ships with wrappers for these [Akka functions](https://github.com/apache/spark/blob/v1.1.0/core/src/main/scala/org/apache/spark/util/AkkaUtils.scala) in the form of utilities which we can wrap ourselves, then make necessary requests to our cluster. Spark code has a lot of source level security so be sure to put your Akka wrappers in the same package space...
<pre>
package org.apache.spark

object MySparkAkkaUtility {


	def askWithReply[T](
      message: Any,
      actor: ActorRef,
      retryAttempts: Int,
      retryInterval: Int,
      timeout: FiniteDuration): T = {
	    AkkaUtils.askWithReply(message, actor, retryAttempts, retryInterval, timeout)
	}
}
</pre>
When you look inside the [AkkaUtils.askWithReply()](https://github.com/apache/spark/blob/v1.1.0/core/src/main/scala/org/apache/spark/util/AkkaUtils.scala#L159) function, you can see there's lots of code for making and retrying network requests - which is a good thing bcs we call know that network communication has it's reliability faults so there should always be plans for redundancy and validation. <br />
You'll also notice the use of [Scala futures](https://alvinalexander.com/scala/concurrency-with-scala-futures-tutorials-examples), which is an implementation of concurrent execution, with the use of [Await](https://www.scala-lang.org/api/2.10.1/index.html#scala.concurrent.Await$). This is important bcs making multiple network calls is a form of programmatic use of shared I/O, which means resource blocking, which means our application needs to accommodate for this or else it'll either fail or hang. <br /> 
We use the Akka system to send/receive messages across the cluster and it's important to note that the messages that we can send. For a list of all the commands we can ask and what responses to expect in return we can look at the Spark code for the [Master](https://github.com/apache/spark/blob/v1.1.0/core/src/main/scala/org/apache/spark/deploy/master/Master.scala) and [Client](https://github.com/apache/spark/blob/v1.1.0/core/src/main/scala/org/apache/spark/deploy/Client.scala) nodes. We can also see the [defined communication messages](https://github.com/apache/spark/blob/v1.1.0/core/src/main/scala/org/apache/spark/deploy/DeployMessage.scala).  
<pre>
val conf_master_url = sparkContext.getConf.get("spark.master")
val master_state_future = (SparkEnv.get.actorSystem.actorFor(Master.toAkkaUrl(conf_master_url)) ? RequestMasterState)(timeout).mapTo[MasterStateResponse]
val master_state = Await.result(master_state_future, timeout)
</pre>



Code
--------------------------------------
- [scala-spark-akka-node-runtime-communication (GitHub)](https://github.com/franky1059/scala-spark-akka-node-runtime-communication)



Links
--------------------------------------
- [Hadoop, Storm, Samza, Spark, and Flink: Big Data Frameworks Compared (Digital Ocean)](https://www.digitalocean.com/community/tutorials/hadoop-storm-samza-spark-and-flink-big-data-frameworks-compared)
- [A n00bs guide to Apache Spark](https://towardsdatascience.com/apache-spark-101-3f961c89b8c5)
- [What exactly is Apache Spark and how does it work? What does a cluster computing system mean?](https://www.quora.com/What-exactly-is-Apache-Spark-and-how-does-it-work-What-does-a-cluster-computing-system-mean)
- [Why Spark 1.6 does not use Akka?](https://stackoverflow.com/questions/37448757/why-spark-1-6-does-not-use-akka)
- [Akka Actors Explained (Medium)](https://medium.com/akka-for-newbies/akka-actors-explained-dc9068c49568)
- [Akka Actor Systems (Akka Docs)](https://doc.akka.io/docs/akka/current/general/actor-systems.html)
- [Akka Actors (Akka Docs)](https://doc.akka.io/docs/akka/current/general/actors.html)
- [spark-submit (Spark 1.1.0)](https://github.com/apache/spark/blob/v1.1.0/bin/spark-submit)
- [spark-class (Spark 1.1.0)](https://github.com/apache/spark/blob/v1.1.0/bin/spark-class)
- [SparkSubmit.scala (Spark 1.1.0)](https://github.com/apache/spark/blob/v1.1.0/core/src/main/scala/org/apache/spark/deploy/SparkSubmit.scala)
- [Master.scala (Spark 1.1.0)](https://github.com/apache/spark/blob/v1.1.0/core/src/main/scala/org/apache/spark/deploy/master/Master.scala)
- [Client.scala (Spark 1.1.0)](https://github.com/apache/spark/blob/v1.1.0/core/src/main/scala/org/apache/spark/deploy/Client.scala)
- [DeployMessage.scala (Spark 1.1.0)](https://github.com/apache/spark/blob/v1.1.0/core/src/main/scala/org/apache/spark/deploy/DeployMessage.scala)
- [start-master.sh (Spark 1.1.0)](https://github.com/apache/spark/blob/v1.1.0/sbin/start-master.sh)
- [start-slave.sh (Spark 1.1.0)](https://github.com/apache/spark/blob/v1.1.0/sbin/start-slave.sh)
- [Monitoring and Instrumentation (Spark 1.1.0)](https://spark.apache.org/docs/latest/monitoring.html)
- [How to get all jobs status through spark REST API? (Spark 1.1.0)](https://stackoverflow.com/questions/33495623/how-to-get-all-jobs-status-through-spark-rest-api)
- [Spark Job Status API Scripts (dev blog)](https://gist.github.com/arturmkrtchyan/5d8559b2911ac951d34a)







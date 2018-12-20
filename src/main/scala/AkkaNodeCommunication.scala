// ../sbt/bin/sbt package
// ../sbt/bin/sbt "runMain AkkaNodeCommunication <fcn to run>"

package org.apache.spark.deploy

import scala.concurrent.Await
import scala.xml.Node

import akka.pattern.ask
import org.json4s.JValue

import org.apache.spark.deploy.JsonProtocol
import org.apache.spark.deploy.DeployMessages._
import org.apache.spark.deploy.master.{ApplicationInfo, DriverInfo, WorkerInfo}
import org.apache.spark.deploy.master.{DriverState, Master}

import com.typesafe.config.ConfigFactory
import org.apache.log4j.{Level, Logger}

import org.apache.spark.{Logging, SecurityManager, SparkConf, SparkEnv, SparkException}

import org.apache.spark.MySparkAkkaUtility




object AkkaNodeCommunication {


	def main(args: Array[String]) {

		var conf = new SparkConf(true).setAppName("AkkaNodeCommunication") 
		sparkContext = new SparkContext(conf)
	
		val akka_timeout = MySparkAkkaUtility.askTimeout(sparkContext.getConf)	
		val akka_retry_attempts: Int = MySparkAkkaUtility.numRetries(sparkContext.getConf)
		val akka_retry_interval_ms: Int = MySparkAkkaUtility.retryWaitMs(sparkContext.getConf)
		
		val master_url = SparkWrapper.sparkContext.getConf.get("spark.master")
		
		val node_state = MySparkAkkaUtility.askWithReply(RequestMasterState, SparkEnv.get.actorSystem.actorFor(Master.toAkkaUrl(master_url)), akka_retry_attempts, akka_retry_interval_ms, akka_timeout)[MasterStateResponse]
		
		println("node_state: ")
		println(node_state)		
	}


}
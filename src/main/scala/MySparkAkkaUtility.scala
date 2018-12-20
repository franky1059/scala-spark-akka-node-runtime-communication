

package org.apache.spark

import org.apache.spark.util.AkkaUtils

import scala.collection.JavaConversions.mapAsJavaMap
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem}
import akka.pattern.ask

import com.typesafe.config.ConfigFactory
import org.apache.log4j.{Level, Logger}

import org.apache.spark.{Logging, SecurityManager, SparkConf, SparkEnv, SparkException}






object MySparkAkkaUtility {


    def askWithReply[T](
      message: Any,
      actor: ActorRef,
      retryAttempts: Int,
      retryInterval: Int,
      timeout: FiniteDuration): T = {
        AkkaUtils.askWithReply(message, actor, retryAttempts, retryInterval, timeout)
    }
	
	
	def askTimeout(conf: SparkConf): FiniteDuration = {
	    AkkaUtils.askTimeout(conf)
	}


	def numRetries(conf: SparkConf): Int = {
    	AkkaUtils.numRetries(conf)
  	}

  	def retryWaitMs(conf: SparkConf): Int = {
	    AkkaUtils.retryWaitMs(conf)
	  }
	  
}
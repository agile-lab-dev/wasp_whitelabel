package it.agilelab.bigdata.wasp.pipegraph.metro.strategies

import it.agilelab.bigdata.wasp.consumers.strategies._
import it.agilelab.bigdata.wasp.core.models.TopicModel
import it.agilelab.bigdata.wasp.pipegraph.metro.models.MetroTopicModel.MetroTopic
import org.apache.spark.sql.DataFrame

case class MetroStrategy() extends Strategy {

  def transform(dataFrames: Map[ReaderKey, DataFrame]) = {

    val input = dataFrames.get(ReaderKey(TopicModel.readerType, "metro.topic")).get

    /** Put your transformation here. */

    input.filter(input("longitude") < -118.451683D)
  }

}
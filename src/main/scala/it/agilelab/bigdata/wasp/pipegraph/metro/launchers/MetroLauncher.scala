package it.agilelab.bigdata.wasp.pipegraph.metro.launchers

import it.agilelab.bigdata.wasp.core.models.{IndexModel, PipegraphModel, ProducerModel, TopicModel}
import it.agilelab.bigdata.wasp.core.utils.WaspDB
import it.agilelab.bigdata.wasp.launcher.WaspLauncher
import it.agilelab.bigdata.wasp.pipegraph.metro.models.{MetroIndexModel, MetroPipegraphModel, MetroProducerModel, MetroTopicModel}

/**
  * Created by matbovet on 18/07/2016.
  */
object MetroLauncher extends WaspLauncher {
  override def initializeCustomWorkloads(): Unit = {
    println("Custom workloads init!")

    WaspDB.getDB.insertIfNotExists[TopicModel](MetroTopicModel.metroTopic)
    WaspDB.getDB.insertIfNotExists[IndexModel](MetroIndexModel.metroIndex)
    WaspDB.getDB
      .insertIfNotExists[PipegraphModel](MetroPipegraphModel.metroPipegraph)
    WaspDB.getDB
      .insertIfNotExists[ProducerModel](MetroProducerModel.metroProducer)

  }
}

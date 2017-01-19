package it.agilelab.bigdata.wasp.pipegraph.metro.models

import com.typesafe.config.{Config, ConfigFactory}
import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.models.{IndexModel, StrategyModel, _}
import reactivemongo.bson.BSONObjectID

/**
  * Created by matbovet on 18/07/2016.
  */
object MetroPipegraphModel {

  lazy val metroPipegraphName = "MetroPipegraph6"

  lazy val metroPipegraph = MetroPipegraph()

  lazy val conf: Config = ConfigFactory.load

  lazy val defaultDataStoreIndexed = conf.getString("default.datastore.indexed")

  private[wasp] object MetroPipegraph {

    def apply() =
      PipegraphModel(
          name = MetroPipegraphModel.metroPipegraphName,
          description = "Los Angeles Metro Pipegraph",
          owner = "user",
          system = false,
          creationTime = WaspSystem.now,
          etl = List(
              ETLModel("write on index",
                       List(ReaderModel(MetroTopicModel.metroTopic._id.get,
                                        MetroTopicModel.metroTopic.name,
                                        TopicModel.readerType)),
                       WriterModel.IndexWriter(MetroIndexModel.metroIndex._id.get,
                                               MetroIndexModel.metroIndex.name, defaultDataStoreIndexed),
                       List(),
                       Some(StrategyModel("it.agilelab.bigdata.wasp.pipegraph.metro.strategies.MetroStrategy", None))
              )
          ),
          rt = Nil,
          dashboard = None,
          isActive = false,
          _id = Some(BSONObjectID.generate))

  }
}

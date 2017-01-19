package it.agilelab.bigdata.wasp.pipegraph.metro.models

import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.models.TopicModel
import it.agilelab.bigdata.wasp.core.utils.BSONFormats
import reactivemongo.bson.BSONObjectID

/**
  * Created by matbovet on 18/07/2016.
  */
object MetroTopicModel {

  lazy val metroTopic = MetroTopic()

  private[wasp] object MetroTopic {

    val topic_name = "Metro"

    def apply() = TopicModel(
      name = TopicModel.name(topic_name),
      creationTime = WaspSystem.now,
      partitions = 3,
      replicas = 1,
      topicDataType = "avro",    // avro or json
      schema = Some(BSONFormats.fromString(topicSchema).get),
      _id = Some(BSONObjectID.generate)
    )

    private val topicSchema = s"""
      {"type":"record",
      "namespace":"PublicTransportsTracking",
      "name":"LAMetro",
      "fields":[${TopicModel.schema_base},
        {"name":"last_update","type":"double"},
        {"name":"vehicle_code","type":"int"},
        {"name":"position", "type":{"name":"positionData", "type":"record", "fields":[{"name":"lat","type":"double"}, {"name":"lon","type":"double"}]}},
        {"name":"predictable","type":"boolean"}
  ]}"""

  }

}

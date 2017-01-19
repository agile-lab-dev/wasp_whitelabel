package it.agilelab.bigdata.wasp.pipegraph.metro.models

import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.models.IndexModel
import it.agilelab.bigdata.wasp.core.utils.BSONFormats
import reactivemongo.bson.BSONObjectID

/**
  * Created by matbovet on 18/07/2016.
  */
object MetroIndexModel {

  lazy val metroIndex = MetroIndex()

  private[wasp] object MetroIndex {

    val index_name = "Metro"

    def apply() = IndexModel(
        name = IndexModel.normalizeName(index_name),
        creationTime = WaspSystem.now,
        // Elastic
        schema = BSONFormats.fromString(indexSchemaElastic),
        // Solr
        //schema = BSONFormats.fromString(indexSchemaSolr),
        _id = Some(BSONObjectID.generate),
        query = None,
        // Work only with Solr
        numShards = Some(2),
        // Work only with Solr
        replicationFactor = Some(1)
    )

    private val indexSchemaElastic = s"""
      {"lametro":
          {
          "properties":{
            ${IndexModel.schema_base_elastic},
            "last_update":{"type":"double","index":"not_analyzed","store":"true","enabled":"true"},
            "vehicle_code":{"type":"integer","index":"not_analyzed","store":"true","enabled":"true"},
            "position":{"type":"geo_point","geohash": true, "geohash_prefix" : true, "geohash_precision": 7, "index":"not_analyzed","store":"true","enabled":"true"},
            "predictable":{"type":"boolean","index":"not_analyzed","store":"true","enabled":"true"}
          }}}"""

    private val indexSchemaSolr = s"""
      { "properties":
        [
          ${IndexModel.schema_base_solr},
          { "name":"last_update", "type":"tdouble", "stored":true },
          { "name":"vehicle_code", "type":"tint", "stored":true },
          { "name" :"position", "type" : "string", "stored" : true },
          { "name":"predictable", "type":"boolean", "stored":true }
        ]
      }"""

  }

}

package it.agilelab.bigdata.wasp.pipegraph.metro.producers

import java.util.Date

import akka.actor._
import it.agilelab.bigdata.wasp.core.WaspSystem
import it.agilelab.bigdata.wasp.core.WaspSystem._
import it.agilelab.bigdata.wasp.core.bl.{BatchSchedulersBL, _}
import it.agilelab.bigdata.wasp.core.models.{ProducerModel, TopicModel}
import it.agilelab.bigdata.wasp.core.utils.TimeFormatter
import it.agilelab.bigdata.wasp.master.MasterGuardian
import it.agilelab.bigdata.wasp.pipegraph.metro.models.MetroTopicModel
import it.agilelab.bigdata.wasp.producers._
import org.jboss.netty.handler.timeout.TimeoutException
import play.api.libs.json.{JsValue, Json}
import reactivemongo.bson.BSONArray

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.parsing.json.JSONArray
import scalaj.http.{Http, HttpOptions, HttpResponse}

final class MetroProducer(env: {val producerBL: ProducerBL; val topicBL: TopicBL}) extends ProducerMasterGuardian(env) {

  // http://developer.metro.net/introduction/realtime-api-overview/realtime-api-examples/
  // http://api.metro.net/agencies/lametro/vehicles/
  val name = "MetroProducer"

  val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
  val client = new play.api.libs.ws.ning.NingWSClient(builder.build())

  def startChildActors() = {
    logger.info(s"Starting get data on ${cluster.selfAddress}")

    // Retrive all the active vehicle.
    val url = s"http://api.metro.net/agencies/lametro/vehicles/"

    val response: HttpResponse[String] = Http(url).option(HttpOptions.readTimeout(5000)).asString
    val json: JsValue = Json.parse(response.body)

    val vehicles = (json \ "items").as[List[JsValue]]

    logger.info(s"Number of vehicles getted from the API: ${vehicles.size}")

    vehicles foreach { s =>
      println("Vehicle: " + s)
      val aRef = context.actorOf(Props(new MetroActor(s, kafka_router, associatedTopic)))
      println("created actor")
      aRef ! StartMainTask
      println("told actor to start main task")
    }
  }
}


/** For simplicity, these just go through Akka. */
private[wasp] class MetroActor(vehicle: JsValue, kafka_router: ActorRef, topic: Option[TopicModel]) extends ProducerActor[JsValue](kafka_router, topic) {

  override def preStart(): Unit = {
    logger.info(s"Starting tracking of vehicle code: ${(vehicle \ "id").as[String]}")
    println("in pre start")
  }

  def mainTask() = {
    println("in main task")
    logger.info(s"Starting main task for actor: ${this.getClass.getName}")

    task = Some(context.system.scheduler.schedule(Duration.Zero, 30 seconds) {

      try {
        logger.debug("Forwarding producer message to Kafka: " + (vehicle \ "id").as[String])
        sendMessage(vehicle)
      } catch {
        case e: TimeoutException => logger.warn(s"Unable to process content for the this vehicle cause of ${e.getCause}")
        case e: Exception => logger.warn(s"Unable to process content for the this vehicle cause of ${e.getStackTraceString}")
      }
    })

  }

  def generateOutputJsonMessage(inputJson: JsValue): String = {

    val id_event = s"${System.currentTimeMillis()}${(vehicle \ "id").as[String].toInt}".toDouble
    val source_name = "Metro Trains Position Tracking"
    val topic_name = topic.map(_.name).getOrElse(MetroTopicModel.metroTopic.name)
    val metric_name = (inputJson \\ "id").map(_.as[String]).headOption.getOrElse("")
    val ts = TimeFormatter.format(new Date())
    val latitude = (inputJson \\ "latitude").map(_.as[Double]).headOption.getOrElse(0D)
    val longitude = (inputJson \\ "longitude").map(_.as[Double]).headOption.getOrElse(0D)
    val value = (inputJson \\ "heading").map(_.as[Double]).headOption.getOrElse(0D)
    val payload = inputJson.toString().replaceAll("\"", "") //Required for string bonification
    val lastUpdate = System.currentTimeMillis() - ((inputJson \\ "seconds_since_report").map(_.as[Double]).headOption.getOrElse(0D) * 1000)
    val predictable = (inputJson \\ "predictable").map(t => t.asOpt[Boolean].getOrElse(false)).headOption.getOrElse(false)

    val myJson = s"""{
     "id_event":$id_event,
     "source_name":"$source_name",
     "topic_name":"$topic_name",
     "metric_name":"$metric_name",
     "timestamp":"$ts",
     "latitude":$latitude,
     "longitude":$longitude,
     "value":$value,
     "payload":"$payload",
     "last_update":$lastUpdate,
     "vehicle_code":${metric_name},
     "position":{"lat":$latitude,"lon":$longitude},
     "predictable":$predictable
     }"""

    myJson
  }

  def generateRawOutputJsonMessage(inputJson: JsValue): String = {

    /* The following mappings are just an example made to show the producer in action */
    val id_event = s"${System.currentTimeMillis()}${(vehicle \ "id").as[String].toInt}".toDouble
    val source_name = "Metro Trains Position Tracking"
    val topic_name = topic.map(_.name).getOrElse(MetroTopicModel.metroTopic.name)
    val metric_name = (inputJson \\ "id").map(_.as[String]).headOption.getOrElse("")
    val ts = TimeFormatter.format(new Date())
    val latitude = (inputJson \\ "latitude").map(_.as[Double]).headOption.getOrElse(0D)
    val longitude = (inputJson \\ "longitude").map(_.as[Double]).headOption.getOrElse(0D)
    val value = (inputJson \\ "heading").map(_.as[Double]).headOption.getOrElse(0D)
    val payload = inputJson.toString().replaceAll("\"", "") //Required for string bonification

    val myJson = s"""{
     "id_event":$id_event,
     "source_name":"$source_name",
     "topic_name":"$topic_name",
     "metric_name":"$metric_name",
     "timestamp":"$ts",
     "latitude":$latitude,
     "longitude":$longitude,
     "value":$value,
     "payload":"$payload"
     }"""

    myJson
  }

}

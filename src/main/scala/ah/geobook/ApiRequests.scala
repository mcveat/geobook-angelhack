package ah.geobook

import java.io.ByteArrayOutputStream
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import android.location.Location
import android.widget.{ScrollView, TextView}
import android.util.Log
import ah.geobook.GeoBook._
import scala.Some
import spray.json._
import DefaultJsonProtocol._
import org.apache.http.HttpStatus._
import android.view.View
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.http.{GenericUrl, HttpRequest, HttpRequestInitializer}
import com.google.api.client.googleapis.GoogleHeaders
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.client.json.JsonObjectParser

/**
 * User: mcveat
 */
abstract class ApiHttpRequest[T](view: View, params: Option[Map[String, String]] = None) extends Thread {
  protected def url: String
  protected def process(value: String): Option[T]
  protected def uiUpdate(value: Option[T])

  override def run() {
    val value = call(url).flatMap(process)
    view.post( () => uiUpdate(value) )
  }

  private def call(url: String): Option[String] = {
    val response = new DefaultHttpClient().execute(new HttpGet(url))
    if (response.getStatusLine.getStatusCode != SC_OK) return None
    val out = new ByteArrayOutputStream()
    response.getEntity.writeTo(out)
    out.close()
    Some(out.toString)
  }
}

class LoadAddress(location: Location, view: TextView) extends ApiHttpRequest[String](view) {
  override protected def process(value: String) = {
    val components = value.asJson.asJsObject.getFields("results").head.convertTo[List[JsValue]]
    val streetAddressComponent =
      components.map(_.asJsObject).find(_.getFields("types").head.convertTo[List[String]].contains("street_address"))
    streetAddressComponent.map(_.getFields("formatted_address").head.convertTo[String])
  }

  override protected def url =
    "http://maps.googleapis.com/maps/api/geocode/json?latlng=%s,%s&sensor=true"
      .format(location.getLatitude.toString, location.getLongitude.toString)

  override protected def uiUpdate(address: Option[String]) = {
    address match {
      case Some(s) => view.setText(s)
      case _ => Log.d(TAG, "Error fetching address")
    }
  }
}

case class LocalPlace(name: String)
class LoadLocalPlaces(location: Location, view: ScrollView) extends ApiHttpRequest[List[LocalPlace]](view) {
  protected def url = {
    val params = Map(
      "location" -> "%s,%s".format(location.getLatitude.toString, location.getLongitude.toString),
      "radius" -> (location.getAccuracy.toInt + 25).toString,
      "sensor" -> "true",
      "key" -> "AIzaSyDxjUzqoIACkfhXXbqTrZin0x_1LZ88mwg"
    ).toList.map {
      case (key, value) => "%s=%s".format(key, value)
    }.mkString("&")
    val a = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?%s".format(params)
    Log.d(TAG, a)
    a
  }

  protected def process(value: String): Option[List[LocalPlace]] = {
    Log.d(TAG, value)
    Some(List(LocalPlace("dupa")))
  }

  protected def uiUpdate(value: Option[List[LocalPlace]]) {}
}

class Load(location: Location) extends Thread {
  override def run() {
    val requestFactory = new NetHttpTransport().createRequestFactory(new HttpRequestInitializer {
      def initialize(request: HttpRequest) {
        val gHeaders = new GoogleHeaders()
        gHeaders.setApplicationName("GeoBook")
        request.setHeaders(gHeaders)
        val parser = new JsonObjectParser(new JacksonFactory())
        request.setParser(parser)
      }
    })
    val getRequest =
      requestFactory.buildGetRequest(new GenericUrl("https://maps.googleapis.com/maps/api/place/search/json?"))
    Map(
      "location" -> "%s,%s".format(location.getLatitude.toString, location.getLongitude.toString),
      "radius" -> (location.getAccuracy.toInt + 25).toString,
      "sensor" -> "true",
      "key" -> "AIzaSyDxjUzqoIACkfhXXbqTrZin0x_1LZ88mwg"
    ).toList.foreach {
      case (key, value) => getRequest.getUrl.put(key, value)
    }
    val content = getRequest.execute().parseAsString()
    Log.d(TAG, content)
  }
}

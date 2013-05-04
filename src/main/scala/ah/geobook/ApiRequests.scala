package ah.geobook

import java.io.ByteArrayOutputStream
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import android.location.Location
import android.widget.TextView
import android.util.Log
import ah.geobook.GeoBook._
import scala.Some
import spray.json._
import DefaultJsonProtocol._
import org.apache.http.HttpStatus._
import android.view.View

/**
 * User: mcveat
 */
abstract class ApiHttpRequest[T](view: View) extends Thread {
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

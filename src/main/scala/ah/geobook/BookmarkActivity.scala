package ah.geobook

import android.app.Activity
import android.os.Bundle
import android.content.Context._
import android.location.{Location, LocationListener, LocationManager}
import LocationManager._
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.HttpStatus._
import java.io.ByteArrayOutputStream
import android.util.Log
import GeoBook._
import android.widget.TextView
import spray.json._
import DefaultJsonProtocol._
import android.view.View._
import android.view.ViewGroup.LayoutParams
import android.view.View

/**
 * User: mcveat
 */
class BookmarkActivity extends Activity with TypedActivity with LocationListener {
  var locationManager: LocationManager = _
  var lastLocation: Location = _

  override def onCreate(b: Bundle) {
    super.onCreate(b)
    setContentView(R.layout.bookmark)

    locationManager = getSystemService(LOCATION_SERVICE).asInstanceOf[LocationManager]
    locationManager.requestLocationUpdates(NETWORK_PROVIDER, 0l, 0f, this)
    locationManager.requestLocationUpdates(GPS_PROVIDER, 0l, 0f, this)

    updateLocation(getBestLastLocation)

    val params = getWindow.getAttributes
    params.width = LayoutParams.FILL_PARENT
    getWindow.setAttributes(params)

    findView(TR.save_button).setOnClickListener { v: View =>
      finish()
    }
  }

  def getBestLastLocation: Location = {
    val TWO_MINUTES = 2 * 60 * 1000
    val network = locationManager.getLastKnownLocation(NETWORK_PROVIDER)
    val gps = locationManager.getLastKnownLocation(GPS_PROVIDER)
    if ((System.currentTimeMillis - gps.getTime) < TWO_MINUTES) gps else network
  }

  def updateLocation(l: Location) {
    lastLocation = l
    val description = findView(TR.address_description)
    if (description.getVisibility == GONE) {
      findView(TR.loading).setVisibility(GONE)
      description.setVisibility(VISIBLE)
      description.setText(lastLocation.getLatitude.toString + " " + lastLocation.getLongitude.toString)
    }
    new AddressLoadThread(l, description).start()
  }

  def onLocationChanged(newLocation: Location) {
    if (newLocation.getAccuracy < lastLocation.getAccuracy) updateLocation(newLocation)
  }
  def onStatusChanged(p1: String, p2: Int, p3: Bundle) {}
  def onProviderEnabled(p1: String) {}
  def onProviderDisabled(p1: String) {}

  override def onStop() {
    super.onStop()
    locationManager.removeUpdates(this)
  }
}

class AddressLoadThread(location: Location, view: TextView) extends Thread {
  override def run() {
    val address = getAddress
    view.post(new Runnable {
      def run() {
        address match {
          case Some(s) => view.setText(s)
          case _ => Log.d(TAG, "Error fetching address")
        }
      }
    })
  }

  private def getAddress = getGoogleMapsLocation.flatMap { response =>
    val components = response.asJson.asJsObject.getFields("results").head.convertTo[List[JsValue]]
    val streetAddressComponent =
      components.map(_.asJsObject).find(_.getFields("types").head.convertTo[List[String]].contains("street_address"))
    streetAddressComponent.map(_.getFields("formatted_address").head.convertTo[String])
  }

  private def getGoogleMapsLocation: Option[String] = {
    val response = new DefaultHttpClient().execute(new HttpGet(url(location)))
    if (response.getStatusLine.getStatusCode != SC_OK) return None
    val out = new ByteArrayOutputStream()
    response.getEntity.writeTo(out)
    out.close()
    Some(out.toString)
  }

  private def url(l: Location) = "http://maps.googleapis.com/maps/api/geocode/json?latlng=%s,%s&sensor=false"
    .format(l.getLatitude.toString, l.getLongitude.toString)
}

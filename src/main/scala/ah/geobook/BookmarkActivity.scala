package ah.geobook

import android.app.Activity
import android.os.Bundle
import android.content.Context._
import android.location.{Location, LocationListener, LocationManager}
import LocationManager._
import GeoBook._
import android.view.View._
import android.view.ViewGroup.LayoutParams
import android.view.View

/**
 * User: mcveat
 */
class BookmarkActivity extends Activity with TypedActivity with LocationListener {
  var locationManager: LocationManager = _
  var lastLocation: Option[Location] = None

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
    val description = findView(TR.address_description)
    if (description.getVisibility == GONE) {
      findView(TR.loading).setVisibility(GONE)
      description.setVisibility(VISIBLE)
      description.setText(l.getLatitude.toString + " " + l.getLongitude.toString)
    }
    new LoadAddress(l, description).start()
    if (lastLocation.map(l.distanceTo(_) > 50.0f).getOrElse(true))
//      new LoadLocalPlaces(l, findView(TR.local_places)).start()
      new Load(l).start()
    lastLocation = Some(l)
  }

  def onLocationChanged(newLocation: Location) {
    if (lastLocation.map(newLocation.getAccuracy < _.getAccuracy).getOrElse(false)) updateLocation(newLocation)
  }
  def onStatusChanged(p1: String, p2: Int, p3: Bundle) {}
  def onProviderEnabled(p1: String) {}
  def onProviderDisabled(p1: String) {}

  override def onStop() {
    super.onStop()
    locationManager.removeUpdates(this)
  }
}


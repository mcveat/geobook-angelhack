package ah.geobook

import android.app.Activity
import android.os.{Parcelable, Environment, Bundle}
import android.content.Context._
import android.location.{Location, LocationListener, LocationManager}
import LocationManager._
import GeoBook._
import android.view.View._
import android.view.ViewGroup.LayoutParams
import android.view.View
import android.content.Intent
import android.provider.MediaStore
import java.io.File
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import android.graphics.{BitmapFactory, Bitmap}
import android.net.Uri

/**
 * User: mcveat
 */
class BookmarkActivity extends Activity with TypedActivity with LocationListener {
  val IMAGE_TAKEN = 200

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

    findView(TR.photo_button).setOnClickListener { v: View =>
        getOutputMediaFile.map { file =>
          Log.d(TAG, file.getAbsoluteFile.toURI.toString)
          val i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
          i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file).asInstanceOf[Parcelable])
          startActivityForResult(i, IMAGE_TAKEN)
        }
      ()
    }
  }

  def getOutputMediaFile: Option[File] = {
    val mediaStorageDir = new File(
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Geobook")
    if (! mediaStorageDir.exists())
      if (! mediaStorageDir.mkdirs()) {
        Log.d(TAG, "failed to create directory")
        return None
      }

    val timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
    Some(new File(mediaStorageDir.getPath + File.separator + "IMG_"+ timeStamp + ".jpg"))
  }

//  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
//    if (requestCode != IMAGE_TAKEN || resultCode != Activity.RESULT_OK) return
//    Log.d(TAG, data.getData.toString)
//    findView(TR.photo_button).setVisibility(GONE)
//    val photo = findView(TR.photo_preview)
//    photo.setVisibility(VISIBLE)
//    photo.setImageBitmap(BitmapFactory.decodeFile(data.getData.toString))
//  }

  def getBestLastLocation: Location = {
    val TWO_MINUTES = 2 * 60 * 1000
    val network = locationManager.getLastKnownLocation(NETWORK_PROVIDER)
    val gps = locationManager.getLastKnownLocation(GPS_PROVIDER)
    if (gps != null && (System.currentTimeMillis - gps.getTime) < TWO_MINUTES) gps else network
  }

  def updateLocation(l: Location) {
    lastLocation = l
    val description = findView(TR.address_description)
    if (description.getVisibility == GONE) {
      findView(TR.loading).setVisibility(GONE)
      description.setVisibility(VISIBLE)
      description.setText(lastLocation.getLatitude.toString + " " + lastLocation.getLongitude.toString)
    }
    new LoadAddress(l, description).start()
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


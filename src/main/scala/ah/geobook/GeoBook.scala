package ah.geobook

import android.view.View
import View.OnClickListener

/**
 * User: mcveat
 */
object GeoBook {
  val TAG = "GeoBook"

  implicit def functionToOnClickListener(f: (View) => Unit) = new OnClickListener {
    def onClick(v: View) { f(v) }
  }
}

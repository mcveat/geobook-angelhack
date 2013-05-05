package ah.geobook

import android.app.{PendingIntent, Notification, NotificationManager, Activity}
import PendingIntent._
import _root_.android.os.Bundle
import android.content.Context._
import android.widget.RemoteViews
import android.content.Intent

/**
 * User: mcveat
 */
object MainActivity {
  val NOTIFICATION_ID = 1
}

class MainActivity extends Activity with TypedActivity {
  import MainActivity._

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    val notifications = getSystemService(NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
    notifications.notify(NOTIFICATION_ID, getNotification)

    setContentView(R.layout.main)
  }

  private def getNotification = {
    val i = new Intent(this, classOf[BookmarkActivity])
    val a = getActivity(this, 0, i, FLAG_CANCEL_CURRENT)
    val v = new RemoteViews(getPackageName, R.layout.notification)
    v.setOnClickPendingIntent(R.id.notification_bookmark_button, a)
    new Notification.Builder(this)
      .setSmallIcon(android.R.drawable.ic_menu_directions)
      .setContent(v)
      .setAutoCancel(false)
      .setOngoing(true)
      .getNotification
  }
}

package ah.locbook

import android.app.{Notification, NotificationManager, Activity}
import _root_.android.os.Bundle
import android.content.Context._
import android.widget.RemoteViews

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
    findView(TR.textview).setText("hello, world!")
  }

  private def getNotification = {
    new Notification.Builder(this)
      .setSmallIcon(android.R.drawable.ic_menu_directions)
      .setContent(new RemoteViews(getPackageName, R.layout.notification))
      .setAutoCancel(false)
      .setOngoing(true)
      .getNotification
  }
}

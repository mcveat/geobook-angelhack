package ah.geobook

import android.app.{PendingIntent, Notification, NotificationManager, Activity}
import PendingIntent._
import _root_.android.os.Bundle
import android.content.Context._
import android.widget.{Toast, RemoteViews}
import android.content.Intent
import android.view.{WindowManager, Window}
import android.webkit.{GeolocationPermissions, WebViewClient, WebView, WebChromeClient}
import android.util.Log
import GeoBook._
import android.webkit.GeolocationPermissions.Callback

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

    getWindow.requestFeature(Window.FEATURE_PROGRESS)
    this.requestWindowFeature(Window.FEATURE_NO_TITLE)

    setContentView(R.layout.main)

    val webview = findView(TR.webview)
    webview.getSettings.setJavaScriptEnabled(true)

    webview.setWebChromeClient(new WebChromeClient() {
      override def onProgressChanged(view: WebView, progress: Int) {
        MainActivity.this.setProgress(progress * 1000)
      }
      override def onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        callback.invoke(origin, true, false)
      }
    })
    webview.setWebViewClient(new WebViewClient() {
      override def onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
        Toast.makeText(MainActivity.this, "Oh no! " + description, Toast.LENGTH_SHORT).show()
      }
    })

    webview.loadUrl("http://geobookme.herokuapp.com/")
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

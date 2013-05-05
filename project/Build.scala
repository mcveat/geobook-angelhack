import sbt._

import Keys._
import AndroidKeys._

object General {
  // Create new configuration key.
  val googlePlayServices = SettingKey[File]("google-play-services-sdk", "Path to Google Play services SDK")

  val settings = Defaults.defaultSettings ++ Seq (
    name := "AngelhackLocationBookmarks",
    version := "0.1",
    versionCode := 0,
    scalaVersion := "2.9.2",
    platformName in Android := "android-14",
    libraryDependencies += "io.spray" %%  "spray-json" % "1.2.3",
    // Get path to Google Play services SDK from path to Android SDK path.
    googlePlayServices <<= (sdkPath in Android) { path =>
      path / "extras" / "google" / "google_play_services" / "libproject" / "google-play-services_lib"
    },
    // Add Google Maps library project as dependency.
    extractApkLibDependencies in Android <+= googlePlayServices map { path =>
      LibraryProject(
        pkgName = "com.google.android.gms",
        manifest = path / "AndroidManifest.xml",
        sources = Set(),
        resDir = Some(path / "res"),
        assetsDir = None
      )
    },
    // Add Google Maps JAR.
    unmanagedJars in Compile <+= googlePlayServices map { path => Attributed.blank(path / "libs" / "google-play-services.jar") },
    // Protect some classes from filtering out by ProGuard.
    proguardOption in Android ~= { _ + " -keep class * extends java.util.ListResourceBundle { protected Object[][] getContents(); } " },
//    proguardOption in Android ~= { _ + " -keep class com.google.android.gms.maps.SupportMapFragment { *; } " },
//    proguardOption in Android ~= { _ + " -keep class com.google.android.gms.maps.MapFragment { *; } " }
    proguardOption in Android ~= { _ + "-keep class com.google.android.gms.maps.** {  *; }" }
  )

  val proguardSettings = Seq (
    useProguard in Android := true
  )

  lazy val fullAndroidSettings =
    General.settings ++
    AndroidProject.androidSettings ++
    TypedResources.settings ++
    proguardSettings ++
    AndroidManifestGenerator.settings ++
    AndroidMarketPublish.settings ++ Seq (
      keyalias in Android := "change-me",
      libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test"
    )
}

object AndroidBuild extends Build {
  lazy val main = Project (
    "AngelhackLocationBookmarks",
    file("."),
    settings = General.fullAndroidSettings
  )

  lazy val tests = Project (
    "tests",
    file("tests"),
    settings = General.settings ++
               AndroidTest.androidSettings ++
               General.proguardSettings ++ Seq (
      name := "AngelhackLocationBookmarksTests"
    )
  ) dependsOn main
}

package com.myapp

import android.os.Bundle
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

class MainActivity : ReactActivity() {

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  override fun getMainComponentName(): String = "MyApp"

  /**
   * Called when the activity is starting. This is where most initialization should go.
   */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(null) // Pass null to avoid crashes
  }

  /**
   * Returns the instance of the [ReactActivityDelegate]. Here we use a util class [ DefaultReactActivityDelegate] which allows you to easily enable Fabric and Concurrent React
   * (aka React 18) with two boolean flags.
   */
  override fun createReactActivityDelegate(): ReactActivityDelegate =
      DefaultReactActivityDelegate(
          this,
          mainComponentName,
          // If you opted-in for the New Architecture, we enable the Fabric Renderer.
          fabricEnabled
      )
}

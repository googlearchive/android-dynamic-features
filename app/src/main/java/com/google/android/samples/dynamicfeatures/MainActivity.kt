/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.samples.dynamicfeatures

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus


private const val packageName = "com.google.android.samples.dynamicfeatures.ondemand"

private const val instantPackageName = "com.google.android.samples.instantdynamicfeatures"

private const val kotlinSampleClassname = "$packageName.KotlinSampleActivity"

private const val javaSampleClassname = "$packageName.JavaSampleActivity"

private const val nativeSampleClassname = "$packageName.NativeSampleActivity"

private const val instantSampleClassname = "$instantPackageName.SplitInstallInstantActivity"

/** Activity that displays buttons and handles loading of feature modules. */
class MainActivity : AppCompatActivity() {

    /** Listener used to handle changes in state for install requests. */
    private val listener = SplitInstallStateUpdatedListener { state ->
        val multiInstall = state.moduleNames().size > 1
        state.moduleNames().forEach { name ->
            // Handle changes in state.
            when (state.status()) {
                SplitInstallSessionStatus.DOWNLOADING -> {
                    //  In order to see this, the application has to be uploaded to the Play Store.
                    displayLoadingState(state, "Downloading $name")
                }
                SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                    /*
                      This may occur when attempting to download a sufficiently large module.

                      In order to see this, the application has to be uploaded to the Play Store.
                      Then features can be requested until the confirmation path is triggered.
                     */
                    startIntentSender(state.resolutionIntent()?.intentSender, null, 0, 0, 0)
                }
                SplitInstallSessionStatus.INSTALLED -> {
                    onSuccessfulLoad(name, launch = !multiInstall)
                }

                SplitInstallSessionStatus.INSTALLING -> displayLoadingState(state, "Installing $name")
                SplitInstallSessionStatus.FAILED -> {
                    toastAndLog("Error: ${state.errorCode()} for module ${state.moduleNames()}")
                }
            }
        }
    }

    private val moduleKotlin by lazy { getString(R.string.module_feature_kotlin) }
    private val moduleJava by lazy { getString(R.string.module_feature_java) }
    private val moduleNative by lazy { getString(R.string.module_native) }
    private val moduleAssets by lazy { getString(R.string.module_assets) }
    private val instantModule by lazy { getString(R.string.module_instant_feature_split_install) }
    private val instantModuleUrl by lazy { getString(R.string.instant_feature_url) }

    private val clickListener by lazy {
        View.OnClickListener {
            when (it.id) {
                R.id.btn_load_kotlin -> loadAndLaunchModule(moduleKotlin)
                R.id.btn_load_java -> loadAndLaunchModule(moduleJava)
                R.id.btn_load_assets -> loadAndLaunchModule(moduleAssets)
                R.id.btn_load_native -> loadAndLaunchModule(moduleNative)
                R.id.btn_install_all_now -> installAllFeaturesNow()
                R.id.btn_install_all_deferred -> installAllFeaturesDeferred()
                R.id.btn_request_uninstall -> requestUninstall()
                R.id.btn_instant_dynamic_feature_split_install -> loadAndLaunchModule(instantModule)
                R.id.btn_instant_dynamic_feature_url_load -> openUrl(instantModuleUrl)
            }
        }
    }

    private lateinit var manager: SplitInstallManager

    private lateinit var progress: Group

    private lateinit var buttons: Group

    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        manager = SplitInstallManagerFactory.create(this)
        initializeViews()
    }

    override fun onResume() {
        // Listener can be registered even without directly triggering a download.
        manager.registerListener(listener)
        super.onResume()
    }

    override fun onPause() {
        // Make sure to dispose of the listener once it's no longer needed.
        manager.unregisterListener(listener)
        super.onPause()
    }

    /**
     * Load a feature by module name.
     * @param name The name of the feature module to load.
     */
    private fun loadAndLaunchModule(name: String) {
        updateProgressMessage("Loading module $name")
        // Skip loading if the module already is installed. Perform success action directly.
        if (manager.installedModules.contains(name)) {
            updateProgressMessage("Already installed")
            onSuccessfulLoad(name, launch = true)
            return
        }

        // Create request to install a feature module by name.
        val request = SplitInstallRequest.newBuilder()
                .addModule(name)
                .build()

        // Load and install the requested feature module.
        manager.startInstall(request)

        updateProgressMessage("Starting install for $name")
    }

    private fun openUrl(url: String) {

        var intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage(packageName)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        startActivity(intent)
    }

    /** Display assets loaded from the assets feature module. */
    private fun displayAssets() {
        // Get the asset manager with a refreshed context, to access content of newly installed apk.
        val assetManager = createPackageContext(packageName, 0).assets
        // Now treat it like any other asset file.
        val assets = assetManager.open("assets.txt")
        val assetContent = assets.bufferedReader()
                .use {
                    it.readText()
                }

        AlertDialog.Builder(this)
                .setTitle("Asset content")
                .setMessage(assetContent)
                .show()
    }

    /** Install all features but do not launch any of them. */
    private fun installAllFeaturesNow() {
        // Request all known modules to be downloaded in a single session.
        val request = SplitInstallRequest.newBuilder()
                .addModule(moduleKotlin)
                .addModule(moduleJava)
                .addModule(moduleNative)
                .addModule(moduleAssets)
                .build()

        // Start the install with above request.
        manager.startInstall(request).addOnSuccessListener {
            toastAndLog("Loading ${request.moduleNames}")
        }
    }

    /** Install all features deferred. */
    private fun installAllFeaturesDeferred() {

        val modules = listOf(moduleKotlin, moduleJava, moduleAssets, moduleNative)

        manager.deferredInstall(modules).addOnSuccessListener {
            toastAndLog("Deferred installation of $modules")
        }
    }

    /** Request uninstall of all features. */
    private fun requestUninstall() {

        toastAndLog("Requesting uninstall of all modules." +
                "This will happen at some point in the future.")

        val installedModules = manager.installedModules.toList()
        manager.deferredUninstall(installedModules).addOnSuccessListener {
            toastAndLog("Uninstalling $installedModules")
        }
    }

    /**
     * Define what to do once a feature module is loaded successfully.
     * @param moduleName The name of the successfully loaded module.
     * @param launch `true` if the feature module should be launched, else `false`.
     */
    private fun onSuccessfulLoad(moduleName: String, launch: Boolean) {
        if (launch) {
            when (moduleName) {
                moduleKotlin -> launchActivity(kotlinSampleClassname)
                moduleJava -> launchActivity(javaSampleClassname)
                moduleNative -> launchActivity(nativeSampleClassname)
                moduleAssets -> displayAssets()
                instantModule -> launchActivity(instantSampleClassname)
            }
        }

        displayButtons()
    }

    /** Launch an activity by its class name. */
    private fun launchActivity(className: String) {
        Intent().setClassName(packageName, className)
                .also {
                    startActivity(it)
                }
    }

    /** Display a loading state to the user. */
    private fun displayLoadingState(state: SplitInstallSessionState, message: String) {
        displayProgress()

        progressBar.max = state.totalBytesToDownload().toInt()
        progressBar.progress = state.bytesDownloaded().toInt()

        updateProgressMessage(message)
    }

    /** Set up all view variables. */
    private fun initializeViews() {
        buttons = findViewById(R.id.buttons)
        progress = findViewById(R.id.progress)
        progressBar = findViewById(R.id.progress_bar)
        progressText = findViewById(R.id.progress_text)
        setupClickListener()
    }

    /** Set all click listeners required for the buttons on the UI. */
    private fun setupClickListener() {

        setClickListener(R.id.btn_load_kotlin, clickListener)
        setClickListener(R.id.btn_load_java, clickListener)
        setClickListener(R.id.btn_load_assets, clickListener)
        setClickListener(R.id.btn_load_native, clickListener)
        setClickListener(R.id.btn_install_all_now, clickListener)
        setClickListener(R.id.btn_install_all_deferred, clickListener)
        setClickListener(R.id.btn_request_uninstall, clickListener)
        setClickListener(R.id.btn_instant_dynamic_feature_split_install, clickListener)
        setClickListener(R.id.btn_instant_dynamic_feature_url_load, clickListener)
    }

    private fun setClickListener(id: Int, listener: View.OnClickListener) {
        findViewById<View>(id).setOnClickListener(listener)
    }

    private fun updateProgressMessage(message: String) {
        if (progress.visibility != View.VISIBLE) displayProgress()
        progressText.text = message
    }

    /** Display progress bar and text. */
    private fun displayProgress() {
        progress.visibility = View.VISIBLE
        buttons.visibility = View.GONE
    }

    /** Display buttons to accept user input. */
    private fun displayButtons() {
        progress.visibility = View.GONE
        buttons.visibility = View.VISIBLE
    }

}

fun MainActivity.toastAndLog(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    Log.d(TAG, text)
}

private const val TAG = "DynamicFeatures"

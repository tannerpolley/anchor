package com.itsjeel01.remotevcsmanager.ui.components

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest

/**
 * Diagnoses why JCEF might not be available and attempts multiple
 * construction paths to create a JBCefBrowser.
 *
 * Logs detailed environment info AND returns diagnostic messages
 * that can be shown in the UI so users don't have to dig through logs.
 */
object JcefDiagnostics {

    private val LOG = Logger.getInstance("JcefDiagnostics")

    data class Result(
        val browser: JBCefBrowser?,
        val diagnostics: List<String>
    )

    /**
     * Try everything to create a JBCefBrowser, collecting diagnostics.
     */
    fun createBrowser(): Result {
        val diag = mutableListOf<String>()

        diag.add("OS: ${SystemInfo.OS_NAME} ${SystemInfo.OS_VERSION} (${SystemInfo.OS_ARCH})")
        diag.add("Java: ${SystemInfo.JAVA_VERSION}")
        diag.add("EDT: ${javax.swing.SwingUtilities.isEventDispatchThread()}")


        val jcefEnabled = try {
            Registry.`is`("ide.browser.jcef.enabled")
        } catch (e: Exception) {
            null
        }
        diag.add("ide.browser.jcef.enabled = $jcefEnabled")

        if (jcefEnabled != true) {
            try {
                Registry.get("ide.browser.jcef.enabled").setValue(true)
                diag.add("→ set registry flag to true")
            } catch (e: Exception) {
                diag.add("→ could not set flag: ${e.message}")
            }
        }


        val supported = try {
            JBCefApp.isSupported()
        } catch (e: Exception) {
            diag.add("isSupported() threw: ${e.message}")
            false
        }
        diag.add("isSupported() = $supported")

        if (!supported) {

            try {
                val app = JBCefApp.getInstance()
                diag.add("getInstance() = $app")
            } catch (e: Exception) {
                diag.add("getInstance() threw: ${e.message}")
                var c: Throwable? = e.cause
                while (c != null) {
                    diag.add("  caused by: ${c.javaClass.simpleName}: ${c.message}")
                    c = c.cause
                }
            }
        }

        val browser = tryCreate(diag)
        diag.forEach { LOG.info("  $it") }
        return Result(browser, diag)
    }

    private fun tryCreate(diag: MutableList<String>): JBCefBrowser? {

        try {
            val b = JBCefBrowser()
            diag.add("SUCCESS: JBCefBrowser()")
            b.jbCefClient.addRequestHandler(requestHandler(), b.cefBrowser)
            return b
        } catch (e: Exception) {
            diag.add("JBCefBrowser() → ${e.javaClass.simpleName}: ${e.message}")
        }


        try {
            val builder = JBCefBrowser::class.java.getMethod("createBuilder")
            val builderInst = builder.invoke(null)
            val setUrl = builderInst.javaClass.getMethod("setUrl", String::class.java)
            setUrl.invoke(builderInst, "about:blank")
            val build = builderInst.javaClass.getMethod("build")
            val b = build.invoke(builderInst) as JBCefBrowser
            diag.add("SUCCESS: createBuilder().build()")
            b.jbCefClient.addRequestHandler(requestHandler(), b.cefBrowser)
            return b
        } catch (e: Exception) {
            diag.add("createBuilder() → ${e.javaClass.simpleName}: ${e.message}")
        }


        try {
            val ctor = JBCefBrowser::class.java.getConstructor(String::class.java)
            val b = ctor.newInstance("about:blank") as JBCefBrowser
            diag.add("SUCCESS: JBCefBrowser(String)")
            b.jbCefClient.addRequestHandler(requestHandler(), b.cefBrowser)
            return b
        } catch (e: Exception) {
            diag.add("JBCefBrowser(String) → ${e.javaClass.simpleName}: ${e.message}")
        }

        return null
    }

    fun requestHandler() = object : CefRequestHandlerAdapter() {
        override fun onBeforeBrowse(
            browser: CefBrowser?, frame: CefFrame?, request: CefRequest?,
            userGesture: Boolean, isRedirect: Boolean
        ): Boolean {
            val url = request?.url ?: return false
            if (url.startsWith("about:") || url.startsWith("data:") ||
                url.startsWith("#") || url.startsWith("blob:")
            ) return false
            if (url.startsWith("http://") || url.startsWith("https://")) {
                BrowserUtil.browse(url)
                return true
            }
            return false
        }
    }
}

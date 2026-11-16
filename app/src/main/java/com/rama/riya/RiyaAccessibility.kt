package com.rama.riya

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityWindowInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 1) Presses WhatsApp's Send button, only for ~20 seconds after Riya opens a prefilled chat.
 * 2) Works offline: home / back / recents / notifications / screenshot, and "close all apps".
 */
class RiyaAccessibility : AccessibilityService() {

    companion object {
        @Volatile var armedUntil = 0L
        @Volatile var instance: RiyaAccessibility? = null
        fun arm() { armedUntil = System.currentTimeMillis() + 20000 }
    }

    private val worker = Executors.newSingleThreadExecutor()

    override fun onServiceConnected() { instance = this }

    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }

    override fun onDestroy() { instance = null; worker.shutdownNow(); super.onDestroy() }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (System.currentTimeMillis() > armedUntil) return
        val pkg = event?.packageName?.toString() ?: return
        if (!pkg.startsWith("com.whatsapp")) return
        val root = rootInActiveWindow ?: return
        val ids = listOf("com.whatsapp:id/send", "com.whatsapp.w4b:id/send")
        for (id in ids) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty() && click(nodes[0])) { armedUntil = 0; return }
        }
        val byText = root.findAccessibilityNodeInfosByText("Send")
        if (byText.isNotEmpty() && click(byText[0])) armedUntil = 0
    }

    private fun click(n: AccessibilityNodeInfo): Boolean {
        var node: AccessibilityNodeInfo? = n
        while (node != null) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            node = node.parent
        }
        return false
    }

    // ---------- type by voice: works in WhatsApp, mail, notes, browsers ... ----------

    @Volatile private var lastInserted = ""
    private var visitedE = 0

    private fun firstEditable(n: AccessibilityNodeInfo?, depth: Int): AccessibilityNodeInfo? {
        if (n == null || depth > 30 || visitedE > 3000) return null
        visitedE++
        if (n.isEditable && n.isVisibleToUser) return n
        for (i in 0 until n.childCount) {
            val r = firstEditable(n.getChild(i), depth + 1)
            if (r != null) return r
        }
        return null
    }

    private fun appRoots(): List<AccessibilityNodeInfo> {
        val roots = ArrayList<AccessibilityNodeInfo>()
        rootInActiveWindow?.let { roots.add(it) }
        try {
            for (w in windows) {
                if (w.type == AccessibilityWindowInfo.TYPE_APPLICATION) w.root?.let { roots.add(it) }
            }
        } catch (e: Exception) { }
        return roots
    }

    /** The text box that has the cursor; if none has it, the first visible text box on screen (it gets the focus). */
    private fun inputNode(): AccessibilityNodeInfo? {
        val roots = appRoots()
        for (r in roots) {
            val f = r.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (f != null && f.isEditable) return f
        }
        for (r in roots) {
            visitedE = 0
            val e = firstEditable(r, 0)
            if (e != null) {
                e.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                e.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return e
            }
        }
        return null
    }

    private fun fieldText(n: AccessibilityNodeInfo): String =
        if (n.isShowingHintText) "" else (n.text?.toString() ?: "")

    /** Text left of the cursor, or null when no text box is available. */
    fun textBeforeCursor(): String? {
        val n = inputNode() ?: return null
        val t = fieldText(n)
        val sel = n.textSelectionStart
        return t.substring(0, if (sel in 0..t.length) sel else t.length)
    }

    /** Types [s] at the cursor. */
    fun insertAtCursor(s: String): Boolean {
        val n = inputNode() ?: return false
        val cur = fieldText(n)
        val sel = n.textSelectionStart.let { if (it in 0..cur.length) it else cur.length }
        val b = Bundle()
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, cur.substring(0, sel) + s + cur.substring(sel))
        if (n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)) {
            val sb = Bundle()
            sb.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, sel + s.length)
            sb.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, sel + s.length)
            n.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, sb)
            lastInserted = s
            return true
        }
        // some apps refuse set-text: paste instead
        return try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("riya", s))
            val ok = n.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            if (ok) lastInserted = s
            ok
        } catch (e: Exception) { false }
    }

    fun deleteLastInserted(): Boolean {
        val n = inputNode() ?: return false
        val cur = fieldText(n)
        val idx = if (lastInserted.isEmpty()) -1 else cur.lastIndexOf(lastInserted)
        if (idx < 0) return false
        val b = Bundle()
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, cur.removeRange(idx, idx + lastInserted.length))
        lastInserted = ""
        return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
    }

    fun clearField(): Boolean {
        val n = inputNode() ?: return false
        val b = Bundle()
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
        return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
    }

    private fun findSend(n: AccessibilityNodeInfo?, depth: Int): AccessibilityNodeInfo? {
        if (n == null || depth > 30 || visitedE > 3000) return null
        visitedE++
        val d = (n.contentDescription?.toString() ?: "").trim().lowercase()
        val t = (n.text?.toString() ?: "").trim().lowercase()
        val id = (n.viewIdResourceName ?: "").lowercase()
        if (d == "send" || t == "send" || d.startsWith("send ") || id.endsWith(":id/send") || id.endsWith("/send_button")) return n
        for (i in 0 until n.childCount) {
            val r = findSend(n.getChild(i), depth + 1)
            if (r != null) return r
        }
        return null
    }

    /** Presses the Send button of the app on screen (WhatsApp, Gmail, SMS ...). */
    fun pressSend(): Boolean {
        for (r in appRoots()) {
            visitedE = 0
            val s = findSend(r, 0)
            if (s != null && click(s)) return true
        }
        return try {
            val n = inputNode()
            n != null && android.os.Build.VERSION.SDK_INT >= 30 &&
                n.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
        } catch (e: Exception) { false }
    }

    // ---------- WhatsApp call buttons (fallback when the direct call shortcut is not available) ----------

    private fun findCallBtn(n: AccessibilityNodeInfo?, depth: Int, video: Boolean): AccessibilityNodeInfo? {
        if (n == null || depth > 30 || visitedE > 3000) return null
        visitedE++
        val d = (n.contentDescription?.toString() ?: "").trim().lowercase()
        val hit = if (video) d == "video call" else (d == "voice call" || d == "audio call" || d == "call")
        if (hit) return n
        for (i in 0 until n.childCount) {
            val r = findCallBtn(n.getChild(i), depth + 1, video)
            if (r != null) return r
        }
        return null
    }

    private fun findConfirm(n: AccessibilityNodeInfo?, depth: Int): AccessibilityNodeInfo? {
        if (n == null || depth > 30 || visitedE > 3000) return null
        visitedE++
        val t = (n.text?.toString() ?: "").trim().lowercase()
        val cls = n.className?.toString() ?: ""
        if (cls.contains("Button") && n.isClickable && (t == "call" || t == "voice call" || t == "video call" || t == "start")) return n
        for (i in 0 until n.childCount) {
            val r = findConfirm(n.getChild(i), depth + 1)
            if (r != null) return r
        }
        return null
    }

    /** In an open WhatsApp chat: taps the voice / video call button (and the confirm button if WhatsApp asks). */
    fun pressCallButton(video: Boolean): Boolean {
        var pressed = false
        for (r in appRoots()) {
            visitedE = 0
            val b = findCallBtn(r, 0, video)
            if (b != null && click(b)) { pressed = true; break }
        }
        if (!pressed) return false
        Thread.sleep(1200)
        for (r in appRoots()) {
            visitedE = 0
            val c = findConfirm(r, 0)
            if (c != null) { click(c); break }
        }
        return true
    }

    // ---------- offline phone controls ----------

    /** Opens the recent-apps screen and clears it. Blocks, so call it from a background thread. */
    fun clearRecents(): Boolean {
        performGlobalAction(GLOBAL_ACTION_HOME)
        Thread.sleep(500)
        performGlobalAction(GLOBAL_ACTION_RECENTS)
        Thread.sleep(1600)
        var cleared = false
        // 1) find the "clear all" button (text, description or id), scrolling the carousel if needed
        for (attempt in 0 until 3) {
            if (pressClearAll()) { cleared = true; break }
            swipeRight()
            Thread.sleep(600)
        }
        // 2) works on every phone: if there is no button (Realme, Oppo, Samsung ...), swipe each card up
        if (!cleared) {
            var any = false
            for (i in 0 until 14) {
                if (swipeUpCard()) any = true
                Thread.sleep(380)
            }
            cleared = any
        }
        Thread.sleep(500)
        performGlobalAction(GLOBAL_ACTION_HOME)
        return cleared
    }

    private val buttonWords = listOf(
        "clear all", "close all", "clear_all", "clearall", "close_all", "closeall", "delete all", "delete_all",
        "dismiss all", "dismiss_all", "remove all", "remove_all", "clear-all", "clear_recent", "clear all apps",
        "clean_all", "trash", "clear_button", "clear_btn"
    )

    private fun isClearButton(n: AccessibilityNodeInfo): Boolean {
        val id = (n.viewIdResourceName ?: "").lowercase()
        val txt = (n.text?.toString() ?: "").lowercase()
        val desc = (n.contentDescription?.toString() ?: "").lowercase()
        return buttonWords.any { id.contains(it) || txt.contains(it) || desc.contains(it) }
    }

    private var visited = 0

    private fun findClear(n: AccessibilityNodeInfo?, depth: Int): AccessibilityNodeInfo? {
        if (n == null || depth > 30 || visited > 3000) return null
        visited++
        if (isClearButton(n)) return n
        for (i in 0 until n.childCount) {
            val r = findClear(n.getChild(i), depth + 1)
            if (r != null) return r
        }
        return null
    }

    private fun pressClearAll(): Boolean {
        val roots = ArrayList<AccessibilityNodeInfo>()
        rootInActiveWindow?.let { roots.add(it) }
        try { for (w in windows) { w.root?.let { roots.add(it) } } } catch (e: Exception) { }
        for (root in roots) {
            visited = 0
            val hit = findClear(root, 0) ?: continue
            if (click(hit)) return true
            if (tapNode(hit)) return true      // button not "clickable" for accessibility: tap its position
        }
        return false
    }

    private fun tapNode(n: AccessibilityNodeInfo): Boolean {
        val r = android.graphics.Rect()
        n.getBoundsInScreen(r)
        if (r.isEmpty) return false
        val p = Path()
        p.moveTo(r.exactCenterX(), r.exactCenterY())
        return gesture(GestureDescription.StrokeDescription(p, 0, 60))
    }

    private fun gesture(stroke: GestureDescription.StrokeDescription): Boolean {
        val g = GestureDescription.Builder().addStroke(stroke).build()
        val latch = CountDownLatch(1)
        var done = false
        val ok = dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) { done = true; latch.countDown() }
            override fun onCancelled(gestureDescription: GestureDescription?) { latch.countDown() }
        }, null)
        if (ok) latch.await(1500, TimeUnit.MILLISECONDS)
        return ok && done
    }

    private fun swipeRight() {
        val dm = resources.displayMetrics
        val y = dm.heightPixels * 0.45f
        val p = Path()
        p.moveTo(dm.widthPixels * 0.2f, y)
        p.lineTo(dm.widthPixels * 0.85f, y)
        gesture(GestureDescription.StrokeDescription(p, 0, 250))
    }

    /** Flicks the centre recent-app card upwards (that closes it). */
    private fun swipeUpCard(): Boolean {
        val dm = resources.displayMetrics
        val x = dm.widthPixels * 0.5f
        val p = Path()
        p.moveTo(x, dm.heightPixels * 0.62f)
        p.lineTo(x, dm.heightPixels * 0.10f)
        return gesture(GestureDescription.StrokeDescription(p, 0, 160))
    }

    override fun onInterrupt() {}
}

package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.ListView
import androidx.appcompat.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R

/**
 * KFIN: restyles platform PopupMenu rows to match the status bar selection style -
 * pill selection bounds matching the row (via margins), white 80% selection fill
 * and dark text on the selected row.
 */
object PopupMenuStyler {
	private const val TEXT_FOCUSED = 0xE6000000.toInt()
	private const val TEXT_NORMAL = 0xD9FFFFFF.toInt()

	fun style(popup: PopupMenu, backgroundRes: Int = R.drawable.kfin_popup_background) {
		val helperField = PopupMenu::class.java.getDeclaredField("mPopup")
		helperField.isAccessible = true
		val helper = helperField.get(popup)
		val listView = helper.javaClass.getMethod("getListView").invoke(helper) as? ListView
			?: return
		listView.post {

			// selection fill follows the row bounds (rows get margins below)
			listView.selector = ContextCompat.getDrawable(listView.context, R.drawable.kfin_popup_item_selector)

			// shrink rows so the selector pill matches the visible item shape
			val density = listView.resources.displayMetrics.density
			val hMargin = (10 * density).toInt()
			val vMargin = (3 * density).toInt()
			for (i in 0 until listView.childCount) {
				val row = listView.getChildAt(i)
				val lp = row.layoutParams
				if (lp is ViewGroup.MarginLayoutParams) {
					lp.leftMargin = hMargin
					lp.rightMargin = hMargin
					lp.topMargin = vMargin
					lp.bottomMargin = vMargin
					row.layoutParams = lp
				}
			}

			// KFIN: status-bar style glass capsule behind the menu; the popup
			// window's own background goes fully transparent so the default
			// system plate can never peek out at the corners
			val glass = ContextCompat.getDrawable(listView.context, backgroundRes)
			listView.background = glass
			// kill the system plate on the popup window itself: PopupMenu.mPopup is
			// a MenuPopupHelper whose own mPopup is the MenuPopupWindow (a
			// ListPopupWindow) - swap its background for a transparent one so the
			// default grey shadowed plate can never peek out around our glass
			val transparent = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
			runCatching {
				val helperField = helper.javaClass.getDeclaredField("mPopup")
				helperField.isAccessible = true
				val menuPopupWindow = helperField.get(helper)
				menuPopupWindow.javaClass.getMethod(
					"setBackgroundDrawable",
					android.graphics.drawable.Drawable::class.java
				).invoke(menuPopupWindow, transparent)
			}
			// keep the items off the capsule edge
			val menuPad = (8 * listView.resources.displayMetrics.density).toInt()
			listView.clipToPadding = false
			listView.setPadding(menuPad, menuPad, menuPad, menuPad)

			// KFIN: checked boxes/radios are red, unchecked grey
			for (i in 0 until listView.childCount) {
				tintChecks(listView.getChildAt(i))
			}

			// invert row text colors with the selection, status bar style
			listView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
				override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
					updateTextColors(listView, view)
				}

				override fun onNothingSelected(parent: AdapterView<*>?) {
					updateTextColors(listView, null)
				}
			}
			listView.post { updateTextColors(listView, listView.selectedView) }
		}
	}

	private val checkTint: ColorStateList by lazy {
		ColorStateList(
			arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
			intArrayOf(0xFFFF0033.toInt(), 0x80FFFFFF.toInt())
		)
	}

	private fun tintChecks(view: View) {
		when (view) {
			is CompoundButton -> view.buttonTintList = checkTint
			is ViewGroup -> for (i in 0 until view.childCount) tintChecks(view.getChildAt(i))
		}
	}

	private fun updateTextColors(listView: ListView, focusedRow: View?) {
		for (i in 0 until listView.childCount) {
			val row = listView.getChildAt(i)
			invertRow(row, row === focusedRow)
		}
	}

	private fun invertRow(view: View, focused: Boolean) {
		when (view) {
			is TextView -> view.setTextColor(if (focused) TEXT_FOCUSED else TEXT_NORMAL)
			is ViewGroup -> for (i in 0 until view.childCount) invertRow(view.getChildAt(i), focused)
		}
	}
}

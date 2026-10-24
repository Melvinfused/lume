package com.metrolauncher.tv

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class TileContextMenuDialog(
    context: Context,
    private val appItem: AppItem,
    private val isSecondRow: Boolean = false,
    private val onPinToTopRow: (() -> Unit)? = null
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_tile_context_menu)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val imgIcon: ImageView = findViewById(R.id.menu_app_icon)
        val tvTitle: TextView = findViewById(R.id.menu_app_title)
        val tvPackage: TextView = findViewById(R.id.menu_app_package)
        val btnOpen: TextView = findViewById(R.id.menu_action_open)
        val btnPinTop: TextView = findViewById(R.id.menu_action_add_to_top)
        val btnInfo: TextView = findViewById(R.id.menu_action_info)
        val btnUninstall: TextView = findViewById(R.id.menu_action_uninstall)

        if (appItem.brandLogoResId != null) {
            imgIcon.setImageResource(appItem.brandLogoResId)
        } else {
            imgIcon.setImageDrawable(appItem.systemIcon)
        }

        tvTitle.text = appItem.label
        tvPackage.text = appItem.packageName

        if (isSecondRow) {
            btnPinTop.visibility = View.VISIBLE
        } else {
            btnPinTop.visibility = View.GONE
        }

        val focusHighlightListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.setBackgroundResource(R.drawable.bg_focus_border)
                v.animate().scaleX(1.03f).scaleY(1.03f).setDuration(100).start()
            } else {
                v.setBackgroundResource(R.drawable.bg_header_button)
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
        }

        val menuButtons = mutableListOf(btnOpen, btnInfo, btnUninstall)
        if (isSecondRow) {
            menuButtons.add(1, btnPinTop)
        }

        menuButtons.forEach {
            it.onFocusChangeListener = focusHighlightListener
        }

        btnOpen.setOnClickListener {
            dismiss()
            val intent = context.packageManager.getLaunchIntentForPackage(appItem.packageName)
            if (intent != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Cannot open ${appItem.label}", Toast.LENGTH_SHORT).show()
            }
        }

        btnPinTop.setOnClickListener {
            dismiss()
            LauncherConfig.pinAppToTopRow(context, appItem.packageName)
            onPinToTopRow?.invoke()
            Toast.makeText(context, "${appItem.label} added to Top Row", Toast.LENGTH_SHORT).show()
        }

        btnInfo.setOnClickListener {
            dismiss()
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", appItem.packageName, null)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open settings for ${appItem.label}", Toast.LENGTH_SHORT).show()
            }
        }

        btnUninstall.setOnClickListener {
            dismiss()
            try {
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.fromParts("package", appItem.packageName, null)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot uninstall ${appItem.label}", Toast.LENGTH_SHORT).show()
            }
        }

        btnOpen.requestFocus()
    }

    override fun onStart() {
        super.onStart()
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}

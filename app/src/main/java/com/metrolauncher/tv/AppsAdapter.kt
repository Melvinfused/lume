package com.metrolauncher.tv

import android.graphics.Color
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppsAdapter(
    private val apps: List<AppItem>,
    private val isTopRow: Boolean = false,
    private val onNavigateToHeader: ((Int) -> Unit)? = null,
    private val onItemClick: (AppItem) -> Unit
) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.tile_root)
        val tileBody: View = view.findViewById(R.id.tile_body)
        val logoView: ImageView = view.findViewById(R.id.img_app_logo)
        val labelView: TextView = view.findViewById(R.id.txt_app_label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tile_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]

        // 1. Transparent Dark Acrylic Background is handled by @drawable/bg_acrylic_tile_dark on root
        holder.tileBody.setBackgroundColor(Color.TRANSPARENT)

        // 2. Projectivy transparent icon or system fallback
        if (app.brandLogoResId != null) {
            holder.logoView.setImageResource(app.brandLogoResId)
        } else {
            holder.logoView.setImageDrawable(app.systemIcon)
        }

        // 3. Crisp white typography for dark acrylic surface
        holder.labelView.text = app.label
        holder.labelView.setTextColor(Color.WHITE)

        // 4. TV focus scale and elevation animation
        holder.root.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .translationZ(12f)
                    .setDuration(120)
                    .start()
            } else {
                v.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .translationZ(0f)
                    .setDuration(120)
                    .start()
            }
        }

        // 5. Explicit D-Pad navigation to Header (Wi-Fi and Settings) from Top Row
        if (isTopRow && onNavigateToHeader != null) {
            holder.root.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.action == KeyEvent.ACTION_DOWN) {
                    onNavigateToHeader.invoke(position)
                    true
                } else {
                    false
                }
            }
        } else {
            holder.root.setOnKeyListener(null)
        }

        holder.root.setOnClickListener {
            onItemClick(app)
        }
    }

    override fun getItemCount(): Int = apps.size
}

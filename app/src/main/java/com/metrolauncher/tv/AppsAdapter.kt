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
    private val onNavigateLeft: ((Int) -> Unit)? = null,
    private val onItemClick: (AppItem) -> Unit,
    private val onItemLongClick: ((AppItem) -> Unit)? = null
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

        // 1. Transparent Acrylic Background with configured color and transparency
        holder.root.background = LauncherConfig.createTileBackgroundDrawable(holder.root.context)
        holder.tileBody.setBackgroundColor(Color.TRANSPARENT)

        // 2. Vector brand logo or official system icon
        if (app.brandLogoResId != null) {
            holder.logoView.setImageResource(app.brandLogoResId)
        } else {
            holder.logoView.setImageDrawable(app.systemIcon)
        }

        // 3. Crisp themed typography for dark acrylic surface
        holder.labelView.text = app.label
        holder.labelView.setTextColor(LauncherConfig.getTilePrimaryTextColor(holder.root.context))

        // 4. TV focus: Keep tile completely stationary underneath selector overlay
        holder.root.setOnFocusChangeListener { v, _ ->
            v.scaleX = 1.0f
            v.scaleY = 1.0f
            v.translationZ = 0f
        }


        // 5. Remote control key listener: D-pad navigation, Menu button for context menu, and Left from first item
        holder.root.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_MENU) {
                    onItemLongClick?.invoke(app)
                    return@setOnKeyListener true
                }
                if (isTopRow && keyCode == KeyEvent.KEYCODE_DPAD_UP && onNavigateToHeader != null) {
                    onNavigateToHeader.invoke(position)
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && position == 0 && onNavigateLeft != null) {
                    onNavigateLeft.invoke(position)
                    return@setOnKeyListener true
                }
            }
            false
        }

        holder.root.setOnClickListener {
            onItemClick(app)
        }

        holder.root.setOnLongClickListener {
            onItemLongClick?.invoke(app)
            true
        }
    }

    override fun getItemCount(): Int = apps.size
}

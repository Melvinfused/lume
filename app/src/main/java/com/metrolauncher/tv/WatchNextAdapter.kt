package com.metrolauncher.tv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WatchNextAdapter(
    private val items: List<WatchNextItem>,
    private val onItemClick: (WatchNextItem) -> Unit
) : RecyclerView.Adapter<WatchNextAdapter.WatchNextViewHolder>() {

    class WatchNextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.cw_tile_root)
        val imgPoster: ImageView = view.findViewById(R.id.cw_img_poster)
        val txtApp: TextView = view.findViewById(R.id.cw_txt_app)
        val txtTitle: TextView = view.findViewById(R.id.cw_txt_title)
        val txtSubtitle: TextView = view.findViewById(R.id.cw_txt_subtitle)
        val progressBar: ProgressBar = view.findViewById(R.id.cw_progress_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchNextViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tile_continue_watching, parent, false)
        return WatchNextViewHolder(view)
    }

    override fun onBindViewHolder(holder: WatchNextViewHolder, position: Int) {
        val item = items[position]
        holder.root.background = LauncherConfig.createTileBackgroundDrawable(holder.root.context)

        holder.txtApp.text = item.appLabel
        holder.txtTitle.text = item.title
        holder.txtTitle.setTextColor(LauncherConfig.getTilePrimaryTextColor(holder.root.context))

        val subtitle = if (!item.episodeTitle.isNullOrEmpty()) {
            item.episodeTitle
        } else {
            item.appLabel
        }
        holder.txtSubtitle.text = subtitle
        holder.txtSubtitle.setTextColor(LauncherConfig.getTileSecondaryTextColor(holder.root.context))

        val progress = item.progressPercent
        if (progress > 0) {
            holder.progressBar.visibility = View.VISIBLE
            holder.progressBar.progress = progress
        } else {
            holder.progressBar.visibility = View.GONE
        }

        // Load poster artwork with brand logo fallback
        val artUri = item.posterArtUri
        holder.imgPoster.tag = artUri
        if (item.cachedArtwork != null) {
            holder.imgPoster.setImageBitmap(item.cachedArtwork)
            holder.imgPoster.scaleType = ImageView.ScaleType.CENTER_CROP
        } else if (!artUri.isNullOrEmpty()) {
            holder.imgPoster.setImageDrawable(null)
            WatchNextRepository.loadArtwork(holder.itemView.context, artUri) { bitmap ->
                if (holder.imgPoster.tag == artUri && bitmap != null) {
                    item.cachedArtwork = bitmap
                    holder.imgPoster.setImageBitmap(bitmap)
                    holder.imgPoster.scaleType = ImageView.ScaleType.CENTER_CROP
                }
            }
        } else {
            val logoRes = AppItem.resolveBrandLogo(item.packageName)
            if (logoRes != null) {
                holder.imgPoster.setImageResource(logoRes)
                holder.imgPoster.scaleType = ImageView.ScaleType.CENTER_INSIDE
            } else {
                try {
                    val icon = holder.itemView.context.packageManager.getApplicationIcon(item.packageName)
                    holder.imgPoster.setImageDrawable(icon)
                    holder.imgPoster.scaleType = ImageView.ScaleType.CENTER_INSIDE
                } catch (e: Exception) {
                    holder.imgPoster.setImageDrawable(null)
                }
            }
        }

        // TV focus scale and elevation animation
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

        holder.root.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}

package com.example.dakardemdikk

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.util.TimeUtils.formatDuration
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.Util

class PublicationAdapter(private val publications: List<Publication>) : RecyclerView.Adapter<PublicationAdapter.PublicationViewHolder>() {

    private val exoPlayers = mutableMapOf<Int, ExoPlayer>() // Mémoriser les joueurs par position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.mon_signalement, parent, false)
        return PublicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: PublicationViewHolder, position: Int) {
        val publication = publications[position]

        if (!publication.videoUrl.isNullOrEmpty()) {
            holder.imageView.visibility = View.GONE
            holder.videoView.visibility = View.VISIBLE
            holder.soundoff.visibility = View.VISIBLE
            holder.layoutduree.visibility = View.VISIBLE

            // Utiliser le même ExoPlayer si déjà créé pour cette position
            val player = exoPlayers[position] ?: ExoPlayer.Builder(holder.itemView.context).build().also { exoPlayers[position] = it }

            holder.videoView.player = player
            val mediaItem = MediaItem.fromUri(Uri.parse(publication.videoUrl))
            player.setMediaItem(mediaItem)
            player.prepare()

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        val durationMillis = player.duration
                        holder.duree.text = formatDuration(durationMillis)
                        updateDurationInRealTime(player, holder.duree)
                    } else if (state == Player.STATE_ENDED) {
                        holder.play.visibility = View.VISIBLE
                    }
                }
            })

            holder.play.setOnClickListener {
                player.playWhenReady = true
                player.seekTo(0)
                holder.play.visibility = View.GONE
            }

            player.playWhenReady = true
            player.volume = 0f // Démarrer en mode muet

            holder.soundoff.setOnClickListener {
                if (player.volume == 0f) {
                    player.volume = 1f
                    holder.soundofff.setImageResource(R.drawable.soundonnn)
                } else {
                    player.volume = 0f
                    holder.soundofff.setImageResource(R.drawable.sounddedoffre)
                }
            }

        } else {
            holder.imageView.visibility = View.VISIBLE
            holder.videoView.visibility = View.GONE
            holder.soundoff.visibility = View.GONE
            holder.play.visibility = View.GONE
            holder.layoutduree.visibility = View.GONE

            if (!publication.imageUrl.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(publication.imageUrl)
                    .into(holder.imageView)
            }
        }

        holder.descriptionText.text = publication.description
    }

    override fun getItemCount(): Int = publications.size

    class PublicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoView: PlayerView = itemView.findViewById(R.id.playerView)
        val imageView: ImageView = itemView.findViewById(R.id.imagedescrip)
        val soundofff: ImageView = itemView.findViewById(R.id.soundofff)
        val play: ImageView = itemView.findViewById(R.id.play)
        val descriptionText: TextView = itemView.findViewById(R.id.descrip)
        val duree: TextView = itemView.findViewById(R.id.videoDuration)
        val soundoff: RelativeLayout = itemView.findViewById(R.id.soundoff_layout)
        val layoutduree: RelativeLayout = itemView.findViewById(R.id.layoutduree)
    }

    private fun formatDuration(durationMillis: Long): String {
        val totalSeconds = (durationMillis / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateDurationInRealTime(player: ExoPlayer, durationTextView: TextView) {
        val handler = android.os.Handler()
        val runnable = object : Runnable {
            override fun run() {
                if (player.isPlaying) {
                    val currentPosition = player.currentPosition
                    val totalDuration = player.duration
                    val remainingTime = totalDuration - currentPosition
                    durationTextView.text = formatDuration(remainingTime)
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.post(runnable)
    }

    override fun onViewRecycled(holder: PublicationViewHolder) {
        super.onViewRecycled(holder)
        // Libérer le player associé à cette position
        val position = holder.adapterPosition
        exoPlayers[position]?.release()
        exoPlayers.remove(position)
    }

    fun releaseAllPlayers() {
        exoPlayers.values.forEach { it.release() }
        exoPlayers.clear()
    }
}

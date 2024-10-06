package com.example.dakardemdikk

import android.net.Uri
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import java.util.concurrent.TimeUnit

class MediaAdapter(private val mediaList: List<MediaData>) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imagedescrip)
        val playerView: PlayerView = view.findViewById(R.id.playerView)
        val descriptionTextView: TextView = view.findViewById(R.id.descrip)
        val playButton: ImageView = view.findViewById(R.id.play)
        val layoutDuration: RelativeLayout = view.findViewById(R.id.layoutduree)  // Layout parent de la durée
        val videoDuration: TextView = view.findViewById(R.id.videoDuration)  // TextView pour la durée de la vidéo
        var exoPlayer: ExoPlayer? = null
        var handler = Handler()
        var updateRunnable: Runnable? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.mon_signalement, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val publication = mediaList[position]

        if (!publication.videoUrl.isNullOrEmpty()) {
            holder.imageView.visibility = View.GONE
            holder.playerView.visibility = View.VISIBLE
            holder.playButton.visibility = View.VISIBLE
            holder.layoutDuration.visibility = View.VISIBLE  // Afficher la durée car c'est une vidéo

            // Initialiser ExoPlayer
            val player = ExoPlayer.Builder(holder.itemView.context).build()
            holder.exoPlayer = player
            holder.playerView.player = player
            val mediaItem = MediaItem.fromUri(Uri.parse(publication.videoUrl))
            player.setMediaItem(mediaItem)
            player.prepare()

            // Récupérer et afficher la durée de la vidéo après préparation
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val duration = player.duration
                        holder.videoDuration.text = formatTime(duration)

                        // Mettre à jour la durée toutes les secondes
                        startUpdatingDuration(holder, player)
                    }
                }
            })

            // Lecture vidéo au clic sur le bouton de lecture
            holder.playButton.setOnClickListener {
                player.playWhenReady = true
                player.seekTo(0)
                holder.playButton.visibility = View.GONE
            }

        } else {
            holder.imageView.visibility = View.VISIBLE
            holder.playerView.visibility = View.GONE
            holder.playButton.visibility = View.GONE
            holder.videoDuration.visibility = View.GONE  // Masquer la durée si ce n'est pas une vidéo
            holder.layoutDuration.visibility = View.GONE

            if (!publication.imageUrl.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(publication.imageUrl)
                    .into(holder.imageView)
            }
        }

        holder.descriptionTextView.text = publication.description
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    override fun onViewRecycled(holder: MediaViewHolder) {
        super.onViewRecycled(holder)
        holder.exoPlayer?.release()  // Libérer ExoPlayer lorsqu'une vue est recyclée
    }

    // Fonction pour formater la durée en minutes et secondes
    private fun formatTime(duration: Long): String {
        if (duration <= 0) return "00:00" // Si la durée est invalide, retourner 00:00
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    private fun startUpdatingDuration(holder: MediaViewHolder, player: ExoPlayer) {
        holder.updateRunnable = object : Runnable {
            override fun run() {
                // Mettre à jour le TextView avec la durée restante
                val currentPosition = player.currentPosition
                val duration = player.duration
                val remainingTime = duration - currentPosition
                holder.videoDuration.text = formatTime(remainingTime)

                // Redémarrer le Runnable pour la prochaine mise à jour
                holder.handler.postDelayed(this, 1000) // Mettre à jour chaque seconde
            }
        }

        holder.handler.post(holder.updateRunnable!!)
    }
}

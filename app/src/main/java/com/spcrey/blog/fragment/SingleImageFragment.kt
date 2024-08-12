package com.spcrey.blog.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.spcrey.blog.R

class SingleImageFragment : Fragment() {

    private lateinit var view: View

    private val imgImage by lazy {
        view.findViewById<ImageView>(R.id.img_image)
    }
    private val bgUp by lazy {
        view.findViewById<View>(R.id.bg_up)
    }
    private val bgDown by lazy {
        view.findViewById<View>(R.id.bg_down)
    }
    private val imageUrl by lazy {
        requireArguments().getString("imageUrl")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_single_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.view = view

        Glide.with(requireContext())
            .load(imageUrl)
            .into(imgImage)

        imgImage.setOnClickListener {
            activity?.finish()
        }
        bgUp.setOnClickListener {
            activity?.finish()
        }
        bgDown.setOnClickListener {
            activity?.finish()
        }
    }
}
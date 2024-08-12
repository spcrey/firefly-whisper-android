package com.spcrey.blog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.spcrey.blog.fragment.SingleImageFragment

class ImageDisplayActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HomePageFragment"
    }
    private val imageId by lazy {
        intent.getIntExtra("imageId", 0)
    }
    private val imageUrls by lazy {
        intent.getStringArrayListExtra("imageUrls")
    }
    private val textRadio by lazy {
        findViewById<TextView>(R.id.text_ratio)
    }
    private val btnDownload by lazy {
        findViewById<TextView>(R.id.btn_download)
    }
    private val viewPager by lazy {
        findViewById<ViewPager>(R.id.viewPager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_image_display)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        textRadio.text = "${imageId+1}/${imageUrls?.size}"

        val imagesPagerAdapter = imageUrls?.let {
            ImagesPagerAdapter(supportFragmentManager, it.toList())
        }

        btnDownload.setOnClickListener {
            Toast.makeText(this@ImageDisplayActivity, "该功能暂未实现", Toast.LENGTH_SHORT).show()
        }

        viewPager.adapter = imagesPagerAdapter
        viewPager.currentItem = imageId

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                textRadio.text = "${position+1}/${imageUrls?.size}"
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
    }

    class ImagesPagerAdapter(fm: FragmentManager, private val imageUrls: List<String>) : FragmentStatePagerAdapter(fm) {
        override fun getCount(): Int {
            return imageUrls.size
        }

        override fun getItem(position: Int): Fragment {
            val fragment: Fragment = SingleImageFragment()
            val args = Bundle()
            args.putString("imageUrl", imageUrls[position])
            fragment.arguments = args
            return fragment
        }
    }
}
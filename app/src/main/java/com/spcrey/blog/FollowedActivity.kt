package com.spcrey.blog

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FollowedActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "FollowedActivity"
    }

    private val icBack by lazy {
        findViewById<ImageView>(R.id.ic_back)
    }
    private val recyclerView by lazy {
        findViewById<RecyclerView>(R.id.recycler_view)
    }
    private val followedUsers: MutableList<ServerApiManager.User> = mutableListOf()
    private val followedUserAdapter by lazy {
        FollowerUserAdapter(followedUsers)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_followed)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView.layoutManager = StaggeredGridLayoutManager(
            1, StaggeredGridLayoutManager.VERTICAL
        )
        recyclerView.adapter = followedUserAdapter

        followedUserAdapter.setItemOnClickListener(object : FollowerUserAdapter.ItemOnClickListener{
            override fun onClick(userId: Int, userNickname: String, userAvatarUrl: String) {
                val intent = Intent(this@FollowedActivity, MessageListActivity::class.java)

                val existingMessageList = CachedData.multiUserMessageList.userMessageLists.find { it.withUserId  == userId }
                if (existingMessageList == null) {
                    CachedData.multiUserMessageList.userMessageLists.add(
                        ServerApiManager.UserMessageList(
                            withUserId = userId, lastMessageId = 0, messages=mutableListOf(), newMessageCount = null,
                            userNickname = userNickname, userAvatarUrl = userAvatarUrl
                        ))
                }
                intent.putExtra("withUserId", userId)
                startActivity(intent)
                finish()
            }
        })

        lifecycleScope.launch {
            CachedData.token?.let { token ->
                userListFolloweds(token)
            }
        }

        icBack.setOnClickListener {
            finish()
        }
    }

    private suspend fun userListFolloweds(token: String) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.userListFolloweds(token).await()
                when (commonData.code) {
                    1 -> {
                        withContext(Dispatchers.Main) {
                            followedUsers.addAll(commonData.data)
                            followedUserAdapter.notifyDataSetChanged()
                        }
                    } else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FollowedActivity, "参数错误", Toast.LENGTH_SHORT).show()
                    }
                }}
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FollowedActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    class FollowerUserAdapter(data: MutableList<ServerApiManager.User>):
        BaseQuickAdapter<ServerApiManager.User, BaseViewHolder>(R.layout.item_followed, data) {

        interface ItemOnClickListener {
            fun onClick(userId: Int, userNickname: String, userAvatarUrl: String)
        }
        private var itemOnClickListener: ItemOnClickListener? = null

        fun setItemOnClickListener(listener: ItemOnClickListener) {
            itemOnClickListener = listener
        }
        override fun convert(holder: BaseViewHolder, item: ServerApiManager.User) {
            if(holder.layoutPosition == 0) {
                val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
                layoutParams.topMargin = dpToPx(6)
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                layoutParams.height = dpToPx(66)
                holder.itemView.layoutParams = layoutParams
            }
            val imgUserAvatar = holder.getView<ImageView>(R.id.img_user_avatar)
            val textUserNickname = holder.getView<TextView>(R.id.text_user_nickname)
            val textUserPersonalSignature = holder.getView<TextView>(R.id.text_user_personal_signature)

            textUserNickname.text = item.nickname
            textUserPersonalSignature.text = item.personalSignature?.toString() ?: "未设置签名"

            Glide.with(context)
                .load(item.avatarUrl)
                .transform(CenterCrop(), RoundedCorners(dpToPx(4)))
                .into(imgUserAvatar)

            holder.itemView.setOnClickListener {
                itemOnClickListener?.onClick(item.id, item.nickname, item.avatarUrl)
            }
        }

        private fun dpToPx(dp: Int): Int {
            val density = Resources.getSystem().displayMetrics.density
            return (dp * density).toInt()
        }
    }
}
package com.spcrey.blog

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserInfoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "UserInfoActivity"
    }

    private val userId by lazy {
        intent.getIntExtra("userId", 0)
    }
    private val textNickname by lazy {
        findViewById<TextView>(R.id.text_user_nickname)
    }
    private val textUserPersonalSignature by lazy {
        findViewById<TextView>(R.id.text_user_personal_signature)
    }
    private val imgUserAvatar by lazy {
        findViewById<ImageView>(R.id.img_user_avatar)
    }
    private val textUserEmailContent by lazy {
        findViewById<TextView>(R.id.text_user_email_content)
    }
    private val btnChat by lazy {
        findViewById<TextView>(R.id.btn_chat)
    }
    private val btnFollow by lazy {
        findViewById<TextView>(R.id.btn_follow)
    }
    private val icBack by lazy {
        findViewById<ImageView>(R.id.ic_back)
    }
    private lateinit var userNickname: String
    private lateinit var userAvatarUrl: String

    enum class FollowStatus {
        FOLLOW, UN_FOLLOW
    }

    private var followStatus = FollowStatus.UN_FOLLOW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_info)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        btnFollow.isEnabled = false
        btnChat.isEnabled = false

        icBack.setOnClickListener {
            finish()
        }

        btnChat.setOnClickListener {
            CachedData.token?.let {
                CachedData.user?.let { user ->
                    if (userId == user.id) {
                        Toast.makeText(this@UserInfoActivity, "不能与自己聊天", Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(this@UserInfoActivity, MessageListActivity::class.java)

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
                    }
                }
            } ?: run {
                Toast.makeText(this@UserInfoActivity, "未登录", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            userInfoOther()
        }

        btnFollow.setOnClickListener {
            CachedData.token?.let { token ->
                CachedData.user?.let { user ->
                    if (userId == user.id) {
                        Toast.makeText(this@UserInfoActivity, "不能关注自己", Toast.LENGTH_SHORT).show()
                    } else {
                        when (followStatus) {
                            FollowStatus.UN_FOLLOW -> {
                                lifecycleScope.launch {
                                    btnFollow.isEnabled = false
                                    userFollow(token)
                                    btnFollow.isEnabled = true
                                }
                            }
                            FollowStatus.FOLLOW -> {
                                lifecycleScope.launch {
                                    btnFollow.isEnabled = false
                                    userUnfollow(token)
                                    btnFollow.isEnabled = true
                                }
                            }
                        }
                    }
                }
            } ?: run {
                Toast.makeText(this@UserInfoActivity, "未登录", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun userInfoOther() {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.userInfoOther(
                    CachedData.token, userId
                ).await()
                when (commonData.code) {
                    1 -> {
                        withContext(Dispatchers.Main) {
                            val user = commonData.data
                            btnChat.isEnabled = true
                            userAvatarUrl = user.avatarUrl
                            userNickname = user.nickname
                            Glide.with(this@UserInfoActivity)
                                .load(user.avatarUrl)
                                .transform(CircleCrop())
                                .into(imgUserAvatar)
                            textNickname.text = user.nickname
                            user.personalSignature?.let { personalSignature ->
                                textUserPersonalSignature.text = personalSignature
                            } ?: run {
                                textUserPersonalSignature.text = "未设置签名"
                            }

                            user.email?.let { email ->
                                textUserEmailContent.text = email
                            } ?: run {
                                textUserEmailContent.text = "未设置邮箱"
                            }

                            if (user.isFollowed == null || user.isFollowed == false) {
                                btnFollow.text = "点击关注"
                                followStatus = FollowStatus.UN_FOLLOW
                            } else {
                                btnFollow.text = "已关注"
                                followStatus = FollowStatus.FOLLOW
                            }
                            btnFollow.isEnabled = true
                        }
                    } else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UserInfoActivity, "参数错误", Toast.LENGTH_SHORT).show()
                    }
                }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserInfoActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun userFollow(token: String) {
        try {
            val commonData = ServerApiManager.apiService.userFollow(
                token,
                ServerApiManager.UserFollowForm(userId)
            ).await()
            when (commonData.code) {
                1 -> {
                    followStatus  = FollowStatus.FOLLOW
                    withContext(Dispatchers.Main) {
                        btnFollow.text = "已关注"
                    }
                } else -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserInfoActivity, "参数错误", Toast.LENGTH_SHORT).show()
                }
            }
            }
        } catch (e: Exception) {
            Log.d(TAG, "request failed: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@UserInfoActivity, "请求异常", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun userUnfollow(token: String) {
        try {
            val commonData = ServerApiManager.apiService.userUnfollow(
                token,
                ServerApiManager.UserFollowForm(userId)
            ).await()
            when (commonData.code) {
                1 -> {
                    followStatus  = FollowStatus.UN_FOLLOW
                    withContext(Dispatchers.Main) {
                        btnFollow.text = "点击关注"
                    }
                } else -> {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserInfoActivity, "参数错误", Toast.LENGTH_SHORT).show()
                }
            }
            }
        } catch (e: Exception) {
            Log.d(TAG, "request failed: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@UserInfoActivity, "请求异常", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
package com.spcrey.blog

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.spcrey.blog.fragment.MineFragment
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.MessageReceiving
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.FileInputStream
import java.util.Base64

class UserInfoModifyActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "UpdateInfoActivity"
        private const val SHARED_PREFERENCE_NAME = "user"
    }

    private val imageAvatar by lazy {
        findViewById<ImageView>(R.id.img_avatar)
    }
    private val editTextNickname by lazy {
        findViewById<TextView>(R.id.editText_nickname)
    }
    private val editTextEmail by lazy {
        findViewById<EditText>(R.id.editText_email)
    }
    private val editTextPersonalSignature by lazy {
        findViewById<EditText>(R.id.editText_personal_signature)
    }
    private val btnUpdateInfo by lazy {
        findViewById<View>(R.id.btn_update_info)
    }
    private val textUpdateInfo by lazy {
        findViewById<TextView>(R.id.text_update_info)
    }
    private val btnLogout by lazy {
        findViewById<View>(R.id.btn_logout)
    }
    private val textLogout by lazy {
        findViewById<TextView>(R.id.text_logout)
    }
    private val btnBack by lazy {
        findViewById<ImageView>(R.id.btn_back)
    }
    private val sharedPreferences by lazy {
        getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE)
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            btnUpdateInfo.isEnabled = true
            editTextNickname.removeTextChangedListener(this)
            editTextPersonalSignature.removeTextChangedListener(this)
            editTextEmail.removeTextChangedListener(this)
        }
        override fun afterTextChanged(p0: Editable?) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_info_modify)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnUpdateInfo.isEnabled = false

        btnBack.setOnClickListener {
            finish()
        }

        CachedData.user?.let { user ->
            Glide.with(this)
                .load(user.avatarUrl)
                .transform(CircleCrop())
                .into(imageAvatar)
            editTextNickname.text = user.nickname
            editTextEmail.setText(user.email)
            editTextPersonalSignature.setText(user.personalSignature)

            editTextNickname.addTextChangedListener(textWatcher)
            editTextEmail.addTextChangedListener(textWatcher)
            editTextPersonalSignature.addTextChangedListener(textWatcher)
        }

        imageAvatar.setOnClickListener {
            imageAvatar.isEnabled = false
            galleryLauncher.launch("image/*")
        }

        btnUpdateInfo.setOnClickListener {
            CachedData.token?.let { token ->
                val commonData = validateEditText()
                commonData.data?.let { userUpdateForm ->
                    lifecycleScope.launch {
                        btnUpdateInfo.isEnabled = false
                        textUpdateInfo.text = "修改中"
                        userUpdate(token, userUpdateForm)
                        textUpdateInfo.text = "修改信息"
                    }
                } ?: run {
                    Toast.makeText(
                        this@UserInfoModifyActivity,
                        commonData.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        btnLogout.setOnClickListener {
            CachedData.token?.let { token ->
                lifecycleScope.launch {
                    btnLogout.isEnabled = false
                    textLogout.text = "退出中"
                    userLogout(token)
                    btnLogout.isEnabled = true
                    textLogout.text = "退出登录"
                }
            }
        }
    }

    private val galleryLauncher = registerForActivityResult<String, Uri>(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri ->
            val localPath = getRealPath(imageUri)
            val file = localPath?.let {
                File(it)
            }
            val fileBytes = FileInputStream(file).readBytes()
            val fileBase64 = Base64.getEncoder().encodeToString(fileBytes)
            CachedData.token?.let {token ->
                lifecycleScope.launch {
                    userUpdateAvatar(token, fileBase64, imageUri)
                    imageAvatar.isEnabled = true
                }
            } ?: run {
                imageAvatar.isEnabled = true
            }
        } ?: run {
            imageAvatar.isEnabled = true
        }
    }

    private suspend fun userUpdate(token: String, userUpdateForm: ServerApiManager.UserUpdateForm) {
        withContext(Dispatchers.IO){
            try {
                val commonDataUserUpdate =
                    ServerApiManager.apiService.userUpdate(
                        token, userUpdateForm
                    ).await()
                when (commonDataUserUpdate.code) {
                    1 -> {
                        withContext(Dispatchers.Main) {

                            editTextNickname.addTextChangedListener(textWatcher)
                            editTextEmail.addTextChangedListener(textWatcher)
                            editTextPersonalSignature.addTextChangedListener(textWatcher)
                            Toast.makeText(
                                this@UserInfoModifyActivity,
                                "信息修改成功",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        EventBus.getDefault().post(MineFragment.UserInfoUpdateEvent())
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@UserInfoModifyActivity,
                                "参数错误",
                                Toast.LENGTH_SHORT
                            ).show()
                            btnUpdateInfo.isEnabled = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserInfoModifyActivity, "请求异常", Toast.LENGTH_SHORT).show()
                    btnUpdateInfo.isEnabled = true
                }
            }
        }
    }

    private suspend fun userLogout(token: String) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.userLogout(token).await()
                when (commonData.code) {
                    1 -> {
                        CachedData.token = null
                        CachedData.user = null
                        CachedData.multiUserMessageList.userMessageLists.clear()
                        CachedData.multiUserMessageList.lastMessageId = 0
                        val editor = sharedPreferences?.edit()
                        editor?.let {
                            it.remove("token")
                            it.remove("userMessageLists")
                            it.remove("lastMessageId")
                            it.apply()
                            MessageReceiving.stop()
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@UserInfoModifyActivity,
                                "退出登陆成功",
                                Toast.LENGTH_SHORT
                            ).show()
                            EventBus.getDefault().post(MineFragment.UserLogoutEvent())
                            finish()
                        }
                    }

                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@UserInfoModifyActivity,
                                "参数错误",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@UserInfoModifyActivity,
                        "请求异常",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun validateEditText(): ServerApiManager.CommonData<ServerApiManager.UserUpdateForm?> {
        val nickname = editTextNickname.text.toString()
        if (nickname.length !in 3..8) {
            return ServerApiManager.CommonData(0, "昵称需要3到8位", null)
        }
        var email: String? = editTextEmail.text.toString()
        val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        email?.let {
            if (it.isNotBlank() && !it.matches(emailPattern)) {
                return ServerApiManager.CommonData(0, "邮箱格式不正确", null)
            } else if (it.isBlank()){
                email = null
            }
        }
        val personalSignature = editTextPersonalSignature.text.toString().takeUnless {
            it.isBlank()
        }
        return ServerApiManager.CommonData(1, "输入正确", ServerApiManager.UserUpdateForm(
            nickname, email, personalSignature
        ))
    }

    private fun Context.getRealPath(uri: Uri): String? {
        var filePath: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            it.moveToFirst()
            filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
        }
        return filePath
    }

    private suspend fun userUpdateAvatar(token: String, fileBase64: String, filePath: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.userUpdateAvatar(
                        token, ServerApiManager.UserUpdateAvatarForm(fileBase64)).await()
                when(commonData.code) {
                    1 -> {
                        withContext(Dispatchers.Main) {
                            Glide.with(this@UserInfoModifyActivity)
                                .load(filePath)
                                .transform(CircleCrop())
                                .into(imageAvatar)
                            Toast.makeText(this@UserInfoModifyActivity, "头像更新成功", Toast.LENGTH_SHORT).show()
                        }
                        EventBus.getDefault().post(MineFragment.UserInfoUpdateEvent())
                    } else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@UserInfoModifyActivity, "参数错误", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserInfoModifyActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
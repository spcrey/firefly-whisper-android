package com.spcrey.blog

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.spcrey.blog.fragment.MineFragment
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.CodeTimer
import com.spcrey.blog.tools.MessageReceiving
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class LoginByCodeActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LoginByCodeActivity"
        private const val SHARED_PREFERENCE_NAME = "user"
    }

    private val sharedPreferences by lazy {
        getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE)
    }
    private val editTextPhoneNumber by lazy {
        findViewById<EditText>(R.id.editText_phone_number)
    }
    private val editTextCode by lazy {
        findViewById<EditText>(R.id.editText_code)
    }
    private val btnCodeGet by lazy {
        findViewById<TextView>(R.id.btn_code_get)
    }
    private val btnLogin by lazy {
        findViewById<View>(R.id.btn_login)
    }
    private val textLogin by lazy {
        findViewById<TextView>(R.id.text_login)
    }
    private val btnLoginByPassword by lazy {
        findViewById<TextView>(R.id.btn_login_by_password)
    }
    private val icBack by lazy {
        findViewById<ImageView>(R.id.ic_back)
    }
    private var phoneNumber: String? = null
    private var code: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_by_code)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        icBack.setOnClickListener {
            finish()
        }

        btnLoginByPassword.setOnClickListener {
            val intent = Intent(this, LoginByPasswordActivity::class.java)
            startActivity(intent)
            finish()
        }

        CodeTimer.setListener(object : CodeTimer.Listener{
            @SuppressLint("SetTextI18n")
            override suspend fun resumeTimeChange(resumeTime: Int) {
                withContext(Dispatchers.Main) {
                    btnCodeGet.text = "${resumeTime}s后可重新获取"
                    btnCodeGet.alpha = 0.5f
                    btnCodeGet.isEnabled = false
                }
            }
            override suspend fun complete() {
                withContext(Dispatchers.Main) {
                    btnCodeGet.text = getString(R.string.text_code_text)
                    btnCodeGet.alpha = 0.8f
                    btnCodeGet.isEnabled = true
                }
            }
        })

        btnCodeGet.setOnClickListener {
            phoneNumber?.let { phoneNumber ->
                CodeTimer.start()
                lifecycleScope.launch {
                    userSendSms(phoneNumber)
                }
            } ?: run {
                Toast.makeText(this, "手机号格式不正确", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogin.setOnClickListener {
            phoneNumber?.let { phoneNumber ->
                code?.let { code ->
                    lifecycleScope.launch {
                        btnLogin.isEnabled = false
                        textLogin.text = "登录中"
                        userLoginByCode(phoneNumber, code)
                        btnLogin.isEnabled = true
                        textLogin.text = "登录"
                        MessageReceiving.run()
                    }
                }?: run {
                    Toast.makeText(this, "验证码格式不正确", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "手机号格式不正确", Toast.LENGTH_SHORT).show()
            }
        }

        editTextPhoneNumber.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0 != null && p0.length == 11) {
                    phoneNumber = p0.toString()
                    code?.let {
                        btnLogin.background = ContextCompat.getDrawable(
                            this@LoginByCodeActivity, R.drawable.btn_light_blue_r8dp)
                        textLogin.alpha = 1f
                    }
                } else {
                    phoneNumber = null
                    btnLogin.background = ContextCompat.getDrawable(
                        this@LoginByCodeActivity, R.drawable.bg_deep_blue_r8dp)
                    textLogin.alpha = 0.6f
                }
            }
            override fun afterTextChanged(p0: Editable?) { }
        })

        editTextCode.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0 != null && p0.length == 6) {
                    code = p0.toString()
                    phoneNumber?.let {
                        btnLogin.background = ContextCompat.getDrawable(
                            this@LoginByCodeActivity, R.drawable.btn_light_blue_r8dp)
                        textLogin.alpha = 1f
                    }
                } else {
                    code = null
                    btnLogin.background = ContextCompat.getDrawable(
                        this@LoginByCodeActivity, R.drawable.bg_deep_blue_r8dp)
                    textLogin.alpha = 0.6f
                }
            }
            override fun afterTextChanged(p0: Editable?) { }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        CodeTimer.clearListener()
    }

    private suspend fun userSendSms(phoneNumber: String) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.userSendSms(ServerApiManager.UserSendSmsForm(phoneNumber)).await()
                when (commonData.code) {
                    1 -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginByCodeActivity, "验证码发送成功", Toast.LENGTH_SHORT).show()
                        }
                    } else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginByCodeActivity, "参数错误", Toast.LENGTH_SHORT).show()
                    }
                }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginByCodeActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun userLoginByCode(phoneNumber: String, code: String) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.userLoginByCode(ServerApiManager.UserLoginByCodeForm(
                    phoneNumber, code
                )).await()
                when(commonData.code) {
                    1 -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginByCodeActivity, "登陆成功", Toast.LENGTH_SHORT).show()
                            CachedData.token = commonData.data
                            val edit = sharedPreferences.edit()
                            edit.putString("token", commonData.data)
                            edit.apply()
                            EventBus.getDefault().post(MineFragment.UserInfoUpdateEvent())
                            MessageReceiving.current_count = MessageReceiving.max_count
                            MessageReceiving.run()
                            finish()

                        }
                    } else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LoginByCodeActivity, "验证码错误", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginByCodeActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
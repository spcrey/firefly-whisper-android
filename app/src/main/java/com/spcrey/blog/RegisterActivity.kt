package com.spcrey.blog

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RegisterActivity"
    }

    private val textPhoneInput by lazy{
        findViewById<EditText>(R.id.editText_phone_number)
    }
    private val editTextPassword by lazy{
        findViewById<EditText>(R.id.editText_password)
    }
    private val editTextRePassword by lazy{
        findViewById<EditText>(R.id.edit_text_re_password)
    }

    private val btnRegister by lazy{
        findViewById<View>(R.id.btn_register)
    }
    private val textRegister by lazy{
        findViewById<TextView>(R.id.text_register)
    }
    private val icBack by lazy {
        findViewById<ImageView>(R.id.ic_back)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        icBack.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            val commonData = validateEditText()
            commonData.data?.let { userRegisterForm ->
                lifecycleScope.launch {
                    btnRegister.isEnabled = false
                    textRegister.text = "注册中"
                    userRegister(userRegisterForm)
                    btnRegister.isEnabled = true
                    textRegister.text = "注册"
                }
            } ?: run {
                Toast.makeText(
                    this@RegisterActivity, commonData.message, Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun userRegister(userRegisterForm: ServerApiManager.UserRegisterForm) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.userRegister(userRegisterForm).await()
                when (commonData.code) {
                    1 -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@RegisterActivity,
                                "注册成功",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    } else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@RegisterActivity,
                                "该手机号已注册",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "请求异常",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun validateEditText(): ServerApiManager.CommonData<ServerApiManager.UserRegisterForm?> {
        val phoneNumber = textPhoneInput.text.toString()
        if (phoneNumber.length != 11) {
            return ServerApiManager.CommonData(0, "手机号格式不正确", null)
        }
        val password = editTextPassword.text.toString()
        val rePassword = editTextRePassword.text.toString()
        if (password != rePassword) {
            return ServerApiManager.CommonData(0, "两次密码输入不一致", null)
        }
        val passwordPattern = Regex("^(?=.*[a-zA-Z]).{8,24}$")
        if (!password.matches(passwordPattern)) {
            return ServerApiManager.CommonData(0, "密码需要8-24位，且需要包含字母", null)
        }
        return ServerApiManager.CommonData(0, "操纵失败", ServerApiManager.UserRegisterForm(
            phoneNumber, password, rePassword
        ))
    }
}
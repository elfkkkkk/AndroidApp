package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegistrationActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var spCourse: Spinner
    private lateinit var sbDifficulty: SeekBar
    private lateinit var tvDifficultyLevel: TextView
    private lateinit var etBirthDate: EditText
    private lateinit var ivZodiac: ImageView
    private lateinit var btnSubmit: Button
    private lateinit var tvResult: TextView
    private lateinit var ivResultZodiac: ImageView
    private lateinit var llResult: LinearLayout
    private lateinit var tvResultTitle: TextView
    private lateinit var btnBackToMain: Button
    private lateinit var btnBackFromForm: Button

    private var selectedZodiac: String = ""
    private var isFormValid: Boolean = false
    private lateinit var gameRepository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        initViews()
        setupGameRepository()
        setupCourseSpinner()
        setupDifficultySeekBar()
        setupBirthDateInput()
        setupFormValidation()
        setupSubmitButton()
        setupBackButtons()
    }

    private fun initViews() {
        etFullName = findViewById(R.id.etFullName)
        rgGender = findViewById(R.id.rgGender)
        spCourse = findViewById(R.id.spCourse)
        sbDifficulty = findViewById(R.id.sbDifficulty)
        tvDifficultyLevel = findViewById(R.id.tvDifficultyLevel)
        etBirthDate = findViewById(R.id.etBirthDate)
        ivZodiac = findViewById(R.id.ivZodiac)
        btnSubmit = findViewById(R.id.btnSubmit)
        tvResult = findViewById(R.id.tvResult)
        ivResultZodiac = findViewById(R.id.ivResultZodiac)
        llResult = findViewById(R.id.llResult)
        tvResultTitle = findViewById(R.id.tvResultTitle)
        btnBackToMain = findViewById(R.id.btnBackToMain)
        btnBackFromForm = findViewById(R.id.btnBackFromForm)
    }

    private fun setupGameRepository() {
        gameRepository = GameRepository(AppDatabase.getInstance(this))
    }

    private fun setupBackButtons() {
        btnBackFromForm.setOnClickListener {
            returnToMain()
        }

        btnBackToMain.setOnClickListener {
            returnToMain()
        }
    }

    private fun returnToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setupCourseSpinner() {
        val courses = listOf("Course 1", "Course 2", "Course 3", "Course 4", "Course 5", "Course 6")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, courses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCourse.adapter = adapter
    }

    private fun setupDifficultySeekBar() {
        sbDifficulty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val levels = listOf("Very Easy", "Easy", "Medium", "Hard", "Very Hard", "Expert")
                tvDifficultyLevel.text = "${levels[progress]} ($progress)"
                validateForm()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupBirthDateInput() {
        etBirthDate.addTextChangedListener(object : TextWatcher {
            private var isFormatting: Boolean = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return

                isFormatting = true

                val text = s.toString().replace("[^\\d]".toRegex(), "")
                if (text.length <= 8) {
                    val formatted = StringBuilder()
                    for (i in text.indices) {
                        if (i == 2 || i == 4) {
                            formatted.append(".")
                        }
                        formatted.append(text[i])
                    }
                    if (formatted.toString() != s.toString()) {
                        s?.replace(0, s.length, formatted.toString())
                    }
                }

                validateAndCalculateZodiac()
                validateForm()
                isFormatting = false
            }
        })

        etBirthDate.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndCalculateZodiac()
                validateForm()
            }
        }
    }

    private fun setupFormValidation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
        }

        etFullName.addTextChangedListener(textWatcher)

        rgGender.setOnCheckedChangeListener { _, _ ->
            validateForm()
        }
    }

    private fun validateForm() {
        val isNameValid = etFullName.text.toString().trim().isNotEmpty()
        val isGenderSelected = rgGender.checkedRadioButtonId != -1
        val isDateValid = selectedZodiac != "Unknown" && etBirthDate.text.toString().matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}"))

        isFormValid = isNameValid && isGenderSelected && isDateValid
        btnSubmit.isEnabled = isFormValid
    }

    private fun validateAndCalculateZodiac() {
        val dateText = etBirthDate.text.toString().trim()
        if (dateText.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}"))) {
            try {
                val parts = dateText.split(".")
                val day = parts[0].toInt()
                val month = parts[1].toInt()
                val year = parts[2].toInt()

                if (month in 1..12 && day in 1..31 && year in 1900..2023) {
                    selectedZodiac = calculateZodiacSign(month, day)
                    updateZodiacImage(ivZodiac, selectedZodiac)
                    etBirthDate.error = null
                } else {
                    etBirthDate.error = "Invalid date"
                    selectedZodiac = "Unknown"
                }
            } catch (e: Exception) {
                etBirthDate.error = "Invalid date format"
                selectedZodiac = "Unknown"
            }
        } else {
            selectedZodiac = "Unknown"
        }
    }

    private fun setupSubmitButton() {
        btnSubmit.setOnClickListener {
            if (isFormValid) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val playerId = createPlayer()
                        withContext(Dispatchers.Main) {
                            displayPlayerInfo(playerId)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegistrationActivity, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private suspend fun createPlayer(): Long {
        val fullName = etFullName.text.toString()
        val gender = if (rgGender.checkedRadioButtonId == R.id.rbMale) "Male" else "Female"
        val course = spCourse.selectedItemPosition + 1
        val difficulty = sbDifficulty.progress

        // Parse birth date
        val dateText = etBirthDate.text.toString()
        val parts = dateText.split(".")
        val day = parts[0].toInt()
        val month = parts[1].toInt() - 1
        val year = parts[2].toInt()

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        val birthDate = calendar.timeInMillis

        val playerEntity = PlayerEntity(
            fullName = fullName,
            gender = gender,
            course = course,
            difficulty = difficulty,
            birthDate = birthDate,
            zodiacSign = selectedZodiac
        )

        return gameRepository.insertPlayer(playerEntity)
    }

    private fun calculateZodiacSign(month: Int, day: Int): String {
        return when {
            (month == 1 && day >= 20) || (month == 2 && day <= 18) -> "Aquarius"
            (month == 2 && day >= 19) || (month == 3 && day <= 20) -> "Pisces"
            (month == 3 && day >= 21) || (month == 4 && day <= 19) -> "Aries"
            (month == 4 && day >= 20) || (month == 5 && day <= 20) -> "Taurus"
            (month == 5 && day >= 21) || (month == 6 && day <= 20) -> "Gemini"
            (month == 6 && day >= 21) || (month == 7 && day <= 22) -> "Cancer"
            (month == 7 && day >= 23) || (month == 8 && day <= 22) -> "Leo"
            (month == 8 && day >= 23) || (month == 9 && day <= 22) -> "Virgo"
            (month == 9 && day >= 23) || (month == 10 && day <= 22) -> "Libra"
            (month == 10 && day >= 23) || (month == 11 && day <= 21) -> "Scorpio"
            (month == 11 && day >= 22) || (month == 12 && day <= 21) -> "Sagittarius"
            (month == 12 && day >= 22) || (month == 1 && day <= 19) -> "Capricorn"
            else -> "Unknown"
        }
    }

    private fun updateZodiacImage(imageView: ImageView, zodiacSign: String) {
        val resourceId = when (zodiacSign) {
            "Aries" -> R.drawable.aries
            "Taurus" -> R.drawable.taurus
            "Gemini" -> R.drawable.gemini
            "Cancer" -> R.drawable.cancer
            "Leo" -> R.drawable.leo
            "Virgo" -> R.drawable.virgo
            "Libra" -> R.drawable.libra
            "Scorpio" -> R.drawable.scorpio
            "Sagittarius" -> R.drawable.sagittarius
            "Capricorn" -> R.drawable.capricorn
            "Aquarius" -> R.drawable.aquarius
            "Pisces" -> R.drawable.pisces
            else -> R.mipmap.ic_launcher
        }
        imageView.setImageResource(resourceId)
    }

    private suspend fun displayPlayerInfo(playerId: Long) {
        val player = gameRepository.getPlayerById(playerId)
        player?.let {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val birthDateStr = dateFormat.format(Date(player.birthDate))

            val info = """
                Full Name: ${player.fullName}
                Gender: ${player.gender}
                Course: ${player.course}
                Difficulty Level: ${player.difficulty}
                Birth Date: $birthDateStr
                Zodiac Sign: ${player.zodiacSign}
            """.trimIndent()

            withContext(Dispatchers.Main) {
                tvResult.text = info
                updateZodiacImage(ivResultZodiac, player.zodiacSign)

                tvResultTitle.visibility = TextView.VISIBLE
                llResult.visibility = LinearLayout.VISIBLE
                btnBackToMain.visibility = Button.VISIBLE

                etFullName.visibility = TextView.GONE
                rgGender.visibility = RadioGroup.GONE
                spCourse.visibility = Spinner.GONE
                sbDifficulty.visibility = SeekBar.GONE
                tvDifficultyLevel.visibility = TextView.GONE
                etBirthDate.visibility = TextView.GONE
                ivZodiac.visibility = ImageView.GONE
                btnSubmit.visibility = Button.GONE
                btnBackFromForm.visibility = Button.GONE

                Toast.makeText(this@RegistrationActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@RegistrationActivity, "Ошибка: игрок не найден", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
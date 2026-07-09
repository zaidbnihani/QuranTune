package com.example.data

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow

data class SurahItem(val number: Int, val name: String, val englishName: String)
data class QuranReciter(val identifier: String, val name: String, val englishName: String)

val builtInSurahs = listOf(
    SurahItem(-1, "أذكار الصباح", "Morning Adhkar"),
    SurahItem(-2, "أذكار المساء", "Evening Adhkar"),
    SurahItem(-3, "جزء عم", "Juz Amma"),
    SurahItem(1, "سُورَةُ ٱلْفَاتِحَةِ", "Al-Faatiha"),
    SurahItem(2, "سُورَةُ البَقَرَةِ", "Al-Baqara"),
    SurahItem(3, "سُورَةُ آلِ عِمْرَانَ", "Aal-i-Imraan"),
    SurahItem(4, "سُورَةُ النِّسَاءِ", "An-Nisaa"),
    SurahItem(5, "سُورَةُ المَائـِدَةِ", "Al-Maaida"),
    SurahItem(6, "سُورَةُ الأَنْعَامِ", "Al-An'aam"),
    SurahItem(7, "سُورَةُ الأَعْرَافِ", "Al-A'raaf"),
    SurahItem(8, "سُورَةُ الأَنْفَالِ", "Al-Anfaal"),
    SurahItem(9, "سُورَةُ التَّوْبَةِ", "At-Tawba"),
    SurahItem(10, "سُورَةُ يُونُسَ", "Yunus"),
    SurahItem(11, "سُورَةُ هُودٍ", "Hud"),
    SurahItem(12, "سُورَةُ يُوسُفَ", "Yusuf"),
    SurahItem(13, "سُورَةُ الرَّعْدِ", "Ar-Ra'd"),
    SurahItem(14, "سُورَةُ إِبْرَاهِيمَ", "Ibrahim"),
    SurahItem(15, "سُورَةُ الحِجْرِ", "Al-Hijr"),
    SurahItem(16, "سُورَةُ النَّحْلِ", "An-Nahl"),
    SurahItem(17, "سُورَةُ الإِسْرَاءِ", "Al-Israa"),
    SurahItem(18, "سُورَةُ الكَهْفِ", "Al-Kahf"),
    SurahItem(19, "سُورَةُ مَرْيَمَ", "Maryam"),
    SurahItem(20, "سُورَةُ طَهَ", "Taa-Haa"),
    SurahItem(21, "سُورَةُ الأَنْبِيَاءِ", "Al-Anbiyaa"),
    SurahItem(22, "سُورَةُ الحَجِّ", "Al-Hajj"),
    SurahItem(23, "سُورَةُ المُؤْمِنُونَ", "Al-Muminoon"),
    SurahItem(24, "سُورَةُ النُّورِ", "An-Noor"),
    SurahItem(25, "سُورَةُ الفُرْقَانِ", "Al-Furqaan"),
    SurahItem(26, "سُورَةُ الشُّعَرَاءِ", "Ash-Shu'araa"),
    SurahItem(27, "سُورَةُ النَّمْلِ", "An-Naml"),
    SurahItem(28, "سُورَةُ القَصَصِ", "Al-Qasas"),
    SurahItem(29, "سُورَةُ العَنْكَبُوتِ", "Al-Ankaboot"),
    SurahItem(30, "سُورَةُ الرُّومِ", "Ar-Room"),
    SurahItem(31, "سُورَةُ لُقْمَانَ", "Luqman"),
    SurahItem(32, "سُورَةُ السَّجْدَةِ", "As-Sajda"),
    SurahItem(33, "سُورَةُ الأَحْزَابِ", "Al-Ahzaab"),
    SurahItem(34, "سُورَةُ سَبَإٍ", "Saba"),
    SurahItem(35, "سُورَةُ فَاطِرٍ", "Faatir"),
    SurahItem(36, "سُورَةُ يسٓ", "Yaseen"),
    SurahItem(37, "سُورَةُ الصَّافَّاتِ", "As-Saaffaat"),
    SurahItem(38, "سُورَةُ صٓ", "Saad"),
    SurahItem(39, "سُورَةُ الزُّمَرِ", "Az-Zumar"),
    SurahItem(40, "سُورَةُ غَافِرٍ", "Ghafir"),
    SurahItem(41, "سُورَةُ فُصِّلَتْ", "Fussilat"),
    SurahItem(42, "سُورَةُ الشُّورَىٰ", "Ash-Shura"),
    SurahItem(43, "سُورَةُ الزُّخْرُفِ", "Az-Zukhruf"),
    SurahItem(44, "سُورَةُ الدُّخَانِ", "Ad-Dukhaan"),
    SurahItem(45, "سُورَةُ الجَاثِيَةِ", "Al-Jaathiya"),
    SurahItem(46, "سُورَةُ الأَحْقَافِ", "Al-Ahqaf"),
    SurahItem(47, "سُورَةُ مُحَمَّدٍ", "Muhammad"),
    SurahItem(48, "سُورَةُ الفَتْحِ", "Al-Fath"),
    SurahItem(49, "سُورَةُ الحُجُرَاتِ", "Al-Hujuraat"),
    SurahItem(50, "سُورَةُ قٓ", "Qaaf"),
    SurahItem(51, "سُورَةُ الذَّارِيَاتِ", "Adh-Dhaariyat"),
    SurahItem(52, "سُورَةُ الطُّورِ", "At-Tur"),
    SurahItem(53, "سُورَةُ النَّجْمِ", "An-Najm"),
    SurahItem(54, "سُورَةُ القَمَرِ", "Al-Qamar"),
    SurahItem(55, "سُورَةُ الرَّحْمَٰن", "Ar-Rahmaan"),
    SurahItem(56, "سُورَةُ الوَاقِعَةِ", "Al-Waaqia"),
    SurahItem(57, "سُورَةُ الحَدِيدِ", "Al-Hadid"),
    SurahItem(58, "سُورَةُ المُجَادِلَةِ", "Al-Mujaadila"),
    SurahItem(59, "سُورَةُ الحَشْرِ", "Al-Hashr"),
    SurahItem(60, "سُورَةُ المُمْتَحَنَةِ", "Al-Mumtahana"),
    SurahItem(61, "سُورَةُ الصَّفِّ", "As-Saff"),
    SurahItem(62, "سُورَةُ الجُمُعَةِ", "Al-Jumu'a"),
    SurahItem(63, "سُورَةُ المُنَافِقُونَ", "Al-Munaafiqoon"),
    SurahItem(64, "سُورَةُ التَّغَابُنِ", "At-Taghabun"),
    SurahItem(65, "سُورَةُ الطَّلَاقِ", "At-Talaq"),
    SurahItem(66, "سُورَةُ التَّحْرِيمِ", "At-Tahrim"),
    SurahItem(67, "سُورَةُ المُلْكِ", "Al-Mulk"),
    SurahItem(68, "سُورَةُ القَلَمِ", "Al-Qalam"),
    SurahItem(69, "سُورَةُ الحَاقَّةِ", "Al-Haaqqa"),
    SurahItem(70, "سُورَةُ المَعَارِجِ", "Al-Ma'aarij"),
    SurahItem(71, "سُورَةُ نُوحٍ", "Nooh"),
    SurahItem(72, "سُورَةُ الجِنِّ", "Al-Jinn"),
    SurahItem(73, "سُورَةُ المُزَّمِّلِ", "Al-Muzzammil"),
    SurahItem(74, "سُورَةُ المُدَّثِّرِ", "Al-Muddaththir"),
    SurahItem(75, "سُورَةُ القِيَامَةِ", "Al-Qiyaama"),
    SurahItem(76, "سُورَةُ الإِنْسَانِ", "Al-Insaan"),
    SurahItem(77, "سُورَةُ المُرْسَلَاتِ", "Al-Mursalaat"),
    SurahItem(78, "سُورَةُ النَّبَإِ", "An-Naba"),
    SurahItem(79, "سُورَةُ النَّازِعَاتِ", "An-Naaz'iaat"),
    SurahItem(80, "سُورَةُ عَبَسَ", "Abasa"),
    SurahItem(81, "سُورَةُ التَّكْوِيرِ", "At-Takwir"),
    SurahItem(82, "سُورَةُ الانْفِطَارِ", "Al-Infitaar"),
    SurahItem(83, "سُورَةُ المُطَفِّفِينَ", "Al-Mutaffifin"),
    SurahItem(84, "سُورَةُ الانْشِقَاقِ", "Al-Inshiqaaq"),
    SurahItem(85, "سُورَةُ البُرُوجِ", "Al-Burooj"),
    SurahItem(86, "سُورَةُ الطَّارِقِ", "At-Taariq"),
    SurahItem(87, "سُورَةُ الأَعْلَىٰ", "Al-A'laa"),
    SurahItem(88, "سُورَةُ الغَاشِيَةِ", "Al-Ghaashiya"),
    SurahItem(89, "سُورَةُ الفَجْرِ", "Al-Fajr"),
    SurahItem(90, "سُورَةُ البَلَدِ", "Al-Balad"),
    SurahItem(91, "سُورَةُ الشَّمْسِ", "Ash-Shams"),
    SurahItem(92, "سُورَةُ اللَّيْلِ", "Al-Lail"),
    SurahItem(93, "سُورَةُ الضُّحَىٰ", "Ad-Dhuha"),
    SurahItem(94, "سُورَةُ الشَّرْحِ", "Ash-Sharh"),
    SurahItem(95, "سُورَةُ التِّينِ", "At-Tin"),
    SurahItem(96, "سُورَةُ العَلَقِ", "Al-Alaq"),
    SurahItem(97, "سُورَةُ القَدْرِ", "Al-Qadr"),
    SurahItem(98, "سُورَةُ البَيِّنَةِ", "Al-Bayyina"),
    SurahItem(99, "سُورَةُ الزَّلْزَلَةِ", "Az-Zalzala"),
    SurahItem(100, "سُورَةُ العَادِيَاتِ", "Al-Aadiyaat"),
    SurahItem(101, "سُورَةُ القَارِعَةِ", "Al-Qaari'a"),
    SurahItem(102, "سُورَةُ التَّكَاثُرِ", "At-Takaathur"),
    SurahItem(103, "سُورَةُ العَصْرِ", "Al-Asr"),
    SurahItem(104, "سُورَةُ الهُمَزَةِ", "Al-Humaza"),
    SurahItem(105, "سُورَةُ الفِيلِ", "Al-Fil"),
    SurahItem(106, "سُورَةُ قُرَيْشٍ", "Quraish"),
    SurahItem(107, "سُورَةُ المَاعُونِ", "Al-Maa'un"),
    SurahItem(108, "سُورَةُ الكَوْثَرِ", "Al-Kawthar"),
    SurahItem(109, "سُورَةُ الكَافِرُونَ", "Al-Kaafiroon"),
    SurahItem(110, "سُورَةُ النَّصْرِ", "An-Nasr"),
    SurahItem(111, "سُورَةُ المَسَدِ", "Al-Masad"),
    SurahItem(112, "سُورَةُ الإِخْلَاصِ", "Al-Ikhlaas"),
    SurahItem(113, "سُورَةُ الفَلَقِ", "Al-Falaq"),
    SurahItem(114, "سُورَةُ النَّاسِ", "An-Naas")
)

val builtInReciters = listOf(
    QuranReciter("https://server8.mp3quran.net/afs/", "مشاري راشد العفاسي", "Mishary Rashid Alafasy"),
    QuranReciter("https://server7.mp3quran.net/basit/", "عبد الباسط عبد الصمد", "Abdul Basit (Murattal)"),
    QuranReciter("https://server11.mp3quran.net/a_jabr/", "علي جابر", "Ali Jaber"),
    QuranReciter("https://server10.mp3quran.net/minsh/", "محمد صديق المنشاوي", "Mohamed Siddiq al-Minshawi"),
    QuranReciter("https://server11.mp3quran.net/sds/", "عبد الرحمن السديس", "Abdur-Rahman as-Sudais"),
    QuranReciter("https://server13.mp3quran.net/husr/", "محمود خليل الحصري", "Mahmoud Khalil Al-Husary"),
    QuranReciter("https://server12.mp3quran.net/maher/", "ماهر المعيقلي", "Maher Al Muaiqly"),
    QuranReciter("https://server11.mp3quran.net/shatri/", "أبو بكر الشاطري", "Abu Bakr Ash-Shaatree"),
    QuranReciter("https://server10.mp3quran.net/ajm/", "أحمد بن علي العجمي", "Ahmed ibn Ali al-Ajamy"),
    QuranReciter("https://server7.mp3quran.net/s_gmd/", "سعد الغامدي", "Saad al-Ghamdi"),
    QuranReciter("https://server11.mp3quran.net/yasser/", "ياسر الدوسري", "Yasser Al-Dosari")
)

class QuranCardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: QuranCardRepository

    val uiState: StateFlow<List<QuranCard>>
    val surahsList = MutableStateFlow(builtInSurahs)
    val recitersList = MutableStateFlow(builtInReciters)

    init {
        val database = QuranDatabase.getDatabase(application)
        val dao = database.quranCardDao()
        repository = QuranCardRepository(dao)
        uiState = repository.allCards.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        fetchApiReciters()
    }

    private fun fetchApiReciters() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = URL("https://www.mp3quran.net/api/v3/reciters?language=ar").readText()
                val json = JSONObject(response)
                val arr = json.getJSONArray("reciters")
                val fetched = mutableListOf<QuranReciter>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.getString("name")
                    val moshafArr = obj.getJSONArray("moshaf")
                    if (moshafArr.length() > 0) {
                        val server = moshafArr.getJSONObject(0).getString("server")
                        fetched.add(QuranReciter(server, name, name))
                    }
                }
                if (fetched.isNotEmpty()) {
                    recitersList.value = fetched
                }
            } catch (e: Exception) {
                // Silently fallback to built-in list
            }
        }
    }

    fun addCard(title: String, clipboardText: String, imageUri: String?, presetResName: String?, reciterIdentifier: String?, notificationTriggerWord: String?) {
        viewModelScope.launch {
            val card = QuranCard(
                title = title,
                clipboardText = clipboardText,
                imageUri = imageUri,
                presetResName = presetResName,
                reciterIdentifier = reciterIdentifier,
                notificationTriggerWord = notificationTriggerWord
            )
            repository.insertCard(card)
            OfflineDownloader.download(getApplication(), reciterIdentifier, clipboardText)
        }
    }

    fun updateCard(card: QuranCard, title: String, clipboardText: String, imageUri: String?, presetResName: String?, reciterIdentifier: String?, notificationTriggerWord: String?) {
        viewModelScope.launch {
            val updated = card.copy(
                title = title,
                clipboardText = clipboardText,
                imageUri = imageUri,
                presetResName = presetResName,
                reciterIdentifier = reciterIdentifier,
                notificationTriggerWord = notificationTriggerWord
            )
            repository.updateCard(updated)
            OfflineDownloader.download(getApplication(), reciterIdentifier, clipboardText)
        }
    }

    fun deleteCard(card: QuranCard) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun playSequence(context: Context, surahNames: List<String>) {
        viewModelScope.launch {
            val allCards = uiState.value
            val cardsToPlay = mutableListOf<QuranCard>()
            
            for (surahName in surahNames) {
                // Find a card whose title or trigger word matches the requested surah robustly
                val match = allCards.firstOrNull { card ->
                    com.example.ArabicUtils.matches(card.title, surahName) ||
                    (!card.notificationTriggerWord.isNullOrBlank() && com.example.ArabicUtils.matches(card.notificationTriggerWord, surahName))
                }
                if (match != null) {
                    cardsToPlay.add(match)
                }
            }
            
            if (cardsToPlay.isNotEmpty()) {
                QuranAudioPlayer.playSequence(context, cardsToPlay)
            } else {
                Toast.makeText(context, "لم يتم العثور على بطاقات مطابقة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun playAudio(context: Context, reciterId: String?, surahNumber: String, title: String?, cardId: String? = null) {
        QuranAudioPlayer.playAudio(context, reciterId, surahNumber, title, cardId)
    }

    fun stopAudio() {
        QuranAudioPlayer.stopAudio()
    }

    override fun onCleared() {
        super.onCleared()
        QuranAudioPlayer.stopAudio()
    }

    fun moveCardUp(card: QuranCard) {
        viewModelScope.launch {
            val currentList = uiState.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == card.id }
            if (index > 0) {
                // Swap elements
                val temp = currentList[index]
                currentList[index] = currentList[index - 1]
                currentList[index - 1] = temp
                repository.updateCardOrders(currentList)
            }
        }
    }

    fun moveCardDown(card: QuranCard) {
        viewModelScope.launch {
            val currentList = uiState.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == card.id }
            if (index >= 0 && index < currentList.size - 1) {
                // Swap elements
                val temp = currentList[index]
                currentList[index] = currentList[index + 1]
                currentList[index + 1] = temp
                repository.updateCardOrders(currentList)
            }
        }
    }

    fun importCards(cards: List<QuranCard>, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val currentCards = uiState.value
                val maxSort = currentCards.maxOfOrNull { it.sortOrder } ?: -1
                val preparedCards = cards.mapIndexed { index, card ->
                    QuranCard(
                        id = 0,
                        title = card.title,
                        clipboardText = card.clipboardText,
                        imageUri = card.imageUri,
                        presetResName = card.presetResName ?: "green",
                        reciterIdentifier = card.reciterIdentifier,
                        notificationTriggerWord = card.notificationTriggerWord,
                        sortOrder = maxSort + 1 + index,
                        timestamp = System.currentTimeMillis()
                    )
                }
                repository.insertCards(preparedCards)
                onComplete(preparedCards.size)
            } catch (e: Exception) {
                onComplete(-1)
            }
        }
    }

    fun exportCardsToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val cards = uiState.value
                val jsonArray = JSONArray()
                for (card in cards) {
                    val jsonObj = JSONObject().apply {
                        put("title", card.title)
                        put("clipboardText", card.clipboardText)
                        put("imageUri", card.imageUri)
                        put("presetResName", card.presetResName)
                        put("reciterIdentifier", card.reciterIdentifier)
                        put("notificationTriggerWord", card.notificationTriggerWord)
                        put("sortOrder", card.sortOrder)
                    }
                    jsonArray.put(jsonObj)
                }
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonArray.toString(4).toByteArray())
                }
                Toast.makeText(context, "تم تصدير البطاقات بنجاح", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "فشل تصدير البطاقات: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun importCardsFromUri(context: Context, uri: Uri, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
                if (jsonString.isNullOrBlank()) {
                    onResult(-1)
                    return@launch
                }
                val jsonArray = JSONArray(jsonString)
                val importedList = mutableListOf<QuranCard>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val title = obj.optString("title", "")
                    val clipboardText = obj.optString("clipboardText", "")
                    if (title.isNotBlank() && clipboardText.isNotBlank()) {
                        val imageUri = if (obj.isNull("imageUri")) null else obj.optString("imageUri", "")
                        val presetResName = if (obj.isNull("presetResName")) null else obj.optString("presetResName", "")
                        val reciterIdentifier = if (obj.isNull("reciterIdentifier")) null else obj.optString("reciterIdentifier", "")
                        val notificationTriggerWord = if (obj.isNull("notificationTriggerWord")) null else obj.optString("notificationTriggerWord", "")
                        val sortOrder = obj.optInt("sortOrder", 0)
                        importedList.add(
                            QuranCard(
                                title = title,
                                clipboardText = clipboardText,
                                imageUri = imageUri,
                                presetResName = presetResName,
                                reciterIdentifier = reciterIdentifier,
                                notificationTriggerWord = notificationTriggerWord,
                                sortOrder = sortOrder
                            )
                        )
                    }
                }
                if (importedList.isNotEmpty()) {
                    importCards(importedList, onResult)
                } else {
                    onResult(0)
                }
            } catch (e: Exception) {
                onResult(-1)
            }
        }
    }
}

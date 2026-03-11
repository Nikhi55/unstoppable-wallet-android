package io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonicnonstandard

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic.RestoreMnemonicModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonicnonstandard.RestoreMnemonicNonStandardModule.UiState
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.IThirdKeyboard
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.hdwalletkit.WordList
import io.horizontalsystems.oxyrakit.CakeWalletStyleConverter

class RestoreMnemonicNonStandardViewModel(
    accountFactory: IAccountFactory,
    private val wordsManager: WordsManager,
    private val thirdKeyboardStorage: IThirdKeyboard,
) : ViewModelUiState<UiState>() {

    val mnemonicLanguages = Language.values().toList()

    private var passphraseEnabled: Boolean = false
    private var passphrase: String = ""
    private var passphraseError: String? = null
    private var wordItems: List<RestoreMnemonicModule.WordItem> = listOf()
    private var invalidWordItems: List<RestoreMnemonicModule.WordItem> = listOf()
    private var invalidWordRanges: List<IntRange> = listOf()
    private var error: String? = null
    private var accountType: AccountType? = null
    private var wordSuggestions: RestoreMnemonicModule.WordSuggestions? = null
    private var language = Language.English
    private var text = ""
    private var cursorPosition = 0
    private var mnemonicWordList = WordList.wordList(language)

    private val regex = Regex("\\S+")

    val defaultName = accountFactory.getNextAccountName()
    var accountName: String = defaultName
        get() = field.ifBlank { defaultName }
        private set

    val isThirdPartyKeyboardAllowed: Boolean
        get() = CoreApp.thirdKeyboardStorage.isThirdPartyKeyboardAllowed

    override fun createState() = UiState(
        passphraseEnabled = passphraseEnabled,
        passphraseError = passphraseError,
        invalidWordRanges = invalidWordRanges,
        error = error,
        accountType = accountType,
        wordSuggestions = wordSuggestions,
        language = language,
    )

    private val moneroWordSet = CakeWalletStyleConverter.MONERO_WORDLIST.toSet()

    private fun isMoneroWord(word: String): Boolean = word in moneroWordSet

    private fun processText() {
        wordItems = wordItems(text)

        // For 25-word input, validate against Monero wordlist instead of BIP39
        val isOxyraMoneroSeed = wordItems.size == 25
        invalidWordItems = if (isOxyraMoneroSeed) {
            wordItems.filter { !isMoneroWord(it.word) }
        } else {
            wordItems.filter { !mnemonicWordList.validWord(it.word, false) }
        }

        val wordItemWithCursor = wordItems.find {
            it.range.contains(cursorPosition - 1)
        }

        val invalidWordItemsExcludingCursoredPartiallyValid = when {
            wordItemWithCursor != null && (
                mnemonicWordList.validWord(wordItemWithCursor.word, true) ||
                (isOxyraMoneroSeed && isMoneroWord(wordItemWithCursor.word))
            ) -> {
                invalidWordItems.filter { it != wordItemWithCursor }
            }
            else -> invalidWordItems
        }

        invalidWordRanges = invalidWordItemsExcludingCursoredPartiallyValid.map { it.range }
        wordSuggestions = wordItemWithCursor?.let {
            RestoreMnemonicModule.WordSuggestions(it, mnemonicWordList.fetchSuggestions(it.word))
        }
    }

    fun onTogglePassphrase(enabled: Boolean) {
        passphraseEnabled = enabled
        passphrase = ""
        passphraseError = null

        emitState()
    }

    fun onEnterName(name: String) {
        accountName = name
    }

    fun onEnterPassphrase(passphrase: String) {
        this.passphrase = passphrase
        passphraseError = null

        emitState()
    }

    fun onEnterMnemonicPhrase(text: String, cursorPosition: Int) {
        error = null
        this.text = text
        this.cursorPosition = cursorPosition
        processText()

        emitState()
    }

    fun setMnemonicLanguage(language: Language) {
        this.language = language
        mnemonicWordList = WordList.wordList(language)
        processText()

        emitState()
    }

    fun onProceed() {
        val allowedWordCounts = Mnemonic.EntropyStrength.values().map { it.wordCount } + 25

        android.util.Log.e("eee", "onProceed: wordCount=${wordItems.size} invalidWords=${invalidWordItems.size} invalidWords=${invalidWordItems.map { it.word }} allowedCounts=$allowedWordCounts passphraseEnabled=$passphraseEnabled")

        when {
            invalidWordItems.isNotEmpty() -> {
                android.util.Log.e("eee", "onProceed: BLOCKED by invalidWordItems: ${invalidWordItems.map { it.word }}")
                invalidWordRanges = invalidWordItems.map { it.range }
            }
            wordItems.size !in allowedWordCounts -> {
                android.util.Log.e("eee", "onProceed: BLOCKED by wordCount=${wordItems.size} not in $allowedWordCounts")
                error = Translator.getString(R.string.Restore_Error_MnemonicWordCount, wordItems.size)
            }
            passphraseEnabled && passphrase.isBlank() -> {
                android.util.Log.e("eee", "onProceed: BLOCKED by empty passphrase")
                passphraseError = Translator.getString(R.string.Restore_Error_EmptyPassphrase)
            }
            else -> {
                try {
                    val words = wordItems.map { it.word }
                    if (words.size == 25) {
                        android.util.Log.e("eee", "onProceed: 25-word Oxyra seed, creating AccountType.Mnemonic")
                        // 25-word Oxyra/Monero seed — skip BIP39 checksum validation
                        accountType = AccountType.Mnemonic(words, passphrase)
                        error = null
                    } else {
                        android.util.Log.e("eee", "onProceed: BIP39 seed (${words.size} words), validating checksum")
                        wordsManager.validateChecksum(words)
                        accountType = AccountType.Mnemonic(words, passphrase)
                        error = null
                    }
                    android.util.Log.e("eee", "onProceed: SUCCESS accountType=$accountType")
                } catch (checksumException: Exception) {
                    android.util.Log.e("eee", "onProceed: EXCEPTION: ${checksumException.message}", checksumException)
                    error = Translator.getString(R.string.Restore_InvalidChecksum)
                }
            }
        }

        emitState()
    }

    fun onSelectCoinsShown() {
        accountType = null

        emitState()
    }

    fun onAllowThirdPartyKeyboard() {
        thirdKeyboardStorage.isThirdPartyKeyboardAllowed = true
    }

    private fun wordItems(text: String): List<RestoreMnemonicModule.WordItem> {
        return regex.findAll(text.lowercase())
            .map { RestoreMnemonicModule.WordItem(it.value, it.range) }
            .toList()
    }
}

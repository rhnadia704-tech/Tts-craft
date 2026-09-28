package com.example.data.model

data class Voice(
    val id: String,
    val name: String,
    val locale: String,
    val languageDisplayName: String,
    val gender: VoiceGender,
    val flagEmoji: String,
    val description: String,
    val isPopular: Boolean = false
)

enum class VoiceGender {
    FEMALE, MALE, NEUTRAL
}

object VoiceCatalog {
    val ALL_VOICES: List<Voice> = listOf(
        // Français
        Voice(
            id = "fr-FR-DeniseNeural",
            name = "Denise",
            locale = "fr-FR",
            languageDisplayName = "Français (France)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇫🇷",
            description = "Voix naturelle, fluide et chaleureuse",
            isPopular = true
        ),
        Voice(
            id = "fr-FR-HenriNeural",
            name = "Henri",
            locale = "fr-FR",
            languageDisplayName = "Français (France)",
            gender = VoiceGender.MALE,
            flagEmoji = "🇫🇷",
            description = "Voix posée, claire et informative",
            isPopular = true
        ),
        Voice(
            id = "fr-FR-EloiseNeural",
            name = "Éloïse",
            locale = "fr-FR",
            languageDisplayName = "Français (France)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇫🇷",
            description = "Voix expressive et dynamique",
            isPopular = true
        ),
        Voice(
            id = "fr-FR-SylvieNeural",
            name = "Sylvie",
            locale = "fr-FR",
            languageDisplayName = "Français (France)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇫🇷",
            description = "Voix douce et posée pour narration"
        ),
        Voice(
            id = "fr-FR-RemyMultilingualNeural",
            name = "Rémy",
            locale = "fr-FR",
            languageDisplayName = "Français (France)",
            gender = VoiceGender.MALE,
            flagEmoji = "🇫🇷",
            description = "Voix multilingue nouvelle génération"
        ),
        Voice(
            id = "fr-FR-VivienneMultilingualNeural",
            name = "Vivienne",
            locale = "fr-FR",
            languageDisplayName = "Français (France)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇫🇷",
            description = "Voix multilingue moderne et expressive"
        ),
        Voice(
            id = "fr-CA-AntoineNeural",
            name = "Antoine",
            locale = "fr-CA",
            languageDisplayName = "Français (Canada)",
            gender = VoiceGender.MALE,
            flagEmoji = "🇨🇦",
            description = "Accent québécois naturel"
        ),
        Voice(
            id = "fr-CA-SylvieNeural",
            name = "Sylvie (CA)",
            locale = "fr-CA",
            languageDisplayName = "Français (Canada)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇨🇦",
            description = "Voix féminine canadienne"
        ),
        Voice(
            id = "fr-BE-CharlineNeural",
            name = "Charline",
            locale = "fr-BE",
            languageDisplayName = "Français (Belgique)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇧🇪",
            description = "Voix belge naturelle et claire"
        ),
        Voice(
            id = "fr-CH-ArianeNeural",
            name = "Ariane",
            locale = "fr-CH",
            languageDisplayName = "Français (Suisse)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇨🇭",
            description = "Voix suisse douce et articulée"
        ),

        // Anglais
        Voice(
            id = "en-US-JennyNeural",
            name = "Jenny",
            locale = "en-US",
            languageDisplayName = "English (US)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇺🇸",
            description = "Friendly, dynamic and engaging",
            isPopular = true
        ),
        Voice(
            id = "en-US-GuyNeural",
            name = "Guy",
            locale = "en-US",
            languageDisplayName = "English (US)",
            gender = VoiceGender.MALE,
            flagEmoji = "🇺🇸",
            description = "Authentic, casual and clear",
            isPopular = true
        ),
        Voice(
            id = "en-US-AriaNeural",
            name = "Aria",
            locale = "en-US",
            languageDisplayName = "English (US)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇺🇸",
            description = "Warm, versatile and expressive"
        ),
        Voice(
            id = "en-US-ChristopherNeural",
            name = "Christopher",
            locale = "en-US",
            languageDisplayName = "English (US)",
            gender = VoiceGender.MALE,
            flagEmoji = "🇺🇸",
            description = "Deep narrative and audiobook style"
        ),
        Voice(
            id = "en-GB-SoniaNeural",
            name = "Sonia",
            locale = "en-GB",
            languageDisplayName = "English (UK)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇬🇧",
            description = "Refined British broadcast voice"
        ),
        Voice(
            id = "en-GB-RyanNeural",
            name = "Ryan",
            locale = "en-GB",
            languageDisplayName = "English (UK)",
            gender = VoiceGender.MALE,
            flagEmoji = "🇬🇧",
            description = "Warm and calm British voice"
        ),

        // Espagnol
        Voice(
            id = "es-ES-ElviraNeural",
            name = "Elvira",
            locale = "es-ES",
            languageDisplayName = "Español (España)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇪🇸",
            description = "Voz castellana fluida y clara",
            isPopular = true
        ),
        Voice(
            id = "es-ES-AlvaroNeural",
            name = "Álvaro",
            locale = "es-ES",
            languageDisplayName = "Español (España)",
            gender = VoiceGender.MALE,
            flagEmoji = "🇪🇸",
            description = "Voz masculina energética"
        ),
        Voice(
            id = "es-MX-DaliaNeural",
            name = "Dalia",
            locale = "es-MX",
            languageDisplayName = "Español (México)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇲🇽",
            description = "Voz mexicana amigable"
        ),

        // Allemand
        Voice(
            id = "de-DE-KatjaNeural",
            name = "Katja",
            locale = "de-DE",
            languageDisplayName = "Deutsch (Deutschland)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇩🇪",
            description = "Präzise und ausdrucksstarke deutsche Stimme"
        ),
        Voice(
            id = "de-DE-ConradNeural",
            name = "Conrad",
            locale = "de-DE",
            languageDisplayName = "Deutsch (Deutschland)",
            gender = VoiceGender.MALE,
            flagEmoji = "🇩🇪",
            description = "Tiefe, professionelle Erzählstimme"
        ),

        // Italien
        Voice(
            id = "it-IT-ElsaNeural",
            name = "Elsa",
            locale = "it-IT",
            languageDisplayName = "Italiano (Italia)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇮🇹",
            description = "Voce italiana melodiosa e calda"
        ),
        Voice(
            id = "it-IT-DiegoNeural",
            name = "Diego",
            locale = "it-IT",
            languageDisplayName = "Italiano (Italia)",
            gender = VoiceGender.MALE,
            flagEmoji = "🇮🇹",
            description = "Voce brillante e naturale"
        ),

        // Arabe
        Voice(
            id = "ar-EG-SalmaNeural",
            name = "Salma",
            locale = "ar-EG",
            languageDisplayName = "العربية (مصر)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇪🇬",
            description = "صوت عربي فصيح وواضح"
        ),
        Voice(
            id = "ar-SA-HamedNeural",
            name = "Hamed",
            locale = "ar-SA",
            languageDisplayName = "العربية (السعودية)",
            gender = VoiceGender.MALE,
            flagEmoji = "🇸🇦",
            description = "صوت رخيم وواضح"
        ),

        // Portugais
        Voice(
            id = "pt-BR-FranciscaNeural",
            name = "Francisca",
            locale = "pt-BR",
            languageDisplayName = "Português (Brasil)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇧🇷",
            description = "Voz brasileira suave e expressiva"
        ),

        // Japonais
        Voice(
            id = "ja-JP-NanamiNeural",
            name = "Nanami",
            locale = "ja-JP",
            languageDisplayName = "日本語 (日本)",
            gender = VoiceGender.FEMALE,
            flagEmoji = "🇯🇵",
            description = "自然で親しみやすい日本語音声"
        )
    )

    fun getDefaultVoice(): Voice = ALL_VOICES.first()

    fun findById(id: String): Voice {
        return ALL_VOICES.find { it.id == id } ?: getDefaultVoice()
    }
}

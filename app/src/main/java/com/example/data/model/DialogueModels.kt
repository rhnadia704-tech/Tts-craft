package com.example.data.model

data class DialogueParticipant(
    val id: String,
    val name: String,
    val voice: Voice,
    val speed: Float = 1.0f,
    val pitch: Float = 0.0f,
    val colorHex: Long = 0xFF00E5FF
)

data class DialogueLineItem(
    val id: String,
    val participantId: String,
    val text: String,
    val pauseAfterMs: Int = 350
)

data class DialoguePreset(
    val title: String,
    val description: String,
    val participants: List<DialogueParticipant>,
    val lines: List<DialogueLineItem>
)

object DialoguePresets {
    fun getPresets(): List<DialoguePreset> {
        val p1 = DialogueParticipant(
            id = "part_1",
            name = "Alice",
            voice = VoiceCatalog.findById("fr-FR-DeniseNeural"),
            colorHex = 0xFF00E5FF
        )
        val p2 = DialogueParticipant(
            id = "part_2",
            name = "Thomas",
            voice = VoiceCatalog.findById("fr-FR-HenriNeural"),
            colorHex = 0xFFFFB300
        )
        val p3 = DialogueParticipant(
            id = "part_3",
            name = "Éloïse",
            voice = VoiceCatalog.findById("fr-FR-EloiseNeural"),
            colorHex = 0xFFB388FF
        )

        return listOf(
            DialoguePreset(
                title = "🎙️ Débat Tech & IA",
                description = "Discussion animée entre Alice et Thomas sur la synthèse vocale moderne.",
                participants = listOf(p1, p2),
                lines = listOf(
                    DialogueLineItem("l1", "part_1", "Bonjour Thomas ! As-tu écouté les dernières voix neuronales de Microsoft Edge ?"),
                    DialogueLineItem("l2", "part_2", "Salut Alice ! Oui, c'est saisissant, l'intonation est tellement naturelle qu'on jurerait entendre un humain."),
                    DialogueLineItem("l3", "part_1", "Exactement, et avec les dialogues multi-voix, on peut créer de véritables fictions audio en quelques secondes !"),
                    DialogueLineItem("l4", "part_2", "C'est l'avenir du podcast et de l'accessibilité sur mobile.")
                )
            ),
            DialoguePreset(
                title = "☕ Rencontre au Café",
                description = "Échange convivial dans un café parisien.",
                participants = listOf(p1, p2),
                lines = listOf(
                    DialogueLineItem("l1", "part_1", "Un double expresso pour moi s'il vous plaît, et toi Thomas ?"),
                    DialogueLineItem("l2", "part_2", "Je vais prendre un cappuccino bien chaud. Il fait plutôt frais dehors ce matin."),
                    DialogueLineItem("l3", "part_1", "C'est vrai, l'automne arrive à grands pas. Prends une table près de la fenêtre !")
                )
            ),
            DialoguePreset(
                title = "🎭 Scène de Théâtre à 3",
                description = "Dialogue scénarisé avec trois personnages distincts.",
                participants = listOf(p1, p2, p3),
                lines = listOf(
                    DialogueLineItem("l1", "part_3", "Silence dans la salle ! Qui a laissé cette porte déverrouillée ?"),
                    DialogueLineItem("l2", "part_1", "Ce n'est pas moi Éloïse, je viens à peine d'arriver par le corridor est."),
                    DialogueLineItem("l3", "part_2", "Je plaide coupable... Le vent a soufflé plus fort que prévu."),
                    DialogueLineItem("l4", "part_3", "Alors hâtons-nous, le spectacle commence dans cinq minutes !")
                )
            )
        )
    }
}

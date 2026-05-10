package id.ac.pnm.quizbattleapp.domain.model

enum class GameMode(val label: String, val description: String, val icon: String) {
    ONLINE_BATTLE ("1v1 Online Battle", "Tantang pemain lewat internet",  "🌐"),
    SOLO_TRAINING ("Solo Training",     "Latihan mandiri tanpa lawan",    "📚")
}

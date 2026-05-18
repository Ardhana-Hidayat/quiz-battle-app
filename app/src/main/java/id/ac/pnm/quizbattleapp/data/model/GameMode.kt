package id.ac.pnm.quizbattleapp.data.model

enum class GameMode(val label: String, val description: String, val icon: String) {
    ONLINE_BATTLE ("1v1 Online Battle", "Tantang pemain lewat internet",  "ðŸŒ"),
    SOLO_TRAINING ("Solo Training",     "Latihan mandiri tanpa lawan",    "ðŸ“š")
}

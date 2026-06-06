package id.ac.pnm.quizbattleapp.data.model

enum class GameMode(val label: String, val description: String) {
    ONLINE_BATTLE("1v1 Online Battle", "Tantang pemain lewat internet"),
    SOLO_TRAINING("Solo Training",     "Latihan mandiri tanpa lawan"),
    LEADERBOARD  ("Leaderboard",       "Lihat peringkat pemain terbaik"),
    HISTORY      ("Riwayat Battle",    "Lihat hasil battle online kamu")
}
package id.ac.pnm.quizbattleapp.data.model

data class RoomPlayer(
    val uid: String         = "",
    val displayName: String = "",
    val isReady: Boolean    = false
)

data class GameRoom(
    val roomId: String      = "",
    val status: String      = RoomStatus.WAITING,
    val createdAt: Long     = 0L,
    val player1: RoomPlayer = RoomPlayer(),
    val player2: RoomPlayer = RoomPlayer(),
    val questions: List<Question> = emptyList()
) {
    val isFull: Boolean get() = player2.uid.isNotBlank()
    val bothReady: Boolean get() = isFull
}

object RoomStatus {
    const val WAITING  = "waiting"
    const val READY    = "ready"
    const val PLAYING  = "playing"
    const val FINISHED = "finished"
}
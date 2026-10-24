package im.miaoli.data

data class Post(
    val id: String,
    /** Server timestamp in yyyyMMddHHmmss form. */
    val time: String,
    val author: String,
    /** Message body, possibly containing HTML markup. */
    val message: String,
    val tribune: Tribune,
) {
    val key: String
        get() = "${tribune.id}:$id"

    val clock: String
        get() = if (time.length >= 14) {
            "${time.substring(8, 10)}:${time.substring(10, 12)}:${time.substring(12, 14)}"
        } else {
            time
        }
}

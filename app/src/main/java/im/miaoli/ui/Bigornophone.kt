package im.miaoli.ui

/** The suffix used to address a login on a tribune. */
internal const val BIGORNOPHONE_SUFFIX = "<"

/** Returns true when [message] contains the exact `login<` sequence. */
internal fun isBigornophone(message: String, login: String): Boolean {
    val configuredLogin = login.trim()
    if (configuredLogin.isEmpty()) return false
    val token = configuredLogin + BIGORNOPHONE_SUFFIX
    return parseMessage(message).text.contains(token)
}
